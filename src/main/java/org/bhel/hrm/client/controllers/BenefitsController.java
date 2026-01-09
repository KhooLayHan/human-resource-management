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
            boolean alreadyEnrolled = "ENROLLED".equalsIgnoreCase(selected.status());
            enrollButton.setDisable(alreadyEnrolled);
        });
    }

    private void setupTableColumns() {
        // NOTE: BenefitPlanRow exposes id(), planName(), provider(), costPerMonth(), description(), status()
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
        this.hrm = serviceManager.getHrmService();

        setStatus("Loading benefit plans...");
        refreshPlansAsync(); // resolves employeeId + loads plans
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
                // Resolve employeeId once
                if (employeeId <= 0) {
                    EmployeeDTO emp = hrm.getEmployeeByUserId(currentUser.id());
                    employeeId = emp.id();
                }

                // Load plans
                List<BenefitPlanDTO> allPlans = hrm.getAllBenefitPlans();
                List<BenefitPlanDTO> myPlans = hrm.getMyBenefitPlans(employeeId);

                Set<Integer> enrolledIds = myPlans.stream()
                        .map(BenefitPlanDTO::id)
                        .collect(Collectors.toSet());

                List<BenefitPlanRow> rows = allPlans.stream()
                        .map(p -> new BenefitPlanRow(
                                p,
                                enrolledIds.contains(p.id()) ? "ENROLLED" : "NOT ENROLLED"
                        ))
                        .toList();

                Platform.runLater(() -> {
                    plansTable.setItems(FXCollections.observableArrayList(rows));
                    setStatus("Loaded " + rows.size() + " plans");
                    setBusy(false);

                    // keep selection consistent (optional)
                    plansTable.getSelectionModel().clearSelection();
                    selectedPlanLabel.setText("(none)");
                    enrollButton.setDisable(true);
                });

            } catch (RemoteException | HRMException ex) {
                logger.error("Failed to refresh benefit plans", ex);
                Platform.runLater(() -> {
                    setBusy(false);
                    DialogManager.showErrorDialog("Benefits Error", ex.getMessage());
                    setStatus("Failed to load plans");
                });
            } catch (Exception ex) {
                logger.error("Unexpected error while refreshing plans", ex);
                Platform.runLater(() -> {
                    setBusy(false);
                    DialogManager.showErrorDialog("Benefits Error", "Unexpected error: " + ex.getMessage());
                    setStatus("Failed to load plans");
                });
            }
        });
    }

    private void enrollSelectedAsync() {
        BenefitPlanRow selected = plansTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if ("ENROLLED".equalsIgnoreCase(selected.status())) {
            setStatus("Already enrolled in this plan.");
            enrollButton.setDisable(true);
            return;
        }

        setBusy(true);

        runAsync(() -> {
            try {
                // Ensure employeeId
                if (employeeId <= 0) {
                    EmployeeDTO emp = hrm.getEmployeeByUserId(currentUser.id());
                    employeeId = emp.id();
                }

                int planId = selected.getId();
                hrm.enrollInBenefitPlan(employeeId, planId);

                Platform.runLater(() -> setStatus("Enrolled successfully. Refreshing..."));

                // Reload so status column updates
                refreshPlansAsync();

            } catch (RemoteException | HRMException ex) {
                logger.error("Enroll failed", ex);
                Platform.runLater(() -> {
                    setBusy(false);
                    DialogManager.showErrorDialog("Enroll Failed", ex.getMessage());
                    setStatus("Enroll failed");
                });
            } catch (Exception ex) {
                logger.error("Unexpected enroll error", ex);
                Platform.runLater(() -> {
                    setBusy(false);
                    DialogManager.showErrorDialog("Enroll Failed", "Unexpected error: " + ex.getMessage());
                    setStatus("Enroll failed");
                });
            }
        });
    }

    // ---------- helpers ----------

    private void runAsync(Runnable job) {
        if (executorService != null) executorService.submit(job);
        else new Thread(job).start();
    }

    private void setBusy(boolean busy) {
        refreshButton.setDisable(busy);

        // Enroll is only enabled when selected + not enrolled; when busy always disable
        if (busy) {
            enrollButton.setDisable(true);
            return;
        }

        BenefitPlanRow selected = plansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            enrollButton.setDisable(true);
        } else {
            enrollButton.setDisable("ENROLLED".equalsIgnoreCase(selected.status()));
        }
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg == null ? "" : msg);
    }


}
