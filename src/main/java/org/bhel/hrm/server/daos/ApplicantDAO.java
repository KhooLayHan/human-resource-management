package org.bhel.hrm.server.daos;

import org.bhel.hrm.server.domain.Applicant;

import java.util.List;

/**
 * Data Access Object interface for Applicant entities.
 */
public interface ApplicantDAO extends DAO<Applicant, Integer> {
    /**
 * Finds applicants that applied for the specified job opening.
 *
 * @param jobOpeningId the identifier of the job opening
 * @return a {@link List} of {@link Applicant} instances who applied for the job opening
 */
    List<Applicant> findByJobOpeningId(int jobOpeningId);
}