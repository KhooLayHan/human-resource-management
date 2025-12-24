package org.bhel.hrm.server.services;

import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.SimpleSecurity;
import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.bhel.hrm.server.domain.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A client for sending secure messages to the Payroll System (PRS) via sockets.
 */
public class PayrollSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(PayrollSocketClient.class);

    private final String host;
    private final int port;

    public PayrollSocketClient(Configuration configuration) {
        this.host = configuration.getPayrollHost();
        this.port = configuration.getPayrollPort();
//        if (portStr == null || portStr.isBlank())
//            throw new IllegalStateException("Payroll port is not configured.");
//
//        try {
//            this.port = Integer.parseInt(portStr);
//        } catch (NumberFormatException | NullPointerException e) {
//            throw new IllegalStateException("Invalid payroll port: " + portStr, e);
//        }
    }

    /**
     * Notifies the PRS about a new employee registration.
     * @param employee The newly registered employee.
     */
    public boolean notifyNewEmployee(Employee employee) {
        String message = String.format(
            "ACTION=NEW_HIRE;ID=%d;FIRST_NAME=%s;LAST_NAME=%s;IC=%s",
            employee.getId(),
            employee.getFirstName(), employee.getLastName(),
            employee.getIcPassport()
        );

        logger.info("Initiating secure payroll notification for Employee ID: {}", employee.getId());

//        String secureMessage = SimpleSecurity.encrypt(message);
//        logger.info("Sending new hire notification to PRS for employee ID: {}", employee.getId());

        try {
            // 1. Encrypt payload (AES-GCM)
            String securePayload = CryptoUtils.encrypt(message);

            // 2. Establish secure connection with TLS 1.3
            SSLSocketFactory sslFactory = SslContextFactory.createSslContext().getSocketFactory();

            try (
                SSLSocket socket = (SSLSocket) sslFactory.createSocket(host, port);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
            ) {
                socket.startHandshake(); // Ensure SSL handshake succeeds before sending data

                // 3. Sends data
                writer.println(securePayload);
                logger.info("Notification sent successfully.");

                return true;
            }
        } catch (Exception e) {
            logger.error(
                "Failed to send notification to PRS at {}:{}. Is the PayrollServer running?",
                host, port, e
            );

            return false;
        }

//        try (
//            // Consider using SSLSocketFactory for SSL/TLS encrypted socket in the future.
//            // Known security limitation and planned for future enhancement
//            Socket socket = new Socket(host, port);
//            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
//        ) {
//            socket.setSoTimeout(5000); // 5 seconds read timeout
//
//            // Sends the single line of encrypted data
//            writer.println(secureMessage);
//            logger.info("Notification sent successfully");
//            return true;
//        } catch (Exception e) {
//            logger.error("Failed to send notification to PRS at {}:{}. Is the PayrollServer running?",
//                host, port, e);
//            return false;
//        }
    }
}
