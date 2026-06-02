"""
Consumer Kafka — topic : product.catalog
Payload : { eventType, id, name, description, price, subCategoryId, brand, tags, imageUrls, slug, rating }
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

_REQUIRED_FIELDS = {"id", "eventType"}
_MAX_RETRIES = 3
_BACKOFF_BASE = 2  # secondes


async def _delete_from_qdrant(product_id: str, label: str):
    """Supprime un produit de Qdrant avec retry/backoff exponentiel."""
    loop = asyncio.get_running_loop()
    for attempt in range(1, _MAX_RETRIES + 1):
        try:
            await loop.run_in_executor(
                None,
                lambda: get_qdrant().delete(
                    collection_name=settings.qdrant_collection,
                    points_selector=PointIdsList(points=[_to_qdrant_id(product_id)]),
                ),
            )
            logger.info("Produit %s retiré de Qdrant (%s).", product_id, label)
            return
        except Exception as exc:
            if attempt == _MAX_RETRIES:
                logger.error(
                    "Échec suppression Qdrant produit %s après %d tentatives : %s",
                    product_id, _MAX_RETRIES, exc,
                )
            else:
                wait = _BACKOFF_BASE ** attempt
                logger.warning(
                    "Erreur suppression Qdrant produit %s (tentative %d/%d), retry dans %ds : %s",
                    product_id, attempt, _MAX_RETRIES, wait, exc,
                )
                await asyncio.sleep(wait)


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

            # Validation minimale du payload
            missing = _REQUIRED_FIELDS - set(payload.keys())
            if missing:
                logger.warning("Payload product.catalog invalide — champs manquants : %s", missing)
                continue

            event_type = payload.get("eventType", "")
            product_id = payload.get("id", "")

            if not product_id:
                continue

            if event_type == "created" or (
                event_type == "updated" and payload.get("status", "ACTIVE") == "ACTIVE"
            ):
                for attempt in range(1, _MAX_RETRIES + 1):
                    try:
                        await embed_and_index_product_async(payload)
                        logger.info("Produit %s indexé dans Qdrant (event: %s).", product_id, event_type)
                        break
                    except Exception as exc:
                        if attempt == _MAX_RETRIES:
                            logger.error(
                                "Échec indexation produit %s après %d tentatives : %s",
                                product_id, _MAX_RETRIES, exc,
                            )
                        else:
                            wait = _BACKOFF_BASE ** attempt
                            logger.warning(
                                "Erreur indexation produit %s (tentative %d/%d), retry dans %ds : %s",
                                product_id, attempt, _MAX_RETRIES, wait, exc,
                            )
                            await asyncio.sleep(wait)

            elif event_type == "updated" and payload.get("status") in ("SOLD", "ARCHIVED"):
                await _delete_from_qdrant(product_id, payload.get("status"))

            elif event_type == "deleted":
                await _delete_from_qdrant(product_id, "deleted")

    finally:
        await consumer.stop()
