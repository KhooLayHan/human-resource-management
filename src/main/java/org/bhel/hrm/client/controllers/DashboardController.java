package org.bhel.hrm.client.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.DashboardDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public class DashboardController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Label welcomeLabel;
    @FXML private PieChart deptPieChart;

    @FXML private HBox cardTotalEmployees;
    @FXML private HBox cardPendingLeaves;
    @FXML private HBox cardOpenJobs;
    @FXML private HBox cardAnnualLeave;

    @FXML private Label labelTotalEmployees;
    @FXML private Label labelPendingLeaves;
    @FXML private Label labelOpenJobs;
    @FXML private Label labelAnnualLeave;

    private ServiceManager serviceManager;
    private ExecutorService executorService;
    private UserDTO currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resourceBundle) {
        deptPieChart.setAnimated(true);

        cardTotalEmployees.sceneProperty().addListener(
            (
            observable,
            oldScene,
            newScene
            ) -> {
                if (newScene != null)
                    initDependencies();
            }
        );
    }

    private void initDependencies() {
        try {
            MainController mainController = getMainController();

            if (mainController != null) {
                serviceManager = mainController.getServiceManager();
                this.executorService = mainController.getExecutorService();

                if (serviceManager != null)
                    this.currentUser = mainController.getCurrentUser();
            }

            setupRoleBasedView();
            loadDashboardData();
        } catch (Exception e) {
            logger.error("Failed to inject dependencies", e);
        }
    }

    /**
     * Gets the MainController from the scene graph.
     */
    private MainController getMainController() {
        try {
            if (
                cardTotalEmployees.getScene() != null &&
                cardTotalEmployees.getScene().getRoot() != null
            ) {
                return (MainController) cardTotalEmployees.getScene()
                    .getRoot().getProperties().get("mainController");
            }
        } catch (Exception e) {
            logger.error("Failed to get MainController.", e);
        }

        return null;
    }

    private void setupRoleBasedView() {
        welcomeLabel.setText("Welcome, " + currentUser.username() + "!");

        boolean isHr = currentUser.role() == UserDTO.Role.HR_STAFF;

        setVisible(cardTotalEmployees, isHr);
        setVisible(cardPendingLeaves, isHr);
        setVisible(cardOpenJobs, isHr);

        setVisible(cardAnnualLeave, !isHr);
    }

    private void setVisible(HBox card, boolean visible) {
        card.setVisible(visible);
        card.setManaged(visible);
    }

    private void loadDashboardData() {
        Task<DashboardDTO> loadDashboardTask = new Task<>() {
            @Override
            protected DashboardDTO call() throws Exception {
                return serviceManager.getHrmService().generateDashboard(currentUser.id());
            }
        };

        loadDashboardTask.setOnSucceeded(e -> updateUI(loadDashboardTask.getValue()));

        loadDashboardTask.setOnFailed(e ->
            DialogManager.showErrorDialog(
                "Dashboard Error",
                "Failed to load dashboard data."
            ));

        if (executorService != null)
            executorService.submit(loadDashboardTask);
        else
            new Thread(loadDashboardTask).start();
    }

    private void updateUI(DashboardDTO dashboard) {
        labelTotalEmployees.setText(String.valueOf(dashboard.totalEmployees()));
        labelPendingLeaves.setText(String.valueOf(dashboard.pendingLeaveRequests()));
        labelOpenJobs.setText(String.valueOf(dashboard.openJobPositions()));
        labelAnnualLeave.setText(String.valueOf(dashboard.annualLeaveBalance()));

        if (dashboard.departmentDistribution() != null) {
            ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

            dashboard.departmentDistribution().forEach((dept, count) ->
                chartData.add(new PieChart.Data(dept, count)));

            deptPieChart.setData(chartData);
        }
    }
}
