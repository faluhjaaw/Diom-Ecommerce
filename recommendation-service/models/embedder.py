"""
Module A — Produits similaires (filtrage basé sur le contenu).

Génère les embeddings via fastembed (ONNX Runtime, pas de PyTorch) et les
stocke dans Qdrant.  Toutes les opérations CPU/IO synchrones (encode, upsert,
search) sont exécutées dans un thread pool via run_in_executor pour ne pas
bloquer la boucle asyncio.
"""
import asyncio
import logging
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
    parts = [
        product.get("name", ""),
        product.get("description", ""),
        product.get("brand", ""),
        " ".join(product.get("tags", []) or []),
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
    """Recherche cosine dans Qdrant (sync, doit tourner dans executor)."""
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

    query_filter = None
    sub_cat = source.payload.get("subCategoryId")
    if sub_cat:
        query_filter = Filter(
            must=[FieldCondition(key="subCategoryId", match=MatchValue(value=sub_cat))]
        )

    hits = qdrant.search(
        collection_name=settings.qdrant_collection,
        query_vector=source_vector,
        query_filter=query_filter,
        limit=limit + 5,
        with_payload=True,
    )

    qdrant_source_id = _to_qdrant_id(product_id)
    similar = []
    for hit in hits:
        if str(hit.id) == qdrant_source_id:
            continue
        hit_price = hit.payload.get("price", 0)
        if source_price > 0 and hit_price > 0:
            ratio = hit_price / source_price
            if not (1 - price_range_pct <= ratio <= 1 + price_range_pct):
                continue
        orig_id = hit.payload.get("productId", str(hit.id))
        similar.append({"productId": orig_id, "score": hit.score, **hit.payload})
        if len(similar) >= limit:
            break

    # Fallback sans filtre prix si résultats insuffisants
    if len(similar) < limit // 2 and query_filter is not None:
        hits_no_filter = qdrant.search(
            collection_name=settings.qdrant_collection,
            query_vector=source_vector,
            limit=limit + 5,
            with_payload=True,
        )
        seen = {s["productId"] for s in similar}
        for hit in hits_no_filter:
            orig_id = hit.payload.get("productId", str(hit.id))
            if str(hit.id) == qdrant_source_id or orig_id in seen:
                continue
            similar.append({"productId": orig_id, "score": hit.score, **hit.payload})
            if len(similar) >= limit:
                break

    return similar


# ---------------------------------------------------------------------------
# Wrappers async (à utiliser dans les coroutines et endpoints)
# ---------------------------------------------------------------------------

async def embed_and_index_product_async(product: dict):
    """Async-safe : encode + upsert dans Qdrant sans bloquer la boucle."""
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(None, _sync_embed_and_index, product)


async def find_similar_async(
    product_id: str,
    limit: int = 10,
    price_range_pct: float = 0.2,
) -> list[dict[str, Any]]:
    """Async-safe : recherche cosine dans Qdrant sans bloquer la boucle."""
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(
        None, _sync_find_similar, product_id, limit, price_range_pct
    )


async def index_all_products():
    """
    Récupère tous les produits du product-service et les indexe dans Qdrant.
    Traitement par batch de 64 pour exploiter le batch encoding de sentence-transformers.
    """
    async with httpx.AsyncClient(timeout=30) as client:
        try:
            resp = await client.get(f"{settings.product_service_url}/api/products")
            resp.raise_for_status()
            products = resp.json()
        except Exception as exc:
            logger.warning("Impossible de récupérer les produits pour l'indexation : %s", exc)
            return

    if not products:
        logger.info("Aucun produit à indexer.")
        return

    loop = asyncio.get_event_loop()
    batch_size = 64
    total = 0

    for i in range(0, len(products), batch_size):
        batch = products[i: i + batch_size]
        try:
            await loop.run_in_executor(None, _sync_index_batch, batch)
            total += len(batch)
        except Exception as exc:
            logger.warning("Erreur indexation batch [%d:%d] : %s", i, i + batch_size, exc)

    logger.info("%d / %d produits indexés dans Qdrant.", total, len(products))


# Alias conservé pour le consumer product_catalog (appelé via embed_and_index_product_async)
embed_and_index_product = _sync_embed_and_index
