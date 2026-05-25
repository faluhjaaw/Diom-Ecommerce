"""MongoDB async via Motor."""
import logging
from motor.motor_asyncio import AsyncIOMotorClient
from app.config import settings

logger = logging.getLogger(__name__)

_client: AsyncIOMotorClient | None = None


def connect_mongo():
    global _client
    _client = AsyncIOMotorClient(settings.MONGODB_URI)
    logger.info("MongoDB connecté.")


def close_mongo():
    if _client:
        _client.close()


def get_collection():
    return _client[settings.MONGODB_DB][settings.MONGODB_COLLECTION]


async def get_all_products() -> list[dict]:
    cursor = get_collection().find({})
    products = []
    async for doc in cursor:
        doc["_id"] = str(doc["_id"])
        products.append(doc)
    return products


async def get_product_by_id(product_id: str) -> dict | None:
    from bson import ObjectId
    try:
        doc = await get_collection().find_one({"_id": ObjectId(product_id)})
        if doc:
            doc["_id"] = str(doc["_id"])
        return doc
    except Exception:
        return None
