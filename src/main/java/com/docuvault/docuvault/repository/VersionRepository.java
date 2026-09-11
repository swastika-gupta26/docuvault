package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionRepository extends JpaRepository<Version, Long> {
    List<Version> findByDocumentOrderByVersionNumberDesc(Document document);
}
