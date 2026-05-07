"""
Module C — Produits populaires.

P1 #3 — Le calcul est entièrement délégué à MongoDB via un pipeline d'agrégation.
Plus aucun document n'est chargé en mémoire Python.

Stratégie de pondération (approximation de la décroissance temporelle) :
  - Événements des 7 derniers jours  → weight × 2.0  (récents)
  - Événements des 8 à 30 derniers jours → weight × 1.0  (moins récents)
"""
import json
import logging
from datetime import datetime, timezone, timedelta
from typing import Any

from db import get_mongo_db, get_redis
from config import settings

logger = logging.getLogger(__name__)

REDIS_KEY_PREFIX = "popularity:"
REDIS_KEY_ALL = "popularity:all"


async def compute_popularity_scores(window_days: int = 30):
    """
    Calcule les scores de popularité via agrégation MongoDB (sans chargement en mémoire).
    Stocke le top-100 global et top-50 par catégorie dans Redis.
    """
    db = get_mongo_db()
    redis = get_redis()
    now = datetime.now(timezone.utc)
    since_30j = now - timedelta(days=window_days)
    since_7j = now - timedelta(days=7)

    pipeline = [
        # Filtre sur la fenêtre glissante (utilise l'index sur created_at)
        {"$match": {"created_at": {"$gte": since_30j}}},

        # Calcul du poids par type d'événement et de la récence
        {"$addFields": {
            "event_weight": {"$switch": {
                "branches": [
                    {"case": {"$eq": ["$event_type", "product_viewed"]},  "then": 1},
                    {"case": {"$eq": ["$event_type", "cart_added"]},      "then": 3},
                    {"case": {"$eq": ["$event_type", "order_completed"]}, "then": 5},
                ],
                "default": 1,
            }},
            # Multiplicateur de récence : ×2 si < 7j, ×1 sinon
            "recency": {
                "$cond": {
                    "if": {"$gte": ["$created_at", since_7j]},
                    "then": 2.0,
                    "else": 1.0,
                }
            },
        }},

        # Score final par document
        {"$addFields": {
            "score_contrib": {"$multiply": ["$event_weight", "$recency"]}
        }},

        # Agrégation par produit + catégorie
        {"$group": {
            "_id": {
                "product_id": "$product_id",
                "sub_category_id": {"$ifNull": ["$sub_category_id", "unknown"]},
            },
            "score": {"$sum": "$score_contrib"},
        }},

        # Tri décroissant
        {"$sort": {"score": -1}},

        # Limite de sécurité (évite de rapatrier une liste infinie)
        {"$limit": 500},
    ]

    cursor = db["interactions"].aggregate(pipeline)

    scores: dict[str, float] = {}
    category_scores: dict[str, dict[str, float]] = {}

    async for doc in cursor:
        product_id = doc["_id"]["product_id"]
        sub_category = doc["_id"]["sub_category_id"]
        score = doc["score"]

        if not product_id:
            continue

        scores[product_id] = scores.get(product_id, 0.0) + score
        category_scores.setdefault(sub_category, {})
        category_scores[sub_category][product_id] = (
            category_scores[sub_category].get(product_id, 0.0) + score
        )

    # Top-100 global → Redis (TTL calé sur le scheduler 1h)
    top_all = sorted(scores.items(), key=lambda x: x[1], reverse=True)[:100]
    await redis.set(
        REDIS_KEY_ALL,
        json.dumps([{"productId": pid, "score": round(s, 4)} for pid, s in top_all]),
        ex=settings.redis_popularity_ttl_seconds,
    )

    # Top-50 par catégorie → Redis
    for cat, cat_scores in category_scores.items():
        top_cat = sorted(cat_scores.items(), key=lambda x: x[1], reverse=True)[:50]
        await redis.set(
            f"{REDIS_KEY_PREFIX}{cat}",
            json.dumps([{"productId": pid, "score": round(s, 4)} for pid, s in top_cat]),
            ex=settings.redis_popularity_ttl_seconds,
        )

    logger.info(
        "Scores popularité recalculés (MongoDB agg.) — %d produits, %d catégories.",
        len(scores),
        len(category_scores),
    )


async def get_popular(category_id: str | None = None, limit: int = 10) -> list[dict[str, Any]]:
    """Lit le top produits depuis Redis."""
    redis = get_redis()
    key = f"{REDIS_KEY_PREFIX}{category_id}" if category_id else REDIS_KEY_ALL
    raw = await redis.get(key)
    if not raw:
        return []
    return json.loads(raw)[:limit]
