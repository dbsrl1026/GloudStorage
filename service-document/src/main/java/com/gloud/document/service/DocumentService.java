package com.gloud.document.service;


import com.gloud.document.dto.*;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DocumentService {


    String uploadFile(MultipartFile file, Long directoryId, String email);

    List<UploadResult> uploadMultipleFiles(List<MultipartFile> files, Long directoryId, String email);

    Page<MetadataDto> getDocuments(String email,int page, int size, String sortBy, String direction,
                                   ProcessingStatus status, String contentType,
                                   LocalDate startDate, LocalDate endDate);

    void deleteDocument(Long metadataId, String email);

    DeleteResultDto deleteDocuments(List<Long> metadataIds, String email);

    DocumentDetailDto getDocumentDetail(Long metadataId, String email);

    ResponseEntity<Resource> downloadFile(Long metadataId, String email);

    void updateProcessingStatus(Long metadataId, ProcessingStatus status);

    Metadata getMetadata(Long metadataId);

    @Async("taskExecutor")
    CompletableFuture<ResponseEntity<String>> retryVectorDBAsync(Long metadataId, String email);

    List<DocumentDetailDto> getDetailsByMetadataIds(List<Long> metadataIds, String email);

    List<MetadataDto> searchDocumentsByTitle(String email, String query);

    void moveDocument(Long metadataId, Long targetDirectoryId, String email);

    List<MetadataDto> getMetadataDetails(List<Long> metadataIds, String email);
}
