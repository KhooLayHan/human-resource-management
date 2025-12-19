package org.bhel.hrm.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.EmployeeReportDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportDialogController {
    private static final Logger logger = LoggerFactory.getLogger(ReportDialogController.class);

    @FXML private Label reportDateLabel;
    @FXML private TextArea reportTextArea;

    private Stage dialogStage;
    private EmployeeReportDTO reportData;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setReportData(EmployeeReportDTO report) {
        this.reportData = report;

        // Set header date
        reportDateLabel.setText("Generated on: " + report.generationDate().format(DATE_FMT));

        // Formatting content
        reportTextArea.setText(generateReportText(report));
    }

    private String generateReportText(EmployeeReportDTO report) {
        StringBuilder builder = new StringBuilder();

        builder.append("BHEL YEARLY REPORT").append(System.lineSeparator());

        builder.append("1. Employee Profile").append(System.lineSeparator());
        builder.append(String.format("Name: \t%s %s%n",
            report.employeeDetails().firstName(), report.employeeDetails().lastName()));
        builder.append(String.format("ID: \t%d%n", report.employeeDetails().id()));
        builder.append(String.format("IC/Passport: \t%s%n", report.employeeDetails().icPassport()));

        builder.append("2. Leave Summary").append(System.lineSeparator());
        if (report.leaveHistorySummary().isEmpty()) {
            builder.append("No leave records found for this period.").append(System.);
        } else {
            for (String line : report.leaveHistorySummary()) {
                builder.append("-> ").append(line).append("\n");
            }
        }
        builder.append("\n");

        builder.append("3. Training & Development\n");
        if (report.trainingHistorySummary().isEmpty()) {
            builder.append("No training records found.\n");
        } else {
            for (String line : report.trainingHistorySummary()) {
                builder.append("-> ").append(line).append("\n");
            }
        }
        builder.append("\n");

        builder.append("4. Benefits Enrollment\n");
        if (report.benefitsSummary().isEmpty()) {
            builder.append("No active benefit plans found.\n");
        } else {
            for (String line : report.benefitsSummary()) {
                builder.append("-> ").append(line).append("\n");
            }
        }
        builder.append("\n");

        builder.append("END OF REPORT\n");

        return builder.toString();
    }

    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName(
            "Report_" + reportData.employeeDetails().firstName() + ".txt");
        fileChooser.getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            saveToFile(file);
        }
    }

    private void saveToFile(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(reportTextArea.getText());
            DialogManager.showInfoDialog(
                "Export Successful", "Report saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            DialogManager.showErrorDialog(
                "Export Failed", "Could not save the file: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }
}
