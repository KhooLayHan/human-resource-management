package org.bhel.hrm.server.services;

import org.bhel.hrm.common.utils.SimpleSecurity;
import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.server.domain.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        String portStr = configuration.getPayrollPort();
        if (portStr == null || portStr.isBlank())
            throw new IllegalStateException("Payroll port is not configured.");

        try {
            this.port = Integer.parseInt(portStr);
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalStateException("Invalid payroll port: " + portStr, e);
        }
    }

    /**
     * Notifies the PRS about a new employee registration.
     * @param employee The newly registered employee.
     */
    public boolean notifyNewEmployee(Employee employee) {
        String message = String.format("ACTION=NEW_HIRE;ID=%d;FIRST_NAME=%s;LAST_NAME=%s",
            employee.getId(), employee.getFirstName(), employee.getLastName());

        String secureMessage = SimpleSecurity.encrypt(message);
        logger.info("Sending new hire notification to PRS for employee ID: {}", employee.getId());

        try (
            // Consider using SSLSocketFactory for SSL/TLS encrypted socket in the future.
            // Known security limitation and planned for future enhancement
            Socket socket = new Socket(host, port);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            socket.setSoTimeout(5000); // 5 seconds read timeout

            // Sends the single line of encrypted data
            writer.println(secureMessage);
            logger.info("Notification sent successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to send notification to PRS at {}:{}. Is the PayrollServer running?",
                host, port, e);
            return false;
        }
    }
}
