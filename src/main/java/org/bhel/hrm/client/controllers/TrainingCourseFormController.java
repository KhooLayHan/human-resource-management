package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.TrainingCourseDTO;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Controller for the Training Course Form dialog.
 * Handles adding new courses and editing existing ones.
 */
public class TrainingCourseFormController {
    private static final Logger logger = LoggerFactory.getLogger(TrainingCourseFormController.class);

    @FXML private Label titleLabel;
    @FXML private TextField titleField;
    @FXML private ComboBox<TrainingCourseDTO.Department> departmentCombo;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private TextArea descriptionArea;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private HRMService hrmService;
    private ExecutorService executorService;
    private Stage dialogStage;
    private TrainingCourseDTO courseToEdit;
    private boolean isSaved = false;

    @FXML
    public void initialize() {
        // Populate departments
        departmentCombo.getItems().setAll(TrainingCourseDTO.Department.values());

        // Config spinner (1 to 100 hours, default 8)
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 8);
        durationSpinner.setValueFactory(valueFactory);

        // Allow manual numeric input for spinner
        durationSpinner.setEditable(true);
    }

    public void setDependencies(HRMService service, ExecutorService executor) {
        this.hrmService = service;
        this.executorService = executor;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Configures the form for editing an existing course.
     */
    public void setCourseToEdit(TrainingCourseDTO course) {
        this.courseToEdit = course;
        if (titleLabel != null) titleLabel.setText("Edit Training Course");

        titleField.setText(course.title());
        departmentCombo.setValue(course.department());
        durationSpinner.getValueFactory().setValue(course.durationInHours());
        descriptionArea.setText(course.description());

        logger.debug("Form configured for editing course ID: {}", course.id());
    }

    public boolean isSaved() {
        return isSaved;
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) return;

        logger.debug("Save button clicked.");

        // Create DTO from form data
        TrainingCourseDTO dto = new TrainingCourseDTO(
                courseToEdit == null ? 0 : courseToEdit.id(),
                titleField.getText().trim(),
                descriptionArea.getText().trim(),
                durationSpinner.getValue(),
                departmentCombo.getValue()
        );

        // Disable UI to prevent double submission
        setFormDisabled(true);

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                hrmService.saveTrainingCourse(dto);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                DialogManager.showInfoDialog("Success", "Course saved successfully.");
                isSaved = true;
                dialogStage.close();
            });
        });

        saveTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setFormDisabled(false); // Re-enable UI on failure
                Throwable error = saveTask.getException();
                logger.error("Failed to save training course", error);
                DialogManager.showErrorDialog("Save Error", error.getMessage());
            });
        });

        if (executorService != null) {
            executorService.submit(saveTask);
        } else {
            new Thread(saveTask).start();
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (titleField.getText().trim().isEmpty()) {
            errors.append("• Title is required.\n");
        }
        if (departmentCombo.getValue() == null) {
            errors.append("• Department is required.\n");
        }
        if (durationSpinner.getValue() == null || durationSpinner.getValue() <= 0) {
            errors.append("• Duration must be greater than 0.\n");
        }

        if (!errors.isEmpty()) {
            DialogManager.showWarningDialog("Validation Error", errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        if (hasUnsavedChanges()) {
            boolean confirm = DialogManager.showConfirmationDialog(
                    "Unsaved Changes",
                    "You have unsaved changes. Are you sure you want to discard them?"
            );
            if (!confirm) return;
        }
        dialogStage.close();
    }

    /**
     * Checks if the user has modified any fields.
     */
    private boolean hasUnsavedChanges() {
        String currentTitle = titleField.getText().trim();
        TrainingCourseDTO.Department currentDept = departmentCombo.getValue();
        int currentDuration = durationSpinner.getValue();
        String currentDesc = descriptionArea.getText().trim();

        if (courseToEdit == null) {
            // New Course: check if any field is populated
            return !currentTitle.isEmpty() || currentDept != null || !currentDesc.isEmpty();
        } else {
            // Edit Course: check if any field differs from original
            return !currentTitle.equals(courseToEdit.title()) ||
                    currentDept != courseToEdit.department() ||
                    currentDuration != courseToEdit.durationInHours() ||
                    !currentDesc.equals(courseToEdit.description() == null ? "" : courseToEdit.description());
        }
    }

    private void setFormDisabled(boolean disabled) {
        titleField.setDisable(disabled);
        departmentCombo.setDisable(disabled);
        durationSpinner.setDisable(disabled);
        descriptionArea.setDisable(disabled);
        saveButton.setDisable(disabled);
        cancelButton.setDisable(disabled);
    }
}