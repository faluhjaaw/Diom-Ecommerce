"""
Auto-complétion Redis.
Stratégie : pour chaque requête enregistrée, on stocke tous ses préfixes
(min 2 chars) dans un Redis Sorted Set avec le score = fréquence.
GET /api/search/autocomplete?q=chaus → ["chaussure", "chaussure sport", ...]
"""
import logging
import redis.asyncio as aioredis
from app.config import settings

logger = logging.getLogger(__name__)
_redis: aioredis.Redis | None = None

AUTOCOMPLETE_KEY = "search:autocomplete"


def connect_redis():
    global _redis
    _redis = aioredis.Redis(
        host=settings.redis_host,
        port=settings.redis_port,
        decode_responses=True,
    )
    logger.info("Redis connecté (autocomplete).")


async def close_redis():
    if _redis:
        await _redis.aclose()


async def record_query(query: str):
    """Enregistre une requête et tous ses préfixes dans Redis."""
    if not _redis or not query:
        return
    q = query.lower().strip()
    # Incrémente le score de la requête complète
    await _redis.zincrby(AUTOCOMPLETE_KEY, 1, q)
    # Indexe aussi tous les préfixes (min 2 chars)
    for i in range(2, len(q)):
        prefix = q[:i]
        await _redis.zincrby(AUTOCOMPLETE_KEY, 0, prefix)  # score 0 = juste indexé
    await _redis.expire(AUTOCOMPLETE_KEY, settings.autocomplete_ttl_seconds)


async def get_suggestions(prefix: str, limit: int = 10) -> list[str]:
    """Retourne les suggestions triées par fréquence pour un préfixe donné."""
    if not _redis or not prefix:
        return []
    p = prefix.lower().strip()
    # Récupère tous les membres dont le score > 0 (vraies requêtes, pas préfixes)
    # On filtre côté Python pour ne garder que ceux qui commencent par le préfixe
    candidates = await _redis.zrevrangebyscore(
        AUTOCOMPLETE_KEY, "+inf", 1, start=0, num=settings.autocomplete_top_k
    )
    suggestions = [c for c in candidates if c.startswith(p)]
    return suggestions[:limit]
