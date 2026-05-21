"""
Script de seed — indexe des produits de test dans Qdrant.

Usage :
    python seed_products.py

Prérequis : le service doit être lancé (Qdrant accessible selon config.py / .env).
"""
import asyncio
import sys
import os

# Ajoute le répertoire courant au path pour les imports locaux
sys.path.insert(0, os.path.dirname(__file__))

from config import settings
from db import connect_redis, connect_qdrant
from models.embedder import load_model, _sync_index_batch

PRODUCTS = [
    # ── Smartphones ────────────────────────────────────────────────────────────
    {"id": "prod-001", "name": "iPhone 15 Pro", "description": "Smartphone Apple haut de gamme avec puce A17 Pro, caméra 48MP et écran ProMotion 120Hz.", "price": 1199.0, "brand": "Apple", "subCategoryId": "cat-smartphones", "tags": ["apple", "ios", "5g", "pro"], "rating": 4.8, "slug": "iphone-15-pro", "imageUrls": []},
    {"id": "prod-002", "name": "Samsung Galaxy S24 Ultra", "description": "Flagship Samsung avec S Pen intégré, zoom 200x et écran Dynamic AMOLED 6.8 pouces.", "price": 1299.0, "brand": "Samsung", "subCategoryId": "cat-smartphones", "tags": ["samsung", "android", "5g", "s-pen", "ultra"], "rating": 4.7, "slug": "samsung-galaxy-s24-ultra", "imageUrls": []},
    {"id": "prod-003", "name": "Google Pixel 8 Pro", "description": "Smartphone Google avec IA avancée, caméra Tensor G3 et 7 ans de mises à jour Android.", "price": 999.0, "brand": "Google", "subCategoryId": "cat-smartphones", "tags": ["google", "android", "pixel", "ia", "5g"], "rating": 4.6, "slug": "google-pixel-8-pro", "imageUrls": []},
    {"id": "prod-004", "name": "Xiaomi 14 Pro", "description": "Smartphone Xiaomi avec Snapdragon 8 Gen 3, charge 120W et optique Leica.", "price": 899.0, "brand": "Xiaomi", "subCategoryId": "cat-smartphones", "tags": ["xiaomi", "android", "leica", "snapdragon", "5g"], "rating": 4.5, "slug": "xiaomi-14-pro", "imageUrls": []},
    {"id": "prod-005", "name": "OnePlus 12", "description": "Smartphone performant avec Snapdragon 8 Gen 3, charge Supervooc 100W et écran LTPO 120Hz.", "price": 799.0, "brand": "OnePlus", "subCategoryId": "cat-smartphones", "tags": ["oneplus", "android", "snapdragon", "5g", "fast-charge"], "rating": 4.5, "slug": "oneplus-12", "imageUrls": []},
    {"id": "prod-006", "name": "iPhone 15", "description": "iPhone 15 avec Dynamic Island, puce A16 Bionic et port USB-C.", "price": 899.0, "brand": "Apple", "subCategoryId": "cat-smartphones", "tags": ["apple", "ios", "5g", "usb-c"], "rating": 4.6, "slug": "iphone-15", "imageUrls": []},
    {"id": "prod-007", "name": "Samsung Galaxy A55", "description": "Smartphone milieu de gamme Samsung avec écran Super AMOLED 6.6 pouces et 50MP.", "price": 449.0, "brand": "Samsung", "subCategoryId": "cat-smartphones", "tags": ["samsung", "android", "5g", "milieu-de-gamme"], "rating": 4.3, "slug": "samsung-galaxy-a55", "imageUrls": []},

    # ── Laptops ────────────────────────────────────────────────────────────────
    {"id": "prod-011", "name": "MacBook Pro 14 M3 Pro", "description": "Ordinateur portable Apple avec puce M3 Pro, écran Liquid Retina XDR et autonomie 18h.", "price": 2199.0, "brand": "Apple", "subCategoryId": "cat-laptops", "tags": ["apple", "macos", "m3", "pro", "retina"], "rating": 4.9, "slug": "macbook-pro-14-m3", "imageUrls": []},
    {"id": "prod-012", "name": "Dell XPS 15", "description": "Laptop premium Dell avec écran OLED 3.5K, Intel Core i9 et GPU NVIDIA RTX 4070.", "price": 2499.0, "brand": "Dell", "subCategoryId": "cat-laptops", "tags": ["dell", "windows", "oled", "intel", "rtx", "pro"], "rating": 4.7, "slug": "dell-xps-15", "imageUrls": []},
    {"id": "prod-013", "name": "Lenovo ThinkPad X1 Carbon", "description": "Ultrabook professionnel Lenovo ultra-léger, clavier ThinkPad légendaire et certification MIL-SPEC.", "price": 1799.0, "brand": "Lenovo", "subCategoryId": "cat-laptops", "tags": ["lenovo", "windows", "professionnel", "ultrabook", "intel"], "rating": 4.6, "slug": "lenovo-thinkpad-x1-carbon", "imageUrls": []},
    {"id": "prod-014", "name": "ASUS ROG Zephyrus G16", "description": "Laptop gaming ASUS avec AMD Ryzen 9, RTX 4080 et écran QHD 240Hz.", "price": 2299.0, "brand": "ASUS", "subCategoryId": "cat-laptops", "tags": ["asus", "rog", "gaming", "amd", "rtx", "windows"], "rating": 4.7, "slug": "asus-rog-zephyrus-g16", "imageUrls": []},
    {"id": "prod-015", "name": "MacBook Air 15 M3", "description": "Laptop fin et léger Apple avec puce M3, grand écran 15 pouces et Liquid Retina.", "price": 1499.0, "brand": "Apple", "subCategoryId": "cat-laptops", "tags": ["apple", "macos", "m3", "air", "retina", "leger"], "rating": 4.8, "slug": "macbook-air-15-m3", "imageUrls": []},
    {"id": "prod-016", "name": "HP Spectre x360 14", "description": "2-en-1 premium HP avec écran OLED tactile, Intel Core Ultra et stylet inclus.", "price": 1699.0, "brand": "HP", "subCategoryId": "cat-laptops", "tags": ["hp", "windows", "2-en-1", "oled", "tactile", "intel"], "rating": 4.5, "slug": "hp-spectre-x360", "imageUrls": []},

    # ── Casques audio ──────────────────────────────────────────────────────────
    {"id": "prod-021", "name": "Sony WH-1000XM5", "description": "Casque audio Sony avec meilleure réduction de bruit active, 30h d'autonomie et son Hi-Res.", "price": 349.0, "brand": "Sony", "subCategoryId": "cat-audio", "tags": ["sony", "anc", "bluetooth", "hi-res", "casque"], "rating": 4.8, "slug": "sony-wh-1000xm5", "imageUrls": []},
    {"id": "prod-022", "name": "Apple AirPods Pro 2", "description": "Écouteurs Apple avec ANC adaptatif, audio spatial personnalisé et puce H2.", "price": 279.0, "brand": "Apple", "subCategoryId": "cat-audio", "tags": ["apple", "airpods", "anc", "bluetooth", "earbuds"], "rating": 4.7, "slug": "airpods-pro-2", "imageUrls": []},
    {"id": "prod-023", "name": "Bose QuietComfort 45", "description": "Casque Bose avec réduction de bruit légendaire, confort supérieur et 24h autonomie.", "price": 329.0, "brand": "Bose", "subCategoryId": "cat-audio", "tags": ["bose", "anc", "bluetooth", "casque", "confort"], "rating": 4.7, "slug": "bose-quietcomfort-45", "imageUrls": []},
    {"id": "prod-024", "name": "Samsung Galaxy Buds3 Pro", "description": "Écouteurs Samsung avec ANC intelligent, son 360 Audio et design blade.", "price": 229.0, "brand": "Samsung", "subCategoryId": "cat-audio", "tags": ["samsung", "anc", "bluetooth", "earbuds", "360-audio"], "rating": 4.4, "slug": "samsung-galaxy-buds3-pro", "imageUrls": []},
    {"id": "prod-025", "name": "Jabra Evolve2 85", "description": "Casque professionnel certifié Microsoft Teams avec ANC avancé et 37h autonomie.", "price": 499.0, "brand": "Jabra", "subCategoryId": "cat-audio", "tags": ["jabra", "professionnel", "anc", "bluetooth", "teams"], "rating": 4.6, "slug": "jabra-evolve2-85", "imageUrls": []},

    # ── Montres connectées ────────────────────────────────────────────────────
    {"id": "prod-031", "name": "Apple Watch Series 9", "description": "Montre connectée Apple avec puce S9, double tap et Siri on-device.", "price": 399.0, "brand": "Apple", "subCategoryId": "cat-wearables", "tags": ["apple", "watch", "ios", "sante", "sport"], "rating": 4.8, "slug": "apple-watch-series-9", "imageUrls": []},
    {"id": "prod-032", "name": "Samsung Galaxy Watch 7", "description": "Montre connectée Samsung avec suivi santé avancé, BioActive Sensor et Wear OS.", "price": 299.0, "brand": "Samsung", "subCategoryId": "cat-wearables", "tags": ["samsung", "watch", "android", "sante", "sport"], "rating": 4.5, "slug": "samsung-galaxy-watch-7", "imageUrls": []},
    {"id": "prod-033", "name": "Garmin Fenix 7 Pro", "description": "Montre GPS premium Garmin pour sports outdoor, cartographie multi-bande et 18 jours autonomie.", "price": 799.0, "brand": "Garmin", "subCategoryId": "cat-wearables", "tags": ["garmin", "gps", "sport", "outdoor", "running"], "rating": 4.8, "slug": "garmin-fenix-7-pro", "imageUrls": []},
    {"id": "prod-034", "name": "Fitbit Charge 6", "description": "Bracelet fitness Fitbit avec ECG, capteur SpO2 et intégration Google Maps.", "price": 159.0, "brand": "Fitbit", "subCategoryId": "cat-wearables", "tags": ["fitbit", "fitness", "sante", "ecg", "sport"], "rating": 4.3, "slug": "fitbit-charge-6", "imageUrls": []},

    # ── Tablettes ─────────────────────────────────────────────────────────────
    {"id": "prod-041", "name": "iPad Pro 13 M4", "description": "Tablette Apple ultra-fine avec puce M4, écran OLED tandem et Apple Pencil Pro.", "price": 1299.0, "brand": "Apple", "subCategoryId": "cat-tablets", "tags": ["apple", "ipad", "m4", "oled", "pro", "stylet"], "rating": 4.9, "slug": "ipad-pro-13-m4", "imageUrls": []},
    {"id": "prod-042", "name": "Samsung Galaxy Tab S9 Ultra", "description": "Tablette Samsung 14.6 pouces avec S Pen, écran AMOLED 120Hz et Snapdragon 8 Gen 2.", "price": 1199.0, "brand": "Samsung", "subCategoryId": "cat-tablets", "tags": ["samsung", "android", "s-pen", "amoled", "grand-ecran"], "rating": 4.7, "slug": "samsung-galaxy-tab-s9-ultra", "imageUrls": []},
    {"id": "prod-043", "name": "Microsoft Surface Pro 10", "description": "Tablette PC Microsoft avec processeur Intel Core Ultra, stylet Surface Slim Pen 2 et Windows 11.", "price": 1399.0, "brand": "Microsoft", "subCategoryId": "cat-tablets", "tags": ["microsoft", "windows", "2-en-1", "stylet", "intel"], "rating": 4.5, "slug": "surface-pro-10", "imageUrls": []},

    # ── Accessoires ───────────────────────────────────────────────────────────
    {"id": "prod-051", "name": "Chargeur MagSafe 15W Apple", "description": "Chargeur magnétique Apple MagSafe 15W pour iPhone 12 et plus.", "price": 39.0, "brand": "Apple", "subCategoryId": "cat-accessories", "tags": ["apple", "magsafe", "chargeur", "sans-fil"], "rating": 4.5, "slug": "magsafe-charger-15w", "imageUrls": []},
    {"id": "prod-052", "name": "Coque iPhone 15 Pro en silicone Apple", "description": "Coque officielle Apple en silicone avec MagSafe pour iPhone 15 Pro.", "price": 59.0, "brand": "Apple", "subCategoryId": "cat-accessories", "tags": ["apple", "coque", "magsafe", "silicone", "iphone"], "rating": 4.4, "slug": "apple-silicone-case-iphone15pro", "imageUrls": []},
    {"id": "prod-053", "name": "Hub USB-C 7-en-1 Anker", "description": "Hub multiport Anker avec HDMI 4K, 3×USB-A, USB-C PD 100W et lecteur carte SD.", "price": 49.0, "brand": "Anker", "subCategoryId": "cat-accessories", "tags": ["anker", "usb-c", "hub", "hdmi", "multiport"], "rating": 4.6, "slug": "anker-hub-usbc-7en1", "imageUrls": []},
    {"id": "prod-054", "name": "Batterie externe Anker 26800mAh", "description": "Batterie portable haute capacité Anker avec charge rapide 65W et double port USB-C.", "price": 79.0, "brand": "Anker", "subCategoryId": "cat-accessories", "tags": ["anker", "batterie", "powerbank", "usb-c", "charge-rapide"], "rating": 4.7, "slug": "anker-powerbank-26800", "imageUrls": []},
    {"id": "prod-055", "name": "Clavier Apple Magic Keyboard Touch ID", "description": "Clavier sans fil Apple avec Touch ID, compatible Mac et iPad.", "price": 129.0, "brand": "Apple", "subCategoryId": "cat-accessories", "tags": ["apple", "clavier", "touch-id", "bluetooth", "mac"], "rating": 4.6, "slug": "apple-magic-keyboard-touchid", "imageUrls": []},

    # ── Gaming ────────────────────────────────────────────────────────────────
    {"id": "prod-061", "name": "PlayStation 5 Slim", "description": "Console Sony PS5 Slim avec lecteur de disque, 1 To SSD et DualSense.", "price": 449.0, "brand": "Sony", "subCategoryId": "cat-gaming", "tags": ["sony", "ps5", "console", "gaming", "4k"], "rating": 4.8, "slug": "ps5-slim", "imageUrls": []},
    {"id": "prod-062", "name": "Xbox Series X", "description": "Console Microsoft avec 1 To SSD NVMe, 120fps 4K et rétrocompatibilité totale.", "price": 499.0, "brand": "Microsoft", "subCategoryId": "cat-gaming", "tags": ["microsoft", "xbox", "console", "gaming", "4k"], "rating": 4.7, "slug": "xbox-series-x", "imageUrls": []},
    {"id": "prod-063", "name": "Nintendo Switch OLED", "description": "Console hybride Nintendo avec écran OLED 7 pouces, dock amélioré et 64 Go.", "price": 349.0, "brand": "Nintendo", "subCategoryId": "cat-gaming", "tags": ["nintendo", "switch", "oled", "portable", "hybride"], "rating": 4.7, "slug": "nintendo-switch-oled", "imageUrls": []},
    {"id": "prod-064", "name": "Manette DualSense Edge PS5", "description": "Manette professionnelle Sony PS5 avec sticks remplaçables et profils personnalisables.", "price": 239.0, "brand": "Sony", "subCategoryId": "cat-gaming", "tags": ["sony", "ps5", "manette", "pro", "gaming"], "rating": 4.6, "slug": "dualsense-edge", "imageUrls": []},
    {"id": "prod-065", "name": "Razer DeathAdder V3 Pro", "description": "Souris gaming sans fil Razer avec capteur Focus Pro 30K, 90h autonomie et 59g.", "price": 159.0, "brand": "Razer", "subCategoryId": "cat-gaming", "tags": ["razer", "souris", "gaming", "sans-fil", "leger"], "rating": 4.7, "slug": "razer-deathadder-v3-pro", "imageUrls": []},
]


async def main():
    print(f"Connexion Qdrant ({settings.qdrant_mode})...")
    connect_qdrant()

    print("Chargement modèle embedding...")
    load_model()

    print(f"Indexation de {len(PRODUCTS)} produits...")
    batch_size = 16
    total = 0
    for i in range(0, len(PRODUCTS), batch_size):
        batch = PRODUCTS[i:i + batch_size]
        _sync_index_batch(batch)
        total += len(batch)
        print(f"  {total}/{len(PRODUCTS)} indexés")

    print(f"Done — {total} produits indexés dans Qdrant (collection: {settings.qdrant_collection}).")


if __name__ == "__main__":
    asyncio.run(main())
