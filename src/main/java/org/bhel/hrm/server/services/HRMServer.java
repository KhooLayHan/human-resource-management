package org.bhel.hrm.server.services;


import org.bhel.hrm.common.dtos.*;
import org.bhel.hrm.common.error.ErrorContext;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.services.HRMService;
import org.bhel.hrm.common.utils.GlobalExceptionHandler;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.BenefitPlanDAO;
import org.bhel.hrm.server.daos.EmployeeBenefitDAO;
import org.bhel.hrm.server.daos.impls.BenefitPlanDAOImpl;
import org.bhel.hrm.server.daos.impls.EmployeeBenefitDAOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Optional;

public class HRMServer extends UnicastRemoteObject implements HRMService {
    private static final Logger logger = LoggerFactory.getLogger(HRMServer.class);

    private final transient DatabaseManager dbManager;
    private final transient EmployeeService employeeService;
    private final transient UserService userService;
    private final transient DashboardService dashboardService;
    private final transient GlobalExceptionHandler exceptionHandler;
    private final transient LeaveService leaveService;
    private final transient BenefitsService benefitsService;
    private final transient EmployeeBenefitDAO employeeBenefitDAO;
    private final transient BenefitPlanDAO benefitPlanDAO;

    public HRMServer(
            DatabaseManager databaseManager,
            EmployeeService employeeService,
            UserService userService,
            DashboardService dashboardService,
            LeaveService leaveService,
            BenefitsService benefitsService,
            GlobalExceptionHandler exceptionHandler
    ) throws RemoteException {

        this.dbManager = databaseManager;
        this.employeeService = employeeService;
        this.userService = userService;
        this.dashboardService = dashboardService;
        this.leaveService = leaveService;
        this.benefitsService = benefitsService;
        this.exceptionHandler = exceptionHandler;

        this.benefitPlanDAO = new BenefitPlanDAOImpl(databaseManager);
        this.employeeBenefitDAO = new EmployeeBenefitDAOImpl(databaseManager);
    }

    @Override
    public UserDTO authenticateUser(
            String username,
            String password
    ) throws RemoteException, HRMException {
        logger.info("Authentication attempt for user: {}.", username);
        ErrorContext context = ErrorContext.forUser(
                "authenticateUser", username);

        try {
            return userService.authenticate(username, password);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public void updateUserPassword(
            int userId,
            String oldPassword,
            String newPassword
    ) throws RemoteException, HRMException {
        logger.info("Attempting to update password for user ID: {}.", userId);
        ErrorContext context = ErrorContext.forUser(
                "updateUserPassword", String.valueOf(userId));

        try {
            userService.changePassword(userId, oldPassword, newPassword);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            // Ensure transaction is closed even if an unexpected exception occurs.
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public void registerNewEmployee(
            NewEmployeeRegistrationDTO registrationData
    ) throws RemoteException, HRMException {
        logger.info("Attempting to register new employee: {}.", registrationData.username());
        ErrorContext context = ErrorContext.forUser(
                "registerNewEmployee", registrationData.username());

        try {
            userService.registerNewEmployee(registrationData);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            // Ensure transaction is closed even if an unexpected exception occurs.
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public List<EmployeeDTO> getAllEmployees() throws RemoteException, HRMException {
        logger.debug("RMI Call: getAllEmployees() received.");

        try {
            return employeeService.getAllEmployees();
        } catch (Exception e) {
            exceptionHandler.handle(e, "getAllEmployees");
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public EmployeeDTO getEmployeeById(
            int employeeId
    ) throws RemoteException, HRMException {
        logger.debug("RMI Call: getEmployeeById() for ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser(
                "getEmployeeById", String.valueOf(employeeId));

        try {
            return employeeService.getEmployeeById(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public EmployeeDTO getEmployeeByUserId(
            int userId
    ) throws RemoteException, HRMException {
        logger.debug("RMI Call: getEmployeeByUserId() for User ID: {}", userId);
        ErrorContext context = ErrorContext.forUser(
                "getEmployeeByUserId", String.valueOf(userId));

        try {
            return employeeService.getEmployeeByUserId(userId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public void updateEmployeeProfile(
            EmployeeDTO employeeDTO
    ) throws RemoteException, HRMException {
        logger.info("RMI Call: updateEmployeeProfile() for employee ID: {}", employeeDTO.id());
        ErrorContext context = ErrorContext.forUser(
                "updateEmployeeProfile", String.valueOf(employeeDTO.id()));

        try {
            employeeService.updateEmployeeProfile(employeeDTO);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public void deleteEmployeeById(
            int employeeId
    ) throws RemoteException, HRMException {
        logger.info("RMI Call: deleteEmployeeById() for ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser(
                "deleteEmployeeById", String.valueOf(employeeId));

        try {
            employeeService.deleteEmployeeById(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public EmployeeReportDTO generateEmployeeReport(
            int employeeId
    ) throws RemoteException, HRMException {
        logger.info("RMI Call: generateEmployeeReport() for ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser(
                "generateEmployeeReport", String.valueOf(employeeId));

        try {
            return employeeService.generateYearlyReport(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public DashboardDTO generateDashboard(
            int userId
    ) throws RemoteException, HRMException {
        logger.info("RMI Call: generateDashboard() for ID: {}", userId);
        ErrorContext context = ErrorContext.forUser(
                "generateDashboard", String.valueOf(userId));

        try {
            return dashboardService.getDashboardData(userId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public void applyForLeave(LeaveApplicationDTO leaveApplicationDTO)
            throws RemoteException, HRMException {
        logger.info("RMI Call: applyForLeave() for employee ID: {}", leaveApplicationDTO.employeeId());
        ErrorContext context = ErrorContext.forUser(
                "applyForLeave", String.valueOf(leaveApplicationDTO.employeeId()));

        try {
            leaveService.applyForLeave(leaveApplicationDTO);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }


    @Override
    public List<LeaveApplicationDTO> getLeaveHistoryForEmployees(int employeeId)
            throws RemoteException, HRMException {
        logger.info("RMI Call: getLeaveHistoryForEmployees() for employee ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser("getLeaveHistoryForEmployees", String.valueOf(employeeId));

        try {
            return leaveService.getLeaveHistory(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public List<TrainingCourseDTO> getAllTrainingCourses() throws RemoteException {
        return List.of();
    }

    @Override
    public void enrollInTraining(int employeeId, int courseId) throws RemoteException {
        throw new RemoteException("not yet implemented");
    }

    @Override
    public List<JobOpeningDTO> getAllJobOpenings() throws RemoteException {
        return List.of();
    }

    @Override
    public List<ApplicantDTO> getApplicantsForJob(int jobOpeningId) throws RemoteException {
        return List.of();
    }

    @Override
    public List<BenefitPlanDTO> getAllBenefitPlans() throws RemoteException, HRMException {
        try {
            return benefitsService.getAllBenefitPlans();
        } catch (Exception e) {
            exceptionHandler.handle(e, "getAllBenefitPlans");
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public void enrollInBenefitPlan(int employeeId, int planId) throws RemoteException, HRMException {
        ErrorContext context = ErrorContext.forUser("enrollInBenefitPlan", employeeId + ":" + planId);
        try {
            benefitsService.enrollInBenefitPlan(employeeId, planId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }



    @Override
    public List<BenefitPlanDTO> getMyBenefitPlans(int employeeId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("getMyBenefitPlans", String.valueOf(employeeId));

        try {
            return employeeBenefitDAO.findPlansForEmployee(employeeId).stream()
                    // planId -> Optional<BenefitPlan>
                    .map(benefitPlanDAO::findById)
                    // Optional<BenefitPlan> -> BenefitPlan
                    .flatMap(Optional::stream)
                    // BenefitPlan -> DTO
                    .map(p -> new BenefitPlanDTO(
                            p.getId(),
                            p.getPlanName(),
                            p.getProvider(),
                            p.getDescription(),
                            p.getCostPerMonth()
                    ))
                    .toList();

        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public List<LeaveApplicationDTO> getPendingLeaveRequests() throws RemoteException, HRMException {
        logger.info("RMI Call: getPendingLeaveRequests()");
        ErrorContext context = ErrorContext.forUser("getPendingLeaveRequests", "HR");

        try {
            return leaveService.getPendingLeaves();
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }


    @Override
    public void decideLeave(int leaveId, boolean approve, int hrUserId, String decisionReason)
            throws RemoteException, HRMException {

        logger.info("RMI Call: decideLeave() leaveId={} approve={} hrUserId={}", leaveId, approve, hrUserId);
        ErrorContext context = ErrorContext.forUser("decideLeave", leaveId + ":" + hrUserId);

        try {
            leaveService.decideLeave(leaveId, approve, hrUserId, decisionReason);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }


}
