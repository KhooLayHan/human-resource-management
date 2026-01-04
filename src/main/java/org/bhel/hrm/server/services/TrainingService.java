package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.TrainingCourseDTO;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.TrainingCourseDAO;
import org.bhel.hrm.server.domain.TrainingCourse;
import org.bhel.hrm.server.mapper.TrainingCourseMapper;

import java.util.List;

public class TrainingService {
    private final DatabaseManager dbManager;
    private final TrainingCourseDAO trainingDAO;

    public TrainingService(DatabaseManager dbManager, TrainingCourseDAO trainingDAO) {
        this.dbManager = dbManager;
        this.trainingDAO = trainingDAO;
    }

    public List<TrainingCourseDTO> getAllCourses() {
        List<TrainingCourse> courses = trainingDAO.findAll();
        return courses.stream().map(TrainingCourseMapper::mapToDto).toList();
    }

    public void createCourse(TrainingCourseDTO courseDTO) throws Exception {
        dbManager.executeInTransaction(() -> {
            TrainingCourse course = TrainingCourseMapper.toDomain(courseDTO);
            course.setId(0);
            trainingDAO.save(course);
        });
    }

    // enrollInTraining can be implemented here later
}