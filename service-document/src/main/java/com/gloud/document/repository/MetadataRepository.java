package com.gloud.document.repository;

import com.gloud.document.entity.Directory;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MetadataRepository extends JpaRepository<Metadata, Long>, JpaSpecificationExecutor<Metadata> {


    @Query("SELECT m FROM Metadata m WHERE m.uploaderEmail = :email AND LOWER(m.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Metadata> searchByTitleLike(@Param("email") String email, @Param("query") String query);

    List<Metadata> findByDirectory(Directory directory);

}