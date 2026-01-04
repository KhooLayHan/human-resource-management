package org.bhel.hrm.common.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public record JobOpeningDTO(
    int id,
    String title,
    String description,
    String department,
    JobStatus status,
    LocalDate postedDate,
    LocalDate closingDate
) implements Serializable {
    public enum JobStatus { OPEN, CLOSED, ON_HOLD }
}
