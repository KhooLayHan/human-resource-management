package org.bhel.hrm.common.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public record TrainingEnrollmentDTO(
    int id,
    int employeeId,
    int courseId,
    Status status,
    LocalDateTime enrollmentDate
) implements Serializable {
    public enum Status { ENROLLED, COMPLETED, CANCELLED, FAILED }
}
