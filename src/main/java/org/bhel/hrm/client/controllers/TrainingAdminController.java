package org.bhel.hrm.client.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.bhel.hrm.client.constants.FXMLPaths; // Import your constants
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.client.utils.ViewManager;
import org.bhel.hrm.common.dtos.TrainingCourseDTO;
import org.bhel.hrm.common.services.HRMService;
import org.bhel.hrm.client.controllers.components.PageHeaderController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class TrainingAdminController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(TrainingAdminController.class);

    @FXML private PageHeaderController pageHeaderController;

    @FXML private TextField searchField;
    @FXML private TableView<TrainingCourseDTO> courseTable;
    @FXML private TableColumn<TrainingCourseDTO, Integer> idColumn;
    @FXML private TableColumn<TrainingCourseDTO, String> titleColumn;
    @FXML private TableColumn<TrainingCourseDTO, String> departmentColumn;
    @FXML private TableColumn<TrainingCourseDTO, Integer> durationColumn;
    @FXML private TableColumn<TrainingCourseDTO, String> descriptionColumn;

    @FXML private Button refreshButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button assignButton;

    private HRMService hrmService;
    private ExecutorService executorService;
    private ObservableList<TrainingCourseDTO> allCourses;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pageHeaderController.setTitle("Training Administration");
        pageHeaderController.setSubtitle("Manage training courses and catalog.");

        setupTableColumns();
        setupSelectionListener();

        // Setup search listener for real-time filtering
        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
    }

    public void setDependencies(HRMService service, ExecutorService executor) {
        this.hrmService = service;
        this.executorService = executor;
        loadCourses();

    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().id()).asObject());
        titleColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().title()));
        departmentColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().department() != null ? d.getValue().department().toString() : ""));
        durationColumn.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().durationInHours()).asObject());
        descriptionColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().description()));
    }

    private void setupSelectionListener() {
        courseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);

            // FIX: This logic MUST be inside this method
            if (assignButton != null) {
                assignButton.setDisable(!hasSelection);
            }
        });
    }


    private void loadCourses() {
        if (hrmService == null) return;

        courseTable.setDisable(true);
        courseTable.setPlaceholder(new Label("Loading courses..."));

        Task<List<TrainingCourseDTO>> task = new Task<>() {
            @Override
            protected List<TrainingCourseDTO> call() throws Exception {
                return hrmService.getAllTrainingCourses();
            }
        };

        task.setOnSucceeded(e -> {
            allCourses = FXCollections.observableArrayList(task.getValue());
            courseTable.setItems(allCourses);
            courseTable.setDisable(false);
            courseTable.setPlaceholder(new Label("No training courses found."));

            // Re-apply search filter if exists
            handleSearch();
        });

        task.setOnFailed(e -> {
            courseTable.setDisable(false);
            logger.error("Failed to load courses", task.getException());
            DialogManager.showErrorDialog("Load Error", "Failed to load courses: " + task.getException().getMessage());
        });

        if (executorService != null) {
            executorService.submit(task);
        } else {
            new Thread(task).start();
        }
    }

    @FXML
    private void handleAddCourse() {
        showCourseFormDialog(null);
    }

    @FXML
    private void handleEditCourse() {
        TrainingCourseDTO selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showCourseFormDialog(selected);
        }
    }

    private void showCourseFormDialog(TrainingCourseDTO course) {
        try {
            String title = (course == null) ? "Add Course" : "Edit Course";

            // IMPROVEMENT: Use the constant if you added it to FXMLPaths.Dialogs
            // Otherwise use the string path: "/org/bhel/hrm/client/view/dialogs/TrainingCourseFormView.fxml"
            String fxmlPath = "/org/bhel/hrm/client/view/dialogs/TrainingCourseFormView.fxml";

            var dialog = ViewManager.loadDialog(
                    fxmlPath,
                    title,
                    (Stage) courseTable.getScene().getWindow()
            );

            TrainingCourseFormController controller = (TrainingCourseFormController) dialog.controller();
            controller.setDependencies(hrmService, executorService);
            controller.setDialogStage(dialog.stage());

            if (course != null) {
                controller.setCourseToEdit(course);
            }

            dialog.stage().showAndWait();

            // Refresh table if saved
            if (controller.isSaved()) {
                loadCourses();
            }

        } catch (Exception e) {
            logger.error("Failed to open course form", e);
            DialogManager.showErrorDialog("UI Error", "Could not open course form.");
        }
    }

    @FXML
    private void handleDeleteCourse() {
        TrainingCourseDTO selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean confirm = DialogManager.showConfirmationDialog(
                "Delete Course",
                "Are you sure you want to delete '" + selected.title() + "'? This cannot be undone."
        );

        if (!confirm) return;

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Ensure this method is in HRMService interface
                hrmService.deleteTrainingCourse(selected.id());
                return null;
            }
        };

        deleteTask.setOnSucceeded(e -> {
            DialogManager.showInfoDialog("Success", "Course deleted successfully.");
            loadCourses(); // Refresh table
        });

        deleteTask.setOnFailed(e -> {
            logger.error("Failed to delete course", deleteTask.getException());
            DialogManager.showErrorDialog("Delete Error", "Failed to delete course: " + deleteTask.getException().getMessage());
        });

        if (executorService != null) {
            executorService.submit(deleteTask);
        } else {
            new Thread(deleteTask).start();
        }
    }

    @FXML
    private void handleSearch() {
        if (allCourses == null) return;

        String query = searchField.getText();

        if (query == null || query.trim().isEmpty()) {
            courseTable.setItems(allCourses);
        } else {
            String lowerQuery = query.toLowerCase().trim();

            List<TrainingCourseDTO> filteredList = allCourses.stream()
                    .filter(c ->
                            c.title().toLowerCase().contains(lowerQuery) ||
                                    c.department().toString().toLowerCase().contains(lowerQuery) ||
                                    (c.description() != null && c.description().toLowerCase().contains(lowerQuery))
                    )
                    .collect(Collectors.toList());

            courseTable.setItems(FXCollections.observableArrayList(filteredList));
        }
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        handleSearch(); // Resets list
    }

    @FXML
    private void handleRefresh() {
        handleClearSearch();
        loadCourses();
    }

    @FXML
    private void handleAssignEmployees() {
        TrainingCourseDTO selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            var dialog = ViewManager.loadDialog(
                    "/org/bhel/hrm/client/view/dialogs/EmployeeSelectionView.fxml",
                    "Assign Employees",
                    (Stage) courseTable.getScene().getWindow()
            );

            EmployeeSelectionController controller = (EmployeeSelectionController) dialog.controller();
            controller.setDependencies(hrmService, executorService);
            controller.setConfig(dialog.stage(), selected.id(), selected.title());

            dialog.stage().showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}