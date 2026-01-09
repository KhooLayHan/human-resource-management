package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.*;
import org.bhel.hrm.common.error.ErrorContext;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.services.HRMService;
import org.bhel.hrm.common.utils.GlobalExceptionHandler;
import org.bhel.hrm.server.config.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class HRMServer extends UnicastRemoteObject implements HRMService {
    private static final Logger logger = LoggerFactory.getLogger(HRMServer.class);

    private final transient DatabaseManager dbManager;

    private final transient EmployeeService employeeService;
    private final transient UserService userService;
    private final transient DashboardService dashboardService;
    private final transient TrainingService trainingService;

    private final transient GlobalExceptionHandler exceptionHandler;

    public HRMServer(
        DatabaseManager databaseManager,
        EmployeeService employeeService,
        UserService userService,
        TrainingService trainingService,
        DashboardService dashboardService,
        GlobalExceptionHandler exceptionHandler
    ) throws RemoteException {
        this.dbManager = databaseManager;
        this.employeeService = employeeService;
        this.userService = userService;
        this.trainingService = trainingService;
        this.dashboardService = dashboardService;
        this.exceptionHandler = exceptionHandler;
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
        ErrorContext context = ErrorContext.forOperation(
                "getAllEmployees");

        try {
            return employeeService.getAllEmployees();
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
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
    public List<TrainingEnrollmentDTO> getEmployeeTrainingEnrollments(int employeeId) throws RemoteException, HRMException {
        logger.debug("RMI Call: getEmployeeTrainingEnrollments() for Employee ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser(
                "getEmployeeTrainingEnrollments", String.valueOf(employeeId));

        try {
            return trainingService.getEnrollmentsByEmployee(employeeId);
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
    public void applyForLeave(LeaveApplicationDTO leaveApplicationDTO) throws RemoteException {
        throw new RemoteException("not yet implemented");
    }

    @Override
    public List<LeaveApplicationDTO> getLeaveHistoryForEmployees(int employeeId) throws RemoteException {
        return List.of();
    }

    @Override
    public List<TrainingCourseDTO> getAllTrainingCourses() throws RemoteException, HRMException {
        logger.debug("RMI Call: getAllTrainingCourses()");
        ErrorContext context = ErrorContext.forOperation(
            "getAllTrainingCourses");

        try {
            return trainingService.getAllCourses();
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public void saveTrainingCourse(TrainingCourseDTO courseDTO) throws RemoteException, HRMException {
        if (courseDTO == null)
            throw new IllegalArgumentException("courseDTO must not be null");

        logger.info("RMI Call: saveTrainingCourse for '{}'", courseDTO.title());
        ErrorContext context = ErrorContext.forOperation(
                "saveTrainingCourse");
        try {
            trainingService.saveCourse(courseDTO);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
           } finally {
                if (dbManager.isTransactionActive())
                    dbManager.rollbackTransaction();
        }
    }

    @Override
    public void deleteTrainingCourse(int courseId) throws RemoteException, HRMException {
        logger.info("RMI Call: deleteTrainingCourse ID {}", courseId);
        ErrorContext context = ErrorContext.forOperation(
                "deleteTrainingCourse");
        try {
            trainingService.deleteCourse(courseId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            } finally {
                if (dbManager.isTransactionActive())
                      dbManager.rollbackTransaction();
        }
    }

    @Override
    public void enrollInTraining(int employeeId, int courseId) throws RemoteException, HRMException {
        logger.info("RMI Call: enrollInTraining (Emp: {}, Course: {})", employeeId, courseId);
        ErrorContext context = ErrorContext.forOperation(
                "enrollInTraining");
        try {
            trainingService.enrollEmployee(employeeId, courseId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
    }

    @Override
    public void enrollMultipleEmployees(int courseId, List<Integer> employeeIds) throws RemoteException, HRMException {
        if (employeeIds == null)
            throw new IllegalArgumentException("employeeIds must not be null");
        logger.info("RMI Call: enrollMultipleEmployees (Course: {}, Count: {})", courseId, employeeIds.size());

        ErrorContext context = ErrorContext.forOperation("enrollMultipleEmployees");
        try {
            trainingService.enrollMultipleEmployees(courseId, employeeIds);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive())
                dbManager.rollbackTransaction();
        }
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
    public List<BenefitPlanDTO> getAllBenefitPlans() throws RemoteException {
        return List.of();
    }

    @Override
    public void enrollInBenefitPlan(int employeeId, int planId) throws RemoteException {
        throw new RemoteException("not yet implemented");
    }
}
