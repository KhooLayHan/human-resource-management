package org.bhel.hrm.server.config;

import net.datafaker.Faker;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.UserDAO;
import org.bhel.hrm.server.domain.Employee;
import org.bhel.hrm.server.domain.User;
import org.bhel.hrm.server.services.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class DatabaseSeeder {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final DatabaseManager dbManager;
    private final UserDAO userDAO;
    private final EmployeeDAO employeeDAO;
    private final Faker faker;

    public DatabaseSeeder(DatabaseManager dbManager, UserDAO userDAO, EmployeeDAO employeeDAO) {
        this.dbManager = dbManager;
        this.userDAO = userDAO;
        this.employeeDAO = employeeDAO;
        this.faker = new Faker(Locale.of("en-US"));
    }

    /**
     * Checks if the database is empty and seeds it with initial data if it is.
     * This entire process is wrapped in a single transaction.
     */
    public void seedIfEmpty() {
        // Check if data already exists to prevent re-seeding
        if (userDAO.count() > 0) {
            logger.info("Database already contains data; seeding will be skipped.");
            return;
        }

        logger.info("Database is empty; seeding with initial fake data...");

        try {
            dbManager.beginTransaction();

            // 1. Creates a default HR Staff user
            User hrUser = new User(
                "hr_admin",
                PasswordService.hashPassword("admin123"),
                UserDTO.Role.HR_STAFF
            );
            userDAO.save(hrUser);

            Employee hrEmployee = new Employee(
                hrUser.getId(),
                "Admin",
                "User",
                "S0000000A"
            );
            employeeDAO.save(hrEmployee);

            // 2. Creates a default Employee user
            User employeeUser = new User(
                "employee",
                PasswordService.hashPassword("user123"),
                UserDTO.Role.EMPLOYEE
            );
            userDAO.save(employeeUser);

            Employee testEmployee = new Employee(
                employeeUser.getId(),
                "John",
                "Doe",
                "S1234567B"
            );
            employeeDAO.save(testEmployee);

            // 3. Creates 20 random employees for development
            for (int i = 0; i < 20; i++) {
                User randomUser = new User(
                    faker.name().name(),
                    PasswordService.hashPassword("password"),
                    UserDTO.Role.EMPLOYEE
                );
                userDAO.save(randomUser);

                Employee randomEmployee = new Employee(
                    randomUser.getId(),
                    faker.name().firstName(),
                    faker.name().lastName(),
                    faker.idNumber().ssnValid()
                );
                employeeDAO.save(randomEmployee);
            }

            dbManager.commitTransaction();
            logger.info("Successfully seeded the database with {} users.", userDAO.count());
        } catch (Exception e) {
            logger.error("Database seeding failed. Rolling back transaction.", e);
            dbManager.rollbackTransaction();
        }
    }
}
