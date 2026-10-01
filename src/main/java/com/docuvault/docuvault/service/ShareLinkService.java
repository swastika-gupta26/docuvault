package com.docuvault.docuvault.service;

import com.docuvault.docuvault.dto.ShareLinkRequest;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.ShareLink;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.repository.DocumentRepository;
import com.docuvault.docuvault.repository.ShareLinkRepository;
import com.docuvault.docuvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ShareLink createShareLink(
            Long documentId,
            ShareLinkRequest request,
            String email) {

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the owner can create a share link"
            );
        }

        ShareLink shareLink = new ShareLink();

        shareLink.setDocument(document);
        shareLink.setToken(UUID.randomUUID().toString());
        shareLink.setExpiresAt(request.getExpiresAt());
        shareLink.setOneTimeUse(request.isOneTimeUse());

        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            shareLink.setPasswordHash(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        return shareLinkRepository.save(shareLink);
    }

    public ShareLink getShareLink(String token) {

        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Share link not found"));

        if (shareLink.getExpiresAt() != null &&
                shareLink.getExpiresAt().isBefore(
                        java.time.LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.GONE, "Share link has expired");
        }

        if (shareLink.isOneTimeUse() && shareLink.isUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, "Share link has already been used");
        }

        return shareLink;
    }

    public ShareLink accessShareLink(
            String token,
            String password) {

        ShareLink shareLink = getShareLink(token);

        if (shareLink.getPasswordHash() != null) {

            if (password == null ||
                    !passwordEncoder.matches(
                            password,
                            shareLink.getPasswordHash())) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid share password");
            }
        }

        if (shareLink.isOneTimeUse()) {
            shareLink.setUsed(true);
            shareLinkRepository.save(shareLink);
        }

        return shareLink;
    }
}