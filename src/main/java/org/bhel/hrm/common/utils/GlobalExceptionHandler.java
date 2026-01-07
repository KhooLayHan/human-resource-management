package org.bhel.hrm.common.utils;

import org.bhel.hrm.common.error.*;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.exceptions.TransientDataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.sql.SQLException;

/**
 * Centralized exception handler for the HRM system that provides consistent error handling,
 * logging, and translation of exceptions across the application.
 * <p>
 * This handler follows a three-tier exception handling strategy:
 * <ol>
 *   <li>Business exceptions ({@link HRMException}) - logged and re-thrown as-is</li>
 *   <li>Database exceptions ({@link SQLException}) - translated to business exceptions</li>
 *   <li>Unexpected exceptions - logged as critical errors and wrapped in RemoteException</li>
 * </ol>
 * <p>
 * The handler uses dependency injection for configuration and message providers,
 * allowing for flexible error message customization and exception mapping strategies.
 * All exceptions are logged with unique error IDs for traceability and support.
 *
 * @see ErrorContext
 * @see ExceptionMappingConfig
 * @see ErrorMessageProvider
 */
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ExceptionMappingConfig mappingConfig;
    private final ErrorMessageProvider messageProvider;

    /**
     * Constructs a GlobalExceptionHandler with custom configuration.
     *
     * @param mappingConfig The configuration for mapping SQLExceptions to business exceptions
     * @param messageProvider The provider for user-friendly error messages
     */
    public GlobalExceptionHandler(
        ExceptionMappingConfig mappingConfig,
        ErrorMessageProvider messageProvider
    ) {
        this.mappingConfig = mappingConfig;
        this.messageProvider = messageProvider;
    }

    /**
     * Constructs a GlobalExceptionHandler with default configuration and message provider.
     */
    public GlobalExceptionHandler() {
        this(new ExceptionMappingConfig(), new ErrorMessageProvider());
    }

    /**
     * Handles an exception using the provided error context.
     * <p>
     * Processing flow:
     * <ul>
     *   <li>Null exceptions result in a {@link RemoteException}</li>
     *   <li>{@link HRMException}'s are logged and re-thrown</li>
     *   <li>{@link SQLException}'s are translated to {@link HRMException}'s and thrown</li>
     *   <li>All other exceptions are logged as critical and wrapped in {@link RemoteException}</li>
     * </ul>
     *
     * @param e       The exception to handle
     * @param context The error context containing operation details and metadata
     * @throws RemoteException If an unexpected/unrecoverable error occurs
     * @throws HRMException    If a business or translated database exception occurs
     */
    public void handle(Exception e, ErrorContext context) throws RemoteException, HRMException {
        if (e == null) {
            logger.error("Null exception passed to handler with context: {}", context);
            throw new RemoteException("An unexpected error occurred");
        }

        String errorId = context.getErrorId();
        String operation = context.getOperation();

        logger.debug("Handling exception [errorId={}] for operation: {}",
            errorId, operation);

        // 1. Handle specific and known business exceptions that were thrown.
        if (e instanceof HRMException hrmException) {
            handleBusinessException(hrmException, context);
            throw hrmException;
        }

        // 2. Handle specific and expected database errors and translate them into business exceptions.
        if (e instanceof SQLException sqlException) {
            handleSQLException(sqlException, context);
        }

        // 3. Handle all other unexpected, unrecoverable exceptions
        handleUnexpectedException(e, context);
    }

    /**
     * Convenience method for handling exceptions with operation context only.
     *
     * @param e         The exception to handle
     * @param operation The operation being performed when the exception occurred
     * @throws RemoteException If an unexpected/unrecoverable error occurs
     * @throws HRMException    If a business or translated database exception occurs
     */
    public static void handle(Exception e, String operation) throws RemoteException, HRMException {
        ErrorContext context = ErrorContext.forOperation(operation);

    }

    /**
     * Convenience method for handling exceptions with operation and user context.
     *
     * @param e         The exception to handle
     * @param operation The operation being performed when the exception occurred
     * @param userId    The ID of the user performing the operation
     * @throws RemoteException If an unexpected/unrecoverable error occurs
     * @throws HRMException    If a business or translated database exception occurs
     */
    public void handle(Exception e, String operation, String userId) throws RemoteException, HRMException {
        ErrorContext context = ErrorContext.forUser(operation, userId);
        handle(e, context);
    }

    /**
     * Handles known business exceptions by logging them at the WARN level.
     * <p>
     * Business exceptions are expected errors that represent violated business rules
     * or validation failures. They are logged with their error code and any additional
     * context data for troubleshooting.
     *
     * @param e       The business exception to handle
     * @param context The error context containing operation details
     */
    private void handleBusinessException(HRMException e, ErrorContext context) {
        String errorId = context.getErrorId();
        ErrorCode errorCode = e.getErrorCode();

        logger.warn(
            "Business exception [errorId={}, code={}, operation={}]: {}",
            errorId,
            errorCode.getCode(),
            context.getOperation(),
            e.getMessage()
        );

        if (!context.getAdditionalData().isEmpty()) {
            logger.debug("Additional context [errorId={}]: {}",
                errorId, context.getAdditionalData());
        }
    }

    /**
     * Handles SQL exceptions by translating them into appropriate business exceptions.
     * <p>
     * Uses the configured exception mapping strategy to convert database errors
     * into domain-specific exceptions with user-friendly messages. The original
     * SQL error code and state are preserved in the logs for debugging.
     *
     * @param e       The SQL exception to handle
     * @param context The error context containing operation details
     */
    private void handleSQLException(SQLException e, ErrorContext context) {
        String errorId = context.getErrorId();
        String operation = context.getOperation();

        logger.warn(
            "SQL exception [errorId={}, sqlCode={}, sqlState={}, operation={}]: {}",
            errorId,
            e.getErrorCode(),
            e.getSQLState(),
            operation,
            e.getMessage()
        );

        DataAccessException translated = mappingConfig.translate(e, context);

        logger.debug(
            "Translated to [errorId={}, code={}], {}",
            errorId,
            translated.getErrorCode().getCode(),
            translated.getMessage()
        );

        throw translated;
    }

    /**
     * Handles unexpected exceptions that cannot be recovered from.
     * <p>
     * These are typically programming errors or system failures that were not
     * anticipated. They are logged at the ERROR level with full stack traces
     * and wrapped in a RemoteException with a sanitized message for the client.
     *
     * @param e       The unexpected exception
     * @param context The error context containing operation details
     * @throws RemoteException Wrapping the original exception with a user-safe message
     */
    private void handleUnexpectedException(Exception e, ErrorContext context) throws RemoteException {
        String errorId = context.getErrorId();
        String operation = context.getOperation();

        logger.error(
            "Unrecoverable error [errorId={}, operation={}, exceptionType={}]",
            errorId,
            operation,
            e.getClass().getName(),
            e
        );

        throw new RemoteException(
            String.format(
                "A critical server error occurred [Error ID: %s]. Please contact support.",
                errorId
            ),
            e
        );
    }

    /**
     * Creates a user-safe error response suitable for client consumption.
     * <p>
     * Converts exceptions into standardized error responses that hide implementation
     * details while providing actionable information. Uses the injected message provider
     * to generate user-friendly messages based on error codes.
     *
     * @param e       The exception to convert
     * @param context The error context containing the error ID and timestamp
     * @return An {@link ErrorResponse} containing the error ID, code, message, and timestamp
     */
    public ErrorResponse createErrorResponse(Exception e, ErrorContext context) {
        if (e instanceof HRMException hrmException) {
            return new ErrorResponse(
                context.getErrorId(),
                hrmException.getErrorCode(),
                messageProvider.getMessage(hrmException.getErrorCode()),
                context.getTimestamp()
            );
        }

        return new ErrorResponse(
            context.getErrorId(),
            ErrorCode.SYSTEM_ERROR,
            messageProvider.getMessage(ErrorCode.SYSTEM_ERROR),
            context.getTimestamp()
        );
    }

    /**
     * Determines whether an exception represents a transient error that may succeed on retry.
     * <p>
     * Retryable exceptions include:
     * <ul>
     *   <li>{@link TransientDataAccessException} instances</li>
     *   <li>Database deadlocks: {@code ErrorCode.DB_DEADLOCK}</li>
     *   <li>Connection failures: {@code ErrorCode.DB_CONNECTION_FAILED}</li>
     *   <li>Lock timeouts: {@code ErrorCode.DB_LOCK_TIMEOUT}</li>
     * </ul>
     *
     * @param e The exception to evaluate
     * @return {@code true} if the exception is retryable, false otherwise
     */
    public boolean isRetryable(Exception e) {
        if (e instanceof TransientDataAccessException)
            return true;

        if (e instanceof HRMException hrmException) {
            ErrorCode errorCode = hrmException.getErrorCode();

            return
                errorCode == ErrorCode.DB_DEADLOCK ||
                errorCode == ErrorCode.DB_CONNECTION_FAILED ||
                errorCode == ErrorCode.DB_LOCK_TIMEOUT
            ;
        }

        return false;
    }
}
