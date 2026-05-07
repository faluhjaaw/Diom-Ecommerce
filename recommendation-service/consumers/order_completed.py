"""
Consumer Kafka — topic : user.order_completed
Payload : { orderId, userId, userEmail, productIds }

P0 #2 — identifiant canonique : userEmail si présent, sinon str(userId).
"""
import json
import logging
from datetime import datetime, timezone

from aiokafka import AIOKafkaConsumer

from cache import invalidate_user
from config import settings
from db import get_mongo_db
from models import collaborative

logger = logging.getLogger(__name__)

_purchases_since_last_train = 0
RETRAIN_EVERY = 50


async def consume_order_completed():
    global _purchases_since_last_train

    consumer = AIOKafkaConsumer(
        "user.order_completed",
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        auto_offset_reset="earliest",
    )
    await consumer.start()
    logger.info("Consumer user.order_completed démarré.")
    try:
        async for msg in consumer:
            payload = msg.value
            # userEmail est l'identifiant canonique ; userId en fallback
            user_id = payload.get("userEmail") or str(payload.get("userId", ""))
            product_ids: list[str] = payload.get("productIds") or []
            timestamp = datetime.now(timezone.utc).isoformat()

            if not user_id or not product_ids:
                continue

            now = datetime.now(timezone.utc)
            db = get_mongo_db()
            await db["interactions"].insert_many([
                {
                    "user_id": user_id,
                    "product_id": pid,
                    "event_type": "order_completed",
                    "timestamp": timestamp,
                    "created_at": now,   # datetime natif → TTL MongoDB
                }
                for pid in product_ids
            ])

            # Invalider le cache immédiatement — l'achat change le profil utilisateur
            await invalidate_user(user_id)

            _purchases_since_last_train += len(product_ids)
            if _purchases_since_last_train >= RETRAIN_EVERY:
                logger.info("Seuil atteint — réentraînement du modèle collaboratif.")
                await collaborative.train()
                _purchases_since_last_train = 0

            logger.debug("order_completed — user=%s, %d produits", user_id, len(product_ids))
    finally:
        await consumer.stop()
