package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
