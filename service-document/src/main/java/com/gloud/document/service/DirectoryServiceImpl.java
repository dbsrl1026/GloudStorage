package com.gloud.document.service;


import com.gloud.document.Mapper.DirectoryMapper;
import com.gloud.document.dto.DirectoryContentDto;
import com.gloud.document.dto.DirectoryDto;
import com.gloud.document.entity.Directory;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.exception.BadRequestException;
import com.gloud.document.exception.DuplicateDirectoryException;
import com.gloud.document.repository.DirectoryRepository;
import com.gloud.document.repository.MetadataRepository;
import com.gloud.document.dto.MetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {
    private final DirectoryRepository directoryRepository;
    private final MetadataRepository metadataRepository;
    private final DocumentService documentService;

    @Override
    public DirectoryDto createDirectory(String name, Long parentId, String email) {
        boolean duplicate;

        if (parentId == null) {
            duplicate = directoryRepository.existsByNameAndOwnerEmailAndParentIsNull(name, email);
        } else {
            duplicate = directoryRepository.existsByNameAndOwnerEmailAndParent_DirectoryId(name, email, parentId);
        }

        if (duplicate) {
            throw new DuplicateDirectoryException("동일한 이름의 디렉토리가 이미 존재합니다.");
        }

        Directory directory = new Directory();
        directory.setName(name);
        directory.setOwnerEmail(email);

        if (parentId != null) {
            Directory parent = directoryRepository.findById(parentId)
                    .orElseThrow(() -> new BadRequestException("부모 디렉토리를 찾을 수 없습니다."));
            directory.setParent(parent);
        }

        Directory saved = directoryRepository.save(directory);
        return DirectoryMapper.toDto(saved);
    }

    @Override
    public DirectoryContentDto getDirectoryContents(String email, Long parentId, String sortBy, String direction,
                                                    ProcessingStatus status, String contentType) {
        List<Directory> directories = (parentId == null)
                ? directoryRepository.findByOwnerEmailAndParentIsNull(email)
                : directoryRepository.findByOwnerEmailAndParent_DirectoryId(email, parentId);

        List<Metadata> documents = metadataRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("uploaderEmail"), email));

            if (parentId == null) {
                predicates.add(cb.isNull(root.get("directory")));
            } else {
                predicates.add(cb.equal(root.get("directory").get("id"), parentId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("processingStatus"), status));
            }

            if (contentType != null) {
                predicates.add(cb.equal(root.get("contentType"), contentType.toUpperCase()));
            }

            query.orderBy(
                    direction.equalsIgnoreCase("desc") ?
                            cb.desc(root.get(sortBy)) :
                            cb.asc(root.get(sortBy))
            );

            return cb.and(predicates.toArray(new Predicate[0]));
        });

        return DirectoryContentDto.builder()
                .directories(directories.stream().map(DirectoryMapper::toDto).toList())
                .documents(documents.stream().map(MetadataDto::fromEntity).toList())
                .build();
    }

    @Override
    public DirectoryDto renameDirectory(Long directoryId, String newName, String email) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new IllegalArgumentException("디렉토리를 찾을 수 없습니다."));

        if (!directory.getOwnerEmail().equals(email)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        Long parentId = directory.getParent() != null ? directory.getParent().getDirectoryId() : null;

        boolean duplicate = (parentId == null)
                ? directoryRepository.existsByNameAndOwnerEmailAndParentIsNull(newName, email)
                : directoryRepository.existsByNameAndOwnerEmailAndParent_DirectoryId(newName, email, parentId);

        if (duplicate) {
            throw new DuplicateDirectoryException("해당 이름의 디렉토리가 이미 존재합니다.");
        }

        directory.setName(newName);
        return DirectoryMapper.toDto(directoryRepository.save(directory));
    }

    @Override
    public DirectoryDto moveDirectory(Long directoryId, Long newParentId, String email) {
        Directory target = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new IllegalArgumentException("대상 디렉토리를 찾을 수 없습니다."));

        if (!target.getOwnerEmail().equals(email)) {
            throw new IllegalArgumentException("이 디렉토리를 이동할 권한이 없습니다.");
        }

        if (Objects.equals(target.getDirectoryId(), newParentId)) {
            throw new IllegalArgumentException("디렉토리를 자기 자신 아래로 이동할 수 없습니다.");
        }

        Directory newParent = null;
        if (newParentId != null) {
            newParent = directoryRepository.findById(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("새 부모 디렉토리를 찾을 수 없습니다."));

            if (!newParent.getOwnerEmail().equals(email)) {
                throw new IllegalArgumentException("새 부모 디렉토리에 접근할 권한이 없습니다.");
            }

            //순환 참조 방지 (자식 디렉토리 안에 자기 자신이 있는지 확인)
            Directory cursor = newParent;
            while (cursor != null) {
                if (cursor.getDirectoryId().equals(directoryId)) {
                    throw new IllegalArgumentException("하위 디렉토리로 이동할 수 없습니다.");
                }
                cursor = cursor.getParent();
            }

            // 이름 중복 검사
            boolean duplicate = directoryRepository.existsByNameAndOwnerEmailAndParent_DirectoryId(
                    target.getName(), email, newParentId);
            if (duplicate) {
                throw new DuplicateDirectoryException("해당 이름의 디렉토리가 새 부모 디렉토리에 이미 존재합니다.");
            }
        } else {
            // 루트로 이동 시 중복 검사
            boolean duplicate = directoryRepository.existsByNameAndOwnerEmailAndParentIsNull(
                    target.getName(), email);
            if (duplicate) {
                throw new DuplicateDirectoryException("해당 이름의 디렉토리가 루트에 이미 존재합니다.");
            }
        }

        target.setParent(newParent);
        Directory saved = directoryRepository.save(target);
        return DirectoryMapper.toDto(saved);
    }

    @Override
    public void deleteDirectory(Long directoryId, String email) {
        Directory root = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new IllegalArgumentException("디렉토리를 찾을 수 없습니다."));

        if (!root.getOwnerEmail().equals(email)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        deleteRecursively(root);
    }

    private void deleteRecursively(Directory directory) {
        // 1. 하위 디렉토리들 재귀 삭제
        List<Directory> children = directoryRepository.findByParent(directory);
        for (Directory child : children) {
            deleteRecursively(child);
        }

        // 2. 해당 디렉토리에 속한 문서 삭제 (위임)
        List<Metadata> files = metadataRepository.findByDirectory(directory);
        List<Long> ids = files.stream().map(Metadata::getMetadataId).toList();
        if (!ids.isEmpty()) {
            documentService.deleteDocuments(ids, directory.getOwnerEmail());
        }

        // 3. 디렉토리 삭제
        directoryRepository.delete(directory);
    }


    @Override
    public List<DirectoryDto> getFullPath(Long directoryId, String email) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new IllegalArgumentException("디렉토리를 찾을 수 없습니다."));

        if (!directory.getOwnerEmail().equals(email)) {
            throw new IllegalArgumentException("조회 권한이 없습니다.");
        }

        List<Directory> path = new ArrayList<>();
        Directory current = directory;

        while (current != null) {
            path.add(0, current); // 최상위부터 쌓기 위해 앞에 추가
            current = current.getParent();
        }

        return path.stream().map(DirectoryMapper::toDto).toList();
    }
}
