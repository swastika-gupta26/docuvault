package com.docuvault.docuvault.service;

import com.docuvault.docuvault.dto.DocumentRequest;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.entity.Version;
import com.docuvault.docuvault.repository.DocumentRepository;
import com.docuvault.docuvault.repository.UserRepository;
import com.docuvault.docuvault.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final VersionRepository versionRepository;
    private final FileStorageService fileStorageService;

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Document createDocument(DocumentRequest request) {

        User currentUser = getCurrentUser();

        Document document = new Document();

        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setOwner(currentUser);

        return documentRepository.save(document);
    }
    public List<Document> getMyDocuments() {

        User currentUser = getCurrentUser();

        return documentRepository.findByOwner(currentUser);
    }
    public Document updateDocument(Long id, DocumentRequest request) {

        User currentUser = getCurrentUser();

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this document");
        }

        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());

        return documentRepository.save(document);
    }


    public void deleteDocument(Long id) {

        User currentUser = getCurrentUser();

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this document");
        }

        documentRepository.delete(document);
    }

    public void uploadFile(Long id, MultipartFile file) {

        try {
            User currentUser = getCurrentUser();

            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            // Only owner can upload for now
            if (!document.getOwner().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You are not allowed to upload file");
            }

            if (file.isEmpty()) {
                throw new RuntimeException("File cannot be empty");
            }

            // Store actual file
            String filePath = fileStorageService.storeFile(file);

            // Decide version number
            int versionNumber = 1;

            if (document.getCurrentVersion() != null) {
                versionNumber =
                        document.getCurrentVersion().getVersionNumber() + 1;
            }

            // Create version record
            Version version = new Version();
            version.setVersionNumber(versionNumber);
            version.setFileName(file.getOriginalFilename());
            version.setFilePath(filePath);
            version.setFileSize(file.getSize());
            version.setMimeType(file.getContentType());
            version.setDocument(document);

            versionRepository.save(version);

            // Make this the current version
            document.setCurrentVersion(version);
            documentRepository.save(document);

        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }
    public Version getCurrentVersion(Long id) {

        User currentUser = getCurrentUser();

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Only owner can download for now
        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to download this document");
        }

        if (document.getCurrentVersion() == null) {
            throw new RuntimeException("No file uploaded for this document");
        }

        return document.getCurrentVersion();
    }

}