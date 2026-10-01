package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.AuditLog;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Modifying(flushAutomatically = true)
    @Query("UPDATE AuditLog a SET a.document = null WHERE a.document = :document")
    void detachDocument(@Param("document") Document document);
}
