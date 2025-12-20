package org.bhel.hrm.common.config;

import org.bhel.hrm.common.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private static final String DEFAULT_RMI_HOST = "localhost";
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final String DEFAULT_SERVICE_NAME = "HRMService";

    private final Properties properties;

    public Configuration() {
        this("config.properties");
    }

    public Configuration(String configFileName) {
        properties = new Properties();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFileName)) {
            if (input == null) {
                logger.warn("Configuration file '{}' not found in classpath. Using defaults where applicable.", configFileName);
                throw new IOException("File " + configFileName + " is not found in classpath.");
            }

            properties.load(input);
            logger.info("Configuration loaded successfully from '{}'.", configFileName);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration." + e.getMessage(), e);
        }
    }

    // RMI Configuration
    public String getRMIHost() {
        return properties.getProperty("rmi.host", DEFAULT_RMI_HOST);
    }

    public int getRMIPort() {
        String portStr = properties.getProperty("rmi.port");

        if (portStr == null || portStr.isBlank()) {
            logger.warn("RMI port not configured, using default: {}", DEFAULT_RMI_PORT);
            return DEFAULT_RMI_PORT;
        }

        try {
            int port = Integer.parseInt(portStr.trim());
            if (port < 1 || port > 65_535)
                throw new NumberFormatException("Port out of valid range (1-65535): " + port);

            return port;
        } catch (NumberFormatException e) {
            logger.error("Invalid RMI port value '{}'. Using default: {}", portStr, DEFAULT_RMI_PORT);
            throw new ConfigurationException(
                String.format("Invalid RMI port configuration: '%s'. Must be a number between 1-65535.", portStr),
                e
            );
        }
    }

    public String getRMIServiceName() {
        return properties.getProperty("rmi.service.name", DEFAULT_SERVICE_NAME);
    }

    // Application Configuration
    public String getAppEnvironment() {
        return properties.getProperty("app.environment", "production");
    }

    public String getSecretKey() {
        return properties.getProperty("app.secret.key");
    }

    // Payroll Configuration
    public String getPayrollHost() {
        return properties.getProperty("payroll.host");
    }

    public String getPayrollPort() {
        return properties.getProperty("payroll.port");
    }

    // Database Configuration
    public String getDbUrl() {
        // jdbc:mysql://localhost:3306/hrm_db?useSSL=false&serverTimezone=UTC
        return String.format("%s:%s://%s:%s/%s?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
            getDbDriver(),
            getDbConnection(),
            getDbHost(),
            getDbPort(),
            getDbName()
        );
    }

    public String getDbConnection() {
        return properties.getProperty("db.connection");
    }

    public String getDbDriver() {
        return properties.getProperty("db.driver");
    }

    public String getDbUser() {
        return properties.getProperty("db.user");
    }

    public String getDbPassword() {
        return properties.getProperty("db.password");
    }

    public String getDbHost() {
        return properties.getProperty("db.host");
    }

    public String getDbPort() {
        return properties.getProperty("db.port");
    }

    public String getDbName() {
        return properties.getProperty("db.name");
    }
}
