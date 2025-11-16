package org.bhel.hrm.server.services;

import org.bhel.hrm.common.utils.SimpleSecurity;
import org.bhel.hrm.server.config.Configuration;
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

    private final Configuration configuration;
    private final String host;
    private final int port;

    public PayrollSocketClient(Configuration configuration) {
        this.configuration = configuration;
        this.host = configuration.getRMIHost();
        this.port = Integer.parseInt(configuration.getRMIPort());
    }

    /**
     * Notifies the PRS about a new employee registration.
     * @param employee The newly registered employee.
     */
    public void notifyNewEmployee(Employee employee) {
        String message = String.format("ACTION=NEW_HIRE;ID=%d;FIRST_NAME=%s;LAST_NAME=%s",
            employee.getId(), employee.getFirstName(), employee.getLastName());

        String secureMessage = SimpleSecurity.encrypt(message);
        logger.info("Sending new hire notification to PRS for employee ID: {}", employee.getId());

        try (
            Socket socket = new Socket(host, port);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Sends the single line of encrypted data
            writer.println(secureMessage);
            logger.info("Notification sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send notification to PRS at {}:{}. Is the PayrollServer running?",
                host, port, e);
        }
    }
}
