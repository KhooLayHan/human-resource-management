package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.exceptions.InvalidInputException;
import org.bhel.hrm.common.exceptions.ResourceNotFoundException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.domain.Employee;
import org.bhel.hrm.server.mapper.EmployeeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Service class containing business logic for employee profile management.
 */
public class EmployeeService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final DatabaseManager dbManager;
    private final EmployeeDAO employeeDAO;

    public EmployeeService(DatabaseManager databaseManager, EmployeeDAO employeeDAO) {
        this.dbManager = databaseManager;
        this.employeeDAO = employeeDAO;
    }

    /**
     * Retrieves a list of all employees.
     *
     * @return A list of EmployeeDTOs; never null
     */
    public List<EmployeeDTO> getAllEmployees() {
        List<Employee> employees = employeeDAO.findAll();

        logger.info("Successfully retrieved all employees.");
        return EmployeeMapper.mapToDtoList(employees);
    }

    /**
     * Retrieves a single employee by their ID.
     *
     * @param employeeId The ID of the employee to fetch; must be positive
     * @return The {@link EmployeeDTO} for the found employee; never null
     * @throws ResourceNotFoundException If no employee with the given ID is found
     */
    public EmployeeDTO getEmployeeById(int employeeId) throws ResourceNotFoundException {
        Employee employee = employeeDAO.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.EMPLOYEE_NOT_FOUND,
                        "Employee ID",
                        employeeId
                ));

        logger.info("Successfully retrieved employee by ID: {}.",
                employeeId);
        return EmployeeMapper.mapToDto(employee);
    }

    /**
     * Retrieves a single employee by their ID.
     *
     * @param userId The ID of the user to fetch; must be positive
     * @return The {@link EmployeeDTO} for the found employee; never null
     * @throws ResourceNotFoundException If no employee with the given ID is found
     */
    public EmployeeDTO getEmployeeByUserId(int userId) throws ResourceNotFoundException {
        Employee employee = employeeDAO.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.EMPLOYEE_NOT_FOUND,
                        "Employee with User ID",
                        userId
                ));

        logger.info("Successfully retrieved employee by the associated user ID: {}.",
                userId);
        return EmployeeMapper.mapToDto(employee);
    }

    /**
     * Updates an employee's profile information within a transaction.
     *
     * @param employeeDTO The DTO containing updated data; must not be null and must have a valid ID
     * @throws HRMException If validation fails, employee not found, or business rule violation occurs
     * @throws SQLException If a database transaction error occurs
     */
    public void updateEmployeeProfile(EmployeeDTO employeeDTO) throws SQLException, HRMException {
        validateEmployeeDTO(employeeDTO);

        dbManager.executeInTransaction(() -> {
            Employee existingEmployee = employeeDAO.findById(employeeDTO.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                    ErrorCode.EMPLOYEE_NOT_FOUND,
                    "Employee",
                    employeeDTO.id()
                ));

            existingEmployee.setFirstName(employeeDTO.firstName());
            existingEmployee.setLastName(employeeDTO.lastName());
            existingEmployee.setIcPassport(employeeDTO.icPassport());

            employeeDAO.save(existingEmployee);
        });

        logger.info("Successfully updated profile for employee ID: {}",
            employeeDTO.id());
    }

    /**
     * Validates the given EmployeeDTO.
     *
     * @param dto The EmployeeDTO to validate; must not be null
     * @throws InvalidInputException If any required field is missing or invalid
     */
    private void validateEmployeeDTO(EmployeeDTO dto) throws InvalidInputException {
        if (dto == null) {
            throw new InvalidInputException("EmployeeDTO must not be null.");
        }

        if (dto.id() <= 0) {
            throw new InvalidInputException("A valid employee ID is required and must be greater than 0.");
        }

        if (dto.firstName() == null || dto.firstName().trim().isEmpty()) {
            throw new InvalidInputException("First Name is a required field.");
        }

        if (dto.lastName() == null || dto.lastName().trim().isEmpty()) {
            throw new InvalidInputException("Last Name is a required field.");
        }

        if (dto.icPassport() == null || dto.icPassport().trim().isEmpty()) {
            throw new InvalidInputException("IC/Passport is a required field.");
        }
    }
}
