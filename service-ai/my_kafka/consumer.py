from kafka import KafkaConsumer
import json
from dto import DocumentKafkaMessage
from vector.embed_qdrant import index_text_to_qdrant
from vector.delete_qdrant import remove_from_qdrant_and_bm25
from config import settings
from utils.logging_util import LoggingUtil

logger = LoggingUtil.get_logger(__name__)
# kafka_upload_recover_topic, kafka_delete_recover_topic
def consume_kafka():
    indexed_documents = set()
    consumer = KafkaConsumer(
        settings.kafka_upload_topic,
        settings.kafka_delete_topic,
        bootstrap_servers=settings.kafka_broker.split(","),
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        auto_offset_reset='latest',
        enable_auto_commit=False,
        # Kafka consumer group to allow horizontal scaling (e.g., 3 instances)
        group_id=settings.kafka_consumer_group,
    )

    logger.info(f"Listening to Kafka topics: {settings.kafka_upload_topic}, {settings.kafka_delete_topic}")

    for message in consumer:
        try:
            topic = message.topic

            if topic == settings.kafka_upload_topic:
                msg = DocumentKafkaMessage(**message.value)
                logger.info(f"[Kafka] Received: {msg.originalFilename} from {msg.email}")

                if msg.metadataId in indexed_documents:
                    logger.info(f"[Kafka] Skipping duplicate document ID: {msg.metadataId}")
                    continue
                indexed_documents.add(msg.metadataId)

                metadata = {
                    "document_id": msg.metadataId,
                    "email": msg.email,
                    "filename": msg.originalFilename,
                    "upload_time": str(msg.uploadTime),
                }

                index_text_to_qdrant(msg.textContent, metadata)
                logger.info(f"[Qdrant] Document {msg.metadataId} indexed.")

            elif topic == settings.kafka_delete_topic:
                metadata_ids = message.value  # expecting List[Long]
                logger.info(f"[Kafka] Deletion request for metadata IDs: {metadata_ids}")
                for metadata_id in metadata_ids:
                    remove_from_qdrant_and_bm25(metadata_id)

            consumer.commit()

        except Exception as e:
            logger.error(f"[Error] Failed to process message from topic {message.topic}: {e}")