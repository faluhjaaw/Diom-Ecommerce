"""
Consumer Kafka — topic : search.query_made
Payload : { userEmail, query, timestamp }
Alimente l'auto-complétion Redis.
"""
import json
import logging
from aiokafka import AIOKafkaConsumer
from app.config import settings
from app.services.autocomplete_service import record_query

logger = logging.getLogger(__name__)


async def consume_search_query():
    consumer = AIOKafkaConsumer(
        "search.query_made",
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        auto_offset_reset="earliest",
    )
    await consumer.start()
    logger.info("Consumer search.query_made démarré.")
    try:
        async for msg in consumer:
            payload = msg.value
            query = payload.get("query", "").strip()
            if query:
                await record_query(query)
                logger.debug("Requête enregistrée pour autocomplete : %s", query)
    finally:
        await consumer.stop()
