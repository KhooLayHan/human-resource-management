package org.bhel.hrm.server.daos;

import org.bhel.hrm.server.domain.LeaveApplication;

import java.util.List;

/**
 * Data Access Object interface for LeaveApplication entities.
 */
public interface LeaveApplicationDAO extends DAO<LeaveApplication, Integer> {
    /**
     * Finds all leave applications submitted by a specific employee.
     *
     * @param employeeId The ID of the employee.
     * @return A {@link List} of leave applications for that employee, which may be empty.
     */
    List<LeaveApplication> findByEmployeeId(int employeeId);
}
