package com.docuvault.docuvault.controller;

import com.docuvault.docuvault.dto.DocumentRequest;
import com.docuvault.docuvault.dto.DocumentResponse;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.Version;
import com.docuvault.docuvault.service.DocumentService;
import com.docuvault.docuvault.service.EncryptionService;
import org.springframework.core.io.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.docuvault.docuvault.service.EncryptionService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final EncryptionService encryptionService;

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestBody DocumentRequest request) {

        Document document = documentService.createDocument(request);

        return ResponseEntity.ok(new DocumentResponse(document));
    }
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getMyDocuments() {

        List<Document> documents = documentService.getMyDocuments();

        List<DocumentResponse> response = documents.stream()
                .map(DocumentResponse::new)
                .toList();

        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request) {

        Document document = documentService.updateDocument(id, request);

        return ResponseEntity.ok(new DocumentResponse(document));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable Long id) {

        documentService.deleteDocument(id);

        return ResponseEntity.ok("Document deleted successfully");
    }

    @PostMapping("/{id}/upload")
    public ResponseEntity<String> uploadFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        documentService.uploadFile(id, file);

        return ResponseEntity.ok("File uploaded successfully");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {

        Version version = documentService.getCurrentVersion(id);

        try {
            Path path = Paths.get(version.getFilePath());

            byte[] encryptedBytes = Files.readAllBytes(path);

            byte[] decryptedBytes =
                    encryptionService.decrypt(encryptedBytes);

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(version.getMimeType())
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    version.getFileName() + "\""
                    )
                    .body(decryptedBytes);

        } catch (IOException e) {
            throw new RuntimeException("File download failed", e);
        }
    }
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<Version>> getVersionHistory(
            @PathVariable Long id) {

        List<Version> versions =
                documentService.getVersionHistory(id);

        return ResponseEntity.ok(versions);
    }
    @GetMapping("/{documentId}/versions/{versionId}/download")
    public ResponseEntity<byte[]> downloadSpecificVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {

        Version version =
                documentService.getSpecificVersion(documentId, versionId);

        try {
            Path path = Paths.get(version.getFilePath());

            byte[] encryptedBytes = Files.readAllBytes(path);

            byte[] decryptedBytes =
                    encryptionService.decrypt(encryptedBytes);

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(version.getMimeType())
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    version.getFileName() + "\""
                    )
                    .body(decryptedBytes);

        } catch (IOException e) {
            throw new RuntimeException("File download failed", e);
        }
    }
    @PostMapping("/{documentId}/versions/{versionId}/restore")
    public ResponseEntity<Version> restoreVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {

        Version restoredVersion =
                documentService.restoreVersion(documentId, versionId);

        return ResponseEntity.ok(restoredVersion);
    }
}