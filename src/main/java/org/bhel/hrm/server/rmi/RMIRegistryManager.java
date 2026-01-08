package org.bhel.hrm.server.rmi;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.server.services.HRMServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Manages RMI registry creation and service binding.
 * Encapsulates all RMI-specific setup logic.
 */
public class RMIRegistryManager {
    private static final Logger logger = LoggerFactory.getLogger(RMIRegistryManager.class);

    private final Configuration configuration;
    private Registry registry;

    public RMIRegistryManager(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Creates the RMI registry on the configured port.
     *
     * @return The created Registry instance
     * @throws RemoteException If registry creation fails
     */
    public Registry createRegistry() throws RemoteException {
        int port = configuration.getRMIPort();

        try {
            registry = LocateRegistry.createRegistry(port);
            logger.info("RMI registry created successfully on port {}", port);

            return registry;
        } catch (RemoteException e) {
            logger.error("Failed to create RMI registry on port {}", port);
            throw e;
        }
    }

    /**
     * Binds the HRM server to the registry with the configured service name.
     *
     * @param server The HRMServer instance to bind
     * @throws RemoteException If binding fails
     */
    public void bindService(HRMServer server) throws RemoteException {
        if (registry == null)
            throw new IllegalStateException("Registry not created. Call createRegistry() first.");

        String serviceName = configuration.getRMIServiceName();

        try {
            registry.rebind(serviceName, server);
            logger.info("HRM service '{}' bound successfully to registry", serviceName);
        } catch (RemoteException e) {
            logger.error("Failed to bind service '{}'to registry", serviceName, e);
        }
    }

    /**
     * Creates registry and binds the service in one operation.
     *
     * @param server The HRMServer instance to bind
     * @throws RemoteException If registry creation or binding fails
     */
    public void startAndBind(HRMServer server) throws RemoteException {
        createRegistry();
        bindService(server);

        logger.info("RMI service is running at {}:{} with name '{}'",
            configuration.getRMIHost(),
            configuration.getRMIPort(),
            configuration.getRMIServiceName()
        );
    }

    /**
     * Unbinds the service from the registry.
     *
     * @throws RemoteException If unbinding fails
     */
    public void unbindService() throws RemoteException, NotBoundException {
        if (registry == null) {
            logger.warn("No registry to unbind from.");
            return;
        }

        String serviceName = configuration.getRMIServiceName();

        try {
            registry.unbind(serviceName);
            logger.info("Service '{}' unbound from registry", serviceName);
        } catch (Exception e) {
            logger.error("Failed to unbind service '{}'", serviceName, e);
        }
    }

    public Registry getRegistry() {
        return registry;
    }
}
