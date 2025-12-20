package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.UserDAO;
import org.bhel.hrm.server.domain.Employee;
import org.bhel.hrm.server.domain.User;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("EmployeeDAO implementation tests")
class EmployeeDAOImplTest {
    @Container
    private static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.0");

    private static UserDAO userDAO;
    private static EmployeeDAO employeeDAO;

    @BeforeAll
    static void setup() {
        Configuration mockConfig = mock(Configuration.class);

        when(mockConfig.getDbUrl()).thenReturn(mysql.getJdbcUrl());
        when(mockConfig.getDbUser()).thenReturn(mysql.getUsername());
        when(mockConfig.getDbPassword()).thenReturn(mysql.getPassword());

        DatabaseManager dbManager = new DatabaseManager(mockConfig);
        userDAO = new UserDAOImpl(dbManager);
        employeeDAO = new EmployeeDAOImpl(dbManager);
    }

    @AfterEach
    void tearDown() {
        employeeDAO.findAll().forEach(employee -> {
            employeeDAO.deleteById(employee.getId());
        });
        userDAO.findAll().forEach(user -> {
            userDAO.deleteById(user.getId());
        });
    }

    @Nested
    @DisplayName("save and find operations")
    class SaveAndFindTests {
        @Test
        @DisplayName("save() should save a new employee and correctly set the generated ID")
        void save_shouldInsertNewEmployeeAndSetId() {
            // Given: A prerequisite User is created and saved
            User testUser = createAndSaveTestUser("test_user");

            // Given: A new employee object linked to the user
            Employee newEmployee = new Employee(0, testUser.getId(), "Jane", "Doe", "S1234567A");

            // When: The employee is saved
            employeeDAO.save(newEmployee);

            // Then: The employee object should have a non-zero ID assigned to it
            assertThat(newEmployee.getId()).isGreaterThan(0);
        }

        @Test
        @DisplayName("findById() should return the correct employee when they exist")
        void findById_shouldReturnCorrectEmployee_whenEmployeeExists() {
            // Given: An employee is saved to the database
            User testUser = createAndSaveTestUser("find_me");
            Employee savedEmployee = createAndSaveTestEmployee(testUser,"Find", "Me", "F111");

            // When: We find that employee by their generated ID
            Optional<Employee> foundEmployeeOpt = employeeDAO.findById(savedEmployee.getId());

            // Then: The employee should be found and all properties matches
            assertThat(foundEmployeeOpt).hasValueSatisfying(employee ->
                assertThat(employee)
                    .usingRecursiveComparison()
                    .isEqualTo(savedEmployee)
            );
        }

        @Test
        @DisplayName("findById() should return an empty Optional when the employee does not exist")
        void findById_shouldReturnEmpty_whenEmployeeDoesNotExist() {
            // Given: No employee exists with the given ID

            // When: We try to find an employee with a non-existent ID
            Optional<Employee> foundEmployeeOpt = employeeDAO.findById(99999);

            // Then: The result should be an empty Optional
            assertThat(foundEmployeeOpt).isEmpty();
        }
    }

    @Nested
    @DisplayName("update and delete operations")
    class UpdateAndDeleteTests {
        @Test
        @DisplayName("save() should update an existing employee's details")
        void save_shouldUpdateExistingEmployee() {
            // Given: An existing employee is saved
            User testUser = createAndSaveTestUser("original");
            Employee employee = createAndSaveTestEmployee(testUser, "Original", "Name", "0123");
            int originalId = employee.getId();

            // When: We change the employee's properties and call save again
            employee.setFirstName("Updated");
            employee.setIcPassport("U456");
            employeeDAO.save(employee);

            // Then: The original ID should be unchanged, and the retrieved record should have the new data
            Optional<Employee> updatedEmployeeOpt = employeeDAO.findById(originalId);
            assertThat(employee.getId()).isEqualTo(originalId);
            assertThat(updatedEmployeeOpt).hasValueSatisfying(e -> {
                assertThat(e.getFirstName()).isEqualTo("Updated");
                assertThat(e.getIcPassport()).isEqualTo("U456");
            });
        }

        @Test
        @DisplayName("deleteById() should permanently remove an employee")
        void deleteById_shouldRemoveEmployee() {
            // Given: An employee is saved
            User testUser = createAndSaveTestUser("delete_me");
            Employee employee = createAndSaveTestEmployee(testUser, "Delete", "Me", "D789");
            assertThat(employeeDAO.findById(employee.getId())).isPresent(); // Verify it exists first

            // When: We delete the employee by its ID
            employeeDAO.deleteById(employee.getId());

            // Then: Finding them again should return an empty Optional
            assertThat(employeeDAO.findById(employee.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("query operations")
    class QueryTests {
        @Test
        @DisplayName("findAll() should return all employees sorted by last name, then first name")
        void findAll_shouldReturnAllEmployeesInOrder() {
            // Given: Multiple employees are saved in a specific order
            User user1 = createAndSaveTestUser("user_c");
            User user2 = createAndSaveTestUser("user_a");
            User user3 = createAndSaveTestUser("user_b");
            createAndSaveTestEmployee(user1, "Charlie", "Smith", "C1");
            createAndSaveTestEmployee(user2, "Alice", "Williams", "A2");
            createAndSaveTestEmployee(user3, "Bob", "Smith", "B3");

            // When: We retrieve all employees
            List<Employee> employees = employeeDAO.findAll();

            // Then: The list should be correct size and sorted correctly
            assertThat(employees).hasSize(3)
                .extracting(Employee::getLastName, Employee::getFirstName)
                .containsExactly(
                    tuple("Smith", "Bob"),
                    tuple("Smith", "Charlie"),
                    tuple("Williams", "Alice")
                );
        }

        @Test
        @DisplayName("count() should return the total number of employees")
        void count_shouldReturnTotalNumberOfEmployees() {
            // Given: We save a known number of employees
            User user1 = createAndSaveTestUser("employee1");
            User user2 = createAndSaveTestUser("employee2");
            createAndSaveTestEmployee(user1, "E1", "L1", "IC1");
            createAndSaveTestEmployee(user2, "E2", "L2", "IC2");

            // When: We call count
            long employeeCount = employeeDAO.count();

            // Then: The count should be correct
            assertThat(employeeCount).isEqualTo(2);
        }
    }

    // Helper methods
    private User createAndSaveTestUser(String username) {
        User user = new User(0, username, "password", UserDTO.Role.EMPLOYEE);
        userDAO.save(user);
        return user;
    }

    private Employee createAndSaveTestEmployee(User user, String firstName, String lastName, String icPassport) {
        Employee employee = new Employee(0, user.getId(), firstName, lastName, icPassport);
        employeeDAO.save(employee);
        return employee;
    }
}
