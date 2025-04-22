package com.gloud.document.processor;

import com.gloud.document.dto.DocumentKafkaMessage;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ContentType;
import com.gloud.document.kafka.DocumentKafkaProducer;
import com.gloud.document.util.TextExtractionUtil;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAsyncProcessor {

    private final DocumentKafkaProducer documentKafkaProducer;

    @Async("taskExecutor")
    public void processUploadAsync(Metadata metadata, ContentType contentType, MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            String text = TextExtractionUtil.extractText(contentType, is);
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            DocumentKafkaMessage message = DocumentKafkaMessage.builder()
                    .metadataId(metadata.getMetadataId())
                    .email(metadata.getUploaderEmail())
                    .originalFilename(metadata.getOriginalFilename())
                    .contentType(contentType.name())
                    .textContent(text)
                    .uploadTime(metadata.getUploadTime().format(formatter))
                    .build();
            documentKafkaProducer.uploadPublish(message);
        } catch (Exception e) {
            log.warn("비동기 텍스트 추출 또는 Kafka 발행 실패: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void processDeleteAsync(List<Long> metadataIds) {
        try {
            documentKafkaProducer.deletePublish(metadataIds);
        } catch (Exception e) {
            log.warn("비동기 Kafka 삭제 발행 실패: {}", e.getMessage());
        }
    }
}