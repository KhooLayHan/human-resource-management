package org.bhel.hrm.client.services;

import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Manages the RMI connection to the HRM service.
 * Uses Dependency Injection (DI) design pattern.
 */
public class ServiceManager {
    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    private HRMService hrmService;
    private boolean connected = false;
    private final String host;
    private final int port;

    /**
     * Creates a new ServiceManager with default connection settings.
     */
    public ServiceManager() {
        this("localhost", 1099);
    }

    /**
     * Creates a new ServiceManager with custom connection settings.
     */
    public ServiceManager(String host, int port) {
        this.host = host;
        this.port = port;

        connect();
    }

    /**
     * Establishes connection to the RMI service.
     */
    private void connect() {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            this.hrmService = (HRMService) registry.lookup(HRMService.SERVICE_NAME);
            this.connected = true;

            logger.info("Successfully connected to the RMI service at {}:{}.", host, port);
        } catch (Exception e) {
            logger.error("Failed to connect to the RMI service at {}:{}", host, port, e);
            this.connected = false;
        }
    }

    /**
     * Attempts to reconnect if not currently connected.
     */
    public void reconnect() {
        if (!connected) {
            logger.info("Attempting to reconnect to the RMI service...");
            connect();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public HRMService getHrmService() {
        if (!connected)
            reconnect();

        return hrmService;
    }

    /**
     * Closes the connection and cleans up resources.
     */
    public void disconnect() {
        this.hrmService = null;
        this.connected = false;
        logger.info("Disconnected from the RMI service.");
    }
}
