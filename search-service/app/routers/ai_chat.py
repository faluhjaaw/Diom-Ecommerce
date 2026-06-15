"""
Route IA conseils achat.
POST /api/ai/chat → relaie vers Claude API avec contexte produits ShopSen
"""
import logging

import httpx
from fastapi import APIRouter

from app.services.embedding_service import embed_text_async
from app.services.qdrant_service import search_products
from app.services.symspell_service import correct_query

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/chat")
async def ai_chat(request: dict):
    """Endpoint IA conseils achat — relaie vers Claude API"""
    user_message = request.get("message", "")
    history = request.get("history", [])

    # Recherche sémantique de produits pertinents
    GREETINGS = {'hello', 'hi', 'bonjour', 'bonsoir', 'salut', 'hey', 'salam', 'coucou', 'yo'}
    is_greeting = user_message.lower().strip() in GREETINGS or len(user_message.strip()) < 5

    products = []
    product_context = ""
    if not is_greeting:
        try:
            corrected = correct_query(user_message)
            query_vector = await embed_text_async(corrected)
            results = await search_products(query_vector=query_vector, limit=3)
            products = [
                {
                    "id": r.payload.get("mongo_id"),
                    "name": r.payload.get("name"),
                    "description": r.payload.get("description"),
                    "price": r.payload.get("price"),
                    "imageUrls": r.payload.get("imageUrls", []),
                }
                for r in results
            ]
            if products:
                product_context = "\n\nProduits disponibles sur ShopSen:\n" + "\n".join([
                    f"- {p['name']}: {p.get('price', 0):,.0f} FCFA — {(p.get('description') or '')[:80]}"
                    for p in products
                ])
        except Exception as e:
            logger.warning("Erreur recherche produits pour AI chat: %s", e)

    async with httpx.AsyncClient() as client:
        response = await client.post(
            "http://host.docker.internal:11434/api/chat",
            json={
                "model": "llama3.2:1b",
                "messages": [
                    {
                        "role": "system",
                        "content": (
                            "Tu es un assistant shopping pour ShopSen, marketplace sénégalaise. "
                            "Tu réponds UNIQUEMENT en français, max 2-3 phrases courtes. "
                            "Si le message est une salutation (bonjour, hello, hi, etc.), réponds juste par une salutation et demande ce que l'utilisateur cherche. "
                            "Si le message est vague, pose une question précise sur le budget ou le type de produit. "
                            "Sinon recommande des produits du contexte avec leur prix en FCFA. "
                            "Ne recommande jamais de produits si le message ne concerne pas un achat."
                        )
                    },
                    *history,
                    {"role": "user", "content": user_message + product_context}
                ],
                "stream": False
            },
            timeout=30.0,
        )
        data = response.json()
        ai_response = data.get("message", {}).get("content", "Désolé, je n'ai pas pu traiter votre demande.")

    return {"response": ai_response, "products": products[:3]}
