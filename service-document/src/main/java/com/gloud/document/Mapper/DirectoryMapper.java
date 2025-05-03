package com.gloud.document.Mapper;

import com.gloud.document.dto.DirectoryDto;
import com.gloud.document.entity.Directory;

import java.util.List;

public class DirectoryMapper {

    public static DirectoryDto toDto(Directory entity) {
        if (entity == null) return null;

        return DirectoryDto.builder()
                .directoryId(entity.getDirectoryId())
                .name(entity.getName())
                .ownerEmail(entity.getOwnerEmail())
                .build();
    }

    public static List<DirectoryDto> toDtoList(List<Directory> list) {
        return list.stream().map(DirectoryMapper::toDto).toList();
    }
}