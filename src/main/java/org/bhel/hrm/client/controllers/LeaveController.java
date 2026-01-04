package org.bhel.hrm.client.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bhel.hrm.client.constants.FXMLPaths;
import org.bhel.hrm.client.controllers.LeaveApplicationFormController;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public class LeaveController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    @FXML private Label annualBalLabel;
    @FXML private Label medicalBalLabel;
    @FXML private TableView<LeaveApplicationDTO> leaveTable;
    @FXML private TableColumn<LeaveApplicationDTO, String> typeCol;
    @FXML private TableColumn<LeaveApplicationDTO, String> startCol;
    @FXML private TableColumn<LeaveApplicationDTO, String> endCol;
    @FXML private TableColumn<LeaveApplicationDTO, String> statusCol;
    @FXML private TableColumn<LeaveApplicationDTO, String> reasonCol;
    @FXML private TableColumn<LeaveApplicationDTO, Long> daysCol;

    private ServiceManager serviceManager;
    private ExecutorService executorService;
    private UserDTO currentUser;

    // We need the Employee ID to fetch leave, not just User ID.
    // For now, we assume we fetch it or the server handles UserID -> EmployeeID mapping.
    private int currentEmployeeId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        // Wait for dependency injection via MainController
        leaveTable.sceneProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) initDependencies();
        });
    }

    private void setupTable() {
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));

        // Calculate days duration on the client side for display
        daysCol.setCellValueFactory(data -> {
            long days = ChronoUnit.DAYS.between(data.getValue().startDateTime(), data.getValue().endDateTime()) + 1;
            return new javafx.beans.property.SimpleObjectProperty<>(days);
        });
    }

    private void initDependencies() {
        try {
            MainController main = (MainController) leaveTable.getScene().getRoot().getProperties().get("mainController");
            this.serviceManager = main.getServiceManager();
            this.executorService = main.getExecutorService();
            this.currentUser = main.getCurrentUser();

            // In a real app, you'd fetch the Employee ID based on User ID here.
            // For now, let's assume we fetch the employee profile first to get the ID.
            fetchEmployeeIdAndLoadData();

        } catch (Exception e) {
            logger.error("Failed to inject dependencies", e);
        }
    }

    private void fetchEmployeeIdAndLoadData() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                // RMI Call: User ID -> Employee DTO -> Employee ID
                var emp = serviceManager.getHrmService().getEmployeeByUserId(currentUser.id());
                return emp.id();
            }
        };

        task.setOnSucceeded(e -> {
            this.currentEmployeeId = task.getValue();
            loadLeaveHistory();
        });

        task.setOnFailed(e -> DialogManager.showErrorDialog("Error", "Could not load employee details."));
        executorService.submit(task);
    }

    private void loadLeaveHistory() {
        leaveTable.setPlaceholder(new Label("Loading..."));

        Task<List<LeaveApplicationDTO>> task = new Task<>() {
            @Override
            protected List<LeaveApplicationDTO> call() throws Exception {
                return serviceManager.getHrmService().getLeaveHistoryForEmployees(currentEmployeeId);
            }
        };

        task.setOnSucceeded(e -> {
            leaveTable.setItems(FXCollections.observableArrayList(task.getValue()));
            if (task.getValue().isEmpty()) leaveTable.setPlaceholder(new Label("No leave records found."));
        });

        task.setOnFailed(e -> DialogManager.showErrorDialog("Error", "Failed to load leave history."));
        executorService.submit(task);
    }

    @FXML
    private void handleApplyLeave() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/bhel/hrm/client/view/dialogs/LeaveApplicationFormView.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Apply for Leave");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(leaveTable.getScene().getWindow());
            stage.setScene(new Scene(loader.load()));

            LeaveApplicationFormController controller = loader.getController();
            controller.setDependencies(serviceManager.getHrmService(), executorService, stage, currentEmployeeId);

            stage.showAndWait();

            if (controller.isSuccess()) {
                loadLeaveHistory(); // Refresh table
            }

        } catch (IOException e) {
            logger.error("Failed to open leave form", e);
        }
    }

    @FXML
    private void handleRefresh() {
        if (currentEmployeeId > 0) loadLeaveHistory();
    }
}