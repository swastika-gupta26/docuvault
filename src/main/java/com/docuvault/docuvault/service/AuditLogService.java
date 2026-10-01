package com.docuvault.docuvault.service;

import com.docuvault.docuvault.entity.AuditLog;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(
            User user,
            Document document,
            String action,
            String ipAddress) {

        AuditLog auditLog = new AuditLog();

        auditLog.setUser(user);
        auditLog.setDocument(document);
        auditLog.setAction(action);
        auditLog.setIpAddress(ipAddress);

        auditLogRepository.saveAndFlush(auditLog);
    }
}