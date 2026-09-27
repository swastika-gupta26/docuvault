package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwner(User owner);
    List<Document> findByOwnerAndTitleContainingIgnoreCase(
            User owner,
            String title
    );
    @Query("""
    SELECT DISTINCT d
    FROM Document d
    LEFT JOIN Permission p ON p.document = d
    WHERE (d.owner = :user OR p.user = :user)
    AND LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%'))
""")
    List<Document> searchAccessibleDocuments(
            @Param("user") User user,
            @Param("title") String title
    );
}
