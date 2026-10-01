package com.docuvault.docuvault.controller;

import com.docuvault.docuvault.dto.DocumentRequest;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.service.DocumentService;
import com.docuvault.docuvault.service.EncryptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.docuvault.docuvault.entity.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private DocumentController documentController;

    private Document document;

    @BeforeEach
    void setUp() {

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        document = new Document();
        document.setId(1L);
        document.setTitle("Test Document");
        document.setDescription("Test Description");
        document.setOwner(owner);


    }

    @Test
    void createDocument_shouldReturnDocumentResponse() {

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Test Document");
        request.setDescription("Test Description");

        when(httpRequest.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(documentService.createDocument(
                request,
                "127.0.0.1"
        )).thenReturn(document);

        ResponseEntity<?> response =
                documentController.createDocument(
                        request,
                        httpRequest
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        verify(documentService)
                .createDocument(request, "127.0.0.1");
    }

    @Test
    void getMyDocuments_shouldReturnDocuments() {

        when(documentService.getMyDocuments())
                .thenReturn(List.of(document));

        ResponseEntity<?> response =
                documentController.getMyDocuments();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        verify(documentService)
                .getMyDocuments();
    }

    @Test
    void searchDocuments_shouldReturnDocuments() {

        when(documentService.searchDocuments("Test"))
                .thenReturn(List.of(document));

        ResponseEntity<?> response =
                documentController.searchDocuments("Test");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        verify(documentService)
                .searchDocuments("Test");
    }

    @Test
    void updateDocument_shouldReturnDocumentResponse() {

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Updated Document");
        request.setDescription("Updated Description");

        when(httpRequest.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(documentService.updateDocument(
                1L,
                request,
                "127.0.0.1"
        )).thenReturn(document);

        ResponseEntity<?> response =
                documentController.updateDocument(
                        1L,
                        request,
                        httpRequest
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        verify(documentService)
                .updateDocument(
                        1L,
                        request,
                        "127.0.0.1"
                );
    }
}