package com.gloud.document.service;

import com.gloud.document.dto.*;
import com.gloud.document.entity.Directory;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ContentType;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.kafka.DocumentKafkaProducer;

import com.gloud.document.processor.DocumentAsyncProcessor;
import com.gloud.document.repository.DirectoryRepository;
import com.gloud.document.repository.MetadataRepository;
import com.gloud.document.util.DocumentToPdfConverter;
import com.gloud.document.util.TextExtractionUtil;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final MetadataRepository metadataRepository;
    private final DirectoryRepository directoryRepository;
    private final DocumentKafkaProducer documentKafkaProducer;
    private final DocumentAsyncProcessor documentAsyncProcessor;


    private static final int MAX_UPLOAD_COUNT = 20;

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx != -1 && idx < filename.length() - 1) ? filename.substring(idx + 1) : null;
    }

    @Override
    public String uploadFile(MultipartFile file, Long directoryId, String email) {
        Directory directory = null;
        if (directoryId != null) {
            directory = directoryRepository.findById(directoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Directory not found"));
        }
        LocalDate now = LocalDate.now();
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
//        String objectName = String.format("documents/%s/%d/%02d/%s", email, now.getYear(), now.getMonthValue(), fileName);
        String objectName = String.format("documents/%s/%d/%s", email, directoryId, fileName);
        File previewPdf = null;
        String previewObjectName = null;

        try (InputStream is = file.getInputStream()) {
            // Validate content type and extension
            String extension = getExtension(file.getOriginalFilename());
            ContentType contentType = ContentType.fromExtension(extension)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported file extension: " + extension));
            String contentTypeHeader = contentType.getMimeType();

            if (contentTypeHeader == null || extension == null) {
                throw new IllegalArgumentException("Invalid file: content type or extension is missing.");
            }

            // 1. Upload original to S3
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .contentType(contentTypeHeader)
                    .build(), RequestBody.fromInputStream(is, file.getSize()));

            // 2. Convert to preview PDF
            previewPdf = DocumentToPdfConverter.convertToPdf(contentType, file.getInputStream(), file.getOriginalFilename());
            previewObjectName = objectName.replaceFirst("documents/", "previews/").replaceAll("\\.[^.]+$", ".pdf");

            try (InputStream previewIs = new FileInputStream(previewPdf)) {
                s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(previewObjectName)
                        .contentType("application/pdf")
                        .build(), RequestBody.fromInputStream(previewIs, previewPdf.length()));
            }

            // 3. Save metadata
            Metadata metadata = Metadata.builder()
                    .originalFilename(file.getOriginalFilename())
                    .filePath(objectName)
                    .previewPath(previewObjectName)
                    .contentType(contentType)
                    .directory(directory)
                    .uploaderEmail(email)
                    .processingStatus(ProcessingStatus.PROCESSING)
                    .fileSize(file.getSize() / 1024.0)
                    .build();

            metadataRepository.save(metadata);
            documentAsyncProcessor.processUploadAsync(metadata, contentType, file);
            return objectName;
        } catch (Exception e) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .build());
                if (previewObjectName != null) {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(previewObjectName)
                            .build());
                }
            } catch (Exception removeEx) {
                e.addSuppressed(removeEx);
            }

            throw new RuntimeException("Upload failed. Rolled back both original and preview uploads.", e);
        } finally {
            if (previewPdf != null && previewPdf.exists()) {
                previewPdf.delete();
            }
        }
    }

    @Override
    public List<UploadResult> uploadMultipleFiles(List<MultipartFile> files, Long directoryId, String email) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided.");
        }

        if (files.size() > MAX_UPLOAD_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many files. Max allowed: " + MAX_UPLOAD_COUNT);
        }

        List<UploadResult> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String path = uploadFile(file, directoryId, email);
                uploaded.add(UploadResult.builder()
                        .originalFilename(file.getOriginalFilename())
                        .success(true)
                        .filePath(path)
                        .build());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                uploaded.add(UploadResult.builder()
                        .originalFilename(file.getOriginalFilename())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }
        return uploaded;
    }


    @Override
    public Page<MetadataDto> getDocuments(String email, int page, int size, String sortBy, String direction,
                                          ProcessingStatus status, String contentType,
                                          LocalDate startDate, LocalDate endDate) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                direction.equalsIgnoreCase("desc") ?
                        Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()
        );

        Page<Metadata> metadataPage = metadataRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add((cb.equal(root.get("uploaderEmail"), email)));
            if (status != null) {
                predicates.add(cb.equal(root.get("processingStatus"), status));
            }

            if (contentType != null) {
                predicates.add(cb.equal(root.get("contentType"), ContentType.fromExtension(contentType)));
            }

            if (startDate != null && endDate != null) {
                predicates.add(cb.between(
                        root.get("uploadTime"),
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return metadataPage.map(MetadataDto::fromEntity);
    }

    @Override
    public void deleteDocument(Long metadataId, String email) {
        Metadata metadata = metadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find metadata with id: " + metadataId));

        if (!metadata.getUploaderEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access");
        }

        String objectName = metadata.getFilePath(); // 또는 newFilename

        try {
            metadataRepository.deleteById(metadataId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete metadata: " + e.getMessage(), e);
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build());

            String previewPath = metadata.getPreviewPath();
            if (previewPath != null) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(previewPath)
                            .build());
                } catch (Exception e) {
                    log.warn("Preview file deletion failed for metadataId {}: {}", metadataId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("S3 deletion failed for metadataId {}: {}", metadataId, e.getMessage());
        }
        documentAsyncProcessor.processDeleteAsync(List.of(metadataId));
    }

    @Override
    public DeleteResultDto deleteDocuments(List<Long> metadataIds, String email) {
        List<Long> successIds = new ArrayList<>();
        List<FailedDeleteInfo> failed = new ArrayList<>();


        List<Metadata> metadataList = metadataRepository.findAllById(metadataIds);

        for (Metadata metadata : metadataList) {
            Long id = metadata.getMetadataId();

            if (!metadata.getUploaderEmail().equals(email)) {
                failed.add(new FailedDeleteInfo(id, "Unauthorized access"));
                continue;
            }

            try {
                metadataRepository.delete(metadata);
                successIds.add(id);
            } catch (Exception e) {
                failed.add(new FailedDeleteInfo(id, "RDB deletion failed: " + e.getMessage()));
                log.warn("RDB deletion failed: {}", e.getMessage());
                continue;
            }

            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(metadata.getFilePath())
                        .build());
                String previewPath = metadata.getPreviewPath();
                if (previewPath != null) {
                    try {
                        s3Client.deleteObject(DeleteObjectRequest.builder()
                                .bucket(bucketName)
                                .key(previewPath)
                                .build());
                    } catch (Exception e) {
                        log.warn("Preview file deletion failed for metadataId {}: {}", id, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("S3 deletion failed for metadataId {}: {}", id, e.getMessage());
            }
        }

        documentAsyncProcessor.processDeleteAsync(successIds);

        return DeleteResultDto.builder()
                .successIds(successIds)
                .failed(failed)
                .build();
    }

    @Override
    public DocumentDetailDto getDocumentDetail(Long metadataId, String email) {
        Metadata metadata = metadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find metadata"));

        if (!metadata.getUploaderEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access");
        }

        String previewPath = metadata.getPreviewPath();
        String previewUrl = null;
        if (previewPath != null) {
            try {
                PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .getObjectRequest(GetObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(previewPath)
                                        .build())
                                .signatureDuration(Duration.ofMinutes(5))
                                .build());
                previewUrl = presignedRequest.url().toString();
            } catch (Exception e) {
                log.warn("Presigned URL 생성 실패: {}", e.getMessage());
                // 실패해도 전체 흐름은 유지
            }
        }

        return DocumentDetailDto.builder()
                .metadataId(metadataId)
                .originalFilename(metadata.getOriginalFilename())
                .contentType(metadata.getContentType().name())
                .uploadTime(metadata.getUploadTime())
                .filePath(metadata.getFilePath())
                .fileSize(metadata.getFileSize())
                .directoryId(metadata.getDirectory() != null ? metadata.getDirectory().getDirectoryId() : null)
                .previewPath(previewUrl)
                .build();
    }


    @Override
    public List<DocumentDetailDto> getDetailsByMetadataIds(List<Long> ids, String email) {
        List<Metadata> metadatas = metadataRepository.findAllById(ids);
        return metadatas.stream()
                .filter(m -> m.getUploaderEmail().equals(email))
                .map(m -> {
                    // 기존 getDocumentDetail 참고해 텍스트 요약 또는 preview 링크 포함
                    return DocumentDetailDto.builder()
                            .metadataId(m.getMetadataId())
                            .originalFilename(m.getOriginalFilename())
                            .contentType(m.getContentType().name())
                            .uploadTime(m.getUploadTime())
                            .filePath(m.getFilePath())
                            .fileSize(m.getFileSize())
                            .directoryId(m.getDirectory() != null ? m.getDirectory().getDirectoryId() : null)
                            .previewPath(m.getPreviewPath())
                            .build();
                })
                .toList();
    }

    @Override
    public List<MetadataDto> searchDocumentsByTitle(String email, String query) {
        List<Metadata> results = metadataRepository.searchByTitleLike(email, query);
        return results.stream()
                .map(MetadataDto::fromEntity)
                .toList();
    }

    @Override
    public ResponseEntity<Resource> downloadFile(Long metadataId, String email) {
        Metadata metadata = metadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find metadata"));

        if (!metadata.getUploaderEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access");
        }


        String objectName = metadata.getFilePath();

        try {
            ResponseInputStream<GetObjectResponse> is = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build());

            byte[] fileBytes = IOUtils.toByteArray(is);  // Apache Commons IO

            String contentType = metadata.getContentType().getMimeType();
            String encodedFilename = URLEncoder.encode(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(new ByteArrayResource(fileBytes));

        } catch (Exception e) {
            throw new RuntimeException("file download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateProcessingStatus(Long metadataId, ProcessingStatus status) {
        Optional<Metadata> optional = metadataRepository.findById(metadataId);
        if (optional.isEmpty()) {
            log.warn("Metadata not found for ID: {}", metadataId);
            return;
        }
        Metadata metadata = optional.get();
        if (metadata.getProcessingStatus() == ProcessingStatus.COMPLETED) {
            log.info("이미 완료된 문서입니다. 재처리 생략.");
            return;
        }
        metadata.setProcessingStatus(status);
        metadataRepository.save(metadata);
        log.info("Metadata ID {} updated to status: {}", metadataId, status);
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<ResponseEntity<String>> retryVectorDBAsync(Long metadataId, String email) {
        Metadata metadata = getMetadata(metadataId);
        if (metadata == null) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body("Metadata not found."));
        }

        if (!metadata.getUploaderEmail().equals(email)) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access"));
        }

        if (metadata.getProcessingStatus() != ProcessingStatus.FAILED) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body("Document is not in FAILED state."));
        }

        try (ResponseInputStream<GetObjectResponse> is = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(metadata.getFilePath())
                .build())) {

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            String text = TextExtractionUtil.extractText(metadata.getContentType(), is);
            DocumentKafkaMessage message = DocumentKafkaMessage.builder()
                    .metadataId(metadata.getMetadataId())
                    .email(metadata.getUploaderEmail())
                    .originalFilename(metadata.getOriginalFilename())
                    .contentType(metadata.getContentType().name())
                    .textContent(text)
                    .uploadTime(metadata.getUploadTime().format(formatter))
                    .build();

            documentKafkaProducer.uploadPublish(message);
            updateProcessingStatus(metadataId, ProcessingStatus.PROCESSING);
            return java.util.concurrent.CompletableFuture.completedFuture(ResponseEntity.ok("Retry successful."));
        } catch (Exception e) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                    ResponseEntity.internalServerError().body("Retry failed: " + e.getMessage()));
        }
    }


    @Override
    public Metadata getMetadata(Long metadataId) {
        return metadataRepository.findById(metadataId).orElse(null);
    }

    @Override
    public void moveDocument(Long metadataId, Long targetDirectoryId, String email) {
        Metadata metadata = metadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find metadata"));

        if (!metadata.getUploaderEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access");
        }

        Directory newDirectory = null;
        if (targetDirectoryId != null) {
            newDirectory = directoryRepository.findById(targetDirectoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Target directory not found"));
            if (!newDirectory.getOwnerEmail().equals(email)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized move target");
            }
        }

        metadata.setDirectory(newDirectory);
        metadataRepository.save(metadata);
    }

    @Override
    public List<MetadataDto> getMetadataDetails(List<Long> metadataIds, String email) {
        List<Metadata> metadatas = metadataRepository.findAllById(metadataIds);
        return metadatas.stream()
                .filter(m -> m.getUploaderEmail().equals(email))  // 소유자 검증
                .map(MetadataDto::fromEntity)
                .toList();
    }

}
