"""
Module A — Produits similaires (filtrage basé sur le contenu).

Génère les embeddings via fastembed (ONNX Runtime, pas de PyTorch) et les
stocke dans Qdrant.  Toutes les opérations CPU/IO synchrones (encode, upsert,
search) sont exécutées dans un thread pool via run_in_executor pour ne pas
bloquer la boucle asyncio.
"""
import asyncio
import logging
import re
import uuid
from typing import Any

import httpx
from fastembed import TextEmbedding
from qdrant_client.models import Filter, FieldCondition, MatchValue, PointStruct

from config import settings
from db import get_qdrant

logger = logging.getLogger(__name__)

_model: TextEmbedding | None = None


def load_model():
    """Charge le modèle d'embedding multilingue (appelé une seule fois au démarrage)."""
    global _model
    logger.info("Chargement du modèle fastembed...")
    _model = TextEmbedding("sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
    logger.info("Modèle chargé.")


def _build_text(product: dict) -> str:
    # Brand stripped from name and tags — shared brand across unrelated categories
    # (Samsung phone vs Samsung fridge) would inflate cosine similarity.
    brand = product.get("brand", "")
    brand_pattern = re.compile(re.escape(brand), re.IGNORECASE) if brand else None

    name = product.get("name", "")
    description = product.get("description", "")
    if brand_pattern:
        name = brand_pattern.sub("", name).strip()
        description = brand_pattern.sub("", description).strip()

    tags = [
        t for t in (product.get("tags", []) or [])
        if not (brand and t.lower() == brand.lower())
    ]

    parts = [
        name,
        description,
        " ".join(tags),
    ]
    return " ".join(p for p in parts if p)


def _to_qdrant_id(product_id: str) -> str:
    """Convert any string (e.g. MongoDB ObjectId) to a valid RFC 4122 UUID for Qdrant."""
    return str(uuid.uuid5(uuid.NAMESPACE_OID, product_id))


def _build_payload(product: dict) -> dict:
    return {
        "productId": product.get("id"),          # original MongoDB ObjectId string
        "name": product.get("name"),
        "price": float(product.get("price", 0) or 0),
        "subCategoryId": product.get("subCategoryId"),
        "brand": product.get("brand"),
        "rating": product.get("rating"),
        "slug": product.get("slug"),
        "imageUrls": product.get("imageUrls", []),
    }


# ---------------------------------------------------------------------------
# Opérations synchrones (appellées depuis run_in_executor)
# ---------------------------------------------------------------------------

def _sync_embed_and_index(product: dict):
    """Encode + upsert un produit dans Qdrant (sync, doit tourner dans executor)."""
    if _model is None:
        raise RuntimeError("Embedding model not loaded")
    text = _build_text(product)
    # fastembed.embed() retourne un générateur de numpy arrays
    vector = next(_model.embed([text])).tolist()
    get_qdrant().upsert(
        collection_name=settings.qdrant_collection,
        points=[PointStruct(id=_to_qdrant_id(product["id"]), vector=vector, payload=_build_payload(product))],
    )


def _sync_index_batch(products: list[dict]):
    """Encode un batch de produits et les upsert en une seule requête Qdrant."""
    if _model is None:
        raise RuntimeError("Embedding model not loaded")
    texts = [_build_text(p) for p in products]
    vectors = [v.tolist() for v in _model.embed(texts)]
    points = [
        PointStruct(id=_to_qdrant_id(p["id"]), vector=v, payload=_build_payload(p))
        for p, v in zip(products, vectors)
    ]
    get_qdrant().upsert(collection_name=settings.qdrant_collection, points=points)


def _sync_find_similar(product_id: str, limit: int, price_range_pct: float) -> list[dict[str, Any]]:
    """Recherche cosine dans Qdrant avec re-ranking sémantique + qualité.

    Scoring final :
        score = cosine * category_boost * rating_boost * price_penalty
    - category_boost : ×1.2 si même subCategoryId, ×1.0 sinon
    - rating_boost   : 1 + rating/10 si rating présent, ×1.0 sinon
    - price_penalty  : ×0.7 si hors ±price_range_pct, ×1.0 sinon (souple, pas d'élimination)
    """
    qdrant = get_qdrant()

    results = qdrant.retrieve(
        collection_name=settings.qdrant_collection,
        ids=[_to_qdrant_id(product_id)],
        with_vectors=True,
    )
    if not results:
        return []

    source = results[0]
    source_vector = source.vector
    source_price = source.payload.get("price", 0)
    source_sub_cat = source.payload.get("subCategoryId")

    # Fetch large pool sans filtre dur — re-rank ensuite
    candidates = qdrant.search(
        collection_name=settings.qdrant_collection,
        query_vector=source_vector,
        limit=limit * 4,
        with_payload=True,
    )

    qdrant_source_id = _to_qdrant_id(product_id)
    scored = []
    for hit in candidates:
        if str(hit.id) == qdrant_source_id:  # exclure le produit source lui-même
            continue

        payload = hit.payload
        cosine = hit.score

        # Category boost/penalty (soft)
        hit_sub_cat = payload.get("subCategoryId")
        same_cat = source_sub_cat and hit_sub_cat == source_sub_cat
        if same_cat:
            category_boost = 1.2
        elif source_sub_cat and hit_sub_cat != source_sub_cat:
            category_boost = 0.15  # strong cross-category penalty
        else:
            category_boost = 1.0

        # Rating boost — clamp sur 0-10 pour gérer toutes les échelles
        rating = payload.get("rating")
        if rating is not None:
            rating_clamped = max(0.0, min(10.0, float(rating)))
            rating_boost = 1.0 + rating_clamped / 10.0
        else:
            rating_boost = 1.0

        # Price penalty (soft — pas d'élimination)
        hit_price = payload.get("price", 0)
        price_penalty = 1.0
        if source_price > 0 and hit_price > 0:
            ratio = hit_price / source_price
            if not (1 - price_range_pct <= ratio <= 1 + price_range_pct):
                price_penalty = 0.7

        final_score = cosine * category_boost * rating_boost * price_penalty
        orig_id = payload.get("productId", str(hit.id))
        scored.append({"productId": orig_id, "score": round(final_score, 4), **payload})

    scored.sort(key=lambda x: x["score"], reverse=True)
    return scored[:limit]


# ---------------------------------------------------------------------------
# Wrappers async (à utiliser dans les coroutines et endpoints)
# ---------------------------------------------------------------------------

async def embed_and_index_product_async(product: dict):
    """Async-safe : encode + upsert dans Qdrant sans bloquer la boucle."""
    loop = asyncio.get_running_loop()
    await loop.run_in_executor(None, _sync_embed_and_index, product)


async def find_similar_async(
    product_id: str,
    limit: int = 10,
    price_range_pct: float = 0.2,
) -> list[dict[str, Any]]:
    """Async-safe : recherche cosine dans Qdrant sans bloquer la boucle."""
    loop = asyncio.get_running_loop()
    return await loop.run_in_executor(
        None, _sync_find_similar, product_id, limit, price_range_pct
    )


async def index_all_products():
    """
    Récupère tous les produits du product-service et les indexe dans Qdrant.
    Fetch la liste pour avoir les IDs, puis récupère le détail de chaque produit
    (la route /api/products/{id} retourne subCategoryId contrairement à la liste).
    Traitement par batch de 64 pour exploiter le batch encoding.
    """
    async with httpx.AsyncClient(timeout=30) as client:
        try:
            resp = await client.get(f"{settings.product_service_url}/api/products")
            resp.raise_for_status()
            product_list = resp.json()
        except Exception as exc:
            logger.warning("Impossible de récupérer les produits pour l'indexation : %s", exc)
            return

    if not product_list:
        logger.info("Aucun produit à indexer.")
        return

    # Fetch détail de chaque produit en parallèle (semaphore = 10 workers max)
    sem = asyncio.Semaphore(10)

    async def _fetch_one(client: httpx.AsyncClient, item: dict) -> dict:
        pid = item.get("id")
        if not pid:
            return item
        async with sem:
            try:
                r = await client.get(f"{settings.product_service_url}/api/products/{pid}")
                return r.json() if r.status_code == 200 else item
            except Exception as exc:
                logger.warning("Détail produit %s inaccessible : %s", pid, exc)
                return item

    async with httpx.AsyncClient(timeout=10) as client:
        products = await asyncio.gather(*[_fetch_one(client, item) for item in product_list])

    loop = asyncio.get_running_loop()
    batch_size = 64
    total = 0

    for i in range(0, len(products), batch_size):
        batch = products[i: i + batch_size]
        try:
            await loop.run_in_executor(None, _sync_index_batch, batch)
            total += len(batch)
        except Exception as exc:
            logger.warning("Erreur indexation batch [%d:%d] : %s", i, i + batch_size, exc)

    logger.info("%d / %d produits indexés dans Qdrant.", total, len(product_list))


# Alias conservé pour le consumer product_catalog (appelé via embed_and_index_product_async)
embed_and_index_product = _sync_embed_and_index
