package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.Permission;
import com.docuvault.docuvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByDocumentAndUser(
            Document document,
            User user
    );
}