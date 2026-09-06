package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}