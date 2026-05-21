"""
Endpoints REST du recommendation-service.

GET /api/recommendations/similar/{product_id}       → Module A (content-based)
GET /api/recommendations/popular                    → Module C (popularité)
GET /api/recommendations/personalized/{user_id}     → Module B (collaboratif)
GET /api/recommendations/hybrid/{user_id}           → Score hybride final

POST /api/admin/*  → endpoints protégés par X-Admin-Key
"""
import httpx
from fastapi import APIRouter, Depends, Header, HTTPException, Query

from cache import (
    get_cached,
    invalidate_user,
    make_hybrid_key,
    make_personalized_key,
    set_cached,
)
from config import settings
from models.embedder import embed_and_index_product_async, find_similar_async, index_all_products
from models.popularity import compute_popularity_scores, get_popular
from models import collaborative

router = APIRouter(prefix="/api/recommendations", tags=["recommendations"])
admin_router = APIRouter(prefix="/api/admin", tags=["admin"])


# ---------------------------------------------------------------------------
# Dépendance d'authentification admin
# ---------------------------------------------------------------------------

async def verify_admin_key(x_admin_key: str = Header(..., alias="X-Admin-Key")):
    if x_admin_key != settings.admin_api_key:
        raise HTTPException(status_code=403, detail="Clé admin invalide.")


# ---------------------------------------------------------------------------
# Endpoints publics
# ---------------------------------------------------------------------------

@router.get("/similar/{product_id}")
async def similar_products(
    product_id: str,
    limit: int = Query(default=10, ge=1, le=50),
):
    """Produits similaires basés sur les embeddings (content-based)."""
    results = await find_similar_async(product_id, limit=limit)
    return {"productId": product_id, "similar": results}


@router.get("/popular")
async def popular_products(
    category_id: str | None = Query(default=None),
    limit: int = Query(default=10, ge=1, le=50),
):
    """Top produits sur fenêtre glissante, segmentés par catégorie si fournie."""
    results = await get_popular(category_id=category_id, limit=limit)
    return {"categoryId": category_id, "popular": results}


@router.get("/personalized/{user_id}")
async def personalized(
    user_id: str,
    limit: int = Query(default=10, ge=1, le=50),
):
    """
    Recommandations personnalisées via filtrage collaboratif (ALS).
    Retourne les produits populaires si l'utilisateur est en cold start.
    Résultat mis en cache Redis 15 min, invalidé après chaque achat.
    """
    cache_key = make_personalized_key(user_id, limit)
    cached = await get_cached(cache_key)
    if cached:
        return cached

    interaction_count = await collaborative.get_interaction_count(user_id)

    if interaction_count < settings.cold_start_threshold:
        popular = await get_popular(limit=limit)
        result = {"userId": user_id, "mode": "cold_start", "recommendations": popular}
    else:
        results = await collaborative.recommend(user_id, limit=limit)
        if not results:
            popular = await get_popular(limit=limit)
            result = {"userId": user_id, "mode": "cold_start_fallback", "recommendations": popular}
        else:
            result = {"userId": user_id, "mode": "collaborative", "recommendations": results}

    await set_cached(cache_key, result)
    return result


@router.get("/hybrid/{user_id}")
async def hybrid(
    user_id: str,
    product_id: str | None = Query(default=None, description="Produit actuellement consulté"),
    limit: int = Query(default=10, ge=1, le=50),
):
    """
    Score hybride : α×sémantique + β×collaboratif + γ×popularité.
    Les poids s'ajustent dynamiquement selon le profil de l'utilisateur.
    Résultat mis en cache Redis 15 min, invalidé après chaque achat.
    """
    cache_key = make_hybrid_key(user_id, limit, product_id)
    cached = await get_cached(cache_key)
    if cached:
        return cached

    interaction_count = await collaborative.get_interaction_count(user_id)

    # Poids dynamiques
    if interaction_count < settings.cold_start_threshold:
        alpha, beta, gamma = 0.2, 0.1, 0.7
    elif interaction_count < 50:
        alpha, beta, gamma = 0.3, 0.4, 0.3
    else:
        alpha, beta, gamma = 0.2, 0.6, 0.2

    semantic_scores: dict[str, float] = {}
    if product_id:
        for item in await find_similar_async(product_id, limit=limit * 2):
            semantic_scores[item["productId"]] = item["score"]

    collab_scores: dict[str, float] = {}
    if interaction_count >= settings.cold_start_threshold:
        raw_collab = await collaborative.recommend(user_id, limit=limit * 2)
        if raw_collab:
            max_c = max(item["score"] for item in raw_collab) or 1.0
            for item in raw_collab:
                collab_scores[item["productId"]] = item["score"] / max_c

    # Cold-start diversification: mix category-specific + global popularity
    pop_scores: dict[str, float] = {}
    sub_cat: str | None = None
    if product_id:
        from db import get_qdrant
        from models.embedder import _to_qdrant_id
        import asyncio as _asyncio
        loop = _asyncio.get_event_loop()
        pts = await loop.run_in_executor(
            None,
            lambda: get_qdrant().retrieve(
                collection_name=settings.qdrant_collection,
                ids=[_to_qdrant_id(product_id)],
                with_payload=True,
            ),
        )
        if pts:
            sub_cat = pts[0].payload.get("subCategoryId")

    popular_global = await get_popular(limit=limit * 2)
    popular_cat = await get_popular(category_id=sub_cat, limit=limit * 2) if sub_cat else []

    def _merge_popular(cat_list, global_list, cat_weight=0.6):
        merged: dict[str, float] = {}
        if global_list:
            g_max = global_list[0]["score"] or 1.0
            for item in global_list:
                merged[item["productId"]] = (1 - cat_weight) * item["score"] / g_max
        if cat_list:
            c_max = cat_list[0]["score"] or 1.0
            for item in cat_list:
                pid = item["productId"]
                merged[pid] = merged.get(pid, 0.0) + cat_weight * item["score"] / c_max
        return merged

    pop_scores = _merge_popular(popular_cat, popular_global)

    all_ids = set(semantic_scores) | set(collab_scores) | set(pop_scores)
    ranked = [
        {
            "productId": pid,
            "score": round(
                alpha * semantic_scores.get(pid, 0.0)
                + beta * collab_scores.get(pid, 0.0)
                + gamma * pop_scores.get(pid, 0.0),
                4,
            ),
        }
        for pid in all_ids
    ]
    ranked.sort(key=lambda x: x["score"], reverse=True)

    result = {
        "userId": user_id,
        "productId": product_id,
        "weights": {"alpha": alpha, "beta": beta, "gamma": gamma},
        "recommendations": ranked[:limit],
    }
    await set_cached(cache_key, result)
    return result


# ---------------------------------------------------------------------------
# Endpoints admin (protégés par X-Admin-Key)
# ---------------------------------------------------------------------------

@admin_router.post("/reindex", dependencies=[Depends(verify_admin_key)])
async def reindex_all():
    """Ré-indexation complète de tous les produits dans Qdrant."""
    await index_all_products()
    return {"status": "reindex triggered"}


@admin_router.post("/reindex/{product_id}", dependencies=[Depends(verify_admin_key)])
async def reindex_one(product_id: str):
    """Ré-indexe un seul produit."""
    async with httpx.AsyncClient(timeout=10) as client:
        resp = await client.get(f"{settings.product_service_url}/api/products/{product_id}")
        if resp.status_code != 200:
            raise HTTPException(status_code=404, detail=f"Produit {product_id} introuvable")
        await embed_and_index_product_async(resp.json())
    return {"status": "indexed", "productId": product_id}


@admin_router.post("/recompute-popularity", dependencies=[Depends(verify_admin_key)])
async def recompute_popularity():
    """Force le recalcul des scores de popularité."""
    await compute_popularity_scores()
    return {"status": "popularity scores recomputed"}


@admin_router.post("/retrain-collaborative", dependencies=[Depends(verify_admin_key)])
async def retrain_collaborative():
    """Force le réentraînement du modèle ALS collaboratif."""
    await collaborative.train()
    return {"status": "collaborative model retrained"}


@admin_router.post("/invalidate-cache/{user_id}", dependencies=[Depends(verify_admin_key)])
async def invalidate_cache(user_id: str):
    """Invalide manuellement le cache d'un utilisateur."""
    await invalidate_user(user_id)
    return {"status": "cache invalidated", "userId": user_id}
