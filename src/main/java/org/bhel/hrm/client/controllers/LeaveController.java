package org.bhel.hrm.client.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.bhel.hrm.client.services.ServiceManager;
import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.common.services.HRMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bhel.hrm.common.exceptions.HRMException;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class LeaveController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    // -------- FXML fields --------
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<LeaveApplicationDTO.LeaveType> leaveTypeCombo;
    @FXML private TextField reasonField;

    @FXML private Button applyButton;
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;

    @FXML private TableView<LeaveApplicationDTO> leaveHistoryTable;
    @FXML private TableColumn<LeaveApplicationDTO, Integer> colId;
    @FXML private TableColumn<LeaveApplicationDTO, LocalDateTime> colStart;
    @FXML private TableColumn<LeaveApplicationDTO, LocalDateTime> colEnd;
    @FXML private TableColumn<LeaveApplicationDTO, LeaveApplicationDTO.LeaveType> colType;
    @FXML private TableColumn<LeaveApplicationDTO, LeaveApplicationDTO.LeaveStatus> colStatus;
    @FXML private TableColumn<LeaveApplicationDTO, String> colReason;

    private ExecutorService executorService;
    private UserDTO currentUser;
    private HRMService hrm;
    private int employeeId = -1;

    // -------- JavaFX lifecycle --------
    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().id()));
        colStart.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().startDateTime()));
        colEnd.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().endDateTime()));
        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().type()));
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().status()));
        colReason.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().reason()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colStart.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(fmt));
            }
        });
        colEnd.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(fmt));
            }
        });

        leaveTypeCombo.setItems(FXCollections.observableArrayList(LeaveApplicationDTO.LeaveType.values()));
        leaveTypeCombo.getSelectionModel().select(LeaveApplicationDTO.LeaveType.ANNUAL);

        applyButton.setOnAction(e -> onApply());
        refreshButton.setOnAction(e -> refreshHistoryAsync());
    }

    // -------- Dependency injection --------
    public void initDependencies(ServiceManager serviceManager,
                                 ExecutorService executorService,
                                 UserDTO currentUser) {
        this.executorService = executorService;
        this.currentUser = currentUser;
        this.hrm = serviceManager.getHrmService();

        setStatus("Loading leave history...");
        refreshHistoryAsync();
    }

    // -------- Actions --------
    private void onApply() {
        if (employeeId <= 0) {
            showError("Employee ID not loaded yet. Please try again.");
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showWarning("Please select both Start Date and End Date.");
            return;
        }
        if (endDate.isBefore(startDate)) {
            showWarning("End Date cannot be before Start Date.");
            return;
        }

        LeaveApplicationDTO.LeaveType type = leaveTypeCombo.getValue();
        if (type == null) {
            showWarning("Please select a leave type.");
            return;
        }

        String reason = reasonField.getText();

        // FIX: include full end date
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.plusDays(1).atStartOfDay();

        LeaveApplicationDTO dto = new LeaveApplicationDTO(
                0,
                employeeId,
                start,
                end,
                type,
                LeaveApplicationDTO.LeaveStatus.PENDING,
                reason
        );

        setBusy(true);
        setStatus("Submitting leave application...");

        executorService.submit(() -> {
            try {
                hrm.applyForLeave(dto);

                Platform.runLater(() -> {
                    setStatus("Leave application submitted.");
                    clearForm();
                });

                refreshHistoryAsync();

            } catch (RemoteException | HRMException ex) {
                logger.error("applyForLeave failed", ex);
                Platform.runLater(() -> {
                    showError("Failed to apply for leave: " + ex.getMessage());
                    setBusy(false);
                });
            }
        });
    }

    private void refreshHistoryAsync() {
        setBusy(true);

        executorService.submit(() -> {
            try {
                if (employeeId <= 0) {
                    EmployeeDTO emp = hrm.getEmployeeByUserId(currentUser.id());
                    employeeId = emp.id();
                }

                List<LeaveApplicationDTO> history =
                        hrm.getLeaveHistoryForEmployees(employeeId);

                Platform.runLater(() -> {
                    leaveHistoryTable.setItems(FXCollections.observableArrayList(history));
                    setStatus("Loaded " + history.size() + " leave records.");
                });
            } catch (RemoteException | HRMException ex) {
                logger.error("refreshHistory failed", ex);
                Platform.runLater(() ->
                        showError("Failed to load leave history: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setBusy(false));
            }
        });
    }

    // -------- Helpers --------
    private void clearForm() {
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        leaveTypeCombo.getSelectionModel()
                .select(LeaveApplicationDTO.LeaveType.ANNUAL);
        reasonField.clear();
    }

    private void setBusy(boolean busy) {
        applyButton.setDisable(busy);
        refreshButton.setDisable(busy);
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg == null ? "" : msg);
    }

    private void showWarning(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
        setStatus(msg);
    }
}
