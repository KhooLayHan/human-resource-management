package org.bhel.hrm.common.utils;

import org.bhel.hrm.common.config.Configuration;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Enterprise-grade encryption utility using AES-GCM.
 * Replaces the insecure XOR implementation.
 */
public class CryptoUtils {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // Authentication Tag Length
    private static final int IV_LENGTH_BYTE = 12; // Standard IV length for GCM
    private static final int SALT_LENGTH_BYTE = 16;
    private static final int KEY_LENGTH_BIT = 256;
    private static final int ITERATION_COUNT = 65_536;

    // How to inject via .env...? "BHEL_HRM_SUPER_SECRET_MASTER_KEY_2026"
    private static final String MASTER_SECRET = new Configuration().getSecretKey();

    private CryptoUtils() {
        throw new UnsupportedOperationException("CryptoUtils is a utility class and should not be instantiated.");
    }

    /**
     * Encrypts text using AES-GCM.
     * Generates a random IV and Salt for every encryption operation.
     * Output format (Base64): [Salt (16b) | IV (12b) | Encrypted Data]
     */
    public static String encrypt(String plainText) throws Exception {
        // 1. Generate random Salt and IV
        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[SALT_LENGTH_BYTE];
        random.nextBytes(salt);

        byte[] iv = new byte[IV_LENGTH_BYTE];
        random.nextBytes(iv);

        // 2. Derive strong key from password and salt
        SecretKey secretKey = deriveKey(MASTER_SECRET, salt);

        // 3. Encrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        byte[] cipherText = cipher.doFinal(plainText.getBytes());

        // 4. Combine Salt, IV and cipherText
        ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
        byteBuffer.put(salt);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * Decrypts AES-GCM encrypted text.
     * Expects input format: [Salt (16b) | IV (12b) | Encrypted Data]
     */
    public static String decrypt(String encryptedbase64) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedbase64);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

        // 1. Extract Salt and IV
        byte[] salt = new byte[SALT_LENGTH_BYTE];
        byteBuffer.get(salt);

        byte[] iv = new byte[IV_LENGTH_BYTE];
        byteBuffer.get(iv);

        // 2. Extract cipherText
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        // 3. Derive key (must be same with encryption)
        SecretKey secretKey = deriveKey(MASTER_SECRET, salt);

        // 4. Decrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
        byte[] plainTextBytes = cipher.doFinal(cipherText);

        return new String(plainTextBytes);
    }

    /**
     * PBKDF2 Key Derivation
     */
    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT);
        SecretKey temp = factory.generateSecret(spec);

        return new SecretKeySpec(temp.getEncoded(), "AES");
    }
}
