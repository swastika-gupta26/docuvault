package com.docuvault.docuvault.service;

import com.docuvault.docuvault.dto.PermissionRequest;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.Permission;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.repository.DocumentRepository;
import com.docuvault.docuvault.repository.PermissionRepository;
import com.docuvault.docuvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PermissionService permissionService;

    private User owner;
    private User targetUser;
    private Document document;

    @BeforeEach
    void setUp() {

        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        targetUser = new User();
        targetUser.setId(2L);
        targetUser.setEmail("target@example.com");

        document = new Document();
        document.setId(10L);
        document.setTitle("Test Document");
        document.setOwner(owner);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner@example.com",
                        null
                )
        );
    }

    @Test
    void assignPermission_shouldAssignPermission() {

        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));

        PermissionRequest request = new PermissionRequest();
        request.setUserId(2L);
        request.setPermissionType(
                Permission.PermissionType.VIEWER
        );

        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));

        when(permissionRepository
                .findByDocumentAndUser(document, targetUser))
                .thenReturn(Optional.empty());

        Permission savedPermission = new Permission();
        savedPermission.setDocument(document);
        savedPermission.setUser(targetUser);
        savedPermission.setPermissionType(
                Permission.PermissionType.VIEWER
        );

        when(permissionRepository.save(any(Permission.class)))
                .thenReturn(savedPermission);

        Permission result =
                permissionService.assignPermission(10L, request);

        assertNotNull(result);
        assertEquals(targetUser, result.getUser());
        assertEquals(
                Permission.PermissionType.VIEWER,
                result.getPermissionType()
        );

        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void isOwner_shouldReturnTrueForOwner() {

        assertTrue(
                permissionService.isOwner(document, owner)
        );
    }

    @Test
    void isOwner_shouldReturnFalseForOtherUser() {

        assertFalse(
                permissionService.isOwner(document, targetUser)
        );
    }

    @Test
    void getUserPermission_shouldReturnPermission() {

        Permission permission = new Permission();
        permission.setDocument(document);
        permission.setUser(targetUser);
        permission.setPermissionType(
                Permission.PermissionType.EDITOR
        );

        when(permissionRepository
                .findByDocumentAndUser(document, targetUser))
                .thenReturn(Optional.of(permission));

        Permission.PermissionType result =
                permissionService.getUserPermission(
                        document,
                        targetUser
                );

        assertEquals(
                Permission.PermissionType.EDITOR,
                result
        );
    }

    @Test
    void getUserPermission_shouldReturnNullForOwner() {

        Permission.PermissionType result =
                permissionService.getUserPermission(
                        document,
                        owner
                );

        assertNull(result);

        verify(
                permissionRepository,
                never()
        ).findByDocumentAndUser(any(), any());
    }
}