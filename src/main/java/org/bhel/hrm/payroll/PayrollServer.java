package org.bhel.hrm.payroll;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.error.ErrorContext;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple, multithreaded socket server to simulate a Payroll System (PRS).
 * It listens on a port, accepts one connection at a time, reads a single line of data,
 * prints it, and then waits for the next connection.
 */
public class PayrollServer {
    private static final Logger logger = LoggerFactory.getLogger(PayrollServer.class);

    private static final int MAX_THREADS = 20;
    private static final int QUEUE_SIZE = 50;

    private final Configuration configuration;
    private final SslContextFactory sslContextFactory;
    private final CryptoUtils cryptoUtils;
    private final ExecutorService executorService;
    private final AtomicBoolean running;
    private SSLServerSocket serverSocket;

    public PayrollServer(
        Configuration configuration,
        SslContextFactory sslContextFactory,
        CryptoUtils cryptoUtils
    ) {
        if (configuration == null || sslContextFactory == null || cryptoUtils == null)
            throw new IllegalArgumentException("Dependencies cannot be null");

        this.configuration = configuration;
        this.sslContextFactory = sslContextFactory;
        this.cryptoUtils = cryptoUtils;
        this.running = new AtomicBoolean(false);

        this.executorService = new ThreadPoolExecutor(
            5,
            MAX_THREADS,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_SIZE),
            new ThreadFactory() {
                private int counter = 0;

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "PayrollServer-Worker-" + counter++);
                    t.setDaemon(false);

                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void start() {
        if (running.getAcquire()) {
            logger.warn("Server is already running...");
            return;
        }

        int port = configuration.getPayrollPort();
        running.set(true);

        try {
            SSLServerSocketFactory sslFactory = sslContextFactory.createSslContext().getServerSocketFactory();
            serverSocket = (SSLServerSocket) sslFactory.createServerSocket(port);

            // Configure SSL parameters
            serverSocket.setEnabledCipherSuites(SslContextFactory.getPreferredCipherSuites());
            serverSocket.setEnabledProtocols(SslContextFactory.getEnabledProtocols());

            serverSocket.setSoTimeout(1_000); // Checks running flag every second

            logger.info("Payroll System (PRS) Server started successfully on port {}", port);
            logger.info("Enabled protocols: {}",
                    String.join(", ", SslContextFactory.getEnabledProtocols()));

            acceptConnections();
        } catch (SocketTimeoutException e) {
          // Expected — allows for periodic checking of running flag
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            running.set(false);
        }
    }

    private void acceptConnections() {
        while (running.get()) {
            try {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                executorService.submit(new PayrollClientHandler(clientSocket, cryptoUtils));

                logger.info("Accepted connection from {}", clientSocket.getRemoteSocketAddress());
            } catch (SocketTimeoutException e) {
                // Normal timeout, continue checking running flag
            } catch (IOException e) {
                if (running.get()) {
                    ErrorContext context = ErrorContext.forOperation("payroll.server.accept");
                    logger.error("Error accepting connection. [{}]", context, e);
                }
            }
        }
    }

    public void shutdown() {
        logger.info("Initiating shutdown...");
        running.set(false);

        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException e) {
            logger.debug("Error closing server socket.", e);
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Server shutdown complete");
    }
}
