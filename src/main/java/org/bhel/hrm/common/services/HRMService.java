package org.bhel.hrm.common.services;

import org.bhel.hrm.common.dtos.*;
import org.bhel.hrm.common.exceptions.AuthenticationException;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.exceptions.UserNotFoundException;
import org.bhel.hrm.server.domain.User;
import org.bhel.hrm.server.services.PasswordService;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;

/**
 * The remote service interface for the BHEL Human Resource Management system.
 * This interface defines the contract between the client and the server, specifying
 * all available remote operations. All data transfers are handled via DTOs.
 */
public interface HRMService extends Remote {
    // --- 1. Authentication & User Management ---

    /**
     * Authenticates a user with their credentials.
     *
     * @param username The user's username.
     * @param password The user's raw password.
     * @return A {@link UserDTO} if authentication is successful
     * @throws RemoteException If a communication-related error occurs.
     * @throws HRMException If an authentication-related business rule is violated
     */
    UserDTO authenticateUser(String username, String password) throws RemoteException, HRMException;

    void updateUserPassword(int userId, String oldPassword, String newPassword) throws RemoteException, HRMException;

    // --- 2. Employee Management (Primarily for HR Staff) ---

    /**
     * Registers a new employee, creating their user account and profile in one atomic operation.
     *
     * @param registrationData A {@link NewEmployeeRegistrationDTO} containing all required details for the new user and employee, not null
     * @throws RemoteException If a communication error occurs
     * @throws HRMException If registration fails (e.g., username already exists) or business rules are violated
     */
    void registerNewEmployee(NewEmployeeRegistrationDTO registrationData) throws RemoteException, HRMException;

    /**
     * Retrieves a list of all employees in the system.
     *
     * @return A list of {@link EmployeeDTO}'s; empty if no employees exist, not null
     * @throws RemoteException If a communication-related error occurs
     * @throws HRMException If a business logic error occurs
     */
    List<EmployeeDTO> getAllEmployees() throws RemoteException, HRMException;

    /**
     * Retrieves the full profile details for a single employee.
     *
     * @param employeeId The ID of the employee to fetch; must be positive
     * @return An {@link EmployeeDTO} containing the employee's details, not null
     * @throws RemoteException If a communication error occurs
     * @throws HRMException If the employee is not found or another business rule is violated
     */
    EmployeeDTO getEmployeeById(int employeeId) throws RemoteException, HRMException;

    /**
     * Updates the profile information for an existing employee.
     *
     * @param employeeDTO The DTO containing updated information; must include a valid ID, not null
     * @throws RemoteException If the update fails or a communication error occurs
     * @throws HRMException If the employee ID is invalid or data validation fails
     */
    void updateEmployeeProfile(EmployeeDTO employeeDTO) throws RemoteException, HRMException;

    /**
     * Deletes an employee and their associated user account from the system.
     * <p>
     *
     * <strong>Warning:</strong> This operation is irreversible. All employee data
     * will be permanently removed from the system.
     *
     * @param employeeId The unique identifier of the employee to delete
     * @throws RemoteException           If a communication error occurs during the remote method call
     * @throws HRMException              If a business rule violation occurs or the employee is not found
     */
    void deleteEmployeeById(int employeeId) throws RemoteException, HRMException;

    /**
     * Generates a comprehensive yearly report for the specified employee.
     *
     * @param employeeId The unique identifier of the employee for whom to generate the report
     * @return An {@link EmployeeReportDTO} containing the complete yearly report with generation timestamp
     * @throws RemoteException If a communication error occurs during the remote method call
     * @throws HRMException    If a business rule violation occurs or the employee is not found
     */
    EmployeeReportDTO generateEmployeeReport(int employeeId) throws RemoteException, HRMException;

    // --- 3. Leave Management (For Employees and HR) ---

    /**
     * Submits a new leave application for an employee.
     *
     * @param leaveApplicationDTO The DTO containing the details of the leave request.
     * @throws RemoteException if the application is invalid or a communication error occurs.
     */
    void applyForLeave(LeaveApplicationDTO leaveApplicationDTO) throws RemoteException;

    /**
     * Retrieves the leave history for a specific employee.
     *
     * @param employeeId The ID of the employee whose leave history is being requested.
     * @return A List of the employee's LeaveApplicationDTOs.
     * @throws RemoteException if a communication-related error occurs.
     */
    List<LeaveApplicationDTO> getLeaveHistoryForEmployees(int employeeId) throws RemoteException;

    // --- 4. Training Management (For Employees and HR) ---

    /**
     * Retrieves a list of all available training courses.
     *
     * @return A List of TrainingCourseDTOs.
     * @throws RemoteException if a communication-related error occurs.
     */
    List<TrainingCourseDTO> getAllTrainingCourses() throws RemoteException;

    /**
     * Enrolls an employee in a specific training course.
     *
     * @param employeeId The ID of the employee to enroll.
     * @param courseId The ID of the course to enroll in.
     * @throws RemoteException if enrollment fails (e.g., course is full) or a communication error occurs.
     */
    void enrollInTraining(int employeeId, int courseId) throws RemoteException;

    // --- 5. Recruitment Management (Primarily for HR Staff) ---

    /**
     * Retrieves a list of all open job positions.
     *
     * @return A List of JobOpeningDTOs.
     * @throws RemoteException if a communication-related error occurs.
     */
    List<JobOpeningDTO> getAllJobOpenings() throws RemoteException;

    /**
     * Retrieves all applicants for a specific job opening.
     *
     * @param jobOpeningId The ID of the job opening.
     * @return A List of ApplicantDTOs for that job.
     * @throws RemoteException if a communication-related error occurs.
     */
    List<ApplicantDTO> getApplicantsForJob(int jobOpeningId) throws RemoteException;

    // --- 6. Benefits Management (For Employees and HR) ---
    /**
     * Retrieves a list of all benefit plans offered by the company.
     *
     * @return A List of BenefitPlanDTOs.
     * @throws RemoteException if a communication-related error occurs.
     */
    List<BenefitPlanDTO> getAllBenefitPlans() throws RemoteException;

    /**
     * Enrolls an employee in a specific benefit plan.
     *
     * @param employeeId The ID of the employee to enroll.
     * @param planId The ID of the benefit plan to enroll in.
     * @throws RemoteException if enrollment fails or a communication error occurs.
     */
    void enrollInBenefitPlan(int employeeId, int planId) throws RemoteException;
}
