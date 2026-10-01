package com.docuvault.docuvault.service;

import com.docuvault.docuvault.dto.DocumentRequest;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.Permission;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.entity.Version;
import com.docuvault.docuvault.repository.DocumentRepository;
import com.docuvault.docuvault.repository.PermissionRepository;
import com.docuvault.docuvault.repository.UserRepository;
import com.docuvault.docuvault.repository.VersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VersionRepository versionRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DocumentService documentService;

    private User user;
    private Document document;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        document = new Document();
        document.setId(1L);
        document.setTitle("Test Document");
        document.setDescription("Test Description");
        document.setOwner(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com",
                        null
                )
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
    }

    // ---------------------------------------------------------
    // CREATE DOCUMENT
    // ---------------------------------------------------------

    @Test
    void createDocument_shouldCreateSuccessfully() {

        DocumentRequest request = new DocumentRequest();
        request.setTitle("New Document");
        request.setDescription("New Description");

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Document result =
                documentService.createDocument(request, "127.0.0.1");

        assertNotNull(result);
        assertEquals("New Document", result.getTitle());
        assertEquals("New Description", result.getDescription());
        assertEquals(user, result.getOwner());

        verify(documentRepository).save(any(Document.class));

        verify(auditLogService).log(
                user,
                result,
                "DOCUMENT_CREATED",
                "127.0.0.1"
        );
    }

    // ---------------------------------------------------------
    // GET MY DOCUMENTS
    // ---------------------------------------------------------

    @Test
    void getMyDocuments_shouldReturnOwnedAndSharedDocuments() {

        Document sharedDocument = new Document();
        sharedDocument.setId(2L);

        Permission permission = new Permission();
        permission.setDocument(sharedDocument);
        permission.setUser(user);

        when(documentRepository.findByOwner(user))
                .thenReturn(new java.util.ArrayList<>(List.of(document)));

        when(permissionRepository.findByUser(user))
                .thenReturn(List.of(permission));

        List<Document> result =
                documentService.getMyDocuments();

        assertEquals(2, result.size());
        assertTrue(result.contains(document));
        assertTrue(result.contains(sharedDocument));
    }

    @Test
    void getMyDocuments_shouldNotDuplicateSharedOwnedDocument() {

        Permission permission = new Permission();
        permission.setDocument(document);
        permission.setUser(user);

        when(documentRepository.findByOwner(user))
                .thenReturn(new java.util.ArrayList<>(List.of(document)));

        when(permissionRepository.findByUser(user))
                .thenReturn(List.of(permission));

        List<Document> result =
                documentService.getMyDocuments();

        assertEquals(1, result.size());
    }

    // ---------------------------------------------------------
    // SEARCH
    // ---------------------------------------------------------

    @Test
    void searchDocuments_shouldReturnAccessibleDocuments() {

        when(documentRepository.searchAccessibleDocuments(
                user,
                "Test"
        )).thenReturn(List.of(document));

        List<Document> result =
                documentService.searchDocuments("Test");

        assertEquals(1, result.size());
        assertEquals(document, result.get(0));

        verify(documentRepository)
                .searchAccessibleDocuments(user, "Test");
    }

    // ---------------------------------------------------------
    // UPDATE DOCUMENT
    // ---------------------------------------------------------

    @Test
    void updateDocument_shouldUpdateForOwner() {

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        when(documentRepository.save(document))
                .thenReturn(document);

        Document result =
                documentService.updateDocument(
                        1L,
                        request,
                        "127.0.0.1"
                );

        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());

        verify(auditLogService).log(
                user,
                document,
                "DOCUMENT_UPDATED",
                null
        );
    }

    @Test
    void updateDocument_shouldUpdateForEditor() {

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Editor Updated");
        request.setDescription("Editor Description");

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(false);

        when(permissionService.getUserPermission(document, user))
                .thenReturn(Permission.PermissionType.EDITOR);

        when(documentRepository.save(document))
                .thenReturn(document);

        Document result =
                documentService.updateDocument(
                        1L,
                        request,
                        "127.0.0.1"
                );

        assertEquals("Editor Updated", result.getTitle());
    }

    @Test
    void updateDocument_shouldRejectViewer() {

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(false);

        when(permissionService.getUserPermission(document, user))
                .thenReturn(Permission.PermissionType.VIEWER);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.updateDocument(
                        1L,
                        new DocumentRequest(),
                        "127.0.0.1"
                )
        );

        assertEquals(
                "Only the owner or editor can update this document",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // DELETE DOCUMENT
    // ---------------------------------------------------------

    @Test
    void deleteDocument_shouldDeleteForOwner() {

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        documentService.deleteDocument(1L, "127.0.0.1");

        verify(auditLogService).log(
                user,
                document,
                "DOCUMENT_DELETED",
                null
        );

        verify(documentRepository).delete(document);
    }

    @Test
    void deleteDocument_shouldRejectNonOwner() {

        User anotherUser = new User();
        anotherUser.setId(2L);

        document.setOwner(anotherUser);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.deleteDocument(
                        1L,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "You are not allowed to delete this document",
                exception.getMessage()
        );

        verify(documentRepository, never()).delete(any());
    }

    // ---------------------------------------------------------
    // UPLOAD FILE
    // ---------------------------------------------------------

    @Test
    void uploadFile_shouldUploadFirstVersion() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test.pdf",
                        "application/pdf",
                        "test content".getBytes()
                );

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        when(encryptionService.encrypt(any(byte[].class)))
                .thenReturn("encrypted".getBytes());

        when(fileStorageService.storeEncryptedFile(
                any(byte[].class),
                eq("test.pdf")
        )).thenReturn("uploads/test.pdf");

        when(versionRepository.save(any(Version.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        documentService.uploadFile(
                1L,
                file,
                "127.0.0.1"
        );

        assertNotNull(document.getCurrentVersion());
        assertEquals(
                1,
                document.getCurrentVersion().getVersionNumber()
        );
        assertEquals(
                "test.pdf",
                document.getCurrentVersion().getFileName()
        );

        verify(versionRepository).save(any(Version.class));
        verify(documentRepository).save(document);
    }

    @Test
    void uploadFile_shouldCreateSecondVersion() throws Exception {

        Version currentVersion = new Version();
        currentVersion.setVersionNumber(1);
        currentVersion.setDocument(document);

        document.setCurrentVersion(currentVersion);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "second.pdf",
                        "application/pdf",
                        "second content".getBytes()
                );

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        when(encryptionService.encrypt(any(byte[].class)))
                .thenReturn("encrypted".getBytes());

        when(fileStorageService.storeEncryptedFile(
                any(byte[].class),
                eq("second.pdf")
        )).thenReturn("uploads/second.pdf");

        when(versionRepository.save(any(Version.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        documentService.uploadFile(
                1L,
                file,
                "127.0.0.1"
        );

        assertEquals(
                2,
                document.getCurrentVersion().getVersionNumber()
        );
    }

    @Test
    void uploadFile_shouldRejectViewer() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test.pdf",
                        "application/pdf",
                        "test".getBytes()
                );

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(false);

        when(permissionService.getUserPermission(document, user))
                .thenReturn(Permission.PermissionType.VIEWER);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.uploadFile(
                        1L,
                        file,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "Only the owner or editor can upload files",
                exception.getMessage()
        );
    }

    @Test
    void uploadFile_shouldRejectEmptyFile() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "empty.pdf",
                        "application/pdf",
                        new byte[0]
                );

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.uploadFile(
                        1L,
                        file,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "File cannot be empty",
                exception.getMessage()
        );
    }

    @Test
    void uploadFile_shouldHandleIOException() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test.pdf",
                        "application/pdf",
                        "test".getBytes()
                );

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        doThrow(new IOException("Storage error"))
                .when(fileStorageService)
                .storeEncryptedFile(
                        any(byte[].class),
                        anyString()
                );

        when(encryptionService.encrypt(any(byte[].class)))
                .thenReturn("encrypted".getBytes());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.uploadFile(
                        1L,
                        file,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "File upload failed",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // CURRENT VERSION
    // ---------------------------------------------------------

    @Test
    void getCurrentVersion_shouldReturnVersionForOwner() {

        Version version = new Version();
        version.setVersionNumber(1);
        version.setDocument(document);

        document.setCurrentVersion(version);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        Version result =
                documentService.getCurrentVersion(
                        1L,
                        "127.0.0.1"
                );

        assertEquals(version, result);

        verify(auditLogService).log(
                user,
                document,
                "DOCUMENT_DOWNLOADED",
                "127.0.0.1"
        );
    }

    @Test
    void getCurrentVersion_shouldAllowViewer() {

        Version version = new Version();
        version.setVersionNumber(1);
        version.setDocument(document);

        document.setCurrentVersion(version);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(false);

        when(permissionService.getUserPermission(document, user))
                .thenReturn(Permission.PermissionType.VIEWER);

        Version result =
                documentService.getCurrentVersion(
                        1L,
                        "127.0.0.1"
                );

        assertEquals(version, result);
    }

    @Test
    void getCurrentVersion_shouldRejectUnauthorizedUser() {

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(false);

        when(permissionService.getUserPermission(document, user))
                .thenReturn(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.getCurrentVersion(
                        1L,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "You do not have permission to view this document",
                exception.getMessage()
        );
    }

    @Test
    void getCurrentVersion_shouldRejectWhenNoFile() {

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.getCurrentVersion(
                        1L,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "No file uploaded for this document",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // VERSION HISTORY
    // ---------------------------------------------------------

    @Test
    void getVersionHistory_shouldReturnVersionsForOwner() {

        Version version = new Version();
        version.setVersionNumber(1);
        version.setDocument(document);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(versionRepository
                .findByDocumentOrderByVersionNumberDesc(document))
                .thenReturn(List.of(version));

        List<Version> result =
                documentService.getVersionHistory(1L);

        assertEquals(1, result.size());
        assertEquals(version, result.get(0));
    }

    @Test
    void getVersionHistory_shouldRejectNonOwner() {

        User anotherUser = new User();
        anotherUser.setId(2L);

        document.setOwner(anotherUser);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.getVersionHistory(1L)
        );

        assertEquals(
                "You are not allowed to view version history",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // SPECIFIC VERSION
    // ---------------------------------------------------------

    @Test
    void getSpecificVersion_shouldReturnVersionForOwner() {

        Version version = new Version();
        version.setId(10L);
        version.setDocument(document);

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(versionRepository.findById(10L))
                .thenReturn(Optional.of(version));

        Version result =
                documentService.getSpecificVersion(
                        1L,
                        10L,
                        "127.0.0.1"
                );

        assertEquals(version, result);
    }

    @Test
    void getSpecificVersion_shouldRejectWrongDocument() {

        Document anotherDocument = new Document();
        anotherDocument.setId(2L);

        Version version = new Version();
        version.setId(10L);
        version.setDocument(anotherDocument);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(permissionService.isOwner(document, user))
                .thenReturn(true);

        when(versionRepository.findById(10L))
                .thenReturn(Optional.of(version));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.getSpecificVersion(
                        1L,
                        10L,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "This version does not belong to this document",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // RESTORE VERSION
    // ---------------------------------------------------------

    @Test
    void restoreVersion_shouldRestoreSuccessfully() {

        Version oldVersion = new Version();
        oldVersion.setId(10L);
        oldVersion.setVersionNumber(1);
        oldVersion.setFileName("old.pdf");
        oldVersion.setFilePath("uploads/old.pdf");
        oldVersion.setFileSize(100L);
        oldVersion.setMimeType("application/pdf");
        oldVersion.setDocument(document);

        Version currentVersion = new Version();
        currentVersion.setVersionNumber(2);
        currentVersion.setDocument(document);

        document.setCurrentVersion(currentVersion);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(versionRepository.findById(10L))
                .thenReturn(Optional.of(oldVersion));

        when(versionRepository.save(any(Version.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(documentRepository.save(document))
                .thenReturn(document);

        Version result =
                documentService.restoreVersion(
                        1L,
                        10L,
                        "127.0.0.1"
                );

        assertNotNull(result);
        assertEquals(3, result.getVersionNumber());
        assertEquals("old.pdf", result.getFileName());
        assertEquals("uploads/old.pdf", result.getFilePath());
        assertEquals(100L, result.getFileSize());
        assertEquals("application/pdf", result.getMimeType());

        assertEquals(
                result,
                document.getCurrentVersion()
        );

        verify(auditLogService).log(
                user,
                document,
                "VERSION_RESTORED",
                "127.0.0.1"
        );
    }

    @Test
    void restoreVersion_shouldRejectNonOwner() {

        User anotherUser = new User();
        anotherUser.setId(2L);

        document.setOwner(anotherUser);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.restoreVersion(
                        1L,
                        10L,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "You are not allowed to restore this version",
                exception.getMessage()
        );
    }

    @Test
    void restoreVersion_shouldRejectVersionFromAnotherDocument() {

        Document anotherDocument = new Document();
        anotherDocument.setId(2L);

        Version oldVersion = new Version();
        oldVersion.setId(10L);
        oldVersion.setDocument(anotherDocument);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(versionRepository.findById(10L))
                .thenReturn(Optional.of(oldVersion));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentService.restoreVersion(
                        1L,
                        10L,
                        "127.0.0.1"
                )
        );

        assertEquals(
                "This version does not belong to this document",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    @org.junit.jupiter.api.AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }
}