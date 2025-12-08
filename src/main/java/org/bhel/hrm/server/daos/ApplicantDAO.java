package org.bhel.hrm.server.daos;

import org.bhel.hrm.server.domain.Applicant;

import java.util.List;

/**
 * Data Access Object interface for Applicant entities.
 */
public interface ApplicantDAO extends DAO<Applicant, Integer> {
    /**
     * Finds all applicants who have applied for a specific job opening.
     *
     * @param jobOpeningId The ID of the job opening.
     * @return A {@link List} of applicants for that job.
     */
    List<Applicant> findByJobOpeningId(int jobOpeningId);
}
