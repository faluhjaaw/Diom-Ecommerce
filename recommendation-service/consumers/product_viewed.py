"""
Consumer Kafka — topic : user.product_viewed
Payload : { userEmail, productId, subCategoryId, timestamp }
"""
import asyncio
import json
import logging
from datetime import datetime, timedelta, timezone

import httpx
from aiokafka import AIOKafkaConsumer

from config import settings
from db import get_mongo_db, get_qdrant
from models.embedder import embed_and_index_product_async, _to_qdrant_id

logger = logging.getLogger(__name__)

# Client HTTP réutilisé pour toute la durée du consumer (connection pooling)
_http_client: httpx.AsyncClient | None = None


def _get_http_client() -> httpx.AsyncClient:
    global _http_client
    if _http_client is None or _http_client.is_closed:
        _http_client = httpx.AsyncClient(timeout=10)
    return _http_client


async def _ensure_product_indexed(product_id: str):
    """Indexe le produit dans Qdrant s'il n'y est pas encore (async-safe)."""
    loop = asyncio.get_running_loop()
    existing = await loop.run_in_executor(
        None,
        lambda: get_qdrant().retrieve(
            collection_name=settings.qdrant_collection,
            ids=[_to_qdrant_id(product_id)],
        ),
    )
    if existing:
        return
    try:
        resp = await _get_http_client().get(
            f"{settings.product_service_url}/api/products/{product_id}"
        )
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
            user_id = payload.get("userEmail", "")
            product_id = payload.get("productId", "")
            sub_category_id = payload.get("subCategoryId", "")
            timestamp = payload.get("timestamp", datetime.now(timezone.utc).isoformat())

            if not user_id or not product_id:
                continue

            now = datetime.now(timezone.utc)
            db = get_mongo_db()

            # Déduplication : ignorer si même user+produit vu dans la dernière heure
            recent_cutoff = now - timedelta(hours=1)
            already_seen = await db["interactions"].find_one({
                "user_id": user_id,
                "product_id": product_id,
                "event_type": "product_viewed",
                "created_at": {"$gte": recent_cutoff},
            })
            if already_seen:
                continue

            await db["interactions"].insert_one({
                "user_id": user_id,
                "product_id": product_id,
                "sub_category_id": sub_category_id,
                "event_type": "product_viewed",
                "timestamp": timestamp,
                "created_at": now,
            })

            await _ensure_product_indexed(product_id)
            logger.debug("product_viewed enregistré — user=%s, product=%s", user_id, product_id)
    finally:
        await consumer.stop()
        if _http_client and not _http_client.is_closed:
            await _http_client.aclose()
