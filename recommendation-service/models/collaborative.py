"""
Module B — Filtrage collaboratif (NMF via scikit-learn).

Remplace implicit/ALS par sklearn.decomposition.NMF :
- wheels pre-compilés, pas de compilation C/Cython requise
- API similaire, résultats comparables pour de petits datasets
"""
import asyncio
import logging
import random
from typing import Any

import numpy as np
import scipy.sparse as sp
from sklearn.decomposition import NMF

from config import settings
from db import get_mongo_db

logger = logging.getLogger(__name__)

INTERACTION_WEIGHTS = {
    "product_viewed": 1,
    "cart_added": 3,
    "order_completed": 5,
}

# ---- État global du modèle ----
_model: NMF | None = None
_user_index: dict[str, int] = {}
_product_index: dict[str, int] = {}
_product_ids: list[str] = []
_user_factors: np.ndarray | None = None
_item_factors: np.ndarray | None = None

_train_lock = asyncio.Lock()


def _sync_fit(matrix: sp.csr_matrix):
    """Entraîne le modèle NMF (CPU-bound, appellé depuis run_in_executor)."""
    model = NMF(n_components=64, max_iter=200, random_state=42)
    user_factors = model.fit_transform(matrix)
    item_factors = model.components_.T  # shape: (n_products, n_components)
    return model, user_factors, item_factors


async def train():
    """Reconstruit la matrice et réentraîne le modèle NMF de façon non-bloquante."""
    global _model, _user_index, _product_index, _product_ids, _user_factors, _item_factors

    db = get_mongo_db()
    cursor = db["interactions"].find({})

    rows, cols, data = [], [], []
    user_map: dict[str, int] = {}
    product_map: dict[str, int] = {}

    async for doc in cursor:
        uid = doc.get("user_id", "")
        pid = doc.get("product_id", "")
        event = doc.get("event_type", "product_viewed")
        weight = INTERACTION_WEIGHTS.get(event, 1)

        if not uid or not pid:
            continue

        u_idx = user_map.setdefault(uid, len(user_map))
        p_idx = product_map.setdefault(pid, len(product_map))
        rows.append(u_idx)
        cols.append(p_idx)
        data.append(float(weight))

    if len(data) < 10:
        logger.info("Pas assez d'interactions pour entraîner le modèle (%d).", len(data))
        return

    n_users = len(user_map)
    n_products = len(product_map)
    matrix = sp.coo_matrix((data, (rows, cols)), shape=(n_users, n_products)).tocsr()

    loop = asyncio.get_event_loop()
    new_model, user_factors, item_factors = await loop.run_in_executor(None, _sync_fit, matrix)

    async with _train_lock:
        _model = new_model
        _user_index = user_map
        _product_index = product_map
        _product_ids = [pid for pid, _ in sorted(product_map.items(), key=lambda x: x[1])]
        _user_factors = user_factors
        _item_factors = item_factors

    logger.info("Modèle NMF entraîné — %d utilisateurs, %d produits.", n_users, n_products)


async def get_interaction_count(user_id: str) -> int:
    db = get_mongo_db()
    return await db["interactions"].count_documents({"user_id": user_id})


async def recommend(user_id: str, limit: int = 10, explore_ratio: float = 0.2) -> list[dict[str, Any]]:
    """Retourne les produits recommandés pour un utilisateur (lecture sous verrou).

    explore_ratio: fraction de résultats aléatoires hors top NMF (diversification).
    """
    async with _train_lock:
        if _model is None or user_id not in _user_index:
            return []

        u_idx = _user_index[user_id]
        user_vec = _user_factors[u_idx]
        item_factors_snap = _item_factors.copy()
        product_ids_snap = list(_product_ids)

    n_exploit = max(1, int(limit * (1 - explore_ratio)))
    n_explore = limit - n_exploit

    def _compute_scores():
        scores = item_factors_snap @ user_vec
        top_indices = np.argsort(scores)[::-1][:n_exploit]
        return [(int(i), float(scores[i])) for i in top_indices]

    loop = asyncio.get_event_loop()
    top = await loop.run_in_executor(None, _compute_scores)

    results = []
    seen_indices = set()
    for idx, score in top:
        if idx < len(product_ids_snap):
            results.append({"productId": product_ids_snap[idx], "score": score})
            seen_indices.add(idx)

    # Exploration: random products outside top NMF
    if n_explore > 0:
        explore_pool = [
            i for i in range(len(product_ids_snap)) if i not in seen_indices
        ]
        sample = random.sample(explore_pool, min(n_explore, len(explore_pool)))
        for idx in sample:
            results.append({"productId": product_ids_snap[idx], "score": 0.0})

    return results
