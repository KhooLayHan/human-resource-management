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
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BenefitsController {

    private static final Logger logger = LoggerFactory.getLogger(BenefitsController.class);

    private static final String STATUS_ENROLLED = "ENROLLED";
    private static final String STATUS_NOT_ENROLLED = "NOT ENROLLED";
    private static final String ENROLL_FAILED = "Enroll failed";

    private static final ExecutorService FALLBACK_EXECUTOR =
            Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "BenefitsController-fallback");
                t.setDaemon(true);
                return t;
            });

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

    @SuppressWarnings("unused")
    private MainController mainController;

    private int employeeId = -1;

    @FXML
    public void initialize() {
        setupTableColumns();

        enrollButton.setDisable(true);

        plansTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                selectedPlanLabel.setText("(none)");
                enrollButton.setDisable(true);
                return;
            }

            selectedPlanLabel.setText(selected.getPlanName() + " — " + selected.getProvider());
            enrollButton.setDisable(isEnrolled(selected));
        });
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPlanName()));
        colProvider.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProvider()));
        colCost.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getCostPerMonth()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
    }

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

                    plansTable.getSelectionModel().clearSelection();
                    selectedPlanLabel.setText("(none)");
                    enrollButton.setDisable(true);
                });

            } catch (RemoteException | HRMException ex) {
                logger.error("Failed to refresh benefit plans", ex);
                showErrorAndStatus("Benefits Error", ex.getMessage(), "Failed to load plans");
            } catch (Exception ex) {
                logger.error("Unexpected error while refreshing plans", ex);
                showErrorAndStatus("Benefits Error",
                        "Unexpected error: " + ex.getMessage(),
                        "Failed to load plans");
            } finally {
                setBusy(false);
            }
        });
    }

    private void enrollSelectedAsync() {
        BenefitPlanRow selected = plansTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (isEnrolled(selected)) {
            setStatus("Already enrolled in this plan.");
            enrollButton.setDisable(true);
            return;
        }

        setBusy(true);

        runAsync(() -> {
            try {
                ensureEmployeeId();

                hrm.enrollInBenefitPlan(employeeId, selected.getId());

                onFx(() -> setStatus("Enrolled successfully. Refreshing..."));
                refreshPlansAsync();

            } catch (RemoteException | HRMException ex) {
                logger.error(ENROLL_FAILED, ex);
                showErrorAndStatus("Enroll Failed", ex.getMessage(), ENROLL_FAILED);
            } catch (Exception ex) {
                logger.error("Unexpected enroll error", ex);
                showErrorAndStatus("Enroll Failed",
                        "Unexpected error: " + ex.getMessage(),
                        ENROLL_FAILED);
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
        ExecutorService exec =
                executorService != null ? executorService : FALLBACK_EXECUTOR;
        exec.submit(job);
    }

    private void onFx(Runnable uiWork) {
        if (Platform.isFxApplicationThread()) {
            uiWork.run();
        } else {
            Platform.runLater(uiWork);
        }
    }

    private void showErrorAndStatus(String title, String msg, String status) {
        onFx(() -> {
            DialogManager.showErrorDialog(title, msg);
            setStatus(status);
            setBusy(false);
        });
    }

    private void setBusy(boolean busy) {
        refreshButton.setDisable(busy);

        if (busy) {
            enrollButton.setDisable(true);
            return;
        }

        BenefitPlanRow selected = plansTable.getSelectionModel().getSelectedItem();
        enrollButton.setDisable(selected == null || isEnrolled(selected));
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg == null ? "" : msg);
    }
}
