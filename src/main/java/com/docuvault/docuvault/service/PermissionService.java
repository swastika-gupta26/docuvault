package com.docuvault.docuvault.service;

import com.docuvault.docuvault.dto.PermissionRequest;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.Permission;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.repository.DocumentRepository;
import com.docuvault.docuvault.repository.PermissionRepository;
import com.docuvault.docuvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public Permission assignPermission(
            Long documentId,
            PermissionRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Only owner can assign permission
        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException(
                    "Only the owner can assign permissions"
            );
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        Permission permission = permissionRepository
                .findByDocumentAndUser(document, targetUser)
                .orElse(new Permission());

        permission.setDocument(document);
        permission.setUser(targetUser);
        permission.setPermissionType(request.getPermissionType());

        return permissionRepository.save(permission);
    }
    public boolean isOwner(Document document, User user) {
        return document.getOwner().getId().equals(user.getId());
    }
    public Permission.PermissionType getUserPermission(
            Document document,
            User user) {

        if (isOwner(document, user)) {
            return null;
        }

        return permissionRepository
                .findByDocumentAndUser(document, user)
                .map(Permission::getPermissionType)
                .orElse(null);
    }
}