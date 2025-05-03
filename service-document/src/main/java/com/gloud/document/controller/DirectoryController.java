package com.gloud.document.controller;


import com.gloud.document.annotation.ValidateEmailClaim;
import com.gloud.document.dto.DirectoryContentDto;
import com.gloud.document.dto.DirectoryDto;
import com.gloud.document.entity.Directory;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.service.DirectoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "디렉토리 관리 API", description = "디렉토리 생성/삭제/수정/조회 기능을 제공합니다.")
@RestController
@RequestMapping("/directories")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @ValidateEmailClaim
    @PostMapping("/create")
    public ResponseEntity<DirectoryDto> createDirectory(@RequestParam String name,
                                                        @RequestParam(required = false) Long parentId,
                                                        @RequestParam String email) {
        DirectoryDto created = directoryService.createDirectory(name, parentId, email);
        return ResponseEntity.ok(created);
    }

    @ValidateEmailClaim
    @GetMapping("/contents")
    public ResponseEntity<DirectoryContentDto> getDirectoryContents(
            @RequestParam String email,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "uploadTime") String sortBy, //uploadTime, originalFilename, contentType
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) ProcessingStatus status, //PENDING, PROCESSING, COMPLETED, FAILED
            @RequestParam(required = false) String contentType  //PDF, TXT, MD, DOC, DOCX, HWP, HWPX, CSV, PPT, PPTX
    ) {
        return ResponseEntity.ok(directoryService.getDirectoryContents(email, parentId, sortBy, direction, status, contentType));
    }

    @ValidateEmailClaim
    @PatchMapping("/{directoryId}/rename")
    public ResponseEntity<DirectoryDto> renameDirectory(
            @PathVariable Long directoryId,
            @RequestParam String newName,
            @RequestParam String email) {

        DirectoryDto updated = directoryService.renameDirectory(directoryId, newName, email);
        return ResponseEntity.ok(updated);
    }

    @ValidateEmailClaim
    @PatchMapping("/{directoryId}/move")
    public ResponseEntity<DirectoryDto> moveDirectory(
            @PathVariable Long directoryId,
            @RequestParam(required = false) Long newParentId,
            @RequestParam String email) {
        DirectoryDto moved = directoryService.moveDirectory(directoryId, newParentId, email);
        return ResponseEntity.ok(moved);
    }

    @ValidateEmailClaim
    @DeleteMapping("/{directoryId}")
    public ResponseEntity<Void> deleteDirectory(@PathVariable Long directoryId,
                                                @RequestParam String email) {
        directoryService.deleteDirectory(directoryId, email);
        return ResponseEntity.noContent().build();
    }

    @ValidateEmailClaim
    @DeleteMapping
    public ResponseEntity<Void> deleteDirectories(
            @RequestBody List<Long> directoryIds,
            @RequestParam String email) {
        directoryService.deleteDirectories(directoryIds, email);
        return ResponseEntity.ok().build();
    }

    @ValidateEmailClaim
    @GetMapping("/{directoryId}/path")
    public ResponseEntity<List<DirectoryDto>> getDirectoryFullPath(@PathVariable Long directoryId,
                                                                   @RequestParam String email) {
        return ResponseEntity.ok(directoryService.getFullPath(directoryId, email));
    }
}
