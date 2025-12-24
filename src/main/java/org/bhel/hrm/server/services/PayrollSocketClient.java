package org.bhel.hrm.server.services;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.bhel.hrm.server.domain.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.PrintWriter;

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
    }
}
