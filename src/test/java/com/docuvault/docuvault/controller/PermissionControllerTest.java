package com.docuvault.docuvault.controller;

import com.docuvault.docuvault.dto.PermissionRequest;
import com.docuvault.docuvault.entity.Permission;
import com.docuvault.docuvault.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    @Test
    void assignPermission_shouldReturnPermission() {

        PermissionRequest request = new PermissionRequest();

        Permission permission = new Permission();
        permission.setPermissionType(
                Permission.PermissionType.VIEWER
        );

        when(permissionService.assignPermission(1L, request))
                .thenReturn(permission);

        ResponseEntity<Permission> response =
                permissionController.assignPermission(
                        1L,
                        request
                );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(permission, response.getBody());

        verify(permissionService)
                .assignPermission(1L, request);
    }
}