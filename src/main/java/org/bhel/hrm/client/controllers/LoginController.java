package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.bhel.hrm.client.MainClient;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 300_000; // Equivalent to 5 minutes

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label errorLabel;
    @FXML private CheckBox rememberMeCheckbox;

    private ServiceManager serviceManager;
    private ExecutorService executorService;
    private MainClient mainClient;
    private HRMService hrmService;

    private int loginAttempts = 0;
    private long lockoutEndTime = 0;

    @FXML
    public void initialize() {
        // Clears any previous error messages
        errorLabel.setText("");
        errorLabel.setVisible(false);
        loadingIndicator.setVisible(false);

        // Sets initial focus to username field
        Platform.runLater(() -> usernameField.requestFocus());
    }

    public void setMainApp(MainClient client) {
        this.mainClient = client;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.hrmService = serviceManager.getHrmService();

        if (!serviceManager.isConnected()) {
            errorLabel.setText("Cannot connect to the server. Please check your connection.");
            errorLabel.setVisible(true);
            usernameField.setDisable(true);
            passwordField.setDisable(true);
        }
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @FXML
    protected void handleLoginButtonAction() {
        if (hrmService == null) {
            DialogManager.showErrorDialog(
                "Connection Error",
                "Not connected to the server. Please restart the application."
            );

            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Username and password cannot be empty.");
            errorLabel.setVisible(true);
            return;
        }

        // Shows loading state
        errorLabel.setVisible(true);
        loadingIndicator.setVisible(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);

        Task<UserDTO> loginTask = new Task<UserDTO>() {
            @Override
            protected UserDTO call() throws Exception {
                return hrmService.authenticateUser(username, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            loadingIndicator.setVisible(false);
            usernameField.setDisable(false);
            passwordField.setDisable(false);

            UserDTO authenticatedUser = loginTask.getValue();
            if (authenticatedUser != null) {
                logger.info("Login successful for user: {}", username);
                loginAttempts = 0; // Resets attempts on success
                mainClient.showMainView(authenticatedUser);
            } else {
                passwordField.clear();
                handleFailedLogin("Invalid username or password.");
            }
        });

        loginTask.setOnFailed(event -> {
            loadingIndicator.setVisible(false);
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            passwordField.clear(); // Clears password for security

            Throwable error = loginTask.getException();

            switch (error) {
                case HRMException hrmException -> {
                    logger.warn("Authentication failed for user '{}': {}",
                        username, hrmException.getMessage());
                    handleFailedLogin("Invalid username or password.");
                }
                case RemoteException remoteException -> {
                    logger.error("RMI error during authentication.", remoteException);
                    DialogManager.showErrorDialog(
                        "Server Error",
                        "An error occurred while communicating with the server. Please try again."
                    );
                }
                default -> {
                    logger.error("Unexpected error during login.", error);
                    DialogManager.showErrorDialog(
                        "Login Error",
                        "Unexpected error during login. Please try again."
                    );
                }
            }
        });

        executorService.submit(loginTask);
    }

    private void handleFailedLogin(String message) {
        loginAttempts++;

        showError(message);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    private void handleForgotPassword() {
        DialogManager.showInfoDialog(
            "Forgot Password",
            "Please contact your system administrator to reset your password."
        );
    }
}
