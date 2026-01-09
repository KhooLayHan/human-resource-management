package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.services.HRMService;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class LeaveApprovalController {

    @FXML private Label statusLabel;

    @FXML private TableView<LeaveApplicationDTO> pendingTable;
    @FXML private TableColumn<LeaveApplicationDTO, Integer> colId;
    @FXML private TableColumn<LeaveApplicationDTO, Integer> colEmployeeId;
    @FXML private TableColumn<LeaveApplicationDTO, LocalDateTime> colStart;
    @FXML private TableColumn<LeaveApplicationDTO, LocalDateTime> colEnd;
    @FXML private TableColumn<LeaveApplicationDTO, LeaveApplicationDTO.LeaveType> colType;
    @FXML private TableColumn<LeaveApplicationDTO, String> colReason;

    @FXML private Button refreshButton;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;

    @FXML private Label selectedLabel;
    @FXML private TextArea decisionNoteArea;

    private ExecutorService executorService;
    private UserDTO currentUser;
    private HRMService hrm;

    private LeaveApplicationDTO selected;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().id()));
        colEmployeeId.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().employeeId()));
        colStart.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().startDateTime()));
        colEnd.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().endDateTime()));
        colType.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().type()));
        colReason.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().reason()));

        pendingTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            selected = val;
            boolean has = (val != null);
            approveButton.setDisable(!has);
            rejectButton.setDisable(!has);

            selectedLabel.setText(!has ? "(none)" :
                    "Leave ID: " + val.id() + "\nEmployee ID: " + val.employeeId() +
                            "\n" + val.startDateTime() + " → " + val.endDateTime() +
                            "\nType: " + val.type());
        });

        refreshButton.setOnAction(e -> loadPendingAsync());
        approveButton.setOnAction(e -> decideAsync(true));
        rejectButton.setOnAction(e -> decideAsync(false));
    }

    public void initDependencies(ServiceManager serviceManager,
                                 ExecutorService executorService,
                                 UserDTO currentUser) {
        this.executorService = executorService;
        this.currentUser = currentUser;
        this.hrm = serviceManager.getHrmService();

        setStatus("Loading pending requests...");
        loadPendingAsync();
        setBusy(true);
        setStatus("Initializing...");
    }

    private void loadPendingAsync() {
        setBusy(true);

        Runnable job = () -> {
            try {
                List<LeaveApplicationDTO> pending = hrm.getPendingLeaveRequests();
                Platform.runLater(() -> {
                    pendingTable.setItems(FXCollections.observableArrayList(pending));
                    setStatus("Loaded " + pending.size() + " pending requests.");
                });
            } catch (RemoteException | HRMException ex) {
                Platform.runLater(() -> setStatus("Failed: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setBusy(false));
            }
        };

        // ✅ fallback if executor isn't injected
        if (executorService != null) {
            executorService.submit(job);
        } else {
            new Thread(job, "leave-approval-loader").start();
        }
    }


    private void decideAsync(boolean approve) {
        if (selected == null) return;

        String note = decisionNoteArea.getText();
        int leaveId = selected.id();
        int hrUserId = currentUser.id();

        setBusy(true);
        executorService.submit(() -> {
            try {
                hrm.decideLeave(leaveId, approve, hrUserId, note);
                Platform.runLater(() -> {
                    decisionNoteArea.clear();
                    selectedLabel.setText("(none)");
                    pendingTable.getSelectionModel().clearSelection();
                    setStatus(approve ? "Approved leave " + leaveId : "Rejected leave " + leaveId);
                });
                loadPendingAsync();
            } catch (RemoteException | HRMException ex) {
                Platform.runLater(() -> setStatus("Failed: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setBusy(false));
            }
        });
    }

    private void setBusy(boolean busy) {
        refreshButton.setDisable(busy);
        approveButton.setDisable(busy || selected == null);
        rejectButton.setDisable(busy || selected == null);
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg == null ? "" : msg);
    }
}
