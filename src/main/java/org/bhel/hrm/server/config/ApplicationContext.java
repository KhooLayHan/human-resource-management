package org.bhel.hrm.server.config;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.error.ErrorMessageProvider;
import org.bhel.hrm.common.error.ExceptionMappingConfig;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.GlobalExceptionHandler;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.TrainingCourseDAO;
import org.bhel.hrm.server.daos.TrainingEnrollmentDAO;
import org.bhel.hrm.server.daos.UserDAO;
import org.bhel.hrm.server.daos.impls.EmployeeDAOImpl;
import org.bhel.hrm.server.daos.impls.TrainingCourseDAOImpl;
import org.bhel.hrm.server.daos.impls.TrainingEnrollmentDAOImpl;
import org.bhel.hrm.server.daos.impls.UserDAOImpl;
import org.bhel.hrm.server.services.DashboardService;
import org.bhel.hrm.server.services.EmployeeService;
import org.bhel.hrm.server.services.TrainingService;
import org.bhel.hrm.server.services.PayrollSocketClient;
import org.bhel.hrm.server.services.UserService;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.bhel.hrm.server.daos.*;
import org.bhel.hrm.server.daos.impls.BenefitPlanDAOImpl;
import org.bhel.hrm.server.daos.impls.EmployeeBenefitDAOImpl;
import org.bhel.hrm.server.daos.impls.EmployeeDAOImpl;
import org.bhel.hrm.server.daos.impls.LeaveApplicationDAOImpl;
import org.bhel.hrm.server.daos.impls.UserDAOImpl;
import org.bhel.hrm.server.services.*;
import org.bhel.hrm.server.services.impls.BenefitsServiceImpl;
import org.bhel.hrm.server.services.impls.LeaveServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class);
    private static final ApplicationContext INSTANCE = new ApplicationContext();

    private final Configuration configuration;
    private final DatabaseManager databaseManager;
    private DatabaseSeeder databaseSeeder;

    private final ErrorMessageProvider errorMessageProvider;
    private final ExceptionMappingConfig exceptionMappingConfig;
    private final GlobalExceptionHandler globalExceptionHandler;

    // -------- DAOs --------
    private final UserDAO userDAO;
    private final EmployeeDAO employeeDAO;
    private final TrainingCourseDAO trainingCourseDAO;
    private final TrainingEnrollmentDAO trainingEnrollmentDAO;
    private final LeaveApplicationDAO leaveApplicationDAO;
    private final BenefitPlanDAO benefitPlanDAO;
    private final EmployeeBenefitDAO employeeBenefitDAO;

    // -------- Services --------
    private final UserService userService;
    private final EmployeeService employeeService;
    private final TrainingService trainingService;

    private final DashboardService dashboardService;
    private final LeaveService leaveService;
    private final BenefitsService benefitsService;

    // -------- Other core components --------
    private final SslContextFactory sslContextFactory;
    private final CryptoUtils cryptoUtils;

    // make payroll optional for now
    private final PayrollSocketClient payrollSocketClient;

    private ApplicationContext() {
        logger.info("Initializing Application Context...");

        // ---- config + error handling ----
        this.configuration = new Configuration();
        this.errorMessageProvider = new ErrorMessageProvider();
        this.exceptionMappingConfig = new ExceptionMappingConfig();

        this.databaseManager = new DatabaseManager(configuration);
        this.globalExceptionHandler = new GlobalExceptionHandler(exceptionMappingConfig, errorMessageProvider);

        // ---- security ----
        this.sslContextFactory = new SslContextFactory(configuration);
        this.cryptoUtils = new CryptoUtils(configuration);


        PayrollSocketClient payrollTmp = null;
        try {

            String ks = System.getProperty("keystore.path");
            if (ks != null && !ks.isBlank()) {
                payrollTmp = new PayrollSocketClient(configuration, sslContextFactory, cryptoUtils);
                logger.info("PayrollSocketClient initialized (keystore.path provided).");
            } else {
                logger.warn("PayrollSocketClient disabled: keystore.path not set.");
            }
        } catch (Exception ex) {
            logger.warn("PayrollSocketClient disabled due to init error: {}", ex.getMessage());
        }
        this.payrollSocketClient = payrollTmp;

        // ---- DAOs ----
        this.userDAO = new UserDAOImpl(databaseManager);
        this.employeeDAO = new EmployeeDAOImpl(databaseManager);
        this.trainingCourseDAO = new TrainingCourseDAOImpl(databaseManager);
        this.trainingEnrollmentDAO = new TrainingEnrollmentDAOImpl(databaseManager);
        this.leaveApplicationDAO = new LeaveApplicationDAOImpl(databaseManager);
        this.benefitPlanDAO = new BenefitPlanDAOImpl(databaseManager);
        this.employeeBenefitDAO = new EmployeeBenefitDAOImpl(databaseManager);

        // ---- Services ----
        // UserService must tolerate payrollSocketClient being null (see note below)
        this.userService = new UserService(databaseManager, userDAO, employeeDAO, payrollSocketClient);
        this.employeeService = new EmployeeService(databaseManager, employeeDAO, userDAO);
        this.trainingService = new TrainingService(databaseManager, trainingCourseDAO, trainingEnrollmentDAO);
        this.dashboardService = new DashboardService(userDAO, employeeDAO);

        this.leaveService = new LeaveServiceImpl(leaveApplicationDAO, employeeDAO);
        this.benefitsService = new BenefitsServiceImpl(benefitPlanDAO, employeeBenefitDAO, employeeDAO);

        // ---- Seed DB (dev only) ----
        seedDatabase(configuration, databaseManager, userDAO, employeeDAO);

        logger.info("Application Context initialized successfully");
    }

    private void seedDatabase(
            Configuration config,
            DatabaseManager dbManager,
            UserDAO userDAO,
            EmployeeDAO employeeDAO
    ) {
        if ("development".equalsIgnoreCase(config.getAppEnvironment())) {
            databaseSeeder = new DatabaseSeeder(dbManager, userDAO, employeeDAO, trainingCourseDAO, trainingEnrollmentDAO);
            databaseSeeder.seedIfEmpty();
        }
    }

    public static ApplicationContext get() {
        return INSTANCE;
    }

    // -------- Getters --------

    public Configuration getConfiguration() { return configuration; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DatabaseSeeder getDatabaseSeeder() { return databaseSeeder; }
    public ErrorMessageProvider getErrorMessageProvider() { return errorMessageProvider; }
    public ExceptionMappingConfig getExceptionMappingConfig() { return exceptionMappingConfig; }
    public GlobalExceptionHandler getGlobalExceptionHandler() { return globalExceptionHandler; }

    public UserDAO getUserDAO() { return userDAO; }
    public EmployeeDAO getEmployeeDAO() { return employeeDAO; }
    public LeaveApplicationDAO getLeaveApplicationDAO() { return leaveApplicationDAO; }
    public BenefitPlanDAO getBenefitPlanDAO() { return benefitPlanDAO; }
    public EmployeeBenefitDAO getEmployeeBenefitDAO() { return employeeBenefitDAO; }

    public UserService getUserService() { return userService; }
    public EmployeeService getEmployeeService() { return employeeService; }
    public DashboardService getDashboardService() { return dashboardService; }
    public LeaveService getLeaveService() { return leaveService; }
    public BenefitsService getBenefitsService() { return benefitsService; }

    public SslContextFactory getSslContextFactory() { return sslContextFactory; }
    public CryptoUtils getCryptoUtils() { return cryptoUtils; }

    public PayrollSocketClient getPayrollSocketClient() {
        return payrollSocketClient; // may be null in dev
    }

    public TrainingCourseDAO getTrainingCourseDAO() {
        return trainingCourseDAO;
    }

    public TrainingEnrollmentDAO getTrainingEnrollmentDAO() {
        return trainingEnrollmentDAO;
    }

    public TrainingService getTrainingService() {
        return trainingService;
    }

}
