package org.bhel.hrm.server.config;

import org.bhel.hrm.common.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private final Properties properties;

    public Configuration() {
        properties = new Properties();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.warn("Sorry");
                throw new IOException("File config.properties is not found in classpath.");
            }

            properties.load(input);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration." + e.getMessage(), e);
        }
    }

    public String getAppEnvironment() {
        return properties.getProperty("app.environment", "production");
    }

    public String getSecretKey() {
        return properties.getProperty("app.secret.key");
    }

    public String getPayrollHost() {
        return properties.getProperty("payroll.host");
    }

    public String getPayrollPort() {
        return properties.getProperty("payroll.port");
    }

    public String getRMIHost() {
        return properties.getProperty("rmi.host");
    }

    public String getRMIPort() {
        return properties.getProperty("rmi.port");
    }

    public String getRMIServiceName() {
        return properties.getProperty("rmi.service.name");
    }

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
