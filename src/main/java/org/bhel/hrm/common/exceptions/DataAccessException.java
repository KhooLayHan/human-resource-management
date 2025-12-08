package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * A runtime exception thrown when there is a critical, unrecoverable error
 * during a database operation (e.g., connection failure, SQL syntax error).
 * <p>
 * This exception serves as a base class for more specific data access exceptions
 * and is used to wrap underlying data access failures, propagating them as unchecked exceptions.
 */
public sealed class DataAccessException extends RuntimeException permits
    CannotAcquireLockException,
    DataAccessResourceFailureException,
    DataIntegrityViolationException,
    IncorrectSqlGrammarException,
    TransientDataAccessException {

    private final ErrorCode errorCode;
    private final ErrorContext errorContext;

    /**
     * Constructs a new DataAccessException with the specified detail message and cause.
     *
     * @param message The detail message explaining the data access error
     * @param cause The underlying cause of the failure (typically a SQLException)
     */
    public DataAccessException(
        String message,
        Throwable cause
    ) {
        this(ErrorCode.DB_QUERY_ERROR, message, cause, null);
    }

    public DataAccessException(
        ErrorCode errorCode,
        String message,
        Throwable cause
    ) {
        this(errorCode, message, cause, null);
    }

    public DataAccessException(
        ErrorCode errorCode,
        String message,
        Throwable cause,
        ErrorContext context
    ) {
        super(formatMessage(errorCode, message), cause);

        this.errorCode = errorCode;
        this.errorContext = context;
    }

    private static String formatMessage(
        ErrorCode errorCode,
        String message
    ) {
        return String.format("[%s] %s", errorCode.getCode(), message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public ErrorContext getContext() {
        return errorContext;
    }

    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public String getCode() {
        return errorCode.getCode();
    }
}
