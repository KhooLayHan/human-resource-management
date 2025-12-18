package org.bhel.hrm.common.utils;

import org.bhel.hrm.server.config.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A simple XOR cipher to demonstrate the concept of securing communication.
 * For academic demonstration purposes only.
 */
public class SimpleSecurity {
    private static final Configuration configuration = new Configuration();

    private SimpleSecurity() {
        throw new UnsupportedOperationException("SimpleSecurity is a utility class and should not be instantiated.");
    }

    public static String encrypt(String plainText) {
        byte[] xorResult = xorWithKey(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(xorResult);
    }

    public static String decrypt(String encryptedBase64) {
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedBase64);
        return new String(xorWithKey(decodedBytes), StandardCharsets.UTF_8);
    }

    private static byte[] xorWithKey(byte[] data) {
        String secretKey = configuration.getSecretKey();
        if (secretKey == null || secretKey.isEmpty())
            throw new IllegalStateException("Secret key is not configured.");

        byte[] keyBytes = secretKey.getBytes();
        byte[] output = new byte[data.length];

        for (int i = 0; i < data.length; i++)
            output[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);

        return output;
    }
}
