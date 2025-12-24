package org.bhel.hrm.common.utils;

import org.bhel.hrm.common.config.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public class SslContextFactory {
    private static final Configuration configuration = new Configuration();

    private SslContextFactory() {
        throw new UnsupportedOperationException("SslContextFactory is a utility class and should not be instantiated.");
    }

    public static SSLContext createSslContext() throws
        KeyStoreException,
        NoSuchAlgorithmException,
        IOException,
        CertificateException,
        KeyManagementException,
        UnrecoverableKeyException {
        String keystorePassword = configuration.getKeystorePassword();
        String keystorePath = configuration.getKeystorePath();

        if (keystorePath == null || keystorePassword == null) {
            throw new IllegalStateException("Keystore configuration missing. Set keystore.path and keystore.password.");
        }

        // 1. Loads the KeyStore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream keyStoreInput = SslContextFactory.class.getClassLoader().getResourceAsStream(keystorePath)) {
            if (keyStoreInput == null)
                throw new IllegalStateException("Keystore file not found in classpath: " + keystorePath);

            keyStore.load(keyStoreInput, keystorePassword.toCharArray());
        }

        // 2. Initialize the KeyManager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        // 3. Initialize the TrustManager
        // Assume that the certificate is self-signed and trusts own keystore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // 4. Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }
}
