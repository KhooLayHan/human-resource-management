package org.bhel.hrm.client.controllers;

import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
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
        final int BOX_WIDTH = 60;

        lines.add("│" + "=".repeat(BOX_WIDTH) + "│");
        lines.add("│" + centerText("BHEL HUMAN RESOURCES — YEARLY REPORT", BOX_WIDTH) + "│");
        lines.add("│" + "=".repeat(BOX_WIDTH) + "│");

        // Employee Profile Section
        lines.add("│" + " ".repeat(BOX_WIDTH) + "│");
        lines.add("├ 1. EMPLOYEE PROFILE " + "─".repeat(BOX_WIDTH - 21) + "┤");
        lines.add(
            formatLine(
                "Name\t\t: " +
                    report.employeeDetails().firstName() + " " + report.employeeDetails().lastName(),
                BOX_WIDTH - 6
            )
        );
        lines.add(
            formatLine(
                "Employee ID\t: " + report.employeeDetails().id(),
                BOX_WIDTH
            )
        );
        lines.add(
            formatLine(
                "IC/Passport\t: " + report.employeeDetails().icPassport(),
                BOX_WIDTH
            )
        );
        lines.add("├" + "─".repeat(BOX_WIDTH) + "┤");

        // Leave Summary Section
        lines.add("│" + " ".repeat(BOX_WIDTH) + "│");
        lines.add("├ 2. LEAVE SUMMARY " + "─".repeat(BOX_WIDTH - 18) + "┤");
        if (report.leaveHistorySummary().isEmpty()) {
            lines.add(formatLine("No leave records found for this period.", BOX_WIDTH));
        } else {
            report.leaveHistorySummary().forEach(line ->
                lines.add(formatLine(line, BOX_WIDTH)));
        }
        lines.add("├" + "─".repeat(BOX_WIDTH) + "┤");

        // Training & Development Section
        lines.add("│" + " ".repeat(BOX_WIDTH) + "│");
        lines.add("├ 3. TRAINING & DEVELOPMENT " + "─".repeat(BOX_WIDTH - 27) + "┤");
        if (report.trainingHistorySummary().isEmpty()) {
            lines.add(formatLine("No training records found.", BOX_WIDTH));
        } else {
            report.trainingHistorySummary().forEach(line ->
                lines.add(formatLine(line, BOX_WIDTH)));
        }
        lines.add("├" + "─".repeat(BOX_WIDTH) + "┤");

        // Benefits Enrollment Section
        lines.add("│" + " ".repeat(BOX_WIDTH) + "│");
        lines.add("├ 4. BENEFITS ENROLLMENT " + "─".repeat(BOX_WIDTH - 26) + "┤");
        if (report.benefitsSummary().isEmpty()) {
            lines.add( formatLine("No active benefit plans found.", BOX_WIDTH));
        } else {
            report.benefitsSummary().forEach(line ->
                lines.add(formatLine(line, BOX_WIDTH)));
        }
        lines.add("├" + "─".repeat(BOX_WIDTH) + "┤");

        lines.add("│" + " ".repeat(BOX_WIDTH) + "│");
        lines.add("│" + "=".repeat(BOX_WIDTH) + "│");
        lines.add("│" + centerText("END OF REPORT", BOX_WIDTH) + "│");
        lines.add("│" + "=".repeat(BOX_WIDTH) + "│");

        return String.join(System.lineSeparator(), lines);
    }

    private String formatLine(String text, int boxWidth) {
        String content = "│ " + text;

        int padding = boxWidth - content.length() - 1; // -1 for the right border
        if (padding < 0) {
            // Text is too long, truncate it
            content = content.substring(0, boxWidth - 4) + "...";
            padding = 0;
        }

        return content + " ".repeat(padding) + "│";
    }

    private String centerText(String text, int boxWidth) {
        int total = boxWidth - text.length();
        int left = total / 2;
        int right = total - left;

        return " ".repeat(left) + text + " ".repeat(right);
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

    @FXML
    private void handlePrint() {
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob != null && printerJob.showPrintDialog(dialogStage)) {
            boolean success = printerJob.printPage(reportTextArea);

            if (success)
                printerJob.endJob();
        }
    }
}
