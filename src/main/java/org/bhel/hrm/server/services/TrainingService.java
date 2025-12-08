package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.TrainingCourseDTO;
import org.bhel.hrm.common.exceptions.EnrollmentException;
import org.bhel.hrm.common.exceptions.InvalidInputException;
import org.bhel.hrm.common.exceptions.ResourceNotFoundException;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.TrainingCourseDAO;
import org.bhel.hrm.server.daos.TrainingEnrollmentDAO;
import org.bhel.hrm.server.domain.TrainingCourse;
import org.bhel.hrm.server.domain.TrainingEnrollment;
import org.bhel.hrm.server.mapper.TrainingCourseMapper;

import java.util.List;

public class TrainingService {
    private final DatabaseManager dbManager;
    private final TrainingCourseDAO courseDAO;
    private final TrainingEnrollmentDAO enrollmentDAO;

    public TrainingService(DatabaseManager dbManager, TrainingCourseDAO courseDAO, TrainingEnrollmentDAO enrollmentDAO) {
        this.dbManager = dbManager;
        this.courseDAO = courseDAO;
        this.enrollmentDAO = enrollmentDAO;
    }

    public List<TrainingCourseDTO> getAllCourses() {
        return TrainingCourseMapper.toDtoList(courseDAO.findAll());
    }

    public void createOrUpdateCourse(TrainingCourseDTO dto) throws Exception {
        validateCourse(dto);

        dbManager.executeInTransaction(() -> {
            // Check if update or create based on ID
            TrainingCourse domain = TrainingCourseMapper.toDomain(dto);
            courseDAO.save(domain); // Save handles both insert and update in your DAO implementation
        });
    }

    public void enrollEmployee(int employeeId, int courseId) throws Exception {
        dbManager.executeInTransaction(() -> {
            // 1. Verify Course Exists
            courseDAO.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("TrainingCourse", courseId));

            // 2. Check if already enrolled (Optional, but good business logic)
            // Assuming your TrainingEnrollmentDAO has a custom finder for this
            boolean alreadyEnrolled = enrollmentDAO.findByEmployeeId(employeeId).stream()
                    .anyMatch(e -> e.getCourseId() == courseId);

            if (alreadyEnrolled) {
                throw new EnrollmentException(
                        String.valueOf(employeeId),
                        String.valueOf(courseId),
                        EnrollmentException.EnrollmentFailureReason.ALREADY_ENROLLED,
                        "Employee is already enrolled in this course."
                );
            }

            // 3. Create Enrollment
            TrainingEnrollment enrollment = new TrainingEnrollment();
            enrollment.setEmployeeId(employeeId);
            enrollment.setCourseId(courseId);
            enrollment.setStatus("ENROLLED");

            enrollmentDAO.save(enrollment);
        });
    }

    private void validateCourse(TrainingCourseDTO dto) throws InvalidInputException {
        if (dto.title() == null || dto.title().trim().isEmpty()) {
            throw new InvalidInputException("Course title is required", "title");
        }
        if (dto.durationInHours() <= 0) {
            throw new InvalidInputException("Duration must be greater than 0", "durationInHours");
        }
    }
}