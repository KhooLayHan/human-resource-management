package org.bhel.hrm.server.mapper;

import org.bhel.hrm.common.dtos.ApplicantDTO;
import org.bhel.hrm.common.dtos.JobOpeningDTO;
import org.bhel.hrm.server.domain.Applicant;
import org.bhel.hrm.server.domain.JobOpening;

public final class RecruitmentMapper {
    private RecruitmentMapper() {
        throw new UnsupportedOperationException("This class RecruitmentMapper is a utility class; it should not be instantiated.");
    }

    public static JobOpeningDTO mapToDto(JobOpening domain) {
        if (domain == null)
            return null;

        return new JobOpeningDTO(
            domain.getId(),
            domain.getTitle(),
            domain.getDescription(),
            domain.getDepartment(),
            domain.getStatus(),
            domain.getPostedDate(),
            domain.getClosingDate()
        );
    }

    public static JobOpening mapToDomain(JobOpeningDTO dto) {
        if (dto == null)
            return null;

        return new JobOpening(
            dto.id(),
            dto.title(),
            dto.description(),
            dto.department(),
            dto.status(),
            dto.postedDate(),
            dto.closingDate()
        );
    }

    public static ApplicantDTO mapToDto(Applicant domain) {
        if (domain == null)
            return null;

        return new ApplicantDTO(
                domain.getId(),
                domain.getJobOpeningId(),
                domain.getFullName(),
                domain.getEmail(),
                domain.getPhone(),
                domain.getResumeUrl(),
                domain.getStatus()
        );
    }

    public static Applicant mapToDomain(ApplicantDTO dto) {
        if (dto == null)
            return null;

        return new Applicant(
                dto.id(),
                dto.jobOpeningId(),
                dto.fullName(),
                dto.email(),
                dto.phone(),
                dto.resumeUrl(),
                dto.status()
        );
    }
}
