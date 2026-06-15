"""
search-service — point d'entrée FastAPI.

Démarrage :
    uvicorn main:app --host 0.0.0.0 --port 8087 --reload
"""
import asyncio
import logging
from contextlib import asynccontextmanager

import py_eureka_client.eureka_client as eureka_client
from fastapi import FastAPI
from app.config import settings
from seed_redis import seed_redis
from app.routers import search, indexer, ai_chat
from app.services.embedding_service import load_model
from app.services.mongo_service import connect_mongo, close_mongo
from app.services.autocomplete_service import connect_redis, close_redis
from app.services.symspell_service import load_symspell
from app.services.qdrant_service import init_collection
from app.consumers.search_query import consume_search_query

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
logger = logging.getLogger(__name__)

_consumer_tasks: list[asyncio.Task] = []


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Démarrage du search-service...")

    # 1. Connexions
    connect_mongo()
    connect_redis()
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(
        None, seed_redis, settings.redis_host, settings.redis_port, settings.MONGODB_URI
    )
    await init_collection()
    logger.info("Connexions établies (MongoDB, Redis, Qdrant).")

    # 2. Modèles (CPU-bound → executor)
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(None, load_model)
    await loop.run_in_executor(None, load_symspell)
    logger.info("Modèles chargés (sentence-transformers, SymSpell).")

    # 3. Consumer Kafka
    if settings.kafka_enabled:
        _consumer_tasks.append(
            asyncio.create_task(consume_search_query(), name="consumer-search_query")
        )
        logger.info("Consumer Kafka search.query_made démarré.")
    else:
        logger.info("Kafka désactivé.")

    # 4. Eureka
    if settings.eureka_enabled:
        await eureka_client.init_async(
            eureka_server=settings.eureka_server,
            app_name="search-service",
            instance_port=settings.port,
            instance_host=settings.eureka_instance_host,
            renewal_interval_in_secs=30,
            duration_in_secs=90,
        )
        logger.info("Enregistré auprès d'Eureka.")

    logger.info("search-service prêt sur le port %d.", settings.port)

    yield

    # Shutdown
    if settings.eureka_enabled:
        await eureka_client.stop_async()
    for task in _consumer_tasks:
        task.cancel()
    await asyncio.gather(*_consumer_tasks, return_exceptions=True)
    await close_redis()
    close_mongo()
    logger.info("search-service arrêté proprement.")


app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    description="Recherche sémantique pour la marketplace Diom",
    lifespan=lifespan,
)

app.include_router(search.router, prefix="/api/search", tags=["Search"])
app.include_router(indexer.router, prefix="/api/indexer", tags=["Indexer"])
app.include_router(ai_chat.router, prefix="/api/ai", tags=["AI"])



@app.get("/health")
def health():
    return {"status": "UP", "service": settings.app_name}
