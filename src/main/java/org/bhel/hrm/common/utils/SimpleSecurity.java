package org.bhel.hrm.common.utils;

import org.bhel.hrm.server.config.Configuration;

import javax.swing.*;
import java.util.Base64;

/**
 * A simple XOR cipher to demonstrate the concept of securing communication.
 * For academic demonstration purposes only.
 */
public class SimpleSecurity {
//    private static final String SECRET_KEY = "BHEL_HRM_SECRET_KEY_2026";

    private static Configuration configuration;

    private SimpleSecurity() {
        throw new UnsupportedOperationException("SimpleSecurity is a utility class and should not be instantiated.");
    }

    public static String encrypt(String plainText) {
        byte[] xorResult = xorWithKey(plainText.getBytes());
        return Base64.getEncoder().encodeToString(xorResult);
    }

    public static String decrypt(String encryptedBase64) {
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedBase64);
        return new String(xorWithKey(decodedBytes));
    }

    private static byte[] xorWithKey(byte[] data) {
        byte[] keyBytes = configuration.getSecretKey().getBytes();
        byte[] output = new byte[data.length];

        for (int i = 0; i < data.length; i++)
            output[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);

        return output;
    }
}
