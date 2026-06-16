from fastapi import APIRouter
import httpx

router = APIRouter()


@router.post("/verify-image")
async def verify_image(request: dict):
    image_base64 = request.get("image_base64", "")
    product_name = request.get("product_name", "produit")

    if not image_base64:
        return {"approved": True, "reason": "Pas d'image à vérifier"}

    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                "http://host.docker.internal:11434/api/chat",
                json={
                    "model": "llava:7b",
                    "messages": [
                        {
                            "role": "user",
                            "content": f"Cette image montre-t-elle un(e) {product_name} ? Réponds uniquement par OUI ou NON suivi d'une courte raison en français.",
                            "images": [image_base64],
                        }
                    ],
                    "stream": False,
                },
                timeout=30.0,
            )
            data = response.json()
            answer = data.get("message", {}).get("content", "").upper()
            approved = answer.startswith("OUI")
            reason = data.get("message", {}).get("content", "")
            return {"approved": approved, "reason": reason}
    except Exception as e:
        return {"approved": True, "reason": f"Vérification IA indisponible: {str(e)}"}
