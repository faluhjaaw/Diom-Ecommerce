"""
Helpers de cache Redis pour les endpoints de recommandation.

Structure des clés (scannable, user_id en clair) :
  reco:personalized:{user_id}:{limit}
  reco:hybrid:{user_id}:{limit}:{product_id_or_none}

invalidate_user() utilise SCAN pour supprimer les entrées d'un utilisateur
immédiatement après un achat, sans attendre l'expiration TTL.
"""
import json
import logging

from config import settings
from db import get_redis

logger = logging.getLogger(__name__)


def make_personalized_key(user_id: str, limit: int) -> str:
    return f"reco:personalized:{user_id}:{limit}"


def make_hybrid_key(user_id: str, limit: int, product_id: str | None) -> str:
    pid = product_id or "none"
    return f"reco:hybrid:{user_id}:{limit}:{pid}"


async def get_cached(key: str) -> dict | None:
    try:
        raw = await get_redis().get(key)
        if raw:
            logger.debug("Cache HIT — %s", key)
            return json.loads(raw)
    except Exception as exc:
        logger.warning("Cache GET error (%s): %s", key, exc)
    return None


async def set_cached(key: str, data: dict):
    try:
        await get_redis().set(key, json.dumps(data), ex=settings.redis_ttl_seconds)
        logger.debug("Cache SET — %s (TTL %ds)", key, settings.redis_ttl_seconds)
    except Exception as exc:
        logger.warning("Cache SET error (%s): %s", key, exc)


async def invalidate_user(user_id: str):
    """
    Supprime toutes les entrées de cache d'un utilisateur via SCAN.
    Appelé après chaque achat pour que les nouvelles recommandations
    reflètent immédiatement le comportement récent.
    """
    redis = get_redis()
    patterns = [
        f"reco:personalized:{user_id}:*",
        f"reco:hybrid:{user_id}:*",
    ]
    deleted = 0
    try:
        for pattern in patterns:
            # count=100 : limite les clés renvoyées par batch pour éviter de bloquer Redis
            async for key in redis.scan_iter(pattern, count=100):
                await redis.delete(key)
                deleted += 1
        if deleted:
            logger.debug("Cache invalidé — user=%s, %d clé(s) supprimée(s).", user_id, deleted)
    except Exception as exc:
        logger.warning("Cache invalidation error (user=%s): %s", user_id, exc)
