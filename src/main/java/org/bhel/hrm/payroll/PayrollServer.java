package org.bhel.hrm.payroll;

import org.bhel.hrm.common.utils.SimpleSecurity;
import org.bhel.hrm.server.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A simple, single-threaded socket server to simulate a Payroll System (PRS).
 * It listens on a port, accepts one connection at a time, reads a single line of data,
 * prints it, and then waits for the next connection.
 */
public class PayrollServer {
    private static final Logger logger = LoggerFactory.getLogger(PayrollServer.class);

    public static void main(String[] args) {
        Configuration configuration = new Configuration();
        String portStr = configuration.getPayrollPort();
        if (portStr == null || portStr.isBlank()) {
            logger.error("Payroll port is not configured. Set 'payroll.port' in config.properties.");
            return;
        }

        int payrollPort;
        try {
            payrollPort = Integer.parseInt(new Configuration().getPayrollPort());
        } catch (NumberFormatException e) {
            logger.error("Invalid payroll port: {}", portStr);
            return;
        }

        logger.info("Payroll System (PRS) Server is starting on port {}", payrollPort);

        try (ServerSocket serverSocket = new ServerSocket(payrollPort)) {
            // To keep the server running indefinitely
            while (true) {
                logger.info("Waiting for a connection from the HRM system...");

                // Blocks until a client connects
                try (Socket clientSocket = serverSocket.accept()) {
                    logger.info("HRM System connected from: {}", clientSocket.getInetAddress());

                    // Set up streams to read data from the client
                    InputStreamReader inputReader =
                        new InputStreamReader(clientSocket.getInputStream());
                    BufferedReader reader = new BufferedReader(inputReader);

                    // Reads one line of data as the secure message
                    String receivedData = reader.readLine();
                    if (receivedData == null) {
                        logger.warn("Received empty payload from HRM system; closing connection.");
                        continue;
                    }
                    logger.info("Received raw data: {}", receivedData);

                    String decryptedData = SimpleSecurity.decrypt(receivedData);
                    logger.debug("Decrypted payroll instruction: {}", decryptedData);
                }
            }
        } catch (Exception e) {
            logger.error("Payroll server error: {}", e.getMessage(), e);
        }
    }
}
