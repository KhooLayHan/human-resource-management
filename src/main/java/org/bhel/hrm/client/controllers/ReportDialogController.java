package org.bhel.hrm.client.controllers;

import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bhel.hrm.client.utils.DialogManager;
import org.bhel.hrm.client.utils.PdfReportGenerator;
import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.dtos.EmployeeReportDTO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
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
                BOX_WIDTH - 8
            )
        );
        lines.add(
            formatLine(
                "Employee ID\t: " + report.employeeDetails().id(),
                BOX_WIDTH - 2
            )
        );
        lines.add(
            formatLine(
                "IC/Passport\t: " + report.employeeDetails().icPassport(),
                BOX_WIDTH - 2
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
        lines.add("├ 4. BENEFITS ENROLLMENT " + "─".repeat(BOX_WIDTH - 24) + "┤");
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
        String paddedText = " " + text;

        int padding = boxWidth - paddedText.length();
        if (padding < 0) {
            // Text is too long, truncate it
            paddedText = paddedText.substring(0, boxWidth - 3) + "...";
            padding = 0;
        }

        return "│" + paddedText + " ".repeat(padding) + "│";
    }

    private String centerText(String text, int boxWidth) {
        int total = boxWidth - text.length();
        int left = total / 2;
        int right = total - left;

        return " ".repeat(left) + text + " ".repeat(right);
    }

    @FXML
    private void handleExport() {
        String filename = String.format(
            "Report_%s_%s.%s.txt",
            reportData.employeeDetails().firstName(),
            reportData.employeeDetails().lastName(),
            LocalDate.now()
        );

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName(filename);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(dialogStage);

        if (file != null) {
            String extension = getFileExtension(file);

            switch (extension.toLowerCase()) {
                case "pdf" -> saveAsPdf(file);
                case "csv" -> saveAsCsv(file);
                default -> saveAsText(file);
            }
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int period = name.lastIndexOf('.');

        return period > 0 ? name.substring(period + 1) : "txt";
    }

    private void saveAsPdf(File file) {
        try {
            PdfReportGenerator.generateReport(reportData, file);
            DialogManager.showInfoDialog(
                "Export Successful", "PDF report saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            DialogManager.showErrorDialog(
                "Export Failed", "Could not save the PDF file: " + e.getMessage());
        }
    }

    private void saveAsCsv(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            // CSV Header
            writer.write("Section,Field,Value\n");

            // Employee Profile
            EmployeeDTO employee = reportData.employeeDetails();
            writer.write(String.format("Employee Profile,Name,%s %s%n",
                employee.firstName(), employee.lastName()));
            writer.write(String.format("Employee Profile,Employee ID,%d%n",
                employee.id()));
            writer.write(String.format("Employee Profile,IC/Passport,%s%n",
                employee.icPassport()));

            // Leave Summary
            for (String leave : reportData.leaveHistorySummary()) {
                writer.write(String.format("Leave Summary,Entry,%s%n",
                    leave.replace(",", ";")));
            }

            // Training & Development
            for (String training : reportData.trainingHistorySummary()) {
                writer.write(String.format("Training & Development,Course,%s%n",
                        training.replace(",", ";")));
            }

            // Benefits Enrollment
            for (String benefit : reportData.benefitsSummary()) {
                writer.write(String.format("Benefits Enrollment,Plan,%s%n",
                    benefit.replace(",", ";")));
            }

            DialogManager.showInfoDialog(
                "Export Successful", "CSV report saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            DialogManager.showErrorDialog(
                "Export Failed", "Could not save the CSV file: " + e.getMessage());
        }
    }

    private void saveAsText(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(reportTextArea.getText());
            DialogManager.showInfoDialog(
                "Export Successful", "Text report saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            DialogManager.showErrorDialog(
                "Export Failed", "Could not save the text file: " + e.getMessage());
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
