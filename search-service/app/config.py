from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # App
    app_name: str = "search-service"
    port: int = 8088

    # MongoDB
    MONGODB_URI: str = "mongodb://localhost:27017"
    MONGODB_DB: str = "transversaleProducts"
    MONGODB_COLLECTION: str = "products"

    # Qdrant
    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333
    QDRANT_COLLECTION: str = "search_products"
    EMBEDDING_MODEL: str = "paraphrase-multilingual-MiniLM-L12-v2"
    EMBEDDING_DIM: int = 384

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379
    autocomplete_ttl_seconds: int = 3600      # 1h
    autocomplete_top_k: int = 200             # nb de suggestions gardées par préfixe

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_group_id: str = "search-service-group"
    kafka_enabled: bool = True

    # Eureka
    eureka_server: str = "http://localhost:8761/eureka"
    eureka_enabled: bool = True
    eureka_instance_host: str = "localhost"

    class Config:
        env_file = ".env"

settings = Settings()
