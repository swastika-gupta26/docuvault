package com.docuvault.docuvault.dto;

import com.docuvault.docuvault.entity.Permission;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Permission.PermissionType permissionType;
}