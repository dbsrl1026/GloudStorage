from kafka import KafkaConsumer
from config import settings
from utils.logging_util import LoggingUtil
import json

logger = LoggingUtil.get_logger(__name__)

def consume_dlt():
    consumer = KafkaConsumer(
        settings.kafka_upload_recover_topic,
        settings.kafka_delete_recover_topic,
        bootstrap_servers=[settings.kafka_broker],
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        auto_offset_reset='latest',
        enable_auto_commit=False,
        group_id="gloud-dlt-consumer-group"
    )

    logger.info(f"[DLT] Listening to: {settings.kafka_upload_recover_topic}, {settings.kafka_delete_recover_topic}")

    for message in consumer:
        try:
            topic = message.topic
            if topic == settings.kafka_uploaded_recover_topic:
                logger.warning(f"[DLT] Upload recovery message: {message.value}")
            elif topic == settings.kafka_deleted_recover_topic:
                logger.warning(f"[DLT] Delete recovery message: {message.value}")
            consumer.commit()
        except Exception as e:
            logger.exception(f"[DLT] Error processing message: {e}")
