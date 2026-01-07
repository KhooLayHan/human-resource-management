package org.bhel.hrm.server.config;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.error.ErrorMessageProvider;
import org.bhel.hrm.common.error.ExceptionMappingConfig;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.GlobalExceptionHandler;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.UserDAO;
import org.bhel.hrm.server.daos.impls.EmployeeDAOImpl;
import org.bhel.hrm.server.daos.impls.UserDAOImpl;
import org.bhel.hrm.server.services.DashboardService;
import org.bhel.hrm.server.services.EmployeeService;
import org.bhel.hrm.server.services.PayrollSocketClient;
import org.bhel.hrm.server.services.UserService;
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

    private final UserDAO userDAO;
    private final EmployeeDAO employeeDAO;

    private final UserService userService;
    private final EmployeeService employeeService;
    private final DashboardService dashboardService;

    private final SslContextFactory sslContextFactory;
    private final CryptoUtils cryptoUtils;
    private final PayrollSocketClient payrollSocketClient;

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

        this.sslContextFactory = new SslContextFactory(configuration);
        this.cryptoUtils = new CryptoUtils(configuration);
        this.payrollSocketClient = new PayrollSocketClient(configuration, sslContextFactory, cryptoUtils);

        this.userDAO = new UserDAOImpl(databaseManager);
        this.employeeDAO = new EmployeeDAOImpl(databaseManager);

        this.userService = new UserService(databaseManager, userDAO, employeeDAO, payrollSocketClient);
        this.employeeService = new EmployeeService(databaseManager, employeeDAO, userDAO);
        this.dashboardService = new DashboardService(userDAO, employeeDAO);

        seedDatabase(configuration, databaseManager, userDAO, employeeDAO);

        logger.info("Application Context initialized successfully");
    }

    /**
     * Seeds the database with initial data if in development environment.
     */
    private void seedDatabase(
        Configuration config,
        DatabaseManager dbManager,
        UserDAO userDAO,
        EmployeeDAO employeeDAO
    ) {
        if ("development".equalsIgnoreCase(config.getAppEnvironment())) {
            databaseSeeder = new DatabaseSeeder(dbManager, userDAO, employeeDAO);
            databaseSeeder.seedIfEmpty();
        }
    }

    /**
     * Retrieves the current context's instance.
     *
     * @return The single, global instance of the ApplicationContext.
     */
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
}
