package org.bhel.hrm.server.daos;

import org.bhel.hrm.common.dtos.JobOpeningDTO;
import org.bhel.hrm.server.domain.JobOpening;

import java.util.List;

/**
 * Data Access Object interface for JobOpening entities.
 */
public interface JobOpeningDAO extends DAO<JobOpening, Integer> {
    /**
     * Finds all job openings that match a specific status (e.g., OPEN).
     *
     * @param status The status to filter by.
     * @return A {@link List} of job openings with the specified status.
     */
    List<JobOpening> findAllByStatus(JobOpeningDTO.JobStatus status);
}
