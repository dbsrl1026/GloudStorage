package com.gloud.document.repository;

import com.gloud.document.entity.Directory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {


//  @Query("SELECT d FROM Directory d LEFT JOIN FETCH d.children WHERE d.parent IS NULL AND d.ownerEmail = :email")
//  List<Directory> fetchRootWithChildren(String email);

  List<Directory> findByOwnerEmailAndParentIsNull(String email);
  List<Directory> findByOwnerEmailAndParent_DirectoryId(String email, Long parentId);

  boolean existsByNameAndOwnerEmailAndParent_DirectoryId(String name, String ownerEmail, Long parentId);

  boolean existsByNameAndOwnerEmailAndParentIsNull(String name, String ownerEmail);

  List<Directory> findByParent(Directory parent);
}