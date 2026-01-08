package org.bhel.hrm.client.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.services.HRMService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class EmployeeSelectionController {
    @FXML private Label courseTitleLabel;
    @FXML private TextField searchField;
    @FXML private ListView<EmployeeDTO> employeeListView;

    private HRMService hrmService;
    private ExecutorService executorService;
    private Stage dialogStage;
    private int courseId;
    private boolean isSaved = false;

    // We need to track selection manually or use a wrapper if we want filtering + checkboxes to play nice
    private ObservableList<EmployeeDTO> allEmployees;
    private FilteredList<EmployeeDTO> filteredEmployees;
    private final List<Integer> selectedIds = new ArrayList<>();

    public void setDependencies(HRMService service, ExecutorService executor) {
        this.hrmService = service;
        this.executorService = executor;
        loadEmployees();
    }

    public void setConfig(Stage stage, int courseId, String courseTitle) {
        this.dialogStage = stage;
        this.courseId = courseId;
        this.courseTitleLabel.setText("Course: " + courseTitle);
    }

    public boolean isSaved() { return isSaved; }

    @FXML
    public void initialize() {
        employeeListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Custom rendering for names
        employeeListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(EmployeeDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.firstName() + " " + item.lastName() + " (" + item.icPassport() + ")");
                }
            }
        });

        // Search filter
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredEmployees.setPredicate(emp -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return emp.firstName().toLowerCase().contains(lower) ||
                        emp.lastName().toLowerCase().contains(lower);
            });
        });
    }

    private void loadEmployees() {
        Task<List<EmployeeDTO>> task = new Task<>() {
            @Override
            protected List<EmployeeDTO> call() throws Exception {
                return hrmService.getAllEmployees();
            }
        };

        task.setOnSucceeded(e -> {
            allEmployees = FXCollections.observableArrayList(task.getValue());
            filteredEmployees = new FilteredList<>(allEmployees, p -> true);
            employeeListView.setItems(filteredEmployees);
        });

        task.setOnFailed(e -> {
            DialogManager.showErrorDialog("Load Error", "Failed to load employees: " + task.getException().getMessage());
            employeeListView.setPlaceholder(new Label("Failed to load employees"));
       });

        executorService.submit(task);
    }

    @FXML
    private void handleAssign() {
        List<EmployeeDTO> selectedItems = new ArrayList<>(employeeListView.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) {
            DialogManager.showWarningDialog("No Selection", "Please select at least one employee.");
            return;
        }

        List<Integer> ids = selectedItems.stream().map(EmployeeDTO::id).collect(Collectors.toList());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                hrmService.enrollMultipleEmployees(courseId, ids);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            DialogManager.showInfoDialog("Success", "Employees assigned successfully.");
            isSaved = true;
            dialogStage.close();
        });

        task.setOnFailed(e -> DialogManager.showErrorDialog("Error", task.getException().getMessage()));

        executorService.submit(task);
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}