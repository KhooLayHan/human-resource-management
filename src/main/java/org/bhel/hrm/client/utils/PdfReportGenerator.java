package org.bhel.hrm.client.utils;

import org.bhel.hrm.common.dtos.EmployeeReportDTO;
import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(PdfReportGenerator.class);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font PAGE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PdfReportGenerator() {
        throw new UnsupportedOperationException("PdfReportGenerator is a utility class and should not be instantiated.");
    }

    public static void generateReport(EmployeeReportDTO report, File file) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(file);
            Document document = new Document(PageSize.A4)
        ) {
            PdfWriter writer = PdfWriter.getInstance(document, fos);

            // 1. Metadata & Page Numbers
            addMetadata(document, report);
            addPageNumbers(writer);

            document.open();

            // 2. Organization Header
            addHeader(document, report);

            // 3. Employee Profile Section
            addSectionTitle(document, "1. EMPLOYEE PROFILE");
            addProfileTable(document, report);

            // 4. Leave Summary Section
            addSectionTitle(document, "2. LEAVE SUMMARY");
            addSimpleListTable(document, "Leave Record", report.leaveHistorySummary());

            // 5. Training Section
            addSectionTitle(document, "3. TRAINING & DEVELOPMENT");
            addSimpleListTable(document, "Course List/Activity", report.trainingHistorySummary());

            // 6. Benefits Section
            addSectionTitle(document, "4. BENEFITS ENROLLMENT");
            addSimpleListTable(document, "Benefit Plan", report.benefitsSummary());

            // 7. Footer/End
            addFooter(document);
        } catch (DocumentException e) {
            throw new IOException("Error generating PDF document", e);
        }
    }

    private static void addMetadata(Document document, EmployeeReportDTO report) throws DocumentException {
        document.addTitle("Employee Yearly Report – " + report.employeeDetails().firstName());
        document.addSubject("BHEL HR Report");
        document.addAuthor("BHEL HR Management System");
        document.addCreator("BHEL HRM Application");
        document.addCreationDate();
    }

    private static void addPageNumbers(PdfWriter writer) {
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                try {
                    PdfPTable footer = new PdfPTable(1);
                    footer.setTotalWidth(document.getPageSize().getWidth() - 72);
                    footer.setLockedWidth(true);

                    PdfPCell cell = new PdfPCell(new Phrase(
                        "Page " + writer.getPageNumber(),
                        PAGE_FONT
                    ));
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    footer.addCell(cell);

                    footer.writeSelectedRows(0, -1, 36, 30, writer.getDirectContent());
                } catch (Exception e) {
                    logger.error("Stuff");
                }
            }
        });
    }

    private static void addHeader(Document document, EmployeeReportDTO report) throws DocumentException {
        Paragraph title = new Paragraph("BHEL HUMAN RESOURCES — YEARLY REPORT", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        String dateText = report.generationDate() != null
            ? "Generated on: " + report.generationDate().format(DATE_FMT)
            : "Generated on: [Date unavailable]";

        Paragraph date = new Paragraph(dateText, NORMAL_FONT);
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
        if (report.employeeDetails() == null) {
            document.add(new Paragraph("Employee details not available.", NORMAL_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setSpacingBefore(5);
        table.setWidths(new float[]{ 1.2f, 2.8f });

        // Adds a subtle border
//        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
//        table.getDefaultCell().setPadding(8);

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
//        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(Color.LIGHT_GRAY);

        if (isHeader)
            cell.setBackgroundColor(new Color(245, 245, 245));

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
