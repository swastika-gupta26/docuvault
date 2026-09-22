package com.docuvault.docuvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    @Value("${encryption.key}")
    private String encryptionKey;

    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encrypt(byte[] data) {

        try {
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            GCMParameterSpec spec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec);

            byte[] encryptedData = cipher.doFinal(data);

            byte[] result = new byte[iv.length + encryptedData.length];

            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(
                    encryptedData,
                    0,
                    result,
                    iv.length,
                    encryptedData.length
            );

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) {

        try {
            byte[] iv = new byte[IV_LENGTH];

            System.arraycopy(
                    encryptedData,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            byte[] actualEncryptedData =
                    new byte[encryptedData.length - IV_LENGTH];

            System.arraycopy(
                    encryptedData,
                    IV_LENGTH,
                    actualEncryptedData,
                    0,
                    actualEncryptedData.length
            );

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            GCMParameterSpec spec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);

            return cipher.doFinal(actualEncryptedData);

        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}