package org.bhel.hrm.server.services;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.bhel.hrm.server.domain.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A client for sending secure messages to the Payroll System (PRS) via sockets.
 */
public class PayrollSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(PayrollSocketClient.class);

    private static final int SOCKET_TIMEOUT_MS = 30_000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int BASE_RETRY_DELAY_MS = 1_000;

    private final String host;
    private final int port;
    private final SslContextFactory sslContextFactory;
    private final CryptoUtils cryptoUtils;
    private final SSLSocketFactory sslSocketFactory;
    private final ExecutorService executorService;

    public PayrollSocketClient(
        Configuration configuration,
        SslContextFactory sslContextFactory,
        CryptoUtils cryptoUtils
    ) {
        this.host = configuration.getPayrollHost();
        this.port = configuration.getPayrollPort();
        this.sslContextFactory = sslContextFactory;
        this.cryptoUtils = cryptoUtils;

        try {
            this.sslSocketFactory = sslContextFactory.createSslContext().getSocketFactory();
        } catch (Exception e) {
            logger.error("Failed to initialize SSL context", e);
            throw new RuntimeException("SSL initialization failed", e);
        }

        // Thread pool for async operations
        this.executorService = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "PayrollClient-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Notifies the PRS about a new employee.
     *
     * @param employee The newly registered employee.
     */
    public boolean notifyNewEmployee(Employee employee) {
        if (employee == null)
            throw new IllegalArgumentException("Employee cannot be null");

        String message = buildPayrollMessage(employee);
        logger.info("Initiating payroll notification for employee ID: {}", employee.getId());

        try {
            return sendSecureMessage(message);
        } catch (SocketTimeoutException e) {
            logger.warn("Connection timeout.", e);
        } catch (IOException e) {
            logger.warn("Network error: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error", e);
        }

        logger.error("Failed to notify PRS for Employee ID: {}", employee.getId());
        return false;
    }

    /**
     * Async notification that returns a Future of a new employee.
     */
    public CompletableFuture<Boolean> notifyNewEmployeeAsync(Employee employee) {
        return CompletableFuture.supplyAsync(() -> notifyNewEmployee(employee), executorService);
    }

    /**
     * Sends encrypted message and validates response.
     */
    private boolean sendSecureMessage(String message) throws Exception {
        String encryptedPayload = cryptoUtils.encrypt(message);

        try (
            SSLSocket socket = createSecureSocket();
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            InputStreamReader inputReader = new InputStreamReader(socket.getInputStream());
            BufferedReader reader = new BufferedReader(inputReader);
        ) {
            // Sends encrypted data
            writer.println(encryptedPayload);
            logger.debug("Encrypted payload send with {} bytes.", encryptedPayload.length());

            // Waiting for acknowledgement
            String response = reader.readLine();

            if (response == null) {
                logger.warn("No response received from server.");
                return false;
            }

            return validateResponse(response);

        } catch (Exception e) {
            logger.error(
                "Failed to send notification to PRS at {}:{}. Is the PayrollServer running?",
                host, port, e
            );
        }
    }

    /**
     * Creates and configure a secure SSL socket.
     */
    private SSLSocket createSecureSocket() throws IOException {
        SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port);

        socket.setSoTimeout(SOCKET_TIMEOUT_MS);

        socket.setEnabledProtocols(SslContextFactory.getEnabledProtocols());
        socket.setEnabledCipherSuites(SslContextFactory.getPreferredCipherSuites());

        socket.startHandshake(); // Ensure SSL handshake succeeds before sending data
        logger.debug("SSL handshake completed successfully");

        return socket;
    }

    /**
     * Validates server response.
     */
    private boolean validateResponse(String response) {
        if (response.startsWith("ACK:SUCCESS")) {
            logger.info("Server acknowledged: Success");
            return true;
        } else if (response.startsWith("ERROR:")) {
            String errorCode = response.substring(6);
            logger.error("Server returned error: {}", errorCode);
            return false;
        } else {
            logger.warn("Unexpected server response: {}", response);
            return false;
        }
    }

    /**
     * Builds formatted payroll message.
     */
    private String buildPayrollMessage(Employee employee) {
        return String.format(
            "ACTION=NEW_HIRE;ID=%d;FIRST_NAME=%s;LAST_NAME=%s;IC=%s;TIMESTAMP=%d",
            employee.getId(),
            sanitize(employee.getFirstName()),
            sanitize(employee.getLastName()),
            sanitize(employee.getIcPassport()),
            Instant.now().toEpochMilli()
        );
    }

    /**
     * Sanitizes input to prevent injection attacks.
     */
    private String sanitize(String input) {
        if (input == null)
            return "";

        // not production-ready...
        return input.replaceAll("[;=\n\r]", "_");
    }

    /**
     * Calculates exponential backoff delay.
     */
    private int calculateRetryDelay(int attempt) {
        return BASE_RETRY_DELAY_MS * (int) Math.pow(2, attempt - 1);
    }

    /**
     * Sleep helper that handles interruption.
     */
    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep interrupted", e);
        }
    }

    /**
     * Tests connection to payroll server.
     */
    public boolean testConnection() {
        logger.info("Testing connection to {}:{}", host, port);

        try (SSLSocket socket = createSecureSocket()) {
            logger.info("Connection test successful");
            return true;
        } catch (Exception e) {
            logger.error("Connection test failed", e);
            return false;
        }
    }

    /**
     * Gracefully shuts down the client.
     */
    public void shutdown() {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("PayrollSocketClient has shut down.");
    }
}
