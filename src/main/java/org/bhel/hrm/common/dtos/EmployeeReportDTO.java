package org.bhel.hrm.common.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A composite DTO that aggregates all information required for a yearly report.
 */
public record EmployeeReportDTO(
   LocalDateTime generationDate,
   EmployeeDTO employeeDetails,
   // Placeholders for now, types will be updated later
   List<String> leaveHistorySummary,
   List<String> trainingHistorySummary,
   List<String> benefitsSummary
) implements Serializable {}
