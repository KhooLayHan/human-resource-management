package org.bhel.hrm.client.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.common.dtos.LeaveApplicationDTO.LeaveType;
import org.bhel.hrm.common.dtos.LeaveApplicationDTO.LeaveStatus;
import org.bhel.hrm.common.services.HRMService;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;

public class LeaveApplicationFormController {

    @FXML private ComboBox<LeaveType> typeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea reasonArea;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;

    private HRMService hrmService;
    private ExecutorService executorService;
    private Stage dialogStage;
    private int employeeId;
    private boolean success = false;

    @FXML
    public void initialize() {
        typeComboBox.getItems().setAll(LeaveType.values());
        typeComboBox.getSelectionModel().selectFirst();
    }

    public void setDependencies(HRMService service, ExecutorService executor, Stage stage, int empId) {
        this.hrmService = service;
        this.executorService = executor;
        this.dialogStage = stage;
        this.employeeId = empId;
    }

    public boolean isSuccess() { return success; }

    @FXML
    private void handleSubmit() {
        if (!validate()) return;

        LeaveApplicationDTO dto = new LeaveApplicationDTO(
                0, // New ID
                employeeId,
                startDatePicker.getValue().atStartOfDay(),
                endDatePicker.getValue().atStartOfDay(),
                typeComboBox.getValue(),
                LeaveStatus.PENDING,
                reasonArea.getText().trim()
        );

        submitButton.setDisable(true);
        errorLabel.setVisible(false);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                hrmService.applyForLeave(dto);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            success = true;
            DialogManager.showInfoDialog("Success", "Leave application submitted successfully.");
            dialogStage.close();
        });

        task.setOnFailed(e -> {
            submitButton.setDisable(false);
            String msg = e.getSource().getMessage();
            if (msg == null) msg = "Unknown error occurred.";
            errorLabel.setText("Error: " + msg);
            errorLabel.setVisible(true);
        });

        executorService.submit(task);
    }

    private boolean validate() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            errorLabel.setText("Please select both start and end dates.");
            errorLabel.setVisible(true);
            return false;
        }

        if (end.isBefore(start)) {
            errorLabel.setText("End date cannot be before start date.");
            errorLabel.setVisible(true);
            return false;
        }

        if (start.isBefore(LocalDate.now())) {
            errorLabel.setText("Cannot apply for leave in the past.");
            errorLabel.setVisible(true);
            return false;
        }

        return true;
    }

    @FXML private void handleCancel() { dialogStage.close(); }
}