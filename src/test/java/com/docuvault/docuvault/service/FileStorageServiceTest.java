package com.docuvault.docuvault.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @Test
    void storeFile_shouldStoreFile() throws Exception {

        FileStorageService service = new FileStorageService();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello DocuVault".getBytes()
        );

        String filePath = service.storeFile(file);

        Path path = Paths.get(filePath);

        assertTrue(Files.exists(path));
        assertEquals(
                "Hello DocuVault",
                Files.readString(path)
        );

        Files.deleteIfExists(path);
    }

    @Test
    void storeEncryptedFile_shouldStoreEncryptedData() throws Exception {

        FileStorageService service = new FileStorageService();

        byte[] encryptedData = "Encrypted Data".getBytes();

        String filePath =
                service.storeEncryptedFile(
                        encryptedData,
                        "test.txt"
                );

        Path path = Paths.get(filePath);

        assertTrue(Files.exists(path));
        assertArrayEquals(
                encryptedData,
                Files.readAllBytes(path)
        );

        Files.deleteIfExists(path);
    }
}