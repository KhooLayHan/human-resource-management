package org.bhel.hrm.payroll;

import org.bhel.hrm.common.exceptions.CryptoException;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;

/**
 * Handles individual client connections.
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private static final int SOCKET_TIMEOUT_MS = 30_000;

    private final SSLSocket clientSocket;
    private final CryptoUtils cryptoUtils;

    ClientHandler(SSLSocket clientSocket, CryptoUtils cryptoUtils) {
        this.clientSocket = clientSocket;
        this.cryptoUtils = cryptoUtils;
    }

    @Override
    public void run() {
        try {
            clientSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
            clientSocket.startHandshake();

            String clientAddress = clientSocket.getInetAddress().toString();
            logger.info("Secure connection established from: {}", clientAddress);

            try (
                InputStreamReader inputReader = new InputStreamReader(clientSocket.getInputStream());
                BufferedReader reader = new BufferedReader(inputReader);
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                // Reads one line of data as the secure message
                String encryptedData = reader.readLine();

                if (encryptedData == null || encryptedData.isBlank()) {
                    logger.warn("Received empty payload from {}. Closing connection.", clientAddress);
                    writer.println("ERROR:EMPTY_PAYLOAD");

                    return;
                }

                logger.debug("Received encrypted data ({} bytes) from {}",
                    encryptedData.length(), clientAddress);

                try {
                    // Decrypt using AES-GCM
                    String decryptedData = cryptoUtils.decrypt(encryptedData);
                    logger.debug("Decrypted payroll instruction from {}.",
                        clientAddress);

                    // Process the payroll instruction
                    boolean success = processPayrollInstruction(decryptedData);

                    if (success) {
                        writer.println("ACK:SUCCESS");
                        logger.info("Payroll instruction processed successfully.");
                    } else {
                        writer.println("ERROR:PROCESSING_FAILED");
                        logger.error("Failed to process payroll instruction.");
                    }
                } catch (CryptoException e) {
                    logger.error("Decryption failed for connection from {}", clientAddress, e);
                    writer.println("ERROR:DECRYPTION_FAILED");
                }
            }
        } catch (SocketTimeoutException e) {
            logger.warn("Connection timeout from {}", clientSocket.getInetAddress());
        } catch (Exception e) {
            logger.error("Error processing client connection", e);
        } finally {
            closeSocket();
        }
    }

    private boolean processPayrollInstruction(String instruction) {
        logger.debug("Processing instruction: {}", instruction);

        // TODO: Implement actual payroll processing logic
        return instruction.contains("ACTION=") && instruction.contains("ID=");
    }

    private void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed())
                clientSocket.close();
        } catch (IOException e) {
            logger.debug("Error closing client socket.");
        }
    }
}
