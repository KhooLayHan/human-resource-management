package org.bhel.hrm.common.utils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

public class SslContextFactory {
    private static final String KEYSTORE_PASSWORD = "password123";
    private static final String KEYSTORE_PATH = "payroll_keystore.p12";

    private SslContextFactory() {
        throw new UnsupportedOperationException("SslContextFactory is a utility class and should not be instantiated.");
    }

    public static SSLContext createSslContext() throws Exception {
        // 1. Loads the KeyStore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream keyStoreInput = SslContextFactory.class.getClassLoader().getResourceAsStream(KEYSTORE_PATH)) {
            if (keyStoreInput == null)
                throw new IllegalStateException("Keystore file not found in classpath: " + KEYSTORE_PATH);

            keyStore.load(keyStoreInput, KEYSTORE_PASSWORD.toCharArray());
        }

        // 2. Initialize the KeyManager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        // 3. Initialize the TrustManager
        // Assume that the certificate is self-signed and trusts own keystore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // 4. Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }
}
