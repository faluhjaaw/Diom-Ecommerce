"""
Consumer Kafka — topic : user.product_viewed
Payload : { userEmail, productId, subCategoryId, timestamp }
"""
import json
import logging
from datetime import datetime, timezone

from aiokafka import AIOKafkaConsumer

from config import settings
from db import get_mongo_db
from models.embedder import embed_and_index_product_async, _to_qdrant_id
from db import get_qdrant

logger = logging.getLogger(__name__)


async def _ensure_product_indexed(product_id: str):
    """Indexe le produit dans Qdrant s'il n'y est pas encore (async-safe)."""
    import asyncio, httpx
    loop = asyncio.get_event_loop()
    existing = await loop.run_in_executor(
        None,
        lambda: get_qdrant().retrieve(
            collection_name=settings.qdrant_collection,
            ids=[_to_qdrant_id(product_id)],
        ),
    )
    if existing:
        return
    async with httpx.AsyncClient(timeout=10) as client:
        try:
            resp = await client.get(f"{settings.product_service_url}/api/products/{product_id}")
            if resp.status_code == 200:
                await embed_and_index_product_async(resp.json())
        except Exception as exc:
            logger.warning("Impossible d'indexer le produit %s : %s", product_id, exc)


async def consume_product_viewed():
    consumer = AIOKafkaConsumer(
        "user.product_viewed",
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        auto_offset_reset="earliest",
    )
    await consumer.start()
    logger.info("Consumer user.product_viewed démarré.")
    try:
        async for msg in consumer:
            payload = msg.value
            # P0 #2 — identifiant canonique : userEmail dans tous les cas
            user_id = payload.get("userEmail", "")
            product_id = payload.get("productId", "")
            sub_category_id = payload.get("subCategoryId", "")
            timestamp = payload.get("timestamp", datetime.now(timezone.utc).isoformat())

            if not user_id or not product_id:
                continue

            now = datetime.now(timezone.utc)
            db = get_mongo_db()
            await db["interactions"].insert_one({
                "user_id": user_id,
                "product_id": product_id,
                "sub_category_id": sub_category_id,
                "event_type": "product_viewed",
                "timestamp": timestamp,
                "created_at": now,   # datetime natif → TTL MongoDB
            })

            # Indexation async-safe (ne bloque plus la boucle)
            await _ensure_product_indexed(product_id)
            logger.debug("product_viewed enregistré — user=%s, product=%s", user_id, product_id)
    finally:
        await consumer.stop()
