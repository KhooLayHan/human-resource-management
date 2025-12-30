package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.DashboardDTO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.UserDAO;

import java.util.HashMap;
import java.util.Map;

public class DashboardService {
    private final UserDAO userDAO;
    private final EmployeeDAO employeeDAO;

    public DashboardService(UserDAO userDAO, EmployeeDAO employeeDAO) {
        this.userDAO = userDAO;
        this.employeeDAO = employeeDAO;
    }

    public DashboardDTO getDashboardData(int userId) {
        long totalEmployeeCount = employeeDAO.count();

        // Stubs, will be updated
        int annualLeave = 14;
        int medicalLeave = 15;
        int upcomingTraining = 1;
        int pendingLeaves = 2;
        int openJobs = 3;

        Map<String, Integer> deptDistribution = new HashMap<>();
        deptDistribution.put("Engineering", 12);
        deptDistribution.put("Human Resources", 4);
        deptDistribution.put("Sales", 8);
        deptDistribution.put("Marketing", 5);

        return new DashboardDTO(
            "Welcome back to BHEL HRM!",
            annualLeave,
            medicalLeave,
            upcomingTraining,
            (int) totalEmployeeCount,
            pendingLeaves,
            openJobs,
                deptDistribution
        );
    }
}
