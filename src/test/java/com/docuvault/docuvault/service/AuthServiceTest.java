package com.docuvault.docuvault.service;

import com.docuvault.docuvault.dto.AuthResponse;
import com.docuvault.docuvault.dto.LoginRequest;
import com.docuvault.docuvault.dto.RegisterRequest;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .build();
    }

    // ---------------------------------------------------------
    // REGISTER
    // ---------------------------------------------------------

    @Test
    void register_shouldRegisterUser() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        authService.register(request);

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void register_shouldThrowExceptionWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(request)
        );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    // ---------------------------------------------------------
    // LOGIN
    // ---------------------------------------------------------

    @Test
    void login_shouldReturnTokens() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encodedPassword"
        )).thenReturn(true);

        when(jwtService.generateAccessToken("test@example.com"))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken("test@example.com"))
                .thenReturn("refresh-token");

        AuthResponse result =
                authService.login(request);

        assertNotNull(result);

        verify(jwtService)
                .generateAccessToken("test@example.com");

        verify(jwtService)
                .generateRefreshToken("test@example.com");
    }

    @Test
    void login_shouldThrowExceptionForInvalidEmail() {

        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("wrong@example.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );
    }

    @Test
    void login_shouldThrowExceptionForInvalidPassword() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrongPassword",
                "encodedPassword"
        )).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // REFRESH TOKEN
    // ---------------------------------------------------------

    @Test
    void refreshToken_shouldReturnNewAccessToken() {

        String refreshToken = "refresh-token";

        when(jwtService.isTokenValid(refreshToken))
                .thenReturn(true);

        when(jwtService.extractEmail(refreshToken))
                .thenReturn("test@example.com");

        when(jwtService.generateAccessToken("test@example.com"))
                .thenReturn("new-access-token");

        AuthResponse result =
                authService.refreshToken(refreshToken);

        assertNotNull(result);

        verify(jwtService)
                .generateAccessToken("test@example.com");
    }

    @Test
    void refreshToken_shouldThrowExceptionForInvalidToken() {

        String refreshToken = "invalid-token";

        when(jwtService.isTokenValid(refreshToken))
                .thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.refreshToken(refreshToken)
        );

        assertEquals(
                "Invalid or expired refresh token",
                exception.getMessage()
        );

        verify(jwtService, never())
                .extractEmail(anyString());

        verify(jwtService, never())
                .generateAccessToken(anyString());
    }
}