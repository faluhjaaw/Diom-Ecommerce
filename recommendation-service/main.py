"""
recommendation-service — point d'entrée FastAPI.

Démarrage :
    uvicorn main:app --host 0.0.0.0 --port 8090 --reload
"""
import asyncio
import logging

import py_eureka_client.eureka_client as eureka_client
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from fastapi import FastAPI
from api.routes import router, admin_router
from config import settings
from consumers.cart_added import consume_cart_added
from consumers.order_completed import consume_order_completed
from consumers.product_catalog import consume_product_catalog
from consumers.product_viewed import consume_product_viewed
from db import (
    close_mongo,
    close_redis,
    connect_mongo,
    connect_qdrant,
    connect_redis,
)
from models.collaborative import train as train_collaborative
from models.embedder import index_all_products, load_model
from models.popularity import compute_popularity_scores

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name, version="1.0.0")

app.include_router(router)
app.include_router(admin_router)

_scheduler = AsyncIOScheduler()
_consumer_tasks: list[asyncio.Task] = []


@app.on_event("startup")
async def startup():
    logger.info("Démarrage du recommendation-service...")

    # 1. Connexions
    await connect_mongo()
    await connect_redis()
    connect_qdrant()
    logger.info("Connexions établies (MongoDB, Redis, Qdrant).")

    # 2. Modèle d'embedding
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(None, load_model)

    # 3. Indexation initiale des produits dans Qdrant
    await index_all_products()

    # 4. Calcul initial des scores de popularité
    await compute_popularity_scores()

    # 5. Entraînement initial du modèle collaboratif (si données disponibles)
    await train_collaborative()

    # 6. Scheduler — recalculs périodiques
    _scheduler.add_job(compute_popularity_scores, "interval", hours=1, id="popularity")
    _scheduler.add_job(train_collaborative, "interval", hours=24, id="collaborative")
    # Ré-indexation complète quotidienne pour rattraper d'éventuels écarts
    _scheduler.add_job(index_all_products, "cron", hour=3, minute=0, id="reindex")
    _scheduler.start()
    logger.info("Scheduler démarré (popularité: 1h, collaboratif: 24h, reindex: 03h00).")

    # 7. Consumers Kafka en tâches de fond (optionnel)
    if settings.kafka_enabled:
        _consumer_tasks.extend([
            asyncio.create_task(consume_product_viewed(), name="consumer-product_viewed"),
            asyncio.create_task(consume_cart_added(), name="consumer-cart_added"),
            asyncio.create_task(consume_order_completed(), name="consumer-order_completed"),
            asyncio.create_task(consume_product_catalog(), name="consumer-product_catalog"),
        ])
        logger.info("Consumers Kafka démarrés (%d topics).", len(_consumer_tasks))
    else:
        logger.info("Kafka désactivé (KAFKA_ENABLED=false) — consumers non démarrés.")

    # 8. Enregistrement Eureka
    if settings.eureka_enabled:
        await eureka_client.init_async(
            eureka_server=settings.eureka_server,
            app_name="recommendation-service",
            instance_port=settings.port,
            instance_host=settings.eureka_instance_host,
            renewal_interval_in_secs=30,
            duration_in_secs=90,
        )
        logger.info("Enregistré auprès d'Eureka — %s", settings.eureka_server)

    logger.info("recommendation-service prêt sur le port %d.", settings.port)


@app.on_event("shutdown")
async def shutdown():
    if settings.eureka_enabled:
        await eureka_client.stop_async()
        logger.info("Désenregistré d'Eureka.")
    _scheduler.shutdown(wait=False)
    for task in _consumer_tasks:
        task.cancel()
    await asyncio.gather(*_consumer_tasks, return_exceptions=True)
    await close_redis()
    await close_mongo()
    logger.info("recommendation-service arrêté proprement.")


@app.get("/health")
def health():
    return {"status": "UP", "service": settings.app_name}
