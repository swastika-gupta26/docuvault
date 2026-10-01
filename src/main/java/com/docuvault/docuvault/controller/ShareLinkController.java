package com.docuvault.docuvault.controller;

import com.docuvault.docuvault.dto.ShareLinkRequest;
import com.docuvault.docuvault.dto.ShareLinkResponse;
import com.docuvault.docuvault.entity.ShareLink;
import com.docuvault.docuvault.service.ShareLinkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @PostMapping("/{documentId}")
    public ResponseEntity<ShareLinkResponse> createShareLink(
            @PathVariable Long documentId,
            @RequestBody ShareLinkRequest request,
            HttpServletRequest httpRequest) {

        String email = httpRequest.getUserPrincipal().getName();

        ShareLink shareLink =
                shareLinkService.createShareLink(
                        documentId,
                        request,
                        email
                );

        return ResponseEntity.ok(
                new ShareLinkResponse(shareLink)
        );
    }

    @GetMapping("/{token}")
    public ResponseEntity<ShareLinkResponse> accessShareLink(
            @PathVariable String token,
            @RequestParam(required = false) String password) {

        ShareLink shareLink =
                shareLinkService.accessShareLink(token, password);

        return ResponseEntity.ok(
                new ShareLinkResponse(shareLink)
        );
    }
}