package com.gloud.document.kafka;


import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.repository.MetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStatusTimeoutHandler {

    private final MetadataRepository metadataRepository;
    private final ConcurrentHashMap<Long, Instant> pendingUploads = new ConcurrentHashMap<>();

    // 업로드할 때 이 메서드 호출
    public void registerPendingUpload(Long metadataId) {
        pendingUploads.put(metadataId, Instant.now());
        log.debug("Registered pending metadataId={} at {}", metadataId, Instant.now());
    }

    // 10초마다 검사
    @Scheduled(fixedDelay = 10000)
    public void checkTimeouts() {
        Instant now = Instant.now();
        for (Map.Entry<Long, Instant> entry : pendingUploads.entrySet()) {
            if (Duration.between(entry.getValue(), now).getSeconds() > 30) {
                handleTimeout(entry.getKey());
                pendingUploads.remove(entry.getKey());
            }
        }
    }

    private void handleTimeout(Long metadataId) {
        metadataRepository.findById(metadataId).ifPresent(metadata -> {
            if (metadata.getProcessingStatus() != ProcessingStatus.COMPLETED) {
                metadata.setProcessingStatus(ProcessingStatus.FAILED);
                metadataRepository.save(metadata);
                log.warn("[Timeout] Metadata ID {} forcibly marked as FAILED", metadataId);
            }
        });
    }

    // Kafka Consumer가 정상적으로 완료 이벤트를 수신했을 때 호출해서 제거하는 메서드도 추가 가능
    public void unregister(Long metadataId) {
        pendingUploads.remove(metadataId);
        log.debug("Unregistered metadataId={} from timeout monitoring", metadataId);
    }
}