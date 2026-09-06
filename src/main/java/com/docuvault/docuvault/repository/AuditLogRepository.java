package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.AuditLog;
import com.docuvault.docuvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
