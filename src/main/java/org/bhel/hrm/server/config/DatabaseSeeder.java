package org.bhel.hrm.server.config;

import net.datafaker.Faker;
import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.server.daos.ApplicantDAO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.JobOpeningDAO;
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
    private final JobOpeningDAO jobOpeningDAO; // New DAO
    private final ApplicantDAO applicantDAO;   // New DAO
    private final Faker faker;

    // Updated Constructor to accept new DAOs
    public DatabaseSeeder(
            DatabaseManager dbManager,
            UserDAO userDAO,
            EmployeeDAO employeeDAO,
            JobOpeningDAO jobOpeningDAO,
            ApplicantDAO applicantDAO
    ) {
        this.dbManager = dbManager;
        this.userDAO = userDAO;
        this.employeeDAO = employeeDAO;
        this.jobOpeningDAO = jobOpeningDAO;
        this.applicantDAO = applicantDAO;
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

            // 1. Seed Users and Employees
            //seedUserAndEmployeeData();

            // 2. Seed Recruitment (Jobs and Applicants)
//            seedRecruitmentData();

            dbManager.commitTransaction();
            logger.info("Successfully seeded the database.");
        } catch (Exception e) {
            logger.error("Database seeding failed. Rolling back transaction.", e);
            dbManager.rollbackTransaction();
        }
    }

    private void seedUserAndEmployeeData() {
        // 1. Create a default HR Staff user
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

        // 2. Create a default Employee user
        User employeeUser = new User(
                "employee",
                PasswordService.hashPassword("user123"),
                UserDTO.Role.EMPLOYEE
        );
        userDAO.save(employeeUser);
//
        Employee testEmployee = new Employee(
                employeeUser.getId(),
                "John",
                "Doe",
                "S1234567B"
        );
        employeeDAO.save(testEmployee);

        // 3. Create 20 random employees for development
//        for (int i = 0; i < 20; i++) {
//            // Using a unique username strategy to avoid potential collision
//            String username = faker.name().name();
//
//            User randomUser = new User(
//                    username,
//                    PasswordService.hashPassword("password"),
//                    UserDTO.Role.EMPLOYEE
//            );
//            userDAO.save(randomUser);
//
//            Employee randomEmployee = new Employee(
//                    randomUser.getId(),
//                    faker.name().firstName(),
//                    faker.name().lastName(),
//                    faker.idNumber().ssnValid()
//            );
//            employeeDAO.save(randomEmployee);
//        }
        logger.info("Seeded {} users and employees.", userDAO.count());
    }

    private void seedRecruitmentData() {
        logger.info("Seeding recruitment data...");

        // Create 10 Job Openings
        for (int i = 0; i < 10; i++) {
//            JobOpening job = new JobOpening();
//            job.setTitle(faker.job().title());
//            job.setDescription(faker.lorem().paragraph(3));
//            job.setDepartment(faker.commerce().department());
//
//            // Mix of Statuses (mostly OPEN)
//            if (i == 8) job.setStatus(JobOpeningDTO.JobStatus.CLOSED);
//            else if (i == 9) job.setStatus(JobOpeningDTO.JobStatus.ON_HOLD);
//            else job.setStatus(JobOpeningDTO.JobStatus.OPEN);
//
//            // Generate Dates
//            LocalDate postedDate = faker.timeAndDate().past(30, TimeUnit.DAYS)
//                    .atZone(ZoneId.systemDefault()).toLocalDate();
//            job.setPostedDate(postedDate);
//
//            // Closing date is 30 days after posted
//            job.setClosingDate(postedDate.plusDays(30));
//
//            // Save Job (generates ID)
//            jobOpeningDAO.save(job);
//
//            // Generate 0 to 8 Applicants per Job
//            int numApplicants = faker.number().numberBetween(0, 9);
//            for (int j = 0; j < numApplicants; j++) {
//                Applicant applicant = new Applicant();
//                applicant.setJobOpeningId(job.getId());
//                applicant.setFullName(faker.name().fullName());
//                applicant.setEmail(faker.internet().emailAddress());
//                applicant.setPhone(faker.phoneNumber().cellPhone());
//
//                // Random Status
//                ApplicantDTO.ApplicantStatus[] statuses = ApplicantDTO.ApplicantStatus.values();
//                applicant.setStatus(statuses[faker.number().numberBetween(0, statuses.length)]);
//
//                // Optional: Fake resume URL
//                applicant.setResumeUrl("/docs/resumes/" + faker.file().fileName(null, null, "pdf", null));
//
//                applicantDAO.save(applicant);
//            }
        }
        logger.info("Seeded recruitment data.");
    }
}