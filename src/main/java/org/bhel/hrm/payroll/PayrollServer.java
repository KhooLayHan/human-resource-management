package org.bhel.hrm.payroll;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * A simple, single-threaded socket server to simulate a Payroll System (PRS).
 * It listens on a port, accepts one connection at a time, reads a single line of data,
 * prints it, and then waits for the next connection.
 */
public class PayrollServer {
    private static final Logger logger = LoggerFactory.getLogger(PayrollServer.class);

    public static void main(String[] args) {
        Configuration configuration = new Configuration();
        int port = configuration.getPayrollPort();
//        if (portStr == null || portStr.isBlank()) {
//            logger.error("Payroll port is not configured. Set 'payroll.port' in config.properties.");
//            return;
//        }
//
//        int payrollPort;
//        try {
//            payrollPort = Integer.parseInt(portStr);
//        } catch (NumberFormatException e) {
//            logger.error("Invalid payroll port: {}", portStr);
//            return;
//        }

        logger.info("Payroll System (PRS) Server is starting on port {}", port);

        try {
            SSLServerSocketFactory sslFactory = SslContextFactory.createSslContext().getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) sslFactory.createServerSocket(port);

            // Optional: Require client authentication (Mutual TLS)
            // serverSocket.setNeedClientAuth(true);

            while (true) {
                logger.info("Waiting to secure a connection...");

                // Blocks until a client connects
                try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
                    clientSocket.startHandshake(); // Explicit handshake to verify SSL immediately.
                    logger.info("Secure connection established from: {}", clientSocket.getInetAddress());

                    InputStreamReader inputReader = new InputStreamReader(clientSocket.getInputStream());
                    BufferedReader reader = new BufferedReader(inputReader);

                    // Reads one line of data as the secure message
                    String encryptedData = reader.readLine();

                    if (encryptedData == null) {
                        logger.warn("Received empty payload from HRM system. Closing connection.");
                        continue;
                    }

                    logger.debug("Received CipherText: {}", encryptedData);

                    // Decrypt using AES-GCM
                    String decryptedData = CryptoUtils.decrypt(encryptedData);
                    logger.debug("Decrypted payroll instruction: {}", decryptedData);
                } catch (Exception e) {
                    logger.error("Error processing client connection", e);
                }
            }
        } catch (Exception e) {
            logger.error("Payroll server error: {}", e.getMessage(), e);
        }
    }
}
