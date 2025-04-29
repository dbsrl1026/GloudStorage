package com.gloud.document.kafka;

import com.gloud.document.dto.DocumentKafkaMessage;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.repository.MetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentKafkaProducer {

    private final KafkaTemplate<String, DocumentKafkaMessage> kafkaUploadTemplate;
    private final KafkaTemplate<String, List<Long>> kafkaDeleteTemplate;
    private final MetadataRepository metadataRepository;
    private final DocumentStatusTimeoutHandler timeoutHandler; // 추가 의존성


    @Value("${kafka.topics.uploaded}")
    private String UPLOADED_TOPIC;
    @Value("${kafka.topics.deleted}")
    private String DELETED_TOPIC;
    @Value("${kafka.topics.uploaded.recover}")
    private String UPLOADED_RECOVER_TOPIC;
    @Value("${kafka.topics.deleted.recover}")
    private String DELETED_RECOVER_TOPIC;

    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void uploadPublish(DocumentKafkaMessage message) {
        kafkaUploadTemplate.send(UPLOADED_TOPIC, message);
        log.info("Kafka message published: metadataId={}, email={}", message.getMetadataId(), message.getEmail());

        timeoutHandler.registerPendingUpload(message.getMetadataId());
    }

    @Recover
    public void uploadRecover(Exception e, DocumentKafkaMessage message) {
        log.error("Kafka publish failed after retries: metadataId={}, reason={}", message.getMetadataId(), e.getMessage());
        Optional<Metadata> optional = metadataRepository.findById(message.getMetadataId());
        if (optional.isEmpty()) {
            log.warn("Metadata not found for ID: {}", message.getMetadataId());
            return;
        }
        Metadata metadata = optional.get();
        metadata.setProcessingStatus(ProcessingStatus.FAILED);
        metadataRepository.save(metadata);
        log.info("Metadata ID {} updated to status: {}", metadata.getMetadataId(), ProcessingStatus.FAILED);

        try {
            kafkaUploadTemplate.send(UPLOADED_RECOVER_TOPIC, message);
            log.info("Sent to DLT topic {}: metadataId={}", UPLOADED_RECOVER_TOPIC, message.getMetadataId());
            timeoutHandler.registerPendingUpload(message.getMetadataId());
        } catch (Exception ex) {
            log.error("Failed to send to DLT topic {}: {}", UPLOADED_RECOVER_TOPIC, ex.getMessage());
        }
    }

    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void deletePublish(List<Long> metadatas) {
        log.info("Publishing document.deleted event: {}", metadatas);
        kafkaDeleteTemplate.send(DELETED_TOPIC, metadatas);
    }

    @Recover
    public void deleteRecover(Exception e, List<Long> metadatas) {
        log.error("Kafka deletePublish failed after retries. metadataIds={}, reason={}", metadatas, e.getMessage());
        try {
            kafkaDeleteTemplate.send(DELETED_RECOVER_TOPIC, metadatas);
            log.info("Sent to DLT topic {}: metadataIds={}", DELETED_RECOVER_TOPIC, metadatas);
        } catch (Exception ex) {
            log.error("Failed to send to DLT topic {}: {}", DELETED_RECOVER_TOPIC, ex.getMessage());
        }
    }
}
