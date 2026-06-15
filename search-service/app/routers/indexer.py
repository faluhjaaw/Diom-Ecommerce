"""Indexation des produits MongoDB → Qdrant (async)."""
import logging
from fastapi import APIRouter, BackgroundTasks
from app.services.mongo_service import get_all_products
from app.services.embedding_service import embed_batch_async, build_product_text
from qdrant_client.models import Distance, VectorParams
from app.services.qdrant_service import upsert_product, client
from app.config import settings

logger = logging.getLogger(__name__)
router = APIRouter()

BATCH_SIZE = 64


async def index_all_task():
    # Purge et recréation de la collection pour éviter les doublons
    try:
        await client.delete_collection(collection_name=settings.QDRANT_COLLECTION)
        logger.info("Collection '%s' supprimée.", settings.QDRANT_COLLECTION)
    except Exception:
        pass
    await client.create_collection(
        collection_name=settings.QDRANT_COLLECTION,
        vectors_config=VectorParams(size=settings.EMBEDDING_DIM, distance=Distance.COSINE),
    )
    logger.info("Collection '%s' recréée.", settings.QDRANT_COLLECTION)

    products = await get_all_products()
    logger.info("Indexation de %d produits...", len(products))
    count = 0
    for i in range(0, len(products), BATCH_SIZE):
        batch = products[i:i + BATCH_SIZE]
        texts = [build_product_text(p) for p in batch]
        vectors = await embed_batch_async(texts)
        for product, vector in zip(batch, vectors):
            payload = {
                "name": product.get("name"),
                "description": product.get("description"),
                "price": float(product.get("price", 0)),
                "brand": product.get("brand"),
                "stock": product.get("stock"),
                "tags": product.get("tags", []),
                "subCategoryId": product.get("subCategoryId"),
                "vendorId": str(product.get("vendorId", "")),
                "slug": product.get("slug"),
            }
            await upsert_product(product["_id"], vector, payload)
            count += 1
        logger.info("%d / %d indexés", count, len(products))
    logger.info("Indexation terminée : %d produits.", count)


@router.post("/reindex")
async def reindex_all(background_tasks: BackgroundTasks):
    background_tasks.add_task(index_all_task)
    return {"message": "Indexation lancée en arrière-plan"}


@router.get("/status")
async def index_status():
    info = await client.get_collection(settings.QDRANT_COLLECTION)
    return {
        "vectors_count": info.vectors_count,
        "points_count": info.points_count,
        "status": info.status,
    }
