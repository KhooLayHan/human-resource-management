package org.bhel.hrm.server.daos;

import org.bhel.hrm.server.domain.Employee;

import java.util.Optional;

/**
 * Data Access Object interface for Employee entities.
 * Inherits all standard CRUD operations from the generic DAO interface.
 */
public interface EmployeeDAO extends DAO<Employee, Integer> {
    /**
     * Finds the employee record associated with a specific system user.
     *
     * @param userId The ID of the User account.
     * @return An Optional containing the Employee if found.
     */


    Optional<Employee> findByUserId(int userId);
}
