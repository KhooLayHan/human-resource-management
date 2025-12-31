package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.DashboardDTO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.UserDAO;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

        // Department Distribution
        Map<String, Integer> deptDistribution = new LinkedHashMap<>();
        deptDistribution.put("Engineering", 12);
        deptDistribution.put("Human Resources", 4);
        deptDistribution.put("Sales", 8);
        deptDistribution.put("Marketing", 5);

        // Leave Status Breakdown
        Map<String, Integer> leaveStatus = new LinkedHashMap<>();
        leaveStatus.put("Pending", 5);
        leaveStatus.put("Approved", 12);
        leaveStatus.put("Rejected", 2);

        // Recruitment Pipeline
        Map<String, Integer> recruitmentPipeline = new LinkedHashMap<>();
        recruitmentPipeline.put("New", 5);
        recruitmentPipeline.put("Screening", 12);
        recruitmentPipeline.put("Interviewing", 2);
        recruitmentPipeline.put("Offered", 2);
        recruitmentPipeline.put("Hired", 2);

        // Training Enrollment Trend
        Map<String, Integer> trainingEnrollment = new LinkedHashMap<>();
        trainingEnrollment.put("October", 8);
        trainingEnrollment.put("November", 12);
        trainingEnrollment.put("December", 15);

        return new DashboardDTO(
            "Welcome back to BHEL HRM!",
            annualLeave,
            medicalLeave,
            upcomingTraining,
            (int) totalEmployeeCount,
            pendingLeaves,
            openJobs,
            deptDistribution,
            leaveStatus,
            recruitmentPipeline,
            trainingEnrollment
        );
    }
}
