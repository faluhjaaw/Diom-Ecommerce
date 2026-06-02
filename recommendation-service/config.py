import logging

from pydantic_settings import BaseSettings

logger = logging.getLogger(__name__)


class Settings(BaseSettings):
    # App
    app_name: str = "recommendation-service"
    port: int = 8090

    # MongoDB
    mongo_uri: str = "mongodb://localhost:27017"
    mongo_db: str = "transversaleRecommendations"

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379

    # Qdrant
    qdrant_host: str = "localhost"
    qdrant_port: int = 6333
    qdrant_collection: str = "products"
    embedding_dim: int = 384

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_group_id: str = "recommendation-service-group"
    kafka_enabled: bool = False

    # Product service (pour l'indexation initiale)
    product_service_url: str = "http://localhost:8082"

    # Qdrant mode : "server" (défaut) ou "local" (embedded, pas de serveur requis)
    qdrant_mode: str = "server"
    qdrant_local_path: str = "./qdrant_data"

    # Eureka
    eureka_server: str = "http://localhost:8761/eureka"
    eureka_enabled: bool = True
    eureka_instance_host: str = "localhost"  # hostname local (Docker: surcharger via .env)

    # Hybrid scoring weights — ajustés dynamiquement selon le profil utilisateur
    alpha_semantic: float = 0.3
    beta_collaborative: float = 0.4
    gamma_popularity: float = 0.3

    # Cold-start threshold (nb d'interactions minimum pour passer en collaboratif)
    cold_start_threshold: int = 5

    # TTL cache recommandations (15 min) — séparé du TTL popularité
    redis_ttl_seconds: int = 900
    # TTL cache popularité (1h — calé sur le scheduler)
    redis_popularity_ttl_seconds: int = 3600

    # Clé API pour les endpoints admin (à surcharger via variable d'env en prod)
    admin_api_key: str = "change-me-in-production"

    # TTL MongoDB interactions (90 jours en secondes)
    interactions_ttl_days: int = 90

    class Config:
        env_file = ".env"


settings = Settings()

if settings.admin_api_key == "change-me-in-production":
    logger.warning(
        "ADMIN_API_KEY utilise la valeur par défaut — définir ADMIN_API_KEY en production !"
    )
