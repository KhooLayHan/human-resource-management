package org.bhel.hrm.client.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bhel.hrm.client.constants.FXMLPaths;
import org.bhel.hrm.client.controllers.components.PageHeaderController;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class ProfileController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @FXML private PageHeaderController pageHeaderController;

    @FXML private TextField usernameField;
    @FXML private TextField roleField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField icPassportField;

    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private UserDTO currentUser;
    private EmployeeDTO currentEmployeeProfile;

    private ServiceManager serviceManager;
    private ExecutorService executorService;

    @FXML
    public void initialize() {
        pageHeaderController.setTitle("My Profile");
        pageHeaderController.setSubtitle("View and manage your personal information.");
    }

    public void initDependencies(
        ServiceManager serviceManager,
        ExecutorService executorService,
        UserDTO currentUser
    ) {
        this.serviceManager = serviceManager;
        this.executorService = executorService;
        this.currentUser = currentUser;

        loadProfileData();
    }

    private void loadProfileData() {
        usernameField.setText(currentUser.username());
        roleField.setText(currentUser.role().toString());

        Task<EmployeeDTO> getEmployeeByUserIdTask = getEmployeeByUserIdTask();

        if (executorService != null)
            executorService.submit(getEmployeeByUserIdTask);
        else
            new Thread(getEmployeeByUserIdTask).start();
    }

    private Task<EmployeeDTO> getEmployeeByUserIdTask() {
        Task<EmployeeDTO> getEmployeeByUserIdTask = new Task<>() {
            @Override
            protected EmployeeDTO call() throws Exception {
                return serviceManager.getHrmService()
                    .getEmployeeByUserId(currentUser.id());
            }
        };

        getEmployeeByUserIdTask.setOnSucceeded(e -> {
            currentEmployeeProfile = getEmployeeByUserIdTask.getValue();
            populateFields();
        });

        getEmployeeByUserIdTask.setOnFailed(e -> {
            logger.error("Failed to load profile", getEmployeeByUserIdTask.getException());
            DialogManager.showErrorDialog(
                "Load Error", "Failed to load profile data.");
        });

        return getEmployeeByUserIdTask;
    }

    private void populateFields() {
        if (currentEmployeeProfile == null) {
            logger.warn("Attempted to populate fields but currentEmployeeProfile is null.");
            return;
        }

        firstNameField.setText(currentEmployeeProfile.firstName());
        lastNameField.setText(currentEmployeeProfile.lastName());
        icPassportField.setText(currentEmployeeProfile.icPassport());
    }

    @FXML
    private void handleEditToggle() {
        setEditMode(true);
    }

    @FXML
    private void handleCancel() {
        // Revert changes
        populateFields();
        setEditMode(false);
    }

    private void setEditMode(boolean editing) {
        firstNameField.setEditable(editing);
        lastNameField.setEditable(editing);
        icPassportField.setEditable(editing);

        editButton.setVisible(!editing);
        saveButton.setVisible(editing);
        cancelButton.setVisible(editing);

        // Visual cue
        String editableStyle = editing
            ? "-fx-background-color: white; -fx-border-color: #80bdff; -fx-border-width: 1px;"
            : "";

        firstNameField.setStyle(editableStyle);
        lastNameField.setStyle(editableStyle);
        icPassportField.setStyle(editableStyle);

        // Add style classes for readonly vs editable
        final String READONLY = "readonly-field";
        if (editing) {
            firstNameField.getStyleClass().remove(READONLY);
            lastNameField.getStyleClass().remove(READONLY);
            icPassportField.getStyleClass().remove(READONLY);
        } else {
            firstNameField.getStyleClass().add(READONLY);
            lastNameField.getStyleClass().add(READONLY);
            icPassportField.getStyleClass().add(READONLY);
        }
    }

    @FXML
    private void handleSave() {
        if (currentEmployeeProfile == null) {
            DialogManager.showErrorDialog(
                "Save Error",
                "Profile data is not loaded. Please wait and try again."
            );
            return;
        }

        EmployeeDTO updatedDTO = new EmployeeDTO(
            currentEmployeeProfile.id(),
            currentEmployeeProfile.userId(),
            firstNameField.getText(),
            lastNameField.getText(),
            icPassportField.getText()
        );

        Task<Void> saveTask = getSaveTask(updatedDTO);

        if (executorService != null)
            executorService.submit(saveTask);
        else
            new Thread(saveTask).start();
    }

    private Task<Void> getSaveTask(EmployeeDTO updatedDTO) {
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                serviceManager.getHrmService().updateEmployeeProfile(updatedDTO);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            DialogManager.showInfoDialog(
                "Success", "Profile updated successfully.");

            currentEmployeeProfile = updatedDTO;
            setEditMode(false);
        });

        saveTask.setOnFailed(e ->
            DialogManager.showErrorDialog(
                "Update Failed",
                "Could not update profile: " + saveTask.getException().getMessage())
        );

        return saveTask;
    }

    @FXML
    private void handleChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXMLPaths.Dialogs.CHANGE_PASSWORD));

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(usernameField.getScene().getWindow());
            stage.setScene(new Scene(loader.load()));
            stage.setResizable(false);

            ChangePasswordDialogController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setHrmService(serviceManager.getHrmService());
            controller.setExecutorService(executorService);
            controller.setUserId(currentUser.id());

            stage.showAndWait();

            if (controller.isSuccess()) {
                DialogManager.showInfoDialog(
                    "Success",
                    "Your password has been updated successfully."
                );
            }
        } catch (IOException e) {
            logger.error("Failed to load password dialog");
            DialogManager.showErrorDialog(
                "UI Error",
                "Could not open change password dialog."
            );
        }
    }
}
