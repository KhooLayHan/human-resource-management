package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.ApplicantDTO;
import org.bhel.hrm.common.dtos.JobOpeningDTO;
import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.exceptions.InvalidInputException;
import org.bhel.hrm.common.exceptions.ResourceNotFoundException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.ApplicantDAO;
import org.bhel.hrm.server.daos.JobOpeningDAO;
import org.bhel.hrm.server.domain.Applicant;
import org.bhel.hrm.server.domain.JobOpening;
import org.bhel.hrm.server.mapper.RecruitmentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RecruitmentService {
    private static final Logger logger = LoggerFactory.getLogger(RecruitmentService.class);

    private final DatabaseManager dbManager;
    private final JobOpeningDAO jobOpeningDAO;
    private final ApplicantDAO applicantDAO;

    public RecruitmentService(DatabaseManager dbManager, JobOpeningDAO jobOpeningDAO, ApplicantDAO applicantDAO) {
        this.dbManager = dbManager;
        this.jobOpeningDAO = jobOpeningDAO;
        this.applicantDAO = applicantDAO;
    }

    public List<JobOpeningDTO> getAllJobOpenings() {
        // Simple read-only operation
        List<JobOpening> jobs = jobOpeningDAO.findAll();
        return jobs.stream().map(RecruitmentMapper::mapToDto).toList();
    }

    public List<ApplicantDTO> getApplicantsForJob(int jobOpeningId) {
        // Validate job exists first
        if (jobOpeningDAO.findById(jobOpeningId).isEmpty()) {
            // Depending on requirements, could return empty list or throw exception.
            // Returning empty list is safer for UI.
            return List.of();
        }

        List<Applicant> applicants = applicantDAO.findByJobOpeningId(jobOpeningId);
        return applicants.stream().map(RecruitmentMapper::mapToDto).toList();
    }

    public void createJobOpening(JobOpeningDTO jobDTO) throws Exception {
        validateJobOpening(jobDTO);

        dbManager.executeInTransaction(() -> {
            JobOpening job = RecruitmentMapper.mapToDomain(jobDTO);
            // Ensure ID is 0 for new creation
            job.setId(0);
            jobOpeningDAO.save(job);
        });
        logger.info("Created new job opening: {}", jobDTO.title());
    }

    public void updateApplicantStatus(int applicantId, ApplicantDTO.ApplicantStatus newStatus) throws Exception {
        dbManager.executeInTransaction(() -> {
            Applicant applicant = applicantDAO.findById(applicantId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPLICANT_NOT_FOUND, "Applicant ID"));

            applicant.setStatus(newStatus);
            applicantDAO.save(applicant);
        });
        logger.info("Updated applicant {} status to {}", applicantId, newStatus);
    }

    private void validateJobOpening(JobOpeningDTO dto) throws InvalidInputException {
        if (dto.title() == null || dto.title().isBlank()) {
            throw new InvalidInputException("Job Title is required");
        }
        if (dto.department() == null || dto.department().isBlank()) {
            throw new InvalidInputException("Department is required");
        }
    }
}