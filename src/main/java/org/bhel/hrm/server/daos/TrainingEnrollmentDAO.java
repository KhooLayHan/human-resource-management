package org.bhel.hrm.server.daos;

import org.bhel.hrm.server.domain.TrainingEnrollment;

import java.util.List;

public interface TrainingEnrollmentDAO extends DAO<TrainingEnrollment, Integer> {
    List<TrainingEnrollment> findByEmployeeId(int employeeId);
    List<TrainingEnrollment> findByCourseId(int courseId);
}