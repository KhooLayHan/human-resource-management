package org.bhel.hrm.client.utils;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.bhel.hrm.client.constants.ViewType;
import org.bhel.hrm.client.controllers.DashboardController;
import org.bhel.hrm.client.controllers.MainController;
import org.bhel.hrm.client.controllers.ProfileController;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.common.dtos.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ViewManager {
    private static final Logger logger = LoggerFactory.getLogger(ViewManager.class);

    private static final String LOAD_TITLE_ERROR = "View Load Error";
    private static final String LOAD_MESSAGE_ERROR = "Could not load the requested view. Please try again.";

    private ViewManager() {
        throw new UnsupportedOperationException("ViewManager is a utility class and should not be instantiated.");
    }

    /**
     * Load a view using ViewType enum with automatic dependency injection.
     * This is the PREFERRED method for loading views.
     */
    public static void loadView(
        StackPane container,
        ViewType viewType,
        ServiceManager serviceManager,
        ExecutorService executorService,
        UserDTO currentUser,
        MainController mainController
    ) {
        // Check permissions
        if (!viewType.isAllowedForRole(currentUser.role())) {
            logger.warn("User {} attempted to access unauthorized view: {}",
                currentUser.username(), viewType.getDisplayName());
            DialogManager.showWarningDialog(
                "Access Denied",
                "You do not have permission to access this view."
            );

            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(ViewManager.class.getResource(viewType.getFxmlPath())));
            Parent view = loader.load();

            Object controller = loader.getController();
            navigateToController(controller, serviceManager, executorService, currentUser, mainController);

            container.getChildren().setAll(view);
            logger.debug("Loaded view: {} for user: {}",
                viewType.getDisplayName(), currentUser.username());
        } catch (IOException e) {
            logger.error("Failed to load view: {}", viewType.getDisplayName(), e);
            showErrorInContainer(container, viewType.getFxmlPath());

            DialogManager.showErrorDialog(
                LOAD_TITLE_ERROR,
                LOAD_MESSAGE_ERROR
            );
        }
    }

    private static void navigateToController(
        Object controller,
        ServiceManager serviceManager,
        ExecutorService executorService,
        UserDTO currentUser,
        MainController mainController
    ) {
        if (controller == null) {
            logger.warn("Controller is null, cannot inject dependencies.");
            return;
        }

        switch (controller) {
            case DashboardController dashboardController ->
                dashboardController.initDependencies(
                    serviceManager,
                    executorService,
                    currentUser,
                    mainController
                );
            case ProfileController profileController ->
                profileController.initDependencies(
                    serviceManager,
                    executorService,
                    currentUser,
                    mainController
                );
//            case EmployeeManagementController employeeManagementController ->
//                employeeManagementController.setDependencies(
//                    serviceManager,
//                    executorService,
//                    currentUser
//                );
            default ->
                logger.debug("No dependency injection configured for controller: {}",
                    controller.getClass().getSimpleName());
        }
    }

    /**
     * Loads an FXML view and injects it into a parent container.
     * @param container The Pane (e.g., StackPane, BorderPane) to load the view into.
     * @param fxmlPath The absolute path to the FXML file (e.g., "/org/bhel/hrm/client/view/EmployeeManagementController.fxml").
     */
    public static void loadView(Pane container, String fxmlPath) {
        try {
            logger.debug("Loading view: {}", fxmlPath);

            Parent view = FXMLLoader.load(
                Objects.requireNonNull(ViewManager.class.getResource(fxmlPath)));

            container.getChildren().setAll(view);
        } catch (IOException e) {
            logger.error("Failed to load view: {}", fxmlPath, e);
            showErrorInContainer(container, fxmlPath);

            DialogManager.showErrorDialog(
                LOAD_TITLE_ERROR,
                "Could not load the requested view: " + fxmlPath
            );
        } catch (NullPointerException e) {
            logger.error("FXML file not found: {}", fxmlPath, e);
            DialogManager.showErrorDialog(
                "Configuration Error",
                "View file not found: " + fxmlPath
            );
        }
    }

    /**
     * Loads an FXML view and returns the controller for further configuration.
     *
     * @param container The Pane to load the view into.
     * @param fxmlPath The absolute path to the FXML file.
     * @param <T> The controller type.
     * @return The controller instance, or null if loading failed.
     */
    public static <T> T loadViewWithController(Pane container, String fxmlPath) {
        try {
            logger.debug("Loading view with controller: {}", fxmlPath);

            FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(ViewManager.class.getResource(fxmlPath)));
            Parent view = loader.load();
            container.getChildren().setAll(view);

            return loader.getController();
        } catch (IOException | NullPointerException e) {
            logger.error("Failed to load view with controller: {}", fxmlPath, e);
            showErrorInContainer(container, fxmlPath);

            DialogManager.showErrorDialog(
                LOAD_TITLE_ERROR,
                LOAD_MESSAGE_ERROR
            );
            return null;
        }
    }

    /**
     * Loads a view asynchronously with a loading indicator.
     *
     * @param container The Pane to load the view into.
     * @param fxmlPath The absolute path to the FXML file.
     * @param executor The ExecutorService to run the task on.
     */
    public static void loadViewAsync(Pane container, String fxmlPath, ExecutorService executor) {
        // Show loading indicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);
        container.getChildren().setAll(progressIndicator);

        Task<Parent> loadParentTask = new Task<>() {
            @Override
            protected Parent call() throws Exception {
                logger.debug("Loading view asynchronously: {}", fxmlPath);

                FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource(fxmlPath));
                return loader.load();
            }
        };

        loadParentTask.setOnSucceeded(event ->
            container.getChildren().setAll(loadParentTask.getValue())
        );

        loadParentTask.setOnFailed(event -> {
            logger.error("Failed to load view asynchronously: {}",
                fxmlPath, loadParentTask.getException());
            showErrorInContainer(container, fxmlPath);
        });

        executor.submit(loadParentTask);
    }

    private static void showErrorInContainer(Pane container, String fxmlPath) {
        StackPane errorPane = new StackPane();
        Label errorLabel = new Label("Failed to load view: " + fxmlPath);

        errorLabel.getStyleClass().add("error-pane");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(400);

        errorPane.getChildren().add(errorLabel);
        container.getChildren().setAll(errorLabel);
    }
}
