package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.BenefitPlanDTO;
import org.bhel.hrm.common.dtos.BenefitPlanRow;
import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class BenefitsController {

    private static final Logger logger = LoggerFactory.getLogger(BenefitsController.class);

    // Keep these as constants so checks are consistent and not duplicated
    private static final String STATUS_ENROLLED = "ENROLLED";
    private static final String STATUS_NOT_ENROLLED = "NOT ENROLLED";
    private static final String ENROLL_FAILED = "Enroll failed";
    // -------- FXML fields --------
    @FXML private Label statusLabel;
    @FXML private Label selectedPlanLabel;

    @FXML private Button refreshButton;
    @FXML private Button enrollButton;

    @FXML private TableView<BenefitPlanRow> plansTable;

    @FXML private TableColumn<BenefitPlanRow, Number> colId;
    @FXML private TableColumn<BenefitPlanRow, String> colName;
    @FXML private TableColumn<BenefitPlanRow, String> colProvider;
    @FXML private TableColumn<BenefitPlanRow, java.math.BigDecimal> colCost;
    @FXML private TableColumn<BenefitPlanRow, String> colStatus;
    @FXML private TableColumn<BenefitPlanRow, String> colDesc;

    // -------- injected dependencies --------
    private ServiceManager serviceManager;
    private ExecutorService executorService;
    private UserDTO currentUser;
    private HRMService hrm;

    // If your ViewManager always passes MainController, keep it to avoid “unused param” warning
    @SuppressWarnings("unused")
    private MainController mainController;

    // cached employee id (resolved once)
    private int employeeId = -1;

    @FXML
    public void initialize() {
        // Only UI wiring here — dependencies are null at this stage
        setupTableColumns();

        // Enroll button should start disabled
        enrollButton.setDisable(true);

        plansTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                selectedPlanLabel.setText("(none)");
                enrollButton.setDisable(true);
                return;
            }

            selectedPlanLabel.setText(selected.getPlanName() + " — " + selected.getProvider());

            // Disable enroll if already enrolled
            boolean alreadyEnrolled = STATUS_ENROLLED.equalsIgnoreCase(selected.status());
            enrollButton.setDisable(alreadyEnrolled);
        });
    }

    private void setupTableColumns() {
        // BenefitPlanRow exposes: getId(), getPlanName(), getProvider(), getCostPerMonth(), getDescription(), status()
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPlanName()));
        colProvider.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProvider()));
        colCost.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getCostPerMonth()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
    }

    // DI pattern (called by ViewManager)
    public void initDependencies(ServiceManager serviceManager,
                                 ExecutorService executorService,
                                 UserDTO currentUser,
                                 MainController mainController) {

        this.serviceManager = serviceManager;
        this.executorService = executorService;
        this.currentUser = currentUser;
        this.mainController = mainController;
        this.hrm = serviceManager.getHrmService();

        setStatus("Loading benefit plans...");
        refreshPlansAsync();
    }

    // ---------- UI actions (hook these in FXML onAction) ----------

    @FXML
    private void handleRefreshPlans() {
        refreshPlansAsync();
    }

    @FXML
    private void handleEnrollSelected() {
        enrollSelectedAsync();
    }

    // ---------- main logic ----------

    private void refreshPlansAsync() {
        setBusy(true);

        runAsync(() -> {
            try {
                ensureEmployeeId();

                List<BenefitPlanDTO> allPlans = hrm.getAllBenefitPlans();
                List<BenefitPlanDTO> myPlans = hrm.getMyBenefitPlans(employeeId);

                Set<Integer> enrolledIds = myPlans.stream()
                        .map(BenefitPlanDTO::id)
                        .collect(Collectors.toSet());

                List<BenefitPlanRow> rows = allPlans.stream()
                        .map(p -> new BenefitPlanRow(
                                p,
                                enrolledIds.contains(p.id()) ? STATUS_ENROLLED : STATUS_NOT_ENROLLED
                        ))
                        .toList();

                onFx(() -> {
                    plansTable.setItems(FXCollections.observableArrayList(rows));
                    setStatus("Loaded " + rows.size() + " plans");
                    setBusy(false);

                    plansTable.getSelectionModel().clearSelection();
                    selectedPlanLabel.setText("(none)");
                    enrollButton.setDisable(true);
                });

            } catch (RemoteException | HRMException ex) {
                logger.error("Failed to refresh benefit plans", ex);
                showErrorAndStatus("Benefits Error", ex.getMessage(), "Failed to load plans");
            } catch (Exception ex) {
                logger.error("Unexpected error while refreshing plans", ex);
                showErrorAndStatus("Benefits Error", "Unexpected error: " + ex.getMessage(), "Failed to load plans");
            }
        });
    }

    private void enrollSelectedAsync() {
        BenefitPlanRow selected = plansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        if (isEnrolled(selected)) {
            setStatus("Already enrolled in this plan.");
            enrollButton.setDisable(true);
            return;
        }

        setBusy(true);

        runAsync(() -> {
            try {
                ensureEmployeeId();

                int planId = selected.getId();
                hrm.enrollInBenefitPlan(employeeId, planId);

                onFx(() -> setStatus("Enrolled successfully. Refreshing..."));

                // Reload so status column updates
                refreshPlansAsync();

            } catch (RemoteException | HRMException ex) {
                logger.error(ENROLL_FAILED, ex);
                showErrorAndStatus("Enroll Failed", ex.getMessage(), ENROLL_FAILED);
            } catch (Exception ex) {
                logger.error("Unexpected enroll error", ex);
                showErrorAndStatus("Enroll Failed", "Unexpected error: " + ex.getMessage(), ENROLL_FAILED);
            }
        });
    }

    // ---------- helpers ----------

    private boolean isEnrolled(BenefitPlanRow row) {
        return row != null && STATUS_ENROLLED.equalsIgnoreCase(row.status());
    }

    private void ensureEmployeeId() throws RemoteException, HRMException {
        if (employeeId > 0) return;

        EmployeeDTO emp = hrm.getEmployeeByUserId(currentUser.id());
        employeeId = emp.id();
    }

    private void runAsync(Runnable job) {
        if (executorService != null) {
            executorService.submit(job);
        } else {
            new Thread(job).start();
        }
    }

    private void onFx(Runnable uiWork) {
        if (Platform.isFxApplicationThread()) {
            uiWork.run();
        } else {
            Platform.runLater(uiWork);
        }
    }

    private void showErrorAndStatus(String title, String message, String statusMsg) {
        onFx(() -> {
            setBusy(false);
            DialogManager.showErrorDialog(title, message);
            setStatus(statusMsg);
        });
    }

    private void setBusy(boolean busy) {
        refreshButton.setDisable(busy);

        // When busy always disable enroll
        if (busy) {
            enrollButton.setDisable(true);
            return;
        }

        // When not busy, enable/disable based on selection and enrollment
        BenefitPlanRow selected = plansTable.getSelectionModel().getSelectedItem();
        enrollButton.setDisable(selected == null || isEnrolled(selected));
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg == null ? "" : msg);
    }
}
