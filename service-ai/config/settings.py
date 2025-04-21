from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    jwt_secret_key: str
    jwt_algorithm: str

    kafka_broker: str
    kafka_upload_topic: str
    kafka_upload_recover_topic: str
    kafka_delete_topic: str
    kafka_delete_recover_topic: str
    kafka_consumer_group: str
    kafka_produce_topic: str

    qdrant_host: str
    qdrant_port: int
    qdrant_collection_name: str

    openai_api_key: str
    openai_text_embedding_model: str = "text-embedding-3-large"
    openai_chat_model: str
    openai_classifier_model: str

    bm25_index_path: str

    class Config:
        env_file = ".env"

settings = Settings()