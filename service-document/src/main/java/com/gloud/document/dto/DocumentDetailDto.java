package com.gloud.document.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDetailDto {
    private Long metadataId;
    private String originalFilename;
    private String contentType;
    private LocalDateTime uploadTime;
    private Double fileSize;
    private String filePath;
    private String previewPath;
    private Long directoryId;
}
