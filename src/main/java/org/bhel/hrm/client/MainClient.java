package org.bhel.hrm.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.bhel.hrm.client.constants.FXMLPaths;
import org.bhel.hrm.client.controllers.LoginController;
import org.bhel.hrm.client.controllers.MainController;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainClient extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainClient.class);

    private Stage primaryStage;
    private ExecutorService executorService;
    private ServiceManager serviceManager;

    @Override
    public void init() {
        executorService = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true); // Ensures all threads in the pool are daemon threads
            return thread;
        });

        serviceManager = new ServiceManager();

        if (!serviceManager.isConnected())
            logger.error("Failed to connect to server during initialization.");
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        showLoginView();
    }

    @Override
    public void stop() {
        logger.info("Application is shutting down...");

        if (executorService != null) {
            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
                    executorService.shutdownNow();
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (serviceManager != null)
            serviceManager.disconnect();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    /**
     * Loads and displays the LoginView.
     */
    public void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXMLPaths.LOGIN));
            Parent root = loader.load();

            // Give the LoginController a reference back to this MainClient instance
            LoginController controller = loader.getController();
            if (controller == null)
                throw new IllegalStateException("LoginController was not found in LoginView.fxml");

            // Injects the dependencies to the controller
            controller.setMainApp(this);
            controller.setServiceManager(serviceManager);
            controller.setExecutorService(executorService);

            primaryStage.setMinWidth(380);
            primaryStage.setMinHeight(320);
            primaryStage.setTitle("BHEL Human Resource Management – Login");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            logger.error("Failed to load Login view", e);
            DialogManager.showErrorDialog(
                    "Application Error",
                    "Could not load the Login screen. Please restart the application."
            );
        }
    }

    /**
     * Called by the LoginController upon successful authentication.
     * Switches the scene to the main application view.
     *
     * @param user The authenticated UserDTO
     */
    public void showMainView(UserDTO user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXMLPaths.MAIN));
            Parent root = loader.load();

            // Gets the MainController and pass the authenticated user data to it.
            MainController controller = loader.getController();
            if (controller == null)
                throw new IllegalStateException("MainController was not found in MainView.fxml");

            controller.setMainClient(this);
            controller.setServiceManager(serviceManager);
            controller.setExecutorService(executorService);
            controller.initData(user);

            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(600);
            primaryStage.setTitle("BHEL – Human Resource Management");
            primaryStage.setScene(new Scene(root, 1200, 700));
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            logger.error("Failed to load Main view", e);
            DialogManager.showErrorDialog(
                    "Application Error",
                    "Could not load the Main screen. Returning to Login."
            );

            showLoginView();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}