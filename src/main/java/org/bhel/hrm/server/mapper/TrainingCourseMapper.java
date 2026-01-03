package org.bhel.hrm.server.mapper;

import org.bhel.hrm.common.dtos.TrainingCourseDTO;
import org.bhel.hrm.server.domain.TrainingCourse;

import java.util.List;


public final class TrainingCourseMapper {
    private TrainingCourseMapper() {
        throw new UnsupportedOperationException("This class TrainingCourseMapper is a utility class; it should not be instantiated.");
    }

    public static TrainingCourseDTO mapToDto(TrainingCourse domain) {
        if (domain == null)
            return null;

        return new TrainingCourseDTO(
                domain.getId(),
                domain.getTitle(),
                domain.getDescription(),
                domain.getDurationInHours(),
                domain.getDepartment()
        );
    }

    public static TrainingCourse toDomain(TrainingCourseDTO dto) {
        if (dto == null)
            return null;

        return new TrainingCourse(
                dto.id(),
                dto.title(),
                dto.description(),
                dto.durationInHours(),
                dto.department()
        );

    }

    public static List<TrainingCourseDTO> toDtoList(List<TrainingCourse> all) {
        if (all == null) {
            return List.of();
        }
        return all.stream()
                .map(TrainingCourseMapper::mapToDto)
                .toList();

    }
}



