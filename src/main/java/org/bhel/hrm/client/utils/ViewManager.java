package org.bhel.hrm.client.utils;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ViewManager {
    private static final Logger logger = LoggerFactory.getLogger(ViewManager.class);

    private ViewManager() {
        throw new UnsupportedOperationException("ViewManager is a utility class and should not be instantiated.");
    }
    public record Dialog<T>(Stage stage, T controller) {}

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
                "View Load Error",
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
            logger.error("Failed to load view: {}", fxmlPath, e);
            showErrorInContainer(container, fxmlPath);

            DialogManager.showErrorDialog(
                    "View Load Error",
                    "Could not load the requested view. Please try again."
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

        Task<Parent> loadParentTask =   new Task<>() {
            @Override
            protected Parent call() throws Exception {
                logger.debug("Loading view asynchronously: {}", fxmlPath);

                FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource(fxmlPath));
                return loader.load();
            }
        };

        loadParentTask.setOnSucceeded(event -> {
            container.getChildren().setAll(loadParentTask.getValue());
        });

        loadParentTask.setOnFailed(event -> {
            logger.error("Failed to load view asynchronously: {}",
                fxmlPath, loadParentTask.getException());
            showErrorInContainer(container, fxmlPath);
        });

        executor.submit(loadParentTask);
    }

    /**
     * Loads a new FXML file into a new modal Stage (pop-up window).
     *
     * @param fxmlPath The absolute path to the FXML file.
     * @param title    The title for the new window.
     * @param owner    The parent stage to make the new window modal (block input to parent).
     * @param <T>      The type of the controller.
     * @return A Dialog record containing the new Stage and the FXML's controller.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public static <T> Dialog<T> loadDialog(String fxmlPath, String title, Stage owner) throws IOException {
        logger.debug("Loading dialog: {}", fxmlPath);

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(ViewManager.class.getResource(fxmlPath)));
        Parent root = loader.load();

        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setScene(new Scene(root));
        dialogStage.setResizable(false); // Optional: lock dialog size

        T controller = loader.getController();

        return new Dialog<>(dialogStage, controller);
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
