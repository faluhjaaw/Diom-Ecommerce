"""
Script de seed Redis — suggestions d'autocomplete par défaut.
Exécuté automatiquement au démarrage si Redis est vide.
"""
import redis
from pymongo import MongoClient

SUGGESTIONS = [
    ("smartphone", 10),
    ("samsung", 8),
    ("iphone", 8),
    ("apple", 7),
    ("macbook", 6),
    ("casque audio", 6),
    ("sony", 5),
    ("gaming", 5),
    ("ps5", 5),
    ("nintendo switch", 5),
    ("montre connectee", 4),
    ("tablette", 4),
    ("ipad", 4),
    ("laptop", 4),
    ("ecouteurs", 3),
    ("airpods", 3),
    ("chargeur", 3),
    ("batterie", 3),
    ("dell", 3),
    ("garmin", 3),
]

AUTOCOMPLETE_KEY = "search:autocomplete"


def seed_redis(host: str = "proj-redis", port: int = 6379,
               mongo_uri: str = "mongodb://mongodb:27017"):
    r = redis.Redis(host=host, port=port, decode_responses=True)

    if r.exists(AUTOCOMPLETE_KEY):
        print("Redis autocomplete déjà seedé — skip.")
        return

    # Seed requêtes courantes
    for term, score in SUGGESTIONS:
        r.zincrby(AUTOCOMPLETE_KEY, score, term)

    # Seed noms des produits depuis MongoDB
    try:
        client = MongoClient(mongo_uri)
        db = client["transversaleProducts"]
        products = db["products"].find({}, {"name": 1, "brand": 1, "tags": 1})
        for product in products:
            if product.get("name"):
                r.zincrby(AUTOCOMPLETE_KEY, 2, product["name"].lower())
            if product.get("brand"):
                r.zincrby(AUTOCOMPLETE_KEY, 2, product["brand"].lower())
            for tag in product.get("tags", []):
                r.zincrby(AUTOCOMPLETE_KEY, 1, tag.lower())
        client.close()
        print(f"Produits MongoDB indexés dans Redis.")
    except Exception as e:
        print(f"Warning: impossible d'indexer les produits MongoDB : {e}")

    print(f"Redis seedé avec suggestions + produits.")


if __name__ == "__main__":
    seed_redis()
