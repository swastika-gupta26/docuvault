package com.docuvault.docuvault.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageLocation = Paths.get("uploads");

    public FileStorageService() throws IOException {
        Files.createDirectories(storageLocation);
    }

    public String storeFile(MultipartFile file) throws IOException {

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Path filePath = storageLocation.resolve(fileName);

        Files.copy(file.getInputStream(), filePath);

        return filePath.toString();
    }
}