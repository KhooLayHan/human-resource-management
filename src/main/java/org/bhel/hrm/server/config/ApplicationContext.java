package org.bhel.hrm.server.config;

import org.bhel.hrm.common.error.ErrorMessageProvider;
import org.bhel.hrm.common.error.ExceptionMappingConfig;
import org.bhel.hrm.common.utils.GlobalExceptionHandler;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.UserDAO;
import org.bhel.hrm.server.daos.impls.EmployeeDAOImpl;
import org.bhel.hrm.server.daos.impls.UserDAOImpl;
import org.bhel.hrm.server.services.EmployeeService;
import org.bhel.hrm.server.services.UserService;

/**
 * Responsible for creating and wiring together all the core
 * components, i.e. services, DAOs, managers, of the application.
 * It follows the Singleton pattern to ensure only one context exists.
 */
public class    ApplicationContext {
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

    /**
     * Private constructor to enforce the Singleton pattern.
     * Initializes and wires all application components in the correct order.
     */
    private ApplicationContext() {
        this.configuration = new Configuration();
        this.errorMessageProvider = new ErrorMessageProvider();
        this.exceptionMappingConfig = new ExceptionMappingConfig();

        this.databaseManager = new DatabaseManager(configuration);
        this.globalExceptionHandler = new GlobalExceptionHandler(exceptionMappingConfig, errorMessageProvider);

        this.userDAO = new UserDAOImpl(databaseManager);
        this.employeeDAO = new EmployeeDAOImpl(databaseManager);

        this.userService = new UserService(databaseManager, userDAO, employeeDAO);
        this.employeeService = new EmployeeService(databaseManager, employeeDAO);

        seedDatabase(configuration, databaseManager, userDAO, employeeDAO);
    }

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
}
