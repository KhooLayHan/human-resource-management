package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bhel.hrm.client.constants.FXMLPaths;
import org.bhel.hrm.client.controllers.components.PageHeaderController;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

/**
 * Controller for the Employee Management view.
 * Handles displaying, searching, adding, editing, and deleting employees.
 */
public class EmployeeManagementController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeManagementController.class);
    private static final String EMPLOYEE_TABLE_PLACEHOLDER_MESSAGE =
        "No employees found. Click 'Add New Employee' to get started.";

    @FXML private TableView<EmployeeDTO> employeeTable;
    @FXML private TableColumn<EmployeeDTO, Integer> idColumn;
    @FXML private TableColumn<EmployeeDTO, String> firstNameColumn;
    @FXML private TableColumn<EmployeeDTO, String> lastNameColumn;
    @FXML private TableColumn<EmployeeDTO, String> icPassportColumn;

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button clearSearchButton;
    @FXML private Button addNewButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;

    @FXML private PageHeaderController pageHeaderController;

    private HRMService hrmService;
    private ServiceManager serviceManager;
    private ExecutorService executorService;
    private ObservableList<EmployeeDTO> allEmployees;
    private ObservableList<EmployeeDTO> filteredEmployees;

    private boolean initialized = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Employee Management Controller initialized.");

        // Initialize table columns immediately (doesn't require scene)
        initializeTableColumns();

        // Configure page header
        if (pageHeaderController != null) {
            pageHeaderController.setTitle("Employee Management");
            pageHeaderController.setSubtitle("Manage employee records and profiles");
        }

        // Sets up a listener to complete initialization once the scene is available.
        employeeTable.sceneProperty().addListener(
            (
            observable,
            oldScene,
            newScene
            ) -> {
                if (newScene != null && !initialized) {
                    initialized = true;
                    logger.info("Scene is now available, completing initialization...");
                    completeInitialization();
                }
            }
        );
    }

    /**
     * Completes the initialization once the scene is available.
     */
    private void completeInitialization() {
        // Get dependencies from parent controller
        MainController mainController = getMainController();

        if (mainController != null) {
            this.serviceManager = mainController.getServiceManager();
            this.executorService = mainController.getExecutorService();

            if (serviceManager != null)
                this.hrmService = serviceManager.getHrmService();
        }

        if (hrmService == null) {
            logger.error("HRMService is null - cannot initialize employee management");
            Platform.runLater(() -> DialogManager.showErrorDialog(
                "Connection Error",
                "Could not connect to the server. Please check your connection."
            ));
            return;
        }

        if (executorService == null)
            logger.error("ExecutorService is null – background operations may fail.");

        setupTableSelectionListener();
        setupSearchListener();
        loadEmployees();
    }

    /**
     * Gets the MainController from the scene graph.
     */
    private MainController getMainController() {
        try {
            if (
                employeeTable.getScene() != null &&
                employeeTable.getScene().getRoot() != null
            ) {
                return (MainController) employeeTable.getScene()
                    .getRoot().getProperties().get("mainController");
            }
        } catch (Exception e) {
            logger.error("Failed to get MainController.", e);
        }

        return null;
    }

    /**
     * Initializes the table columns with cell value factories.
     */
    private void initializeTableColumns() {
        idColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().id()).asObject());
        firstNameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().firstName()));
        lastNameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().lastName()));
        icPassportColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().icPassport()));
    }

    /**
     * Sets up the table selection listener to enable/disable action buttons.
     */
    private void setupTableSelectionListener() {
        employeeTable.getSelectionModel().selectedItemProperty().addListener(
            (observable,
             oldValue,
             newValue
            ) -> {
                boolean hasSelection = newValue != null;
                editButton.setDisable(!hasSelection);
                deleteButton.setDisable(!hasSelection);
            }
        );
    }

    /**
     * Sets up the search field listener for real-time filtering.
     */
    private void setupSearchListener() {
        searchField.textProperty().addListener(
            (
            observable,
            oldValue,
            newValue
            ) -> filterEmployees(newValue));
    }

    /**
     * Loads all employees from the server asynchronously.
     */
    private void loadEmployees() {
        if (hrmService == null) {
            logger.warn("Cannot load employees - HRMService is null");
            return;
        }

        // Show loading indicator
        employeeTable.setPlaceholder(new Label("Loading employee..."));
        employeeTable.setDisable(true);

        Task<List<EmployeeDTO>> employeeManagementTask = new Task<>() {
            @Override
            protected List<EmployeeDTO> call() throws Exception {
                logger.debug("Fetching all employees from server...");
                return hrmService.getAllEmployees();
            }
        };

        employeeManagementTask.setOnSucceeded(event -> {
            logger.info("Successfully fetched employee data.");

            List<EmployeeDTO> employees = employeeManagementTask.getValue();
            allEmployees = FXCollections.observableArrayList(employees);
            filteredEmployees = FXCollections.observableArrayList(employees);

            employeeTable.setItems(filteredEmployees);
            employeeTable.setDisable(false);

            logger.info("Loaded {} employees", employees.size());

            // Restore default placeholder
            employeeTable.setPlaceholder(
                new Label(EMPLOYEE_TABLE_PLACEHOLDER_MESSAGE)
            );
        });

        employeeManagementTask.setOnFailed(event -> {
            employeeTable.setDisable(false);
            logger.error("Failed to fetch employee data", employeeManagementTask.getException());

            employeeTable.setPlaceholder(new Label("Failed to load employees. Click 'Refresh' to try again."));

            DialogManager.showErrorDialog(
                "Load Error",
                "Could not fetch employee data."
            );
        });

        if (executorService != null)
            executorService.submit(employeeManagementTask);
        else
            new Thread(employeeManagementTask).start();
    }

    /**
     * Filters the employee list based on search criteria.
     */
    private void filterEmployees(String searchText) {
        if (allEmployees == null)
            return;

        if (searchText == null || searchText.trim().isEmpty()) {
            filteredEmployees.setAll(allEmployees);
            return;
        }

        String lowerSearch = searchText.toLowerCase().trim();
        List<EmployeeDTO> filtered = allEmployees.stream()
            .filter(emp ->
                emp.firstName().toLowerCase().contains(lowerSearch) ||
                emp.lastName().toLowerCase().contains(lowerSearch) ||
                emp.icPassport().toLowerCase().contains(lowerSearch) ||
                String.valueOf(emp.id()).contains(lowerSearch)
            ).toList();

        filteredEmployees.setAll(filtered);
    }

    /**
     * Handles the search button action.
     */
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText();
        filterEmployees(searchText);

        logger.debug("Search performed with text: {}", searchText);
    }

    /**
     * Handles the clear search button action.
     */
    @FXML
    private void handleClearSearch() {
        searchField.clear();
        filterEmployees("");

        logger.debug("Search is cleared...");
    }

    /**
     * Handles the refresh button action.
     */
    @FXML
    private void handleRefresh() {
        logger.info("Refreshing employee list...");

        searchField.clear();
        loadEmployees();
    }

    /**
     * Handles the add new employee button action.
     */
    @FXML
    private void handleAddNewEmployee() {
        logger.info("Opening add employee dialog...");
        showEmployeeFormDialog(null);
    }

    /**
     * Handles the edit selected employee button action.
     */
    @FXML
    private void handleEditSelectedEmployee() {
        EmployeeDTO selectedEmployee =
            employeeTable.getSelectionModel().getSelectedItem();

        if (selectedEmployee == null) {
            DialogManager.showWarningDialog(
                "No selection",
                "Please select an employee from the table to edit."
            );
            return;
        }

        logger.info("Opening edit dialog for employee ID: {}", selectedEmployee.id());
        showEmployeeFormDialog(selectedEmployee);
    }

    /**
     * Shows the employee form dialog for adding or editing an employee
     *
     * @param employee The employee to edit, or null for adding a new employee
     */
    private void showEmployeeFormDialog(EmployeeDTO employee) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(FXMLPaths.Dialogs.EMPLOYEE_FORM));
            Stage dialogStage = new Stage();

            dialogStage.setTitle(employee == null ? "Add Employee" : "Edit Employee");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(employeeTable.getScene().getWindow());
            dialogStage.setScene(new Scene(loader.load()));
            dialogStage.setResizable(false);

            EmployeeFormController controller = loader.getController();
            if (controller == null) {
                logger.error("EmployeeFormController is null");
                DialogManager.showErrorDialog(
                    "Initialization Error",
                    "Form controller could not be initialized."
                );
                return;
            }

            // Injects dependencies to the form controller
            controller.setDialogStage(dialogStage);
            controller.setHrmService(this.hrmService);

            // If editing, set the employee data
            if (employee != null)
                controller.setEmployeeToEdit(employee);

            dialogStage.showAndWait();

            // If saved, reload the employee list
            if (controller.isSaved()) {
                logger.info("Employee saved successfully, reloading list...");
                loadEmployees();
            }
        } catch (IOException e) {
            logger.error("Failed to load employee form dialog", e);
            DialogManager.showErrorDialog(
                "UI Error",
                "Could not open the employee form. Please try again."
            );
        }
    }

    /**
     * Handles the delete selected employee button action
     */
    @FXML
    private void handleDeleteSelectedEmployee() {
        EmployeeDTO selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();

        if (selectedEmployee == null) {
            DialogManager.showWarningDialog(
                "No Selection",
                "Please select an employee from the table to delete."
            );
            return;
        }

        // Confirm deletion
        boolean confirmed = DialogManager.showConfirmationDialog(
            "Confirm Deletion",
            String.format("""
                Are you sure you want to delete employee '%s %s' (ID: %d)?
                This action cannot be undone.
                """,
                selectedEmployee.firstName(),
                selectedEmployee.lastName(),
                selectedEmployee.id()
            )
        );

        if (!confirmed) {
            logger.debug("Employee deletion cancelled by user.");
            return;
        }

        Task<Void> deleteTask = getVoidTask(selectedEmployee);

        if (executorService != null)
            executorService.submit(deleteTask);
        else
            new Thread(deleteTask).start();
    }

    private Task<Void> getVoidTask(EmployeeDTO selectedEmployee) {
        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                hrmService.deleteEmployeeById(selectedEmployee.id());
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> {
            logger.info("Employee deleted successfully.");

            DialogManager.showInfoDialog(
                "Success",
                "Employee deleted successfully."
            );
            loadEmployees();
        });

        deleteTask.setOnFailed(event -> {
            logger.error("Failed to delete employee with ID: {}",
                selectedEmployee.id(), deleteTask.getException());

            DialogManager.showErrorDialog(
                "Delete Error",
                "Could not delete the employee. Please try again."
            );
        });

        return deleteTask;
    }
}
