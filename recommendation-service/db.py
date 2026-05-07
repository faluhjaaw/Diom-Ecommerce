"""Connexions partagées — MongoDB, Redis, Qdrant."""
import redis.asyncio as aioredis
from motor.motor_asyncio import AsyncIOMotorClient
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams

from config import settings

# ---------------------------------------------------------------------------
# MongoDB
# ---------------------------------------------------------------------------
_mongo_client: AsyncIOMotorClient | None = None


def get_mongo_db():
    return _mongo_client[settings.mongo_db]


async def connect_mongo():
    global _mongo_client
    _mongo_client = AsyncIOMotorClient(settings.mongo_uri)
    db = get_mongo_db()
    # TTL réel sur created_at (datetime) — MongoDB supprime automatiquement après N jours
    ttl_seconds = settings.interactions_ttl_days * 86400
    await db["interactions"].create_index(
        "created_at", expireAfterSeconds=ttl_seconds
    )
    await db["interactions"].create_index([("user_id", 1), ("product_id", 1)])


async def close_mongo():
    if _mongo_client:
        _mongo_client.close()


# ---------------------------------------------------------------------------
# Redis
# ---------------------------------------------------------------------------
_redis: aioredis.Redis | None = None


def get_redis() -> aioredis.Redis:
    return _redis


async def connect_redis():
    global _redis
    _redis = aioredis.Redis(
        host=settings.redis_host,
        port=settings.redis_port,
        decode_responses=True,
    )


async def close_redis():
    if _redis:
        await _redis.aclose()


# ---------------------------------------------------------------------------
# Qdrant
# ---------------------------------------------------------------------------
_qdrant: QdrantClient | None = None


def get_qdrant() -> QdrantClient:
    return _qdrant


def connect_qdrant():
    global _qdrant
    if settings.qdrant_mode == "local":
        # Mode embedded : pas de serveur Qdrant requis (données persistées sur disque)
        _qdrant = QdrantClient(path=settings.qdrant_local_path)
    else:
        _qdrant = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)
    # Crée la collection si elle n'existe pas encore
    existing = {c.name for c in _qdrant.get_collections().collections}
    if settings.qdrant_collection not in existing:
        _qdrant.create_collection(
            collection_name=settings.qdrant_collection,
            vectors_config=VectorParams(
                size=settings.embedding_dim,
                distance=Distance.COSINE,
            ),
        )
