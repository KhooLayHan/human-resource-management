package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.NewEmployeeRegistrationDTO;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.common.exceptions.AuthenticationException;
import org.bhel.hrm.common.exceptions.DuplicateUserException;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.exceptions.UserNotFoundException;
import org.bhel.hrm.payroll.PayrollServer;
import org.bhel.hrm.server.config.Configuration;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.UserDAO;
import org.bhel.hrm.server.domain.Employee;
import org.bhel.hrm.server.domain.User;
import org.bhel.hrm.server.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Service class containing business logic for user management,
 * including authentication and registration.
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final DatabaseManager dbManager;
    private final EmployeeDAO employeeDAO;
    private final UserDAO userDAO;
    private final PayrollSocketClient payrollClient;

    public UserService(
        DatabaseManager databaseManager,
        UserDAO userDAO,
        EmployeeDAO employeeDAO,
        PayrollSocketClient payrollClient
    ) {
        this.dbManager = databaseManager;
        this.userDAO = userDAO;
        this.employeeDAO = employeeDAO;
        this.payrollClient = payrollClient;
    }

    /**
     * Authenticates a user based on their username and password.
     *
     * @param username The username to authenticate; must not be null or empty
     * @param password The plain-text password to verify; must not be null
     * @return A {@link UserDTO} representing the authenticated user
     * @throws UserNotFoundException If no user exists with the given username
     * @throws AuthenticationException If the password does not match the stored hash
     */
    public UserDTO authenticate(String username, String password) throws AuthenticationException, UserNotFoundException {
        User user = userDAO.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username));

        boolean passwordMatches = PasswordService.checkPassword(password, user.getPasswordHash());
        if (!passwordMatches)
            throw new AuthenticationException(username);

        logger.info("User '{}' authenticated successfully.", username);
        return UserMapper.mapToDto(user);
    }

    /**
     * Registers a new employee in a single, atomic transaction.
     * This involves creating a User account and an associated Employee profile.
     *
     * @param registrationData DTO containing all necessary data; must not be null
     * @throws SQLException If a database access error occurs during the transaction
     * @throws HRMException If the username already exists or another business rule is violated
     */
    public void registerNewEmployee(NewEmployeeRegistrationDTO registrationData) throws SQLException, HRMException {
        final Employee newEmployee = new Employee();

        dbManager.executeInTransaction(() -> {
            if (userDAO.findByUsername(registrationData.username()).isPresent())
                throw new DuplicateUserException(registrationData.username());

            User newUser = new User(
                registrationData.username(),
                PasswordService.hashPassword(registrationData.initialPassword()),
                registrationData.role()
            );
            userDAO.save(newUser);

            newEmployee.setUserId(newUser.getId());
            newEmployee.setFirstName(registrationData.firstName());
            newEmployee.setLastName(registrationData.lastName());
            newEmployee.setIcPassport(registrationData.icPassport());

            employeeDAO.save(newEmployee);

            logger.info("Successfully registered the new Employee {} with user ID {}.",
                newEmployee.getFirstName(), newUser.getId());
        });

        // After the transaction is successful, notify the payroll system.
        // We run this in a background thread so it doesn't block the RMI response.
        new Thread(() -> payrollClient.notifyNewEmployee(newEmployee)).start();

        new Thread(() -> {
            // Implement new using ExecutorService code here...
        })
    }
}
