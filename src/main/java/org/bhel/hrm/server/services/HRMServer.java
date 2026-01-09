package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.*;
import org.bhel.hrm.common.error.ErrorContext;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.services.HRMService;
import org.bhel.hrm.common.utils.GlobalExceptionHandler;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.BenefitPlanDAO;
import org.bhel.hrm.server.daos.EmployeeBenefitDAO;
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
    private final transient LeaveService leaveService;
    private final transient BenefitsService benefitsService;

    // Needed only because getMyBenefitPlans() uses DAOs directly:
    private final transient BenefitPlanDAO benefitPlanDAO;
    private final transient EmployeeBenefitDAO employeeBenefitDAO;

    private final transient GlobalExceptionHandler exceptionHandler;

    public HRMServer(
            DatabaseManager databaseManager,
            EmployeeService employeeService,
            UserService userService,
            DashboardService dashboardService,
            LeaveService leaveService,
            BenefitsService benefitsService,
            BenefitPlanDAO benefitPlanDAO,
            EmployeeBenefitDAO employeeBenefitDAO,
            GlobalExceptionHandler exceptionHandler
    ) throws RemoteException {
        this.dbManager = databaseManager;
        this.employeeService = employeeService;
        this.userService = userService;
        this.dashboardService = dashboardService;
        this.leaveService = leaveService;
        this.benefitsService = benefitsService;
        this.benefitPlanDAO = benefitPlanDAO;
        this.employeeBenefitDAO = employeeBenefitDAO;
        this.exceptionHandler = exceptionHandler;
    }


    @Override
    public UserDTO authenticateUser(String username, String password)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("authenticateUser", username);
        try {
            return userService.authenticate(username, password);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        }
    }

    @Override
    public void updateUserPassword(int userId, String oldPassword, String newPassword)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("updateUserPassword", String.valueOf(userId));
        try {
            userService.changePassword(userId, oldPassword, newPassword);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public void registerNewEmployee(NewEmployeeRegistrationDTO registrationData)
            throws RemoteException, HRMException {

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
    public List<EmployeeDTO> getAllEmployees()
            throws RemoteException, HRMException {

        try {
            return employeeService.getAllEmployees();
        } catch (Exception e) {
            exceptionHandler.handle(e, "getAllEmployees");
            throw new AssertionError("unreachable");
        }
    }

    @Override
    public EmployeeDTO getEmployeeById(int employeeId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("getEmployeeById", String.valueOf(employeeId));
        try {
            return employeeService.getEmployeeById(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        }
    }

    @Override
    public EmployeeDTO getEmployeeByUserId(int userId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("getEmployeeByUserId", String.valueOf(userId));
        try {
            return employeeService.getEmployeeByUserId(userId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        }
    }

    @Override
    public List<BenefitPlanDTO> getAllBenefitPlans()
            throws RemoteException, HRMException {

        try {
            return benefitsService.getAllBenefitPlans();
        } catch (Exception e) {
            exceptionHandler.handle(e, "getAllBenefitPlans");
            throw new AssertionError("unreachable");
        }
    }

    @Override
    public List<BenefitPlanDTO> getMyBenefitPlans(int employeeId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("getMyBenefitPlans", String.valueOf(employeeId));
        try {
            return employeeBenefitDAO.findPlansForEmployee(employeeId).stream()
                    .map(benefitPlanDAO::findById)
                    .flatMap(Optional::stream)
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
            throw new AssertionError("unreachable");
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public void enrollInBenefitPlan(int employeeId, int planId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("enrollInBenefitPlan", employeeId + ":" + planId);
        try {
            benefitsService.enrollInBenefitPlan(employeeId, planId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }


    @Override
    public void updateEmployeeProfile(EmployeeDTO employeeDTO)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("updateEmployeeProfile",
                employeeDTO == null ? "null" : String.valueOf(employeeDTO.id()));

        try {
            employeeService.updateEmployeeProfile(employeeDTO);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public void deleteEmployeeById(int employeeId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("deleteEmployeeById", String.valueOf(employeeId));
        try {
            employeeService.deleteEmployeeById(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public EmployeeReportDTO generateEmployeeReport(int employeeId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("generateEmployeeReport", String.valueOf(employeeId));
        try {
            return employeeService.generateYearlyReport(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        }
    }


    @Override
    public DashboardDTO generateDashboard(int userId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("generateDashboard", String.valueOf(userId));
        try {
            return dashboardService.getDashboardData(userId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        }
    }


    @Override
    public void applyForLeave(LeaveApplicationDTO leaveApplicationDTO)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("applyForLeave",
                leaveApplicationDTO == null ? "null" : String.valueOf(leaveApplicationDTO.employeeId()));

        try {
            leaveService.applyForLeave(leaveApplicationDTO);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }

    @Override
    public List<LeaveApplicationDTO> getLeaveHistoryForEmployees(int employeeId)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("getLeaveHistoryForEmployees", String.valueOf(employeeId));
        try {
            return leaveService.getLeaveHistory(employeeId);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        }
    }

    @Override
    public List<LeaveApplicationDTO> getPendingLeaveRequests()
            throws RemoteException, HRMException {

        try {
            return leaveService.getPendingLeaves();
        } catch (Exception e) {
            exceptionHandler.handle(e, "getPendingLeaveRequests");
            throw new AssertionError("unreachable");
        }
    }


    @Override
    public void decideLeave(int leaveId, boolean approve, int hrUserId, String decisionReason)
            throws RemoteException, HRMException {

        ErrorContext context = ErrorContext.forUser("decideLeave", leaveId + ":" + hrUserId);
        try {
            leaveService.decideLeave(leaveId, approve, hrUserId, decisionReason);
        } catch (Exception e) {
            exceptionHandler.handle(e, context);
            throw new AssertionError("unreachable");
        } finally {
            if (dbManager.isTransactionActive()) dbManager.rollbackTransaction();
        }
    }


    @Override
    public List<TrainingCourseDTO> getAllTrainingCourses() throws RemoteException {
        throw new UnsupportedOperationException("Training not implemented yet");
    }

    @Override
    public void enrollInTraining(int employeeId, int courseId) throws RemoteException {
        throw new UnsupportedOperationException("Training not implemented yet");
    }

    @Override
    public List<JobOpeningDTO> getAllJobOpenings() throws RemoteException {
        throw new UnsupportedOperationException("Recruitment not implemented yet");
    }

    @Override
    public List<ApplicantDTO> getApplicantsForJob(int jobOpeningId) throws RemoteException {
        throw new UnsupportedOperationException("Recruitment not implemented yet");
    }

}
