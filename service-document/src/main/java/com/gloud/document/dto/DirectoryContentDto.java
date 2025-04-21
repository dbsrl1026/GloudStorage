package com.gloud.document.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class DirectoryContentDto {
    List<DirectoryDto> directories;
    List<MetadataDto> documents;
}
