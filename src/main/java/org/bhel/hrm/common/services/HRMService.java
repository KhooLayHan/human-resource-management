package org.bhel.hrm.common.services;

import org.bhel.hrm.common.dtos.*;
import org.bhel.hrm.common.exceptions.HRMException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;

/**
 * The remote service interface for the BHEL Human Resource Management system.
 */
public interface HRMService extends Remote {
    /** A unique name to reference the service from the RMI registry. */
    String SERVICE_NAME = "HRMService";

    // --- 1. Authentication & User Management ---
    UserDTO authenticateUser(String username, String password) throws RemoteException, HRMException;
    void updateUserPassword(int userId, String oldPassword, String newPassword) throws RemoteException, HRMException;

    // --- 2. Employee Management ---
    void registerNewEmployee(NewEmployeeRegistrationDTO registrationData) throws RemoteException, HRMException;
    List<EmployeeDTO> getAllEmployees() throws RemoteException, HRMException;
    EmployeeDTO getEmployeeById(int employeeId) throws RemoteException, HRMException;
    EmployeeDTO getEmployeeByUserId(int userId) throws RemoteException, HRMException;
    void updateEmployeeProfile(EmployeeDTO employeeDTO) throws RemoteException, HRMException;
    void deleteEmployeeById(int employeeId) throws RemoteException, HRMException;
    EmployeeReportDTO generateEmployeeReport(int employeeId) throws RemoteException, HRMException;

    // --- 3. Leave Management ---
    void applyForLeave(LeaveApplicationDTO leaveApplicationDTO) throws RemoteException, SQLException, HRMException;
    List<LeaveApplicationDTO> getLeaveHistoryForEmployees(int employeeId) throws RemoteException, SQLException, HRMException;

    // --- 4. Training Management ---
    List<TrainingCourseDTO> getAllTrainingCourses() throws RemoteException, HRMException;
    void enrollInTraining(int employeeId, int courseId) throws RemoteException, HRMException;

    // --- 5. Recruitment Management (Primarily for HR Staff) ---

    /**
     * Retrieves a list of all open job positions.
     * @return A List of JobOpeningDTOs.
     * @throws RemoteException if a communication-related error occurs.
     */
    List<JobOpeningDTO> getAllJobOpenings() throws RemoteException, HRMException;

    /**
     * Retrieves all applicants for a specific job opening.
     * @param jobOpeningId The ID of the job opening.
     * @return A List of ApplicantDTOs for that job.
     * @throws RemoteException if a communication-related error occurs.
     */
    List<ApplicantDTO> getApplicantsForJob(int jobOpeningId) throws RemoteException, HRMException;

    // vvvvv ADDED METHODS BELOW vvvvv

    /**
     * Creates a new job opening in the system.
     *
     * @param jobOpeningDTO The data for the new job opening.
     * @throws RemoteException If a communication error occurs.
     * @throws HRMException If validation fails (e.g. missing title).
     */
    void createJobOpening(JobOpeningDTO jobOpeningDTO) throws RemoteException, HRMException;

    /**
     * Updates the status of a specific applicant (e.g., from NEW to INTERVIEWING).
     *
     * @param applicantId The ID of the applicant.
     * @param status The new status to apply.
     * @throws RemoteException If a communication error occurs.
     * @throws HRMException If the applicant is not found or the status transition is invalid.
     */
    void updateApplicantStatus(int applicantId, ApplicantDTO.ApplicantStatus status) throws RemoteException, HRMException;

    // ^^^^^ ADDED METHODS ABOVE ^^^^^

    // --- 6. Benefits Management ---
    List<BenefitPlanDTO> getAllBenefitPlans() throws RemoteException, HRMException;
    void enrollInBenefitPlan(int employeeId, int planId) throws RemoteException, HRMException;
}