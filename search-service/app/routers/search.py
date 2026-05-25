"""
Routes de recherche.
GET /api/search                  → recherche sémantique
GET /api/search/autocomplete     → suggestions temps réel
"""
import logging
from fastapi import APIRouter, Query, BackgroundTasks
from app.services.embedding_service import embed_text_async
from app.services.qdrant_service import search_products
from app.services.autocomplete_service import get_suggestions, record_query
from app.services.symspell_service import correct_query

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("")
async def semantic_search(
    background_tasks: BackgroundTasks,
    q: str = Query(..., description="Requête de recherche"),
    limit: int = Query(10, ge=1, le=50),
    min_price: float = Query(None),
    max_price: float = Query(None),
    category: str = Query(None),
):
    """Recherche sémantique avec correction orthographique."""
    # Correction ortho
    corrected_q = correct_query(q)
    was_corrected = corrected_q.lower() != q.lower()

    # Embedding async
    query_vector = await embed_text_async(corrected_q)

    # Filtres
    filters = {}
    if min_price is not None:
        filters["min_price"] = min_price
    if max_price is not None:
        filters["max_price"] = max_price
    if category:
        filters["category"] = category

    # Recherche Qdrant
    results = await search_products(
        query_vector=query_vector,
        limit=limit,
        filters=filters if filters else None,
    )

    # Enregistre la requête pour autocomplete en arrière-plan
    background_tasks.add_task(record_query, corrected_q)

    return {
        "query": q,
        "corrected_query": corrected_q if was_corrected else None,
        "total": len(results),
        "results": [
            {
                "score": round(r.score, 4),
                "id": r.payload.get("mongo_id"),
                "name": r.payload.get("name"),
                "description": r.payload.get("description"),
                "price": r.payload.get("price"),
                "brand": r.payload.get("brand"),
                "stock": r.payload.get("stock"),
                "tags": r.payload.get("tags", []),
            }
            for r in results
        ],
    }


@router.get("/autocomplete")
async def autocomplete(
    q: str = Query(..., min_length=2, description="Préfixe de recherche"),
    limit: int = Query(10, ge=1, le=20),
):
    """Suggestions d'auto-complétion basées sur les requêtes passées."""
    suggestions = await get_suggestions(q, limit=limit)
    return {"prefix": q, "suggestions": suggestions}
