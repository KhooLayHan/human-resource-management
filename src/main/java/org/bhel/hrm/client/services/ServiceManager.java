package org.bhel.hrm.client.services;

import org.bhel.hrm.common.exceptions.ConfigurationException;
import org.bhel.hrm.common.services.HRMService;
import org.bhel.hrm.common.config.Configuration;
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
    private final Configuration configuration;
    private boolean connected = false;
    private final String host;
    private final int port;
    private final String serviceName;

    /**
     * Creates a new ServiceManager with default connection settings.
     */
    public ServiceManager() {
        this(new Configuration());
    }

    /**
     * Creates a new ServiceManager with custom connection settings.
     */
    public ServiceManager(Configuration configuration) {
        this.configuration = configuration;

        try {
            this.host = configuration.getRMIHost();
            this.port = configuration.getRMIPort();
            this.serviceName = configuration.getRMIServiceName();

            logger.info("ServiceManager initialized with host={}, port={}, service={}",
                host, port, serviceName);
        } catch (ConfigurationException e) {
            logger.error("Failed to initialize ServiceManager due to configuration error", e);
            throw e;
        }

        connect();
    }

    /**
     * Establishes connection to the RMI service.
     */
    private void connect() {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            this.hrmService = (HRMService) registry.lookup(configuration.getRMIServiceName());
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
