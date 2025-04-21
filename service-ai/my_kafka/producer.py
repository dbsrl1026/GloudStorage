from kafka import KafkaProducer
import json
from config import settings
from tenacity import retry, wait_exponential, stop_after_attempt, RetryError
from utils.logging_util import LoggingUtil

logger = LoggingUtil.get_logger(__name__)

producer = KafkaProducer(
    bootstrap_servers=settings.kafka_broker,
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
)

@retry(wait=wait_exponential(multiplier=2, min=2, max=10), stop=stop_after_attempt(3))
def send_status_message(metadata_id: int, status: str):
    message = {
        "metadataId": metadata_id,
        "status": status
    }
    producer.send(settings.kafka_produce_topic, value=message)
    logger.info(f"[Kafka] Sent {status} for document {metadata_id}")

def safe_send_status(metadata_id: int, status: str):
    try:
        send_status_message(metadata_id, status)
    except RetryError as e:
        logger.error(f"[Kafka] All retries failed for {metadata_id}")
        logger.error(f"[Recover] Consider persisting to DB or alerting: {{'metadataId': metadata_id, 'status': status}}")