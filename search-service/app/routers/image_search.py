from fastapi import APIRouter, File, UploadFile, HTTPException
import httpx
import base64
import os

router = APIRouter()

@router.post("/search/image")
async def search_by_image(file: UploadFile = File(...), limit: int = 6):
    """Recherche de produits par image via Claude Vision + Qdrant"""

    image_bytes = await file.read()
    if len(image_bytes) > 5 * 1024 * 1024:
        raise HTTPException(status_code=400, detail="Image trop grande (max 5MB)")

    image_b64 = base64.standard_b64encode(image_bytes).decode("utf-8")
    media_type = file.content_type or "image/jpeg"

    anthropic_key = os.getenv("ANTHROPIC_API_KEY", "")
    if not anthropic_key:
        raise HTTPException(status_code=503, detail="Service IA indisponible")

    async with httpx.AsyncClient() as client:
        response = await client.post(
            "https://api.anthropic.com/v1/messages",
            headers={
                "x-api-key": anthropic_key,
                "anthropic-version": "2023-06-01",
                "Content-Type": "application/json",
            },
            json={
                "model": "claude-haiku-4-5-20251001",
                "max_tokens": 200,
                "messages": [{
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "source": {
                                "type": "base64",
                                "media_type": media_type,
                                "data": image_b64,
                            },
                        },
                        {
                            "type": "text",
                            "text": "Décris ce produit en 5-10 mots clés pour une recherche e-commerce. Réponds uniquement avec les mots clés séparés par des espaces, en français.",
                        },
                    ],
                }],
            },
            timeout=30.0,
        )

        data = response.json()
        search_query = data.get("content", [{}])[0].get("text", "").strip()

    if not search_query:
        raise HTTPException(status_code=500, detail="Impossible d'analyser l'image")

    from app.services.qdrant_service import search_products
    from app.services.embedding_service import embed_text_async

    query_vector = await embed_text_async(search_query)
    results = await search_products(query_vector=query_vector, limit=limit)

    return {
        "query_detected": search_query,
        "results": results,
        "total": len(results),
    }
