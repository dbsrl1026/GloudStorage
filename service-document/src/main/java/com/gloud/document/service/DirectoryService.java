package com.gloud.document.service;

import com.gloud.document.dto.DirectoryContentDto;
import com.gloud.document.dto.DirectoryDto;
import com.gloud.document.enums.ProcessingStatus;

import java.time.LocalDate;
import java.util.List;

public interface DirectoryService {

    DirectoryDto createDirectory(String name, Long parentId, String email);


    DirectoryContentDto getDirectoryContents(String email, Long parentId, String sortBy, String direction,
                                             ProcessingStatus status, String contentType);

    DirectoryDto renameDirectory(Long directoryId, String newName, String email);

    DirectoryDto moveDirectory(Long directoryId, Long newParentId, String email);

    void deleteDirectory(Long directoryId, String email);

    List<DirectoryDto> getFullPath(Long directoryId, String email);

    void deleteDirectories(List<Long> directoryIds, String email);
}
