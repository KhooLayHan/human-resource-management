package org.bhel.hrm.server;

import org.bhel.hrm.server.config.ApplicationContext;
import org.bhel.hrm.server.rmi.RMIRegistryManager;
import org.bhel.hrm.server.services.HRMServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;

/**
 * Main entry point for the HRMServer application.
 * Initializes the application context and starts the RMI service.
 */
public class ServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);

    public static void main(String[] args) {
        try {
            logger.info("HRM Server is starting up...");

            // 1. Retrieves the current application context.
            ApplicationContext context = ApplicationContext.get();

            // 2. Creates and configure the HRMServer.
            HRMServer server = getHRMServer(context);

            // 3. Setup and start the RMI registry.
            RMIRegistryManager registryManager = new RMIRegistryManager(context.getConfiguration());
            registryManager.startAndBind(server);

            logger.info("Server is running and waiting for client connections...");

            // Adds shutdown hook for graceful shutdown
            addShutdownHook(registryManager);
       } catch (Exception e) {
            logger.error("Server exception: {}", e.toString());
            logger.error("A fatal error occurred during startup {}", e.getMessage(), e);

            System.exit(1);
       }
    }

    /**
     * Creates and initializes the HRM Server with all required dependencies.
     *
     * @param context The application context containing all services
     * @return Configured HRMServer instance
     * @throws RemoteException If server creation fails
     */
    private static HRMServer getHRMServer(ApplicationContext context) throws RemoteException {
        String env = context.getConfiguration().getAppEnvironment();
        logger.info("Application is starting in [{}] environment.", env);

        return new HRMServer(
            context.getDatabaseManager(),
            context.getEmployeeService(),
            context.getUserService(),
            context.getGlobalExceptionHandler(),
            context.getRecruitmentService(),
            context.getLeaveService(),
            context.getBenefitService(),
            context.getTrainingService()
        );
    }

    /**
     * Adds a shutdown hook to gracefully unbind the service on JVM shutdown.
     *
     * @param registryManager The RMI registry manager
     */
    private static void addShutdownHook(RMIRegistryManager registryManager) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received, cleaning up...");

            try {
                registryManager.unbindService();

                logger.info("Server shutdown complete.");
            } catch (Exception e) {
                logger.error("Error during shutdown: {}", e.getMessage(), e);
            }
        }));
    }
}
