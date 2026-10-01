package com.docuvault.docuvault.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private final String secretKey =
            "my-super-secret-key-for-jwt-token-generation-123456789";

    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                secretKey
        );
    }

    // ---------------------------------------------------------
    // ACCESS TOKEN
    // ---------------------------------------------------------

    @Test
    void generateAccessToken_shouldGenerateToken() {

        String token =
                jwtService.generateAccessToken(
                        "test@example.com"
                );

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals(
                "test@example.com",
                jwtService.extractEmail(token)
        );
    }

    // ---------------------------------------------------------
    // REFRESH TOKEN
    // ---------------------------------------------------------

    @Test
    void generateRefreshToken_shouldGenerateToken() {

        String token =
                jwtService.generateRefreshToken(
                        "test@example.com"
                );

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals(
                "test@example.com",
                jwtService.extractEmail(token)
        );
    }

    // ---------------------------------------------------------
    // EXTRACT EMAIL
    // ---------------------------------------------------------

    @Test
    void extractEmail_shouldReturnEmail() {

        String token =
                jwtService.generateAccessToken(
                        "test@example.com"
                );

        String email =
                jwtService.extractEmail(token);

        assertEquals(
                "test@example.com",
                email
        );
    }

    // ---------------------------------------------------------
    // VALID TOKEN
    // ---------------------------------------------------------

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {

        String token =
                jwtService.generateAccessToken(
                        "test@example.com"
                );

        assertTrue(
                jwtService.isTokenValid(token)
        );
    }

    // ---------------------------------------------------------
    // INVALID TOKEN
    // ---------------------------------------------------------

    @Test
    void isTokenValid_shouldReturnFalseForInvalidToken() {

        String invalidToken = "invalid.jwt.token";

        assertFalse(
                jwtService.isTokenValid(invalidToken)
        );
    }

    @Test
    void isTokenValid_shouldReturnFalseForTokenSignedWithDifferentKey() {

        JwtService anotherJwtService = new JwtService();

        ReflectionTestUtils.setField(
                anotherJwtService,
                "secretKey",
                "another-secret-key-for-jwt-token-123456789"
        );

        String token =
                anotherJwtService.generateAccessToken(
                        "test@example.com"
                );

        assertFalse(
                jwtService.isTokenValid(token)
        );
    }
}