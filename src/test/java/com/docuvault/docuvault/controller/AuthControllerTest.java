package com.docuvault.docuvault.controller;

import com.docuvault.docuvault.dto.AuthResponse;
import com.docuvault.docuvault.dto.LoginRequest;
import com.docuvault.docuvault.dto.RefreshTokenRequest;
import com.docuvault.docuvault.dto.RegisterRequest;
import com.docuvault.docuvault.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_shouldReturnSuccess() {

        RegisterRequest request = new RegisterRequest();

        doNothing().when(authService).register(request);

        ResponseEntity<String> response =
                authController.register(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "User registered successfully",
                response.getBody()
        );

        verify(authService).register(request);
    }

    @Test
    void login_shouldReturnAuthResponse() {

        LoginRequest request = new LoginRequest();

        AuthResponse authResponse =
                new AuthResponse(
                        "access-token",
                        "refresh-token"
                );

        when(authService.login(request))
                .thenReturn(authResponse);

        ResponseEntity<AuthResponse> response =
                authController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(authResponse, response.getBody());

        verify(authService).login(request);
    }

    @Test
    void refresh_shouldReturnNewAuthResponse() {

        RefreshTokenRequest request =
                new RefreshTokenRequest();

        request.setRefreshToken("refresh-token");

        AuthResponse authResponse =
                new AuthResponse(
                        "new-access-token",
                        "refresh-token"
                );

        when(authService.refreshToken("refresh-token"))
                .thenReturn(authResponse);

        ResponseEntity<AuthResponse> response =
                authController.refresh(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(authResponse, response.getBody());

        verify(authService)
                .refreshToken("refresh-token");
    }
}