package org.bhel.hrm.server.domain;

import java.time.LocalDateTime;

public class TrainingEnrollment {
    private int id;
    private int employeeId;
    private int courseId;
    private LocalDateTime enrollmentDate;
    private String status;
    // Getters and Setters...

    public TrainingEnrollment(int id, int employeeId, int courseId, LocalDateTime enrollmentDate, String status) {
        this.id = id;
        this.employeeId = employeeId;
        this.courseId = courseId;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getCourseId() {
        return courseId;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public String getStatus() {
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

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}