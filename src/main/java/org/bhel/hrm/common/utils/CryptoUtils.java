package org.bhel.hrm.common.utils;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.exceptions.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.*;

/**
 * Enterprise-grade encryption utility using AES-GCM.
 */
public class CryptoUtils {
    private static final Logger logger = LoggerFactory.getLogger(CryptoUtils.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // Authentication Tag Length
    private static final int IV_LENGTH_BYTE = 12; // Standard IV length for GCM
    private static final int SALT_LENGTH_BYTE = 16;
    private static final int KEY_LENGTH_BIT = 256;
    private static final int ITERATION_COUNT = 65_536; // or 100_000

    private final Configuration configuration;
    private final SecureRandom secureRandom;
    private final KeyCache keyCache;

    public CryptoUtils(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        this.configuration = configuration;
        this.secureRandom = new SecureRandom();
        this.keyCache = new KeyCache();
    }

    /**
     * Encrypts text using AES-GCM with authenticated encryption.
     * Output format (Base64): [Salt (16b) | IV (12b) | CipherText + Auth Tag]
     *
     * @param plainText The text to encrypt
     * @return Base64-encoded encrypted data
     * @throws CryptoException If encryption fails
     */
    public String encrypt(String plainText) throws CryptoException {
        if (plainText == null || plainText.isEmpty())
            throw new IllegalArgumentException("Plaintext cannot be null or empty");

        try {
            // 1. Generate random Salt and IV
            byte[] salt = generateRandomBytes(SALT_LENGTH_BYTE);
            byte[] iv = generateRandomBytes(IV_LENGTH_BYTE);

            // 2. Derive strong key from password and salt
            SecretKey secretKey = deriveKey(salt);

            // 3. Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 4. Combine Salt, IV and cipherText
            ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
            byteBuffer.put(salt);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new CryptoException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypts AES-GCM encrypted text with authentication verification.
     * Expects input format: [Salt (16b) | IV (12b) | CipherText + Auth Tag]
     *
     * @param encryptedBase64 Base64-encoded encrypted data
     * @return Decrypted plaintext
     * @throws CryptoException If decryption or authentication fails
     */
    public String decrypt(String encryptedBase64) throws CryptoException {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            final int MIN_DECODER_LENGTH_BYTE = SALT_LENGTH_BYTE + IV_LENGTH_BYTE + TAG_LENGTH_BIT / 8;
            if (decoded.length < MIN_DECODER_LENGTH_BYTE) {
                throw new IllegalArgumentException(
                    String.format("Encrypted data is too short. Expected at least %d bytes, received %d instead.",
                    MIN_DECODER_LENGTH_BYTE, decoded.length
                ));
            }

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
            SecretKey secretKey = deriveKey(salt);

            // 4. Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Decryption failed - data may be corrupted or tampered", e);
            throw new CryptoException("Failed to decrypt data", e);
        }
    }

    /**
     * Derives a cryptographic key using PBKDF2 with salt.
     */
    private SecretKey deriveKey(byte[] salt) throws Exception {
        String masterSecret = configuration.getSecretKey();

        if (masterSecret == null || masterSecret.isBlank()) {
            throw new IllegalStateException("Master secret key not configured");
        }

        // Check cache first
        SecretKey cachedKey = keyCache.get(salt);
        if (cachedKey != null)
            return cachedKey;

        // Derive a new key
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            masterSecret.toCharArray(),
            salt,
            ITERATION_COUNT,
            KEY_LENGTH_BIT
        );

        SecretKey temp = factory.generateSecret(spec);
        SecretKey derivedKey = new SecretKeySpec(temp.getEncoded(), "AES");

        // Cache for future use
        keyCache.put(salt, derivedKey);

        return derivedKey;
    }

    /**
     * Generates cryptographically strong random bytes.
     */
    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * Clears the key cache. Useful for security or testing purposes.
     */
    public synchronized void clearCache() {
        keyCache.clear();
        logger.debug("Key cache cleared");
    }

    /**
     * Simple cache for derived keys to improve performance.
     */
    private static class KeyCache {
        private static final int MAX_CACHE_SIZE = 10;

        private final LinkedHashMap<ByteArrayWrapper, SecretKey> cache;

        public KeyCache() {
            this.cache = new LinkedHashMap<>(
                MAX_CACHE_SIZE + 1,
                0.75f,
                true
            ) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<ByteArrayWrapper, SecretKey> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };
        }

        synchronized SecretKey get(byte[] salt) {
            return cache.get(new ByteArrayWrapper(salt));
        }

        synchronized void put(byte[] salt, SecretKey key) {
            cache.put(new ByteArrayWrapper(salt), key);
        }

        synchronized void clear() {
            cache.clear();
        }
    }

    /**
     * Wrapper for byte arrays to use as HashMap keys.
     */
    private static class ByteArrayWrapper {
        private final byte[] data;
        private final int hashCode;

        public ByteArrayWrapper(byte[] data) {
            this.data = data.clone();
            this.hashCode = Arrays.hashCode(data);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ByteArrayWrapper))
                return false;

            return Arrays.equals(data,((ByteArrayWrapper) obj).data);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
