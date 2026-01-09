package org.bhel.hrm.common.dtos;

import java.io.Serializable;

public record TrainingCourseDTO(
    int id,
    String title,
    String description,
    int durationInHours,
    Department department
) implements Serializable {
    public enum Department { IT, HR, FINANCE, OPERATIONS, SALES }
}
