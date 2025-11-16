package org.bhel.hrm.server;

import org.bhel.hrm.server.config.ApplicationContext;
import org.bhel.hrm.server.services.HRMServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);

    public static void main(String[] args) {
        try {
            logger.info("HRM Server is starting up...");

            // 1. Retrieves the current application context.
            ApplicationContext applicationContext = ApplicationContext.get();

            String serverName = applicationContext.getConfiguration().getRMIServiceName();
            int serverPort = Integer.parseInt(applicationContext.getConfiguration().getRMIPort());

            // 2. Setup and start the RMI server
            HRMServer server = getHRMServer(applicationContext);
            Registry registry = LocateRegistry.createRegistry(serverPort);
            registry.rebind(serverName, server);

            logger.info("Server is running and waiting for client connections...");
       } catch (Exception e) {
            logger.error("Server exception: {}", e.toString());
            logger.error("A fatal error occurred during startup {}", e.getMessage());
       }
    }

    private static HRMServer getHRMServer(ApplicationContext context) throws RemoteException {
        String env = context.getConfiguration().getAppEnvironment();
        logger.info("Application is starting in [{}] environment.", env);

        return new HRMServer(
            context.getDatabaseManager(),
            context.getEmployeeService(),
            context.getUserService(),
            context.getGlobalExceptionHandler()
        );
    }
}
