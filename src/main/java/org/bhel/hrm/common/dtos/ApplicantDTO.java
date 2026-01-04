package org.bhel.hrm.common.dtos;

import java.io.Serializable;

public record ApplicantDTO(
    int id,
    int jobOpeningId,
    String fullName,
    String email,
    String phone,
    String resumeUrl,
    ApplicantStatus status
) implements Serializable {
    public enum ApplicantStatus { NEW, SCREENING, INTERVIEWING, OFFERED, HIRED, REJECTED }
}
