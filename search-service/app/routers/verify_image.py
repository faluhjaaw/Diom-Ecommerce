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
                            "content": f"""Tu es un modérateur pour une marketplace. Analyse cette image.

Le vendeur déclare vendre : "{product_name}"

Réponds REFUSE si :
- L'image ne montre PAS un objet/produit physique
- L'image montre une personne, un paysage, ou du texte uniquement
- L'image est floue ou illisible
- L'objet visible est COMPLETEMENT différent de "{product_name}" (ex: une voiture pour un téléphone)

Réponds APPROUVE si :
- L'image montre un produit physique lié à "{product_name}" ou de la même catégorie
- L'image est claire et de bonne qualité

Commence par APPROUVE ou REFUSE puis une courte explication.""",
                            "images": [image_base64]
                        }
                    ],
                    "stream": False
                },
                timeout=60.0,
            )
            data = response.json()
            answer = data.get("message", {}).get("content", "")
            approved = not answer.upper().startswith("REFUSE")
            reason = answer.replace("APPROUVE", "").replace("REFUSE", "").strip(" :-\n")
            return {"approved": approved, "reason": reason}
    except Exception as e:
        return {"approved": True, "reason": f"Vérification IA indisponible"}
