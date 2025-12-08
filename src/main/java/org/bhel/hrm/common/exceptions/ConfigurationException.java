package org.bhel.hrm.common.exceptions;

/**
 * Thrown when there is a problem with the application configuration.
 * <p>
 * This unchecked exception indicates that the system is misconfigured or
 * required configuration is missing, typically preventing proper startup or operation.
 */
public class ConfigurationException extends RuntimeException {
    /**
     * Constructs a ConfigurationException with message.
     *
     * @param message The detail message explaining the configuration error
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a ConfigurationException with the specified detail message and cause.
     *
     * @param message The detail message explaining the configuration error
     * @param cause The underlying cause of the configuration problem
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
