package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.dtos.NewEmployeeRegistrationDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;

/**
 * Controller for the Employee Form dialog.
 * Handles both adding new employees and editing existing ones.
 */
public class EmployeeFormController {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeFormController.class);

    @FXML private Label titleLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<UserDTO.Role> roleComboBox;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField icPassportField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private HRMService hrmService;
    private Stage dialogStage;
    private EmployeeDTO employeeToEdit;
    private boolean isSaved = false;

    /**
     * Initializes the controller – called automatically by JavaFX.
     */
    @FXML
    public void initialize() {
        roleComboBox.getItems().setAll(UserDTO.Role.values());
        errorLabel.setVisible(false);
        logger.debug("Employee Form Controller initialized.");
    }

    /**
     * Sets the dialog stage for this form.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Sets the HRM service for backend communication.
     */
    public void setHrmService(HRMService hrmService) {
        this.hrmService = hrmService;
    }

    /**
     * Configures the form for editing an existing employee.
     *
     * @param employee The employee to edit
     */
    public void setEmployeeToEdit(EmployeeDTO employee) {
        this.employeeToEdit = employee;
        titleLabel.setText("Edit Employee");

        // Disable user creation fields in Edit mode
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        roleComboBox.setDisable(true);

        // Populate fields with existing data
        firstNameField.setText(employee.firstName());
        lastNameField.setText(employee.lastName());
        icPassportField.setText(employee.icPassport());

        // Set placeholder text to indicate these fields are disabled
        usernameField.setPromptText("Disabled");
        passwordField.setPromptText("Disabled");

        logger.debug("Form configured for editing employee ID: {}", employee.id());
    }

    /**
     * Returns whether the employee was successfully saved.
     */
    public boolean isSaved() {
        return isSaved;
    }

    /**
     * Handles the save button action.
     */
    @FXML
    public void handleSave() {
        logger.debug("Save button clicked.");

        if (!validateInput())
            return;

        // Capture field values before starting background tasks
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText().trim();
        final UserDTO.Role role = roleComboBox.getValue();

        final String firstName = firstNameField.getText().trim();
        final String lastName = lastNameField.getText().trim();
        final String icPassport = icPassportField.getText().trim();

        final boolean isEditMode = employeeToEdit != null;
        final EmployeeDTO employeeSnapshot = employeeToEdit; // Capture reference

        // Disable UI controls while saving
        setFormDisabled(true);
        errorLabel.setVisible(false);

        // Create background task for the save operation
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (isEditMode) {
                    // Update existing employee
                    EmployeeDTO employeeDTO = new EmployeeDTO(
                        employeeSnapshot.id(),
                        employeeSnapshot.userId(),
                        firstName,
                        lastName,
                        icPassport
                    );

                    hrmService.updateEmployeeProfile(employeeDTO);
                    logger.info("Employee ID {} updated successfully.", employeeSnapshot.id());
                } else {
                    // Register new employee
                    NewEmployeeRegistrationDTO registrationDTO = new NewEmployeeRegistrationDTO(
                        username,
                        password,
                        role,
                        firstName,
                        lastName,
                        icPassport
                    );

                    hrmService.registerNewEmployee(registrationDTO);
                    logger.info("New employee '{}' registered successfully.", username);
                }

                return null;
            }
        };

        saveTask.setOnSucceeded(event ->
            Platform.runLater(() -> {
                isSaved = true;

                DialogManager.showInfoDialog(
                    "Success",
                    isEditMode
                        ? "Employee profile updated successfully."
                        : "Employee registered successfully."
                );

                dialogStage.close();
            })
        );

        saveTask.setOnFailed(event ->
            Platform.runLater(() -> {
                setFormDisabled(false);

                Throwable error = saveTask.getException();
                logger.error("Failed to save employee", error);

                switch (error) {
                    case HRMException hrmEx -> {
                        logger.warn("Validation Error: {}", hrmEx.getCode());
                        showError("Validation Error: " + hrmEx.getCode());
                    }
                    case RemoteException remoteEx -> {
                        logger.warn("Server error while saving employee", remoteEx);

                        showError("Server communication error. Please try again.");
                        DialogManager.showErrorDialog(
                            "Server Error",
                            "An error occurred while communicating with the server. Please try again."
                        );
                    }
                    default -> {
                        logger.error("Unexpected error while saving employee");
                        showError("An unexpected error occurred.");
                        saveButton.setDisable(false);

                        DialogManager.showErrorDialog(
                            "Error",
                            "An unexpected error occurred. Please try again."
                        );
                    }
                }
            })
        );

        new Thread(saveTask).start();
    }

    /**
     * Validates all input fields.
     *
     * @return {@code true} if all inputs are valid, false otherwise
     */
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Validate user account fields (only for new employees)
        if (employeeToEdit == null) {
            validateUserAccount(errors);
        }

        // Validate employee profile fields (required for both new and edit)
        validateProfileFields(errors);

        // If there are validation errors, show them
        if (!errors.isEmpty()) {
            showError("Please fix the following errors: \n" + errors.toString());
            return false;
        }

        return true;
    }

    private void validateUserAccount(StringBuilder errors) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        UserDTO.Role role = roleComboBox.getValue();

        if (username.isEmpty())
            errors.append("• Username is required.\n");
        else if (username.length() < 3)
            errors.append("• Username must be at least 3 characters long.\n");

        if (password.isEmpty())
            errors.append("• Password is required.\n");
        else if (password.length() < 6)
            errors.append("• Password must be at least 6 characters long.\n");

        if (role == null)
            errors.append("• Role must be selected.\n");
    }

    private void validateProfileFields(StringBuilder errors) {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String icPassport = icPassportField.getText().trim();

        if (firstName.isEmpty())
            errors.append("• First name is required. \n");
        else if (firstName.length() < 2)
            errors.append("• First name must be at least 2 characters long. \n");

        if (lastName.isEmpty())
            errors.append("• Last name is required. \n");
        else if (lastName.length() < 2)
            errors.append("• Last name must be at least 2 characters long. \n");

        if (icPassport.isEmpty())
            errors.append("• IC/Passport is required. \n");
        else if (icPassport.length() < 5)
            errors.append("• IC/Passport must be at least 5 characters long. \n");
    }

    /**
     * Displays an error message in the form
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Enables or disables all form controls.
     */
    private void setFormDisabled(boolean disabled) {
        usernameField.setDisable(disabled || employeeToEdit != null);
        passwordField.setDisable(disabled || employeeToEdit != null);
        roleComboBox.setDisable(disabled || employeeToEdit != null);

        firstNameField.setDisable(disabled);
        lastNameField.setDisable(disabled);
        icPassportField.setDisable(disabled);
        saveButton.setDisable(disabled);
        cancelButton.setDisable(disabled);
    }

    /**
     * Handles the cancel button action.
     */
    @FXML
    private void handleCancel() {
        logger.debug("Cancel button clicked.");

        // If in edit mode or fields has been modified, confirm cancellation
        if (hasUnsavedChanges()) {
            boolean confirmed = DialogManager.showConfirmationDialog(
                "Discard Changes?",
                "You have unsaved changes. Are you sure you want to close without saving?"
            );

            if (!confirmed)
                return;
        }

        dialogStage.close();
    }

    /**
     * Checks if there are any unsaved changes in the form.
     */
    private boolean hasUnsavedChanges() {
        // Check if any field has been modified
        if (employeeToEdit == null) {
            // New employee form
            return
                !usernameField.getText().trim().isEmpty()   ||
                !passwordField.getText().isEmpty()          ||
                roleComboBox.getValue() != null             ||
                !firstNameField.getText().trim().isEmpty()  ||
                !lastNameField.getText().trim().isEmpty()   ||
                !icPassportField.getText().trim().isEmpty();
        } else {
            // Edit mode
            return
                !firstNameField.getText().trim().equals(employeeToEdit.firstName()) ||
                !lastNameField.getText().trim().equals(employeeToEdit.lastName())   ||
                !icPassportField.getText().trim().equals(employeeToEdit.icPassport());
        }
    }
}