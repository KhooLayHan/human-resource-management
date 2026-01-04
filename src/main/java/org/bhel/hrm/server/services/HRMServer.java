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
    private final transient GlobalExceptionHandler exceptionHandler;

    // Core Services
    private final transient EmployeeService employeeService;
    private final transient UserService userService;

    // Feature Services
    private final transient RecruitmentService recruitmentService;
    private final transient LeaveService leaveService;
    private final transient BenefitService benefitService;
    private final transient TrainingService trainingService;

    public HRMServer(
            DatabaseManager databaseManager,
            EmployeeService employeeService,
            UserService userService,
            GlobalExceptionHandler exceptionHandler,
            RecruitmentService recruitmentService,
            LeaveService leaveService,
            BenefitService benefitService,
            TrainingService trainingService
    ) throws RemoteException {
        this.dbManager = databaseManager;
        this.employeeService = employeeService;
        this.userService = userService;
        this.exceptionHandler = exceptionHandler;
        this.recruitmentService = recruitmentService;
        this.leaveService = leaveService;
        this.benefitService = benefitService;
        this.trainingService = trainingService;
    }

    // --- 1. Authentication & User Management ---

    @Override
    public UserDTO authenticateUser(
            String username,
            String password
    ) throws RemoteException, HRMException {
        logger.info("Authentication attempt for user: {}.", username);
        ErrorContext context = ErrorContext.forUser("authenticateUser", username);

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
        ErrorContext context = ErrorContext.forUser("updateUserPassword", String.valueOf(userId));

        try {
            userService.changePassword(userId, oldPassword, newPassword);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    // --- 2. Employee Management ---

    @Override
    public void registerNewEmployee(
            NewEmployeeRegistrationDTO registrationData
    ) throws RemoteException, HRMException {
        logger.info("Attempting to register new employee: {}.", registrationData.username());
        ErrorContext context = ErrorContext.forUser("registerNewEmployee", registrationData.username());

        try {
            userService.registerNewEmployee(registrationData);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
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
    public EmployeeDTO getEmployeeById(int employeeId) throws RemoteException, HRMException {
        logger.debug("RMI Call: getEmployeeById() for ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser("getEmployeeById", String.valueOf(employeeId));

        try {
            return employeeService.getEmployeeById(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public EmployeeDTO getEmployeeByUserId(int userId) throws RemoteException, HRMException {
        logger.debug("RMI Call: getEmployeeByUserId() for User ID: {}", userId);
        ErrorContext context = ErrorContext.forUser("getEmployeeByUserId", String.valueOf(userId));

        try {
            return employeeService.getEmployeeByUserId(userId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public void updateEmployeeProfile(EmployeeDTO employeeDTO) throws RemoteException, HRMException {
        logger.info("RMI Call: updateEmployeeProfile() for employee ID: {}", employeeDTO.id());
        ErrorContext context = ErrorContext.forUser("updateEmployeeProfile", String.valueOf(employeeDTO.id()));

        try {
            employeeService.updateEmployeeProfile(employeeDTO);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public void deleteEmployeeById(int employeeId) throws RemoteException, HRMException {
        logger.info("RMI Call: deleteEmployeeById() for ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser("deleteEmployeeById", String.valueOf(employeeId));

        try {
            employeeService.deleteEmployeeById(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public EmployeeReportDTO generateEmployeeReport(int employeeId) throws RemoteException, HRMException {
        logger.info("RMI Call: generateEmployeeReport() for ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser("generateEmployeeReport", String.valueOf(employeeId));

        try {
            return employeeService.generateYearlyReport(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        }
    }

    // --- 3. Leave Management ---

    @Override
    public void applyForLeave(LeaveApplicationDTO leaveApplicationDTO) throws RemoteException, HRMException {
        logger.info("RMI Call: applyForLeave() for Employee ID: {}", leaveApplicationDTO.employeeId());
        ErrorContext context = ErrorContext.forUser("applyForLeave", String.valueOf(leaveApplicationDTO.employeeId()));

        try {
            leaveService.applyForLeave(leaveApplicationDTO);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public List<LeaveApplicationDTO> getLeaveHistoryForEmployees(int employeeId) throws RemoteException, HRMException {
        logger.debug("RMI Call: getLeaveHistoryForEmployees() for ID: {}", employeeId);
        ErrorContext context = ErrorContext.forUser("getLeaveHistoryForEmployees", String.valueOf(employeeId));

        try {
            return leaveService.getLeaveHistory(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable code");
        }
    }

    // --- 4. Training Management ---

    @Override
    public List<TrainingCourseDTO> getAllTrainingCourses() throws RemoteException, HRMException {
        logger.debug("RMI Call: getAllTrainingCourses() received.");
        try {
            return trainingService.getAllCourses();
        } catch (Exception e) {
            exceptionHandler.handle(e, "getAllTrainingCourses");
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public void enrollInTraining(int employeeId, int courseId) throws RemoteException, HRMException {
        logger.info("RMI Call: enrollInTraining() Emp: {}, Course: {}", employeeId, courseId);
        ErrorContext context = ErrorContext.forUser("enrollInTraining", String.valueOf(employeeId));

        try {
            trainingService.createCourse(null); // NOTE: Check if you have an 'enroll' method in TrainingService. If not, this needs update.
            // trainingService.enrollInTraining(employeeId, courseId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    // --- 5. Recruitment Management ---

    @Override
    public List<JobOpeningDTO> getAllJobOpenings() throws RemoteException, HRMException {
        logger.debug("RMI Call: getAllJobOpenings() received.");
        try {
            return recruitmentService.getAllJobOpenings();
        } catch (Exception e) {
            exceptionHandler.handle(e, "getAllJobOpenings");
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public List<ApplicantDTO> getApplicantsForJob(int jobOpeningId) throws RemoteException, HRMException {
        logger.debug("RMI Call: getApplicantsForJob() for Job ID: {}", jobOpeningId);
        try {
            return recruitmentService.getApplicantsForJob(jobOpeningId);
        } catch (Exception e) {
            exceptionHandler.handle(e, "getApplicantsForJob: " + jobOpeningId);
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public void createJobOpening(JobOpeningDTO jobOpeningDTO) throws RemoteException, HRMException {
        logger.info("RMI Call: createJobOpening() for title: '{}'", jobOpeningDTO.title());
        ErrorContext context = ErrorContext.forOperation("createJobOpening");
//                addData("title", jobOpeningDTO.title());

        try {
            recruitmentService.createJobOpening(jobOpeningDTO);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public void updateApplicantStatus(int applicantId, ApplicantDTO.ApplicantStatus status) throws RemoteException, HRMException {
        logger.info("RMI Call: updateApplicantStatus() for Applicant ID: {} to {}", applicantId, status);
        ErrorContext context = ErrorContext.forOperation("updateApplicantStatus");
//                .addData("applicantId", applicantId)
//                .addData("status", status);

        try {
            recruitmentService.updateApplicantStatus(applicantId, status);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    // --- 6. Benefits Management ---

    @Override
    public List<BenefitPlanDTO> getAllBenefitPlans() throws RemoteException, HRMException {
        logger.debug("RMI Call: getAllBenefitPlans() received.");
        try {
            return benefitService.getAllPlans();
        } catch (Exception e) {
            exceptionHandler.handle(e, "getAllBenefitPlans");
            throw new AssertionError("unreachable code");
        }
    }

    @Override
    public void enrollInBenefitPlan(int employeeId, int planId) throws RemoteException, HRMException {
        logger.info("RMI Call: enrollInBenefitPlan() Emp: {}, Plan: {}", employeeId, planId);
        ErrorContext context = ErrorContext.forUser("enrollInBenefitPlan", String.valueOf(employeeId));

        try {
            // Note: Ensure BenefitService has this method implemented.
            // benefitService.enrollInPlan(employeeId, planId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }
}