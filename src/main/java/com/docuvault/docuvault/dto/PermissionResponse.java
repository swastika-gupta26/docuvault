package com.docuvault.docuvault.dto;

import com.docuvault.docuvault.entity.Permission;
import lombok.Getter;

@Getter
public class PermissionResponse {

    private Long id;
    private Long documentId;
    private Long userId;
    private Permission.PermissionType permissionType;

    public PermissionResponse(Permission permission) {
        this.id = permission.getId();
        this.documentId = permission.getDocument().getId();
        this.userId = permission.getUser().getId();
        this.permissionType = permission.getPermissionType();
    }
}