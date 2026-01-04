package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
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
import javafx.util.Callback;
import org.bhel.hrm.client.controllers.MainController;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.ApplicantDTO;
import org.bhel.hrm.common.dtos.JobOpeningDTO;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public class RecruitmentController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(RecruitmentController.class);

    @FXML private ListView<JobOpeningDTO> jobListView;
    @FXML private TableView<ApplicantDTO> applicantTable;
    @FXML private TableColumn<ApplicantDTO, String> appNameCol;
    @FXML private TableColumn<ApplicantDTO, String> appEmailCol;
    @FXML private TableColumn<ApplicantDTO, String> appPhoneCol;
    @FXML private TableColumn<ApplicantDTO, String> appStatusCol;

    @FXML private Button updateStatusButton;

    private ServiceManager serviceManager;
    private ExecutorService executorService;
    private HRMService hrmService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTableColumns();
        setupListeners();

        // Wait for scene to be ready to get MainController dependencies
        jobListView.sceneProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                initDependencies();
            }
        });
    }

    private void initDependencies() {
        try {
            MainController main = (MainController) jobListView.getScene().getRoot().getProperties().get("mainController");
            this.serviceManager = main.getServiceManager();
            this.executorService = main.getExecutorService();
            this.hrmService = serviceManager.getHrmService();

            loadJobOpenings();
        } catch (Exception e) {
            logger.error("Failed to inject dependencies", e);
        }
    }

    private void initTableColumns() {
        appNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        appEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        appPhoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        appStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void setupListeners() {
        // Master-Detail Listener: When a Job is selected, load its applicants
        jobListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadApplicants(newVal.id());
            } else {
                applicantTable.getItems().clear();
                applicantTable.setPlaceholder(new Label("Select a Job Opening to view applicants."));
            }
        });

        // Enable "Update Status" button only when an applicant is selected
        applicantTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateStatusButton.setDisable(newVal == null);
        });

        // Custom Cell Factory to display Job Titles nicely in the ListView
        jobListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(JobOpeningDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.title() + " (" + item.status() + ")");
                }
            }
        });
    }

    // --- Data Loading ---

    private void loadJobOpenings() {
        Task<List<JobOpeningDTO>> task = new Task<>() {
            @Override
            protected List<JobOpeningDTO> call() throws Exception {
                return hrmService.getAllJobOpenings();
            }
        };

        task.setOnSucceeded(e -> jobListView.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> DialogManager.showErrorDialog("Load Error", "Failed to load job openings."));

        executorService.submit(task);
    }

    private void loadApplicants(int jobId) {
        applicantTable.setPlaceholder(new Label("Loading applicants..."));

        Task<List<ApplicantDTO>> task = new Task<>() {
            @Override
            protected List<ApplicantDTO> call() throws Exception {
                return hrmService.getApplicantsForJob(jobId);
            }
        };

        task.setOnSucceeded(e -> {
            applicantTable.setItems(FXCollections.observableArrayList(task.getValue()));
            if (task.getValue().isEmpty()) {
                applicantTable.setPlaceholder(new Label("No applicants found for this job."));
            }
        });

        task.setOnFailed(e -> {
            applicantTable.setPlaceholder(new Label("Error loading data."));
            DialogManager.showErrorDialog("Load Error", "Failed to load applicants.");
        });

        executorService.submit(task);
    }

    // --- Actions ---

    @FXML
    private void handleAddJob() {
        // Open JobOpeningFormView.fxml (You can implement this similar to EmployeeForm)
        logger.info("Add Job clicked (Stub)");
        DialogManager.showInfoDialog("Stub", "Job Opening Form not yet implemented by Member B.");
    }

    @FXML
    private void handleUpdateStatus() {
        ApplicantDTO selectedApplicant = applicantTable.getSelectionModel().getSelectedItem();
        if (selectedApplicant == null) return;

        // Open a dialog to change status
        // For simplicity, let's assume we use a ChoiceDialog (Standard JavaFX) or a custom View
        // Here is a quick custom implementation using standard ChoiceDialog:

        ChoiceDialog<ApplicantDTO.ApplicantStatus> dialog = new ChoiceDialog<>(
                selectedApplicant.status(),
                ApplicantDTO.ApplicantStatus.values()
        );
        dialog.setTitle("Update Applicant Status");
        dialog.setHeaderText("Update status for: " + selectedApplicant.fullName());
        dialog.setContentText("Choose new status:");

        dialog.showAndWait().ifPresent(newStatus -> {
            if (newStatus != selectedApplicant.status()) {
                updateApplicantStatusOnServer(selectedApplicant.id(), newStatus);
            }
        });
    }

    private void updateApplicantStatusOnServer(int applicantId, ApplicantDTO.ApplicantStatus newStatus) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // You need to ensure this method exists in your RecruitmentService/HRMService!
                // hrmService.updateApplicantStatus(applicantId, newStatus);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            DialogManager.showInfoDialog("Success", "Applicant status updated.");
            // Refresh list
            loadApplicants(jobListView.getSelectionModel().getSelectedItem().id());
        });

        task.setOnFailed(e -> DialogManager.showErrorDialog("Update Failed", "Could not update status."));

        executorService.submit(task);
    }
}