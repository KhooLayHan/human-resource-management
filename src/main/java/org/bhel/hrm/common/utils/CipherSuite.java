package org.bhel.hrm.common.utils;

import java.util.stream.Stream;

/**
 * Recommended cipher suites for secure communication.
 */
public enum CipherSuite {
    // TLS 1.3 Cipher Suites
    TLS_AES_256_GCM_SHA384("TLS_AES_256_GCM_SHA384"),
    TLS_AES_128_GCM_SHA256("TLS_AES_128_GCM_SHA256"),
    TLS_CHACHA20_POLY1305_SHA256("TLS_CHACHA20_POLY1305_SHA256"),

    // TLS 1.2 Fallback Cipher Suites
    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"),
    TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256("TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256");

    private final String suite;

    CipherSuite(String suite) {
        this.suite = suite;
    }

    public String getSuite() {
        return suite;
    }

    public static String[] toStringArray() {
        return Stream.of(values())
            .map(CipherSuite::getSuite)
            .toArray(String[]::new);
    }
}
