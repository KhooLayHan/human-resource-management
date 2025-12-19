package org.bhel.hrm.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.common.dtos.EmployeeReportDTO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportDialogController {
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
        List<String> lines = new ArrayList<>();

        lines.add("BHEL YEARLY REPORT");

        lines.add("1. Employee Profile");
        lines.add(
            String.format(
                "Name: \t%s %s%n",
                report.employeeDetails().firstName(), report.employeeDetails().lastName()
            )
        );
        lines.add(
            String.format(
                "Employee ID: \t%d%n",
                report.employeeDetails().id()
            )
        );
        lines.add(
            String.format(
                "IC/Passport: \t%s%n",
                report.employeeDetails().icPassport()
            )
        );

        lines.add("2. Leave Summary");
        if (report.leaveHistorySummary().isEmpty()) {
            lines.add("No leave records found for this period.");
        } else {
            report.leaveHistorySummary().forEach(line ->
                lines.add("-> " + line));
        }
        lines.add("");

        lines.add("3. Training & Development");
        if (report.trainingHistorySummary().isEmpty()) {
            lines.add("No training records found.");
        } else {
            report.trainingHistorySummary().forEach(line ->
                lines.add("-> " + line));
        }
        lines.add("");

        lines.add("4. Benefits Enrollment");
        if (report.benefitsSummary().isEmpty()) {
            lines.add("No active benefit plans found.");
        } else {
            report.benefitsSummary().forEach(line ->
                lines.add("-> " + line));
        }
        lines.add("");

        lines.add("END OF REPORT");

        return String.join(System.lineSeparator(), lines);
    }

    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName(
            "Report_" + reportData.employeeDetails().firstName() + ".txt");
        fileChooser.getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        // Considering to add OpenPDF to generate PDFs instead...
        // fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

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
