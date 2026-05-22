"""
Consumer Kafka — topic : product.catalog
Payload : { eventType, id, name, description, price, subCategoryId, brand, tags, imageUrls, slug, rating }

P0 #1 — embed_and_index_product_async (non-bloquant).
P0 #3 — qdrant.delete() utilise PointIdsList (type correct).
"""
import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer
from qdrant_client.models import PointIdsList

from config import settings
from db import get_qdrant
from models.embedder import embed_and_index_product_async, _to_qdrant_id

logger = logging.getLogger(__name__)


async def consume_product_catalog():
    consumer = AIOKafkaConsumer(
        "product.catalog",
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        auto_offset_reset="earliest",
    )
    await consumer.start()
    logger.info("Consumer product.catalog démarré.")
    try:
        async for msg in consumer:
            payload = msg.value
            event_type = payload.get("eventType", "")
            product_id = payload.get("id", "")

            if not product_id:
                continue

            if event_type == "created" or (
                event_type == "updated" and payload.get("status", "ACTIVE") == "ACTIVE"
            ):
                try:
                    await embed_and_index_product_async(payload)
                    logger.info("Produit %s indexé dans Qdrant (event: %s).", product_id, event_type)
                except Exception as exc:
                    logger.warning("Erreur indexation produit %s : %s", product_id, exc)

            elif event_type == "updated" and payload.get("status") in ("SOLD", "ARCHIVED"):
                # Produit vendu ou archivé → retirer des recommandations
                try:
                    loop = asyncio.get_event_loop()
                    await loop.run_in_executor(
                        None,
                        lambda: get_qdrant().delete(
                            collection_name=settings.qdrant_collection,
                            points_selector=PointIdsList(points=[_to_qdrant_id(product_id)]),
                        ),
                    )
                    logger.info("Produit %s retiré de Qdrant (status: %s).", product_id, payload.get("status"))
                except Exception as exc:
                    logger.warning("Erreur suppression produit %s dans Qdrant : %s", product_id, exc)

            elif event_type == "deleted":
                try:
                    loop = asyncio.get_event_loop()
                    # P0 #3 — PointIdsList (type correct)
                    await loop.run_in_executor(
                        None,
                        lambda: get_qdrant().delete(
                            collection_name=settings.qdrant_collection,
                            points_selector=PointIdsList(points=[_to_qdrant_id(product_id)]),
                        ),
                    )
                    logger.info("Produit %s supprimé de Qdrant.", product_id)
                except Exception as exc:
                    logger.warning("Erreur suppression produit %s dans Qdrant : %s", product_id, exc)
    finally:
        await consumer.stop()
