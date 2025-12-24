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
 * A utility class for generating professional PDF reports using the OpenPDF library.
 * <p>
 * This class provides functionality to convert {@link EmployeeReportDTO} objects into
 * well-formatted PDF documents with proper styling, sections, and metadata.
 * </p>
 *
 * The generated PDF includes:
 * <ul>
 *   <li>Document metadata (title, author, creation date)</li>
 *   <li>Employee profile information</li>
 *   <li>Leave summary records</li>
 *   <li>Training and development history</li>
 *   <li>Benefits enrollment details</li>
 * </ul>
 *
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

    /**
     * Generates a complete PDF report for the given employee data and saves it to the specified file.
     *
     * @param report The employee report data to be rendered into PDF format, must not be null
     * @param file   The destination file where the PDF will be saved, must not be null
     * @throws IOException       If an I/O error occurs during file writing
     * @throws DocumentException If an error occurs during PDF document generation (wrapped in IOException)
     */
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

    /**
     * Adds PDF metadata to the document, including title, subject, author, and creation date.
     *
     * @param document The PDF document to add metadata to
     * @param report   The employee report containing the employee name
     * @throws DocumentException If an error occurs while adding metadata
     */
    private static void addMetadata(Document document, EmployeeReportDTO report) throws DocumentException {
        String employeeName =
            report.employeeDetails() != null && report.employeeDetails().firstName() != null
                ? report.employeeDetails().firstName()
                : "Unknown Employee";

        document.addTitle("Employee Yearly Report – " + employeeName);
        document.addSubject("BHEL HR Report");
        document.addAuthor("BHEL HR Management System");
        document.addCreator("BHEL HRM Application");
        document.addCreationDate();
    }

    /**
     * Configures automatic page numbering in the document footer.
     *
     * @param writer The PDF writer instance responsible for rendering the document
     */
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
                    logger.error("Failed to render page number on page {}",
                        writer.getPageNumber(), e);
                }
            }
        });
    }

    /**
     * Adds the document header containing the organization name and report generation date.
     *
     * @param document The PDF document to add the header to
     * @param report   The report containing the generation date
     * @throws DocumentException If an error occurs while adding the header
     */
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

    /**
     * Adds a section title with an underline separator to the document.
     *
     * @param document The PDF document to add the section title to
     * @param title    The text of the section title
     * @throws DocumentException If an error occurs while adding the title
     */
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

    /**
     * Adds a formatted table displaying the employee's profile information.
     *
     * @param document The PDF document to add the profile table to
     * @param report   The report containing employee details
     * @throws DocumentException If an error occurs while adding the table
     */
    private static void addProfileTable(Document document, EmployeeReportDTO report) throws DocumentException {
        if (report.employeeDetails() == null) {
            document.add(new Paragraph("Employee details not available.", NORMAL_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{ 1.2f, 2.8f });

        // Row 1: Name
        String firstName = report.employeeDetails().firstName() != null
            ? report.employeeDetails().firstName().trim()
            : "";
        String lastName = report.employeeDetails().lastName() != null
            ? report.employeeDetails().lastName().trim()
            : "";

        addCell(table, "Full Name: ", true);
        addCell(table, firstName + " " + lastName, false);

        // Row 2: ID
        addCell(table, "Employee ID: ", true);
        addCell(
            table,
            String.valueOf(report.employeeDetails().id()),
            false
        );

        // Row 3: IC/Passport
        String icPassport = report.employeeDetails().icPassport() != null
            ? report.employeeDetails().icPassport()
            : "N/A";

        addCell(table, "IC/Passport: ", true);
        addCell(table, icPassport, false);

        document.add(table);
    }

    /**
     * Adds a simple single-column table displaying a list of items.
     *
     * @param document    The PDF document to add the table to
     * @param headerTitle The title to display in the table header
     * @param items       The list of items to display; may be null or empty
     * @throws DocumentException If an error occurs while adding the table
     */
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
            String safeItem = item != null ? item : "[No Data]";

            PdfPCell cell = new PdfPCell(new Phrase(safeItem, NORMAL_FONT));
            cell.setPadding(5);
            table.addCell(cell);
        }

        document.add(table);
    }

    /**
     * Adds a single cell to a PDF table with appropriate styling.
     *
     * @param table    The table to add the cell to
     * @param text     The text content of the cell; null values are converted to empty strings
     * @param isHeader {@code true} if this is a header cell (applies special styling), false otherwise
     */
    private static void addCell(PdfPTable table, String text, boolean isHeader) {
        String safeText = text != null ? text : "";

        PdfPCell cell = new PdfPCell(new Phrase(safeText, isHeader ? HEADER_FONT : NORMAL_FONT));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);

        if (isHeader)
            cell.setBackgroundColor(new Color(245, 245, 245));

        table.addCell(cell);
    }

    /**
     * Adds a footer section to the document indicating the end of the report.
     *
     * @param document The PDF document to add the footer to
     * @throws DocumentException If an error occurs while adding the footer
     */
    private static void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
            "\n\nEnd of Report",
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }
}
