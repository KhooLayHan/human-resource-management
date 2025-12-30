package org.bhel.hrm.server.domain;

import org.bhel.hrm.common.dtos.TrainingEnrollmentDTO;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TrainingEnrollment {
    private int id;
    private int employeeId;
    private int courseId;
    private LocalDate enrollmentDate;
    private TrainingEnrollmentDTO.Status status;
    // Getters and Setters...

    public TrainingEnrollment(int id, int employeeId, int courseId, LocalDate enrollmentDate, TrainingEnrollmentDTO.Status status) {
        this.id = id;
        this.employeeId = employeeId;
        this.courseId = courseId;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
    }

    public TrainingEnrollment(int employeeId, int courseId, TrainingEnrollmentDTO.Status status) {
        this.employeeId = employeeId;
        this.courseId = courseId;
        this.status = status;
    }

//    public TrainingEnrollment(int employeeId, int courseId, TrainingEnrollmentDTO.Status status) {
//        this.employeeId = employeeId;
//        this.courseId = courseId;
//        this.status = status;
//    }

    public int getId() {
        return id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getCourseId() {
        return courseId;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public TrainingEnrollmentDTO.Status getStatus() {
        return status;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public void setStatus(TrainingEnrollmentDTO.Status status) {
        this.status = status;
    }
}