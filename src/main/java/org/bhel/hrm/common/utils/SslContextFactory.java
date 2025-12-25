package org.bhel.hrm.common.utils;

import org.bhel.hrm.common.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;

/**
 * Factory for creating secure SSLContext instances with enhanced security settings.
 * Uses dependency injection (DI) pattern.
 */
public class SslContextFactory {
    private static final Logger logger = LoggerFactory.getLogger(SslContextFactory.class);

    private final Configuration configuration;
    private SSLContext cachedSslContext;

    public SslContextFactory(Configuration configuration) {
        if (configuration == null)
            throw new IllegalArgumentException("Configuration cannot be null");

        this.configuration = configuration;
    }

    /**
     * Creates or returns cached SSLContext with secure configuration.
     * Thread-safe implementation using synchronized method.
     */
    public synchronized SSLContext createSslContext() throws
        KeyStoreException,
        NoSuchAlgorithmException,
        IOException,
        CertificateException,
        KeyManagementException,
        UnrecoverableKeyException {

        if (cachedSslContext == null) {
            cachedSslContext = initializeSslContext();

            logger.info("SSLContext initialized successfully with protocols: {}",
                Arrays.toString(TLSProtocol.toStringArray()));
        }

        return cachedSslContext;
    }

    private SSLContext initializeSslContext() throws
        KeyStoreException,
        NoSuchAlgorithmException,
        IOException,
        CertificateException,
        KeyManagementException,
        UnrecoverableKeyException {

        String keystorePassword = configuration.getKeystorePassword();
        String keystorePath = configuration.getKeystorePath();

        validateConfiguration(keystorePassword, keystorePath);

        // 1. Loads the KeyStore
        KeyStore keyStore = loadKeyStore(keystorePassword, keystorePath);

        // 2. Initialize the KeyManager
        KeyManagerFactory kmf = initializeKeyManager(keystorePassword, keyStore);

        // 3. Initialize the TrustManager
        TrustManagerFactory tmf = initializeTrustManager(keyStore);

        // 4. Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Uses SecureRandom for better entropy
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    private void validateConfiguration(String keystorePassword, String keystorePath) {
        if (keystorePath == null || keystorePath.isBlank())
            throw new IllegalStateException("Keystore path not configured. Set keystore.path property.");

        if (keystorePassword == null || keystorePassword.isBlank())
            throw new IllegalStateException("Keystore password not configured. Set keystore.password property.");
    }

    private KeyStore loadKeyStore(
        String keystorePassword,
        String keystorePath
    ) throws
        KeyStoreException,
        IOException,
        NoSuchAlgorithmException,
        CertificateException
    {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream keyStoreInput = SslContextFactory.class.getClassLoader().getResourceAsStream(keystorePath)) {
            if (keyStoreInput == null)
                throw new IllegalStateException("Keystore file not found in classpath: " + keystorePath);

            keyStore.load(keyStoreInput, keystorePassword.toCharArray());
            logger.debug("Keystore loaded successfully from: {}", keystorePath);
        }

        return keyStore;
    }

    private KeyManagerFactory initializeKeyManager(
        String keystorePassword,
        KeyStore keyStore
    ) throws
        KeyStoreException,
        NoSuchAlgorithmException,
        UnrecoverableKeyException
    {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        return kmf;
    }

    private TrustManagerFactory initializeTrustManager(
        KeyStore keyStore
    ) throws
        KeyStoreException,
        NoSuchAlgorithmException
    {
        // Assume that the certificate is self-signed and trusts own keystore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        return tmf;
    }

    /**
     * Returns the preferred cipher suites for secure communication.
     */
    public static String[] getPreferredCipherSuites() {
        return CipherSuite.toStringArray();
    }

    /**
     * Returns the enabled protocols (TLSv1.3, TLSv1.2).
     */
    public static String[] getEnabledProtocols() {
        return TLSProtocol.toStringArray();
    }

    /**
     * Clears the cached SSLContext. Useful for testing or reloading certificates.
     */
    public synchronized void clearCache() {
        cachedSslContext = null;
        logger.info("SSLContext cache cleared");
    }

    /**
     * Checks if SSLContext is cached.
     */
    public synchronized boolean isCached() {
        return cachedSslContext != null;
    }
}
