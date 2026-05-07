"""
Consumer Kafka — topic : user.cart_added
Payload : { userId, userEmail, productId, timestamp }

P0 #2 — identifiant canonique : userEmail si présent, sinon str(userId).
"""
import json
import logging
from datetime import datetime, timezone

from aiokafka import AIOKafkaConsumer

from config import settings
from db import get_mongo_db

logger = logging.getLogger(__name__)


async def consume_cart_added():
    consumer = AIOKafkaConsumer(
        "user.cart_added",
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        auto_offset_reset="earliest",
    )
    await consumer.start()
    logger.info("Consumer user.cart_added démarré.")
    try:
        async for msg in consumer:
            payload = msg.value
            # userEmail est l'identifiant canonique ; userId en fallback
            user_id = payload.get("userEmail") or str(payload.get("userId", ""))
            product_id = payload.get("productId", "")
            timestamp = payload.get("timestamp", datetime.now(timezone.utc).isoformat())

            if not user_id or not product_id:
                continue

            now = datetime.now(timezone.utc)
            db = get_mongo_db()
            await db["interactions"].insert_one({
                "user_id": user_id,
                "product_id": product_id,
                "event_type": "cart_added",
                "timestamp": timestamp,
                "created_at": now,   # datetime natif → TTL MongoDB
            })
            logger.debug("cart_added enregistré — user=%s, product=%s", user_id, product_id)
    finally:
        await consumer.stop()
