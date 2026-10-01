package com.docuvault.docuvault.service;

import com.docuvault.docuvault.entity.AuditLog;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void log_shouldSaveAuditLog() {

        User user = new User();
        Document document = new Document();

        auditLogService.log(
                user,
                document,
                "DOCUMENT_CREATED",
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();

        assertEquals(user, savedLog.getUser());
        assertEquals(document, savedLog.getDocument());
        assertEquals("DOCUMENT_CREATED", savedLog.getAction());
        assertEquals("127.0.0.1", savedLog.getIpAddress());
    }
}