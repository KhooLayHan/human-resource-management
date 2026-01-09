package org.bhel.hrm.server.daos;

import java.util.List;

public interface EmployeeBenefitDAO {

    void enroll(int employeeId, int planId);

    List<Integer> findPlansForEmployee(int employeeId);
}
