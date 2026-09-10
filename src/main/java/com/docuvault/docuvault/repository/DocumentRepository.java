package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwner(User owner);
}
