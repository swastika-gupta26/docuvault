package com.docuvault.docuvault.controller;

import com.docuvault.docuvault.dto.DocumentRequest;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.Version;
import com.docuvault.docuvault.service.DocumentService;
import org.springframework.core.io.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<Document> createDocument(
            @Valid @RequestBody DocumentRequest request) {

        Document document = documentService.createDocument(request);

        return ResponseEntity.ok(document);
    }
    @GetMapping
    public ResponseEntity<List<Document>> getMyDocuments() {

        List<Document> documents = documentService.getMyDocuments();

        return ResponseEntity.ok(documents);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request) {

        Document document = documentService.updateDocument(id, request);

        return ResponseEntity.ok(document);
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
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {

        Version version = documentService.getCurrentVersion(id);

        Resource resource = new FileSystemResource(version.getFilePath());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(version.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + version.getFileName() + "\""
                )
                .body(resource);
    }
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<Version>> getVersionHistory(
            @PathVariable Long id) {

        List<Version> versions =
                documentService.getVersionHistory(id);

        return ResponseEntity.ok(versions);
    }
    @GetMapping("/{documentId}/versions/{versionId}/download")
    public ResponseEntity<Resource> downloadSpecificVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {

        Version version =
                documentService.getSpecificVersion(documentId, versionId);

        Resource resource = new FileSystemResource(version.getFilePath());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(version.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + version.getFileName() + "\""
                )
                .body(resource);
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