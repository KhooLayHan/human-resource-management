package org.bhel.hrm.client.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.bhel.hrm.common.exceptions.AuthenticationException;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;

public class ChangePasswordDialogController {
    private static final Logger logger = LoggerFactory.getLogger(ChangePasswordDialogController.class);

    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button updateButton;
    @FXML private Button cancelButton;

    private Stage dialogStage;
    private HRMService hrmService;
    private ExecutorService executorService;

    private int userId;
    private boolean success = false;

    @FXML
    private void handleUpdate() {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            showError("Passwords cannot be empty.");
            return;
        }

        if (newPassword.length() < 6) {
            showError("New password must be at least 6 characters.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("New passwords do not match.");
            return;
        }

        setFormDisabled(true);
        errorLabel.setVisible(false);
        updateButton.setText("Updating...");

        Task<Void> changePasswordTask = getChangePasswordTask(oldPassword, newPassword);

        if (executorService != null)
            executorService.submit(changePasswordTask);
        else
            new Thread(changePasswordTask).start();
    }

    private Task<Void> getChangePasswordTask(String oldPassword, String newPassword) {
        Task<Void> changePasswordTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                hrmService.updateUserPassword(userId, oldPassword, newPassword);
                return null;
            }
        };

        changePasswordTask.setOnSucceeded(e -> {
            logger.info("Password changed successfully for user ID: {}", userId);
            success = true;
            dialogStage.close();
        });

        changePasswordTask.setOnFailed(e -> {
            setFormDisabled(false);
            updateButton.setText("Update Password");

            Throwable ex = changePasswordTask.getException();
            logger.warn("Password change failed", ex);

            if (ex instanceof AuthenticationException) {
                showError("Incorrect current password.");
            } else if (ex instanceof RemoteException) {
                showError("Server communication error. Please try again.");
            } else {
                showError("An unexpected error occurred: " + ex.getMessage());
            }
        });
        return changePasswordTask;
    }

    private void setFormDisabled(boolean disabled) {
        oldPasswordField.setDisable(disabled);
        newPasswordField.setDisable(disabled);
        confirmPasswordField.setDisable(disabled);
        updateButton.setDisable(disabled);
        cancelButton.setDisable(disabled);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    @FXML private void handleCancel() { dialogStage.close(); }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setHrmService(HRMService service) {
        this.hrmService = service;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isSuccess() {
        return success;
    }
}