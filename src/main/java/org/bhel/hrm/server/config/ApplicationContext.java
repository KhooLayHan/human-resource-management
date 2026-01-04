package org.bhel.hrm.server.config;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.error.ErrorMessageProvider;
import org.bhel.hrm.common.error.ExceptionMappingConfig;
import org.bhel.hrm.common.utils.GlobalExceptionHandler;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.*;
import org.bhel.hrm.server.daos.impls.*;
import org.bhel.hrm.server.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for creating and wiring together all the core
 * components, i.e. services, DAOs, managers, of the application.
 * It follows the Singleton pattern to ensure only one context exists.
 */
public class ApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class);
    private static final ApplicationContext INSTANCE = new ApplicationContext();

    private final Configuration configuration;
    private final DatabaseManager databaseManager;
    private DatabaseSeeder databaseSeeder;

    private final ErrorMessageProvider errorMessageProvider;
    private final ExceptionMappingConfig exceptionMappingConfig;
    private final GlobalExceptionHandler globalExceptionHandler;

    // --- DAOs ---
    private final UserDAO userDAO;
    private final EmployeeDAO employeeDAO;
    private final JobOpeningDAO jobOpeningDAO;
    private final ApplicantDAO applicantDAO;
    private final LeaveApplicationDAO leaveApplicationDAO; // NEW
    private final TrainingCourseDAO trainingCourseDAO;     // NEW
    private final BenefitPlanDAO benefitPlanDAO;           // NEW

    // --- Services ---
    private final UserService userService;
    private final EmployeeService employeeService;
    private final RecruitmentService recruitmentService; // NEW
    private final LeaveService leaveService;             // NEW
    private final TrainingService trainingService;       // NEW
    private final BenefitService benefitService;         // NEW

    /**
     * Private constructor to enforce the Singleton pattern.
     * Initializes and wires all application components in the correct order.
     */
    private ApplicationContext() {
        logger.info("Initializing Application Context...");

        this.configuration = new Configuration();
        this.errorMessageProvider = new ErrorMessageProvider();
        this.exceptionMappingConfig = new ExceptionMappingConfig();

        this.databaseManager = new DatabaseManager(configuration);
        this.globalExceptionHandler = new GlobalExceptionHandler(exceptionMappingConfig, errorMessageProvider);

        // --- Initialize DAOs ---
        this.userDAO = new UserDAOImpl(databaseManager);
        this.employeeDAO = new EmployeeDAOImpl(databaseManager);
        this.jobOpeningDAO = new JobOpeningDAOImpl(databaseManager, exceptionMappingConfig);
        this.applicantDAO = new ApplicantDAOImpl(databaseManager, exceptionMappingConfig);

        // Initialize new DAOs (Ensure these implementation classes exist)
        this.leaveApplicationDAO = new LeaveApplicationDAOImpl(databaseManager);
        this.trainingCourseDAO = new TrainingCourseDAOImpl(databaseManager);
        this.benefitPlanDAO = new BenefitPlanDAOImpl(databaseManager);

        // --- Initialize Services ---
        this.userService = new UserService(
                databaseManager,
                userDAO,
                employeeDAO,
                new PayrollSocketClient(configuration)
        );
        this.employeeService = new EmployeeService(databaseManager, employeeDAO, userDAO);

        // Initialize new Services
        this.recruitmentService = new RecruitmentService(databaseManager, jobOpeningDAO, applicantDAO);
        this.leaveService = new LeaveService(databaseManager, leaveApplicationDAO, employeeDAO);
        this.trainingService = new TrainingService(databaseManager, trainingCourseDAO);
        this.benefitService = new BenefitService(databaseManager, benefitPlanDAO);

        // --- Run Seeder ---
        seedDatabase(
                configuration,
                databaseManager,
                userDAO,
                employeeDAO,
                jobOpeningDAO,
                applicantDAO
        );

        logger.info("Application Context initialized successfully");
    }

    /**
     * Seeds the database with initial data if in development environment.
     */
    private void seedDatabase(
            Configuration config,
            DatabaseManager dbManager,
            UserDAO userDAO,
            EmployeeDAO employeeDAO,
            JobOpeningDAO jobOpeningDAO,
            ApplicantDAO applicantDAO
    ) {
        if ("development".equalsIgnoreCase(config.getAppEnvironment())) {
            databaseSeeder = new DatabaseSeeder(
                    dbManager,
                    userDAO,
                    employeeDAO,
                    jobOpeningDAO,
                    applicantDAO
            );
            databaseSeeder.seedIfEmpty();
        }
    }

    public static ApplicationContext get() {
        return INSTANCE;
    }

    // --- Getters ---

    public Configuration getConfiguration() { return configuration; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DatabaseSeeder getDatabaseSeeder() { return databaseSeeder; }
    public ErrorMessageProvider getErrorMessageProvider() { return errorMessageProvider; }
    public ExceptionMappingConfig getExceptionMappingConfig() { return exceptionMappingConfig; }
    public GlobalExceptionHandler getGlobalExceptionHandler() { return globalExceptionHandler; }

    public UserDAO getUserDAO() { return userDAO; }
    public EmployeeDAO getEmployeeDAO() { return employeeDAO; }
    public JobOpeningDAO getJobOpeningDAO() { return jobOpeningDAO; }
    public ApplicantDAO getApplicantDAO() { return applicantDAO; }

    // New DAO Getters
    public LeaveApplicationDAO getLeaveApplicationDAO() { return leaveApplicationDAO; }
    public TrainingCourseDAO getTrainingCourseDAO() { return trainingCourseDAO; }
    public BenefitPlanDAO getBenefitPlanDAO() { return benefitPlanDAO; }

    public UserService getUserService() { return userService; }
    public EmployeeService getEmployeeService() { return employeeService; }

    // New Service Getters
    public RecruitmentService getRecruitmentService() { return recruitmentService; }
    public LeaveService getLeaveService() { return leaveService; }
    public TrainingService getTrainingService() { return trainingService; }
    public BenefitService getBenefitService() { return benefitService; }
}