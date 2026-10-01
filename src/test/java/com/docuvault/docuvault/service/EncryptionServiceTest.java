package com.docuvault.docuvault.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();

        byte[] key = new byte[32];
        String base64Key = Base64.getEncoder().encodeToString(key);

        ReflectionTestUtils.setField(
                encryptionService,
                "encryptionKey",
                base64Key
        );
    }

    @Test
    void encryptAndDecrypt_shouldReturnOriginalData() {

        byte[] originalData =
                "DocuVault test data".getBytes(StandardCharsets.UTF_8);

        byte[] encryptedData =
                encryptionService.encrypt(originalData);

        byte[] decryptedData =
                encryptionService.decrypt(encryptedData);

        assertArrayEquals(originalData, decryptedData);
    }

    @Test
    void encrypt_shouldProduceDifferentOutput() {

        byte[] data =
                "DocuVault test data".getBytes(StandardCharsets.UTF_8);

        byte[] encryptedData =
                encryptionService.encrypt(data);

        assertNotEquals(
                new String(data, StandardCharsets.UTF_8),
                new String(encryptedData, StandardCharsets.UTF_8)
        );
    }

    @Test
    void decrypt_invalidData_shouldThrowException() {

        byte[] invalidData = new byte[20];

        assertThrows(
                RuntimeException.class,
                () -> encryptionService.decrypt(invalidData)
        );
    }
}