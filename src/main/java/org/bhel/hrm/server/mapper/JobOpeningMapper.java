package org.bhel.hrm.server.mapper;

import org.bhel.hrm.common.dtos.BenefitPlanDTO;
import org.bhel.hrm.common.dtos.JobOpeningDTO;
import org.bhel.hrm.server.domain.BenefitPlan;
import org.bhel.hrm.server.domain.JobOpening;

public final class JobOpeningMapper {
    private JobOpeningMapper() {
        throw new UnsupportedOperationException("This class JobOpeningMapper is a utility class; it should not be instantiated.");
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
}
