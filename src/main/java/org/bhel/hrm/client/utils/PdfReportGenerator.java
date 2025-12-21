package org.bhel.hrm.client.utils;

import org.bhel.hrm.common.dtos.EmployeeReportDTO;
import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A utility class to generate professional PDF reports using OpenPDF.
 */
public class PdfReportGenerator {
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void generateReport(EmployeeReportDTO report, File file) throws IOException {

        try (Document document = new Document(PageSize.A4)) {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // 1. Organization Header
            addHeader(document, report);

            // 2. Employee Profile Section
            addSectionTitle(document, "1. EMPLOYEE PROFILE");
            addProfileTable(document, report);

            // 3. Leave Summary Section
            addSectionTitle(document, "2. LEAVE SUMMARY");
            addSimpleListTable(document, "Leave Record", report.leaveHistorySummary());

            // 4. Training Section
            addSectionTitle(document, "3. TRAINING & DEVELOPMENT");
            addSimpleListTable(document, "Course List/Activity", report.trainingHistorySummary());

            // 5. Benefits Section
            addSectionTitle(document, "4. BENEFITS ENROLLMENT");
            addSimpleListTable(document, "Benefit Plan", report.benefitsSummary());

            // 6. Footer/End
            addFooter(document);
        } catch (DocumentException e) {
            throw new IOException("Error generating PDF documents", e);
        }
    }

    private static void addHeader(Document document, EmployeeReportDTO report) throws DocumentException {
        Paragraph title = new Paragraph("BHEL HUMAN RESOURCES — YEARLY REPORT", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph date = new Paragraph("Generated on: " + report.generationDate().format(DATE_FMT), NORMAL_FONT);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(20);
        document.add(date);

        document.add(new Paragraph(" "));
    }

    private static void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph subtitle = new Paragraph(title, HEADER_FONT);
        subtitle.setSpacingBefore(10);
        subtitle.setSpacingAfter(5);
        document.add(subtitle);

        LineSeparator line = new LineSeparator();
        line.setLineColor(Color.LIGHT_GRAY);
        document.add(new Paragraph(" "));
        document.add(line);
        document.add(new Paragraph(" "));
    }

    private static void addProfileTable(Document document, EmployeeReportDTO report) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{ 1.0f, 3.0f });

        // Row 1: Name
        addCell(table, "Full Name: ", true);
        addCell(
            table,
            report.employeeDetails().firstName() + " " + report.employeeDetails().lastName(),
            false
        );

        // Row 2: ID
        addCell(table, "Employee ID: ", true);
        addCell(
            table,
            String.valueOf(report.employeeDetails().id()),
            false
        );

        // Row 3: IC/Passport
        addCell(table, "IC/Passport: ", true);
        addCell(
            table,
            report.employeeDetails().icPassport(),
            false
        );

        document.add(table);
    }

    private static void addSimpleListTable(
        Document document,
        String headerTitle,
        List<String> items
    ) throws DocumentException {
        if (items == null || items.isEmpty()) {
            document.add(new Paragraph("No records found for this section.", NORMAL_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        // Header
        PdfPCell headerCell = new PdfPCell(new Phrase(headerTitle, HEADER_FONT));
        headerCell.setBackgroundColor(Color.LIGHT_GRAY);
        headerCell.setPadding(5);
        table.addCell(headerCell);

        // Data Rows
        for (String item : items) {
            PdfPCell cell = new PdfPCell(new Phrase(item, NORMAL_FONT));
            cell.setPadding(5);
            table.addCell(cell);
        }

        document.add(table);
    }

    private static void addCell(PdfPTable table, String text, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, isHeader ? HEADER_FONT : NORMAL_FONT));
        cell.setPadding(5);

        if (isHeader)
            cell.setBackgroundColor(new Color(240, 240, 240));

        table.addCell(cell);
    }

    private static void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
            "\n\nEnd of Report",
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }
}
