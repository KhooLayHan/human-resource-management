package org.bhel.hrm.server.config;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.error.ErrorMessageProvider;
import org.bhel.hrm.common.error.ExceptionMappingConfig;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.GlobalExceptionHandler;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.bhel.hrm.server.daos.*;
import org.bhel.hrm.server.daos.impls.*;
import org.bhel.hrm.server.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bhel.hrm.server.services.impls.LeaveServiceImpl;
import org.bhel.hrm.server.services.impls.BenefitsServiceImpl;

public class ApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class);
    private static final ApplicationContext INSTANCE = new ApplicationContext();

    private final Configuration configuration;
    private final DatabaseManager databaseManager;
    private DatabaseSeeder databaseSeeder;

    private final ErrorMessageProvider errorMessageProvider;
    private final ExceptionMappingConfig exceptionMappingConfig;
    private final GlobalExceptionHandler globalExceptionHandler;

    private final UserDAO userDAO;
    private final EmployeeDAOImpl employeeDAO;

    private final BenefitPlanDAOImpl benefitPlanDAO;
    private final EmployeeBenefitDAOImpl employeeBenefitDAO;

    private final UserService userService;
    private final EmployeeService employeeService;
    private final DashboardService dashboardService;

    private final SslContextFactory sslContextFactory;
    private final CryptoUtils cryptoUtils;
    private PayrollSocketClient payrollSocketClient; // optional (null when payroll not implemented)


    private final LeaveService leaveService;
    private final BenefitsService benefitsService;

    private ApplicationContext() {
        logger.info("Initializing Application Context...");

        this.configuration = new Configuration();
        this.errorMessageProvider = new ErrorMessageProvider();
        this.exceptionMappingConfig = new ExceptionMappingConfig();

        this.databaseManager = new DatabaseManager(configuration);
        this.globalExceptionHandler = new GlobalExceptionHandler(exceptionMappingConfig, errorMessageProvider);

        this.sslContextFactory = new SslContextFactory(configuration);
        this.cryptoUtils = new CryptoUtils(configuration);

// ✅ Payroll not implemented yet -> do NOT fail startup if SSL config missing
        try {
            this.payrollSocketClient = new PayrollSocketClient(configuration, sslContextFactory, cryptoUtils);
            logger.info("PayrollSocketClient initialized successfully.");
        } catch (Exception e) {
            this.payrollSocketClient = null;
            logger.warn("PayrollSocketClient disabled (not configured / not implemented yet): {}", e.getMessage());
        }


        // DAOs
        this.userDAO = new UserDAOImpl(databaseManager);
        this.employeeDAO = new EmployeeDAOImpl(databaseManager);


        LeaveApplicationDAOImpl leaveApplicationDAO = new LeaveApplicationDAOImpl(databaseManager);
        this.benefitPlanDAO = new BenefitPlanDAOImpl(databaseManager);
        this.employeeBenefitDAO = new EmployeeBenefitDAOImpl(databaseManager);

        // Seed (if dev)
        seedDatabase(configuration, databaseManager, userDAO, employeeDAO);

        // Services
        this.userService = new UserService(databaseManager, userDAO, employeeDAO, payrollSocketClient);
        this.employeeService = new EmployeeService(databaseManager, employeeDAO, userDAO);
        this.dashboardService = new DashboardService(userDAO, employeeDAO);

        this.leaveService = new LeaveServiceImpl(
                leaveApplicationDAO,
                employeeDAO
        );

        this.benefitsService = new BenefitsServiceImpl(
                benefitPlanDAO,
                employeeBenefitDAO,
                employeeDAO
        );


        logger.info("Application Context initialized successfully");
    }

    private void seedDatabase(Configuration config, DatabaseManager dbManager, UserDAO userDAO, EmployeeDAO employeeDAO) {
        if ("development".equalsIgnoreCase(config.getAppEnvironment())) {
            databaseSeeder = new DatabaseSeeder(dbManager, userDAO, employeeDAO);
            databaseSeeder.seedIfEmpty();
        }
    }

    public static ApplicationContext get() {
        return INSTANCE;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DatabaseSeeder getDatabaseSeeder() {
        return databaseSeeder;
    }

    public ErrorMessageProvider getErrorMessageProvider() {
        return errorMessageProvider;
    }

    public ExceptionMappingConfig getExceptionMappingConfig() {
        return exceptionMappingConfig;
    }

    public GlobalExceptionHandler getGlobalExceptionHandler() {
        return globalExceptionHandler;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public EmployeeDAO getEmployeeDAO() {
        return employeeDAO;
    }

    public UserService getUserService() {
        return userService;
    }

    public EmployeeService getEmployeeService() {
        return employeeService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public SslContextFactory getSslContextFactory() {
        return sslContextFactory;
    }

    public CryptoUtils getCryptoUtils() {
        return cryptoUtils;
    }

    public PayrollSocketClient getPayrollSocketClient() {
        return payrollSocketClient;
    }

    public LeaveService getLeaveService() {
        return leaveService;
    }

    public BenefitsService getBenefitsService() {
        return benefitsService;
    }
}
