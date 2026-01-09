package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * A base, checked exception for the HRM application, used for recoverable errors
 * that the caller should be forced to handle.
 * <p>
 * This exception should be extended
 * for specific error conditions within the HRM system. It ensures that
 * exceptional conditions are explicitly handled by client code.
 */
public sealed class HRMException extends Exception permits
    AuthenticationException,
    DuplicateUserException,
    EnrollmentException,
    InvalidInputException,
    LeaveManagementException,
    ResourceNotFoundException,
    UserNotFoundException,
    SecurityException {

    private final ErrorCode errorCode;
    private final ErrorContext errorContext;

    /**
     * Constructs a new HRM exception with the specified detail message.
     *
     * @param message The detail message explaining the exception
     */
    public HRMException(String message) {
        this(ErrorCode.SYSTEM_ERROR, message, null, null);
    }

    /**
     * Constructs a new HRM exception with the specified detail message and cause.
     *
     * @param message The detail message explaining the exception
     * @param cause The underlying cause of the exception
     */
    public HRMException(
        String message,
        Throwable cause
    ) {
        this(ErrorCode.SYSTEM_ERROR, message, cause, null);
    }

    public HRMException(
        ErrorCode errorCode,
        String message
    ) {
        this(errorCode, message, null, null);
    }

    public HRMException(
        ErrorCode errorCode,
        String message,
        ErrorContext context
    ) {
        this(errorCode, message, null, context);
    }

    public HRMException(
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
