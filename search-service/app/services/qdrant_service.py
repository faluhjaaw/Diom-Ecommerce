from qdrant_client import AsyncQdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct
from app.config import settings

client = AsyncQdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)


async def init_collection():
    """Crée la collection Qdrant si elle n'existe pas encore."""
    collections = await client.get_collections()
    names = [c.name for c in collections.collections]
    if settings.QDRANT_COLLECTION not in names:
        await client.create_collection(
            collection_name=settings.QDRANT_COLLECTION,
            vectors_config=VectorParams(
                size=settings.EMBEDDING_DIM,
                distance=Distance.COSINE
            )
        )
        print(f"✅ Collection '{settings.QDRANT_COLLECTION}' créée dans Qdrant")
    else:
        print(f"ℹ️  Collection '{settings.QDRANT_COLLECTION}' déjà existante")


async def upsert_product(product_id: str, vector: list[float], payload: dict):
    """Insère ou met à jour un produit dans Qdrant."""
    await client.upsert(
        collection_name=settings.QDRANT_COLLECTION,
        points=[
            PointStruct(
                id=abs(hash(product_id)) % (2**63),
                vector=vector,
                payload={**payload, "mongo_id": product_id}
            )
        ]
    )


async def search_products(query_vector: list[float], limit: int = 10, filters: dict = None):
    """Recherche les produits les plus proches du vecteur requête."""
    from qdrant_client.models import Filter, FieldCondition, Range, MatchValue

    qdrant_filter = None

    if filters:
        conditions = []
        if "min_price" in filters or "max_price" in filters:
            conditions.append(
                FieldCondition(
                    key="price",
                    range=Range(
                        gte=filters.get("min_price"),
                        lte=filters.get("max_price")
                    )
                )
            )
        if "category" in filters:
            conditions.append(
                FieldCondition(
                    key="subCategoryId",
                    match=MatchValue(value=filters["category"])
                )
            )
        if conditions:
            qdrant_filter = Filter(must=conditions)

    results = await client.query_points(
        collection_name=settings.QDRANT_COLLECTION,
        query=query_vector,
        limit=limit,
        query_filter=qdrant_filter,
        with_payload=True
    )
    return results.points
