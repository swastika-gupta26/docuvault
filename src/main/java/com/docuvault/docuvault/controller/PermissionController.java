package com.docuvault.docuvault.controller;

import com.docuvault.docuvault.dto.PermissionRequest;
import com.docuvault.docuvault.entity.Permission;
import com.docuvault.docuvault.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping("/{documentId}/permissions")
    public ResponseEntity<Permission> assignPermission(
            @PathVariable Long documentId,
            @Valid @RequestBody PermissionRequest request) {

        Permission permission =
                permissionService.assignPermission(documentId, request);

        return ResponseEntity.ok(permission);
    }
}