package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.TrainingCourseDTO;
import org.bhel.hrm.common.dtos.TrainingEnrollmentDTO;
import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TrainingService {
    private static final Logger logger = LoggerFactory.getLogger(TrainingService.class);
    private final DatabaseManager dbManager;
    private final TrainingCourseDAO trainingCourseDAO;
    private final TrainingEnrollmentDAO trainingEnrollmentDAO;

    public TrainingService(DatabaseManager dbManager, TrainingCourseDAO courseDAO, TrainingEnrollmentDAO enrollmentDAO) {
        this.dbManager = dbManager;
        this.trainingCourseDAO = courseDAO;
        this.trainingEnrollmentDAO = enrollmentDAO;
    }

    public List<TrainingCourseDTO> getAllCourses() {
        List<TrainingCourse> courses = trainingCourseDAO.findAll();
        return courses.stream()
        .map(TrainingCourseMapper::mapToDto)
                .collect(Collectors.toList());
    }

    public TrainingCourseDTO getCourseById(int courseId) throws ResourceNotFoundException {
        TrainingCourse course = trainingCourseDAO.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRAINING_COURSE_NOT_FOUND, "TrainingCourse", courseId));
        return TrainingCourseMapper.mapToDto(course);
    }


    public List<TrainingEnrollmentDTO> getEnrollmentsByEmployee(int employeeId) {
        List<TrainingEnrollment> enrollments = trainingEnrollmentDAO.findByEmployeeId(employeeId);
        // Note: You'll need a TrainingEnrollmentMapper. Assuming one exists or mapping manually here:
        return enrollments.stream()
                .map(e -> new TrainingEnrollmentDTO(
                        e.getId(),
                        e.getEmployeeId(),
                        e.getCourseId(),
                        e.getStatus(),
                        e.getEnrollmentDate()
                        ))
                .collect(Collectors.toList());
    }

    public void createOrUpdateCourse(TrainingCourseDTO dto) throws Exception {
        validateCourse(dto);

        dbManager.executeInTransaction(() -> {
            // Check if update or create based on ID
            TrainingCourse domain = TrainingCourseMapper.toDomain(dto);
            trainingCourseDAO.save(domain); // Save handles both insert and update in your DAO implementation
        });
    }

    public void enrollEmployee(int employeeId, int courseId) throws SQLException, HRMException {
        dbManager.executeInTransaction(() -> {
            // 1. Validate Course Exists
            if (trainingCourseDAO.findById(courseId).isEmpty()) {
                throw new ResourceNotFoundException(ErrorCode.TRAINING_COURSE_NOT_FOUND, "TrainingCourse", courseId);
            }

            // 2. Check for Duplicate Enrollment
            boolean alreadyEnrolled = trainingEnrollmentDAO.findByEmployeeId(employeeId).stream()
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
            TrainingEnrollment enrollment = new TrainingEnrollment(
                employeeId,
                courseId,
                LocalDateTime.now(),
                TrainingEnrollmentDTO.Status.ENROLLED
            );
//            enrollment.setEmployeeId(employeeId);
//            enrollment.setCourseId(courseId);
//            enrollment.setEnrollmentDate(LocalDateTime.now());
//            enrollment.setStatus(TrainingEnrollmentDTO.Status.ENROLLED);

            trainingEnrollmentDAO.save(enrollment);
            logger.info("Employee {} successfully enrolled in course {}", employeeId, courseId);
        });
    }


    // 3. Create Enrollment
//            TrainingEnrollment enrollment = new TrainingEnrollment(
//                employeeId,
//                    courseId,
//                    TrainingEnrollmentDTO.Status.ENROLLED
//            );
////            enrollment.setEmployeeId(employeeId);
////            enrollment.setCourseId(courseId);
////            enrollment.setStatus("ENROLLED");
//
//            enrollmentDAO.save(enrollment);
//        });
//    }

public void updateEnrollmentStatus(int enrollmentId, TrainingEnrollmentDTO.Status newStatus) throws SQLException, HRMException {
    dbManager.executeInTransaction(() -> {
        TrainingEnrollment enrollment = trainingEnrollmentDAO.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRAINING_ENROLLMENT_NOT_FOUND, "TrainingEnrollment", enrollmentId));

        enrollment.setStatus(newStatus);
        trainingEnrollmentDAO.save(enrollment);
        logger.info("Updated enrollment {} status to {}", enrollmentId, newStatus);
    });
}

    private void validateCourse(TrainingCourseDTO dto) throws InvalidInputException {
        ErrorContext title = ErrorContext.forUser(
                "TrainingCourse", String.valueOf(dto.id())
        );
        ErrorContext durationInHours = ErrorContext.forUser(
                "TrainingCourse", String.valueOf(dto.id())
        );

        if (dto.title() == null || dto.title().trim().isEmpty()) {
            throw new InvalidInputException("Course title is required", title);
        }
        if (dto.durationInHours() <= 0) {
            throw new InvalidInputException("Duration must be greater than 0", durationInHours);
        }
    }

public void saveCourse(TrainingCourseDTO courseDTO) throws SQLException, HRMException {
    validateCourse(courseDTO);

    dbManager.executeInTransaction(() -> {
        TrainingCourse domain = TrainingCourseMapper.toDomain(courseDTO);
        trainingCourseDAO.save(domain);
        logger.info("Training course '{}' saved successfully.", courseDTO.title());
    });
}
public void deleteCourse(int courseId) throws SQLException, HRMException {
    dbManager.executeInTransaction(() -> {
        // Optional: Check if anyone is enrolled?
        // For now, we rely on DB Cascade Delete or allow it.
        if (trainingCourseDAO.findById(courseId).isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.TRAINING_COURSE_NOT_FOUND, "TrainingCourse", courseId);
        }
        trainingCourseDAO.deleteById(courseId);
        logger.info("Training course ID {} deleted.", courseId);
    });
}
}