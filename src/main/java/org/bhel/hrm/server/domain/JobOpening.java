package org.bhel.hrm.server.domain;

import org.bhel.hrm.common.dtos.JobOpeningDTO.JobStatus;
import java.time.LocalDate;

public class JobOpening {
    private int id;
    private String title;
    private String description;
    private String department;
    private JobStatus status;
    private LocalDate postedDate; // New
    private LocalDate closingDate; // New

    public JobOpening() {}

    public JobOpening(int id, String title, String description, String department, JobStatus status, LocalDate postedDate, LocalDate closingDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.department = department;
        this.status = status;
        this.postedDate = postedDate;
        this.closingDate = closingDate;
    }

    // ... Getters and Setters for all fields ...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public LocalDate getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDate postedDate) { this.postedDate = postedDate; }
    public LocalDate getClosingDate() { return closingDate; }
    public void setClosingDate(LocalDate closingDate) { this.closingDate = closingDate; }
}