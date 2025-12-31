package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.client.utils.ViewManager;
import org.bhel.hrm.common.dtos.DashboardDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    // Header
    @FXML private Label welcomeLabel;

    // Employee Cards
    @FXML private HBox cardAnnualLeave;
    @FXML private HBox cardMedicalLeave;
    @FXML private HBox cardUpcomingTrainings;

    @FXML private Label labelAnnualLeave;
    @FXML private Label labelMedicalLeave;
    @FXML private Label labelUpcomingTrainings;

    @FXML private Label labelAnnualLeaveSubtext;
    @FXML private Label labelMedicalLeaveSubtext;
    @FXML private Label labelUpcomingTrainingsSubtext;

    // HR Cards
    @FXML private HBox cardTotalEmployees;
    @FXML private HBox cardPendingLeaves;
    @FXML private HBox cardOpenJobs;

    @FXML private Label labelTotalEmployees;
    @FXML private Label labelPendingLeaves;
    @FXML private Label labelOpenJobs;

    @FXML private Label labelTotalEmployeesSubtext;
    @FXML private Label labelPendingLeavesSubtext;
    @FXML private Label labelOpenJobsSubtext;

    // Charts
    @FXML private VBox chartDepartmentDistribution;
    @FXML private VBox chartLeaveStatus;
    @FXML private VBox chartRecruitmentPipeline;
    @FXML private VBox chartTrainingTrends;

    @FXML private PieChart departmentPieChart;
    @FXML private BarChart<String, Number> leaveStatusChart;
    @FXML private BarChart<String, Number> recruitmentPipelineChart;
    @FXML private LineChart<String, Number> trainingTrendsChart;

    // Activity Feed
    @FXML private VBox activityFeed;

    // Quick Action Buttons
    @FXML private Button buttonApplyLeave;
    @FXML private Button buttonViewProfile;
    @FXML private Button buttonBrowseTraining;
    @FXML private Button buttonManageLeaveRequests;
    @FXML private Button buttonManageRecruitment;
    @FXML private Button buttonGenerateReports;

    @FXML private URL url;
    @FXML private ResourceBundle resourceBundle;

    private ServiceManager serviceManager;
    private ExecutorService executorService;
    private UserDTO currentUser;
    private MainController mainController;

    @FXML
    public void initialize() {
        if (currentUser == null) {
            logger.error("Cannot initialize dashboard without user data");
            return;
        }

        setupRoleBasedView();
        setupQuickActions();
        loadDashboardData();

//        departmentPieChart.setAnimated(true);
//
//        cardTotalEmployees.sceneProperty().addListener(
//            (
//            observable,
//            oldScene,
//            newScene
//            ) -> {
//                if (newScene != null)
//                    initDependencies();
//            }
//        );
    }

    public void initDependencies(
        ServiceManager serviceManager,
        ExecutorService executorService,
        UserDTO currentUser,
        MainController mainController
    ) {
        this.serviceManager = serviceManager;
        this.executorService = executorService;
        this.currentUser = currentUser;
        this.mainController = mainController;

        initialize();
//        try {
//            MainController mainController = getMainController();
//
//            if (mainController != null) {
//                serviceManager = mainController.getServiceManager();
//                this.executorService = mainController.getExecutorService();
//
//                if (serviceManager != null)
//                    this.currentUser = mainController.getCurrentUser();
//            }
//
//            setupRoleBasedView();
//            loadDashboardData();
//        } catch (Exception e) {
//            logger.error("Failed to inject dependencies", e);
//        }
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

    /**
     * Show/hide components based on user role.
     */
    private void setupRoleBasedView() {
        welcomeLabel.setText("Welcome, " + currentUser.username() + "!");

        boolean isHr = currentUser.role() == UserDTO.Role.HR_STAFF;
        boolean isEmployee = currentUser.role() == UserDTO.Role.EMPLOYEE;

        // Employee Cards
        setVisible(cardAnnualLeave, isHr || isEmployee);
        setVisible(cardMedicalLeave, isHr || isEmployee);
        setVisible(cardUpcomingTrainings, isHr || isEmployee);

        // HR Cards
        setVisible(cardTotalEmployees, isHr);
        setVisible(cardPendingLeaves, isHr);
        setVisible(cardOpenJobs, isHr);

        // Charts
        setVisible(chartDepartmentDistribution, isHr);
        setVisible(chartLeaveStatus, isHr);
        setVisible(chartRecruitmentPipeline, isHr);
        setVisible(chartTrainingTrends, isHr);

        // Quick Action Buttons
        setVisible(buttonApplyLeave, isHr || isEmployee);
        setVisible(buttonViewProfile, isHr || isEmployee);
        setVisible(buttonBrowseTraining, isHr || isEmployee);

        setVisible(buttonManageLeaveRequests, isHr);
        setVisible(buttonManageRecruitment, isHr);
        setVisible(buttonGenerateReports, isHr);
    }

    /**
     * Setup quick action button handlers
     */
    private void setupQuickActions() {
//        if (buttonApplyLeave != null)
//            buttonApplyLeave.setOnAction(e -> navigateToLeave());
    }

    private void loadDashboardData() {
        Task<DashboardDTO> loadDashboardTask = new Task<>() {
            @Override
            protected DashboardDTO call() throws Exception {
            return serviceManager.getHrmService().generateDashboard(currentUser.id());
            }
        };

        loadDashboardTask.setOnSucceeded(e -> updateUI(loadDashboardTask.getValue()));

        loadDashboardTask.setOnFailed(e -> {
            logger.error("Failed to load dashboard data", loadDashboardTask.getException());
            DialogManager.showErrorDialog(
                "Dashboard Error",
                "Failed to load dashboard data."
            );
        });

        if (executorService != null)
            executorService.submit(loadDashboardTask);
        else
            new Thread(loadDashboardTask).start();
    }

    private void updateUI(DashboardDTO dashboard) {
//        labelTotalEmployees.setText(String.valueOf(dashboard.totalEmployees()));
//        labelPendingLeaves.setText(String.valueOf(dashboard.pendingLeaveRequests()));
//        labelOpenJobs.setText(String.valueOf(dashboard.openJobPositions()));
//        labelAnnualLeave.setText(String.valueOf(dashboard.annualLeaveBalance()));
//
//        if (dashboard.departmentDistribution() != null) {
//            ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
//
//            dashboard.departmentDistribution().forEach((dept, count) ->
//                chartData.add(new PieChart.Data(dept, count)));
//
//            departmentPieChart.setData(chartData);
//        }

        updateStatCards(dashboard);
        updateCharts(dashboard);
    }

    private void updateStatCards(DashboardDTO dashboard) {
        Platform.runLater(() -> {
            // Employee Stats
            if (labelAnnualLeave != null) {
                labelAnnualLeave.setText(String.valueOf(dashboard.annualLeaveBalance()));
                labelAnnualLeaveSubtext.setText("days remaining");
            }

            if (labelMedicalLeave != null) {
                labelMedicalLeave.setText(String.valueOf(dashboard.medicalLeaveBalance()));
                labelMedicalLeaveSubtext.setText("days remaining");
            }

            if (labelUpcomingTrainings != null) {
                labelUpcomingTrainings.setText(String.valueOf(dashboard.upcomingTrainingsCount()));
                labelUpcomingTrainingsSubtext.setText("courses enrolled");
            }

            // HR Staff Stats
            if (labelTotalEmployees != null) {
                labelTotalEmployees.setText(String.valueOf(dashboard.totalEmployees()));
                labelTotalEmployeesSubtext.setText("active staff");
            }

            if (labelPendingLeaves != null) {
                labelPendingLeaves.setText(String.valueOf(dashboard.pendingLeaveRequests()));
                labelPendingLeavesSubtext.setText("require action");
            }

            if (labelOpenJobs != null) {
                labelOpenJobs.setText(String.valueOf(dashboard.openJobPositions()));
                labelOpenJobsSubtext.setText("actively recruiting");
            }
        });
    }

    /**
     * Update all charts with data
     */
    private void updateCharts(DashboardDTO dashboard) {
        Platform.runLater(() -> {
            updateDepartmentChart(dashboard.departmentDistribution());
            updateLeaveStatusChart(dashboard.leaveStatusBreakdown());
            updateRecruitmentChart(dashboard.recruitmentPipelineData());
            updateTrainingTrendsChart(dashboard.trainingEnrollmentTrend());
        });
    }

    /**
     * Update department distribution pie chart
     */
    private void updateDepartmentChart(Map<String, Integer> distribution) {
        if (distribution == null || departmentPieChart == null)
            return;

        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        distribution.forEach((dept, count) ->
            chartData.add(new PieChart.Data(dept, count)));

        departmentPieChart.setData(chartData);
        departmentPieChart.setAnimated(true);
    }

    /**
     * Update leave status bar chart.
     */
    private void updateLeaveStatusChart(Map<String, Integer> leave) {
        if (leave == null || leaveStatusChart == null)
            return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Leave Requests");

        leave.forEach((status, count) ->
            series.getData().add(new XYChart.Data<>(status, count)));

        leaveStatusChart.getData().clear();
        leaveStatusChart.getData().add(series);
        leaveStatusChart.setAnimated(true);
    }

    /**
     * Update recruitment pipeline bar chart
     */
    private void updateRecruitmentChart(Map<String, Integer> pipeline) {
        if (pipeline == null || recruitmentPipelineChart == null)
            return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Applicants");

        pipeline.forEach((stage, count) ->
            series.getData().add(new XYChart.Data<>(stage, count)));

        recruitmentPipelineChart.getData().clear();
        recruitmentPipelineChart.getData().add(series);
        recruitmentPipelineChart.setAnimated(true);
    }

    /**
     * Update training enrollment trends line chart
     */
    private void updateTrainingTrendsChart(Map<String, Integer> trend) {
        if (trend == null || trainingTrendsChart == null)
            return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Enrollments");

        trend.forEach((month, count) ->
                series.getData().add(new XYChart.Data<>(month, count)));

        trainingTrendsChart.getData().clear();
        trainingTrendsChart.getData().add(series);
        trainingTrendsChart.setAnimated(true);
    }

    /**
     * Helper to show/hide nodes
     */
    private void setVisible(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

//    private void navigateToLeave() {
//        if (mainController != null) {
//            // Trigger navigation in MainController
//            // You'll need to expose a method like mainController.navigateToView("Leave")
//            logger.info("Navigate to Leave view");

//            mainController
//        }
//    }
//
//    private void navigateToProfile() {
//        if (mainController != null) {
//            logger.info("Navigate to Profile view");
//        }
//    }
//
//    private void navigateToTraining() {
//        if (mainController != null) {
//            logger.info("Navigate to Training view");
//        }
//    }
//
//    private void navigateToLeaveManagement() {
//        if (mainController != null) {
//            logger.info("Navigate to Leave Management view");
//        }
//    }
//
//    private void navigateToRecruitment() {
//        if (mainController != null) {
//            logger.info("Navigate to Recruitment view");
//        }
//    }

    @FXML
    private void handleNavigateToReports() {
        if (mainController != null) {
            logger.info("Navigate to Reports view");
            ViewManager.loadView(c);
        }
    }
}
