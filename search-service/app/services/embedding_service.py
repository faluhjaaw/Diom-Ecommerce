"""
Embedding service — async-safe via run_in_executor.
Le modèle sentence-transformers est CPU-bound, on l'exécute dans un thread pool
pour ne pas bloquer la boucle asyncio.
"""
import asyncio
import logging
from sentence_transformers import SentenceTransformer
from app.config import settings

logger = logging.getLogger(__name__)
_model: SentenceTransformer | None = None


def load_model():
    global _model
    logger.info("Chargement du modèle '%s'...", settings.EMBEDDING_MODEL)
    _model = SentenceTransformer(settings.EMBEDDING_MODEL)
    logger.info("Modèle chargé.")


def _sync_embed(text: str) -> list[float]:
    if _model is None:
        raise RuntimeError("Modèle non chargé")
    return _model.encode(text, normalize_embeddings=True).tolist()


def _sync_embed_batch(texts: list[str]) -> list[list[float]]:
    if _model is None:
        raise RuntimeError("Modèle non chargé")
    return _model.encode(texts, normalize_embeddings=True, batch_size=64).tolist()


async def embed_text_async(text: str) -> list[float]:
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(None, _sync_embed, text)


async def embed_batch_async(texts: list[str]) -> list[list[float]]:
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(None, _sync_embed_batch, texts)


def build_product_text(product: dict) -> str:
    parts = [
        product.get("name", ""),
        product.get("description", ""),
        product.get("brand", ""),
        " ".join(product.get("tags", []) or []),
    ]
    return " | ".join(p for p in parts if p)
