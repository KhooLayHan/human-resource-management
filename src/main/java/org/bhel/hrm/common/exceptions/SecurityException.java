package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * Exception thrown for security-related errors including SSL/TLS, encryption,
 * decryption, certificate, and authentication failures.
 *
 */
public class SecurityException extends RuntimeException {
    private final ErrorCode errorCode;
    private final ErrorContext errorContext;

    public SecurityException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public SecurityException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null);
    }

    public SecurityException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null, null);
    }

    public SecurityException(ErrorCode errorCode, String message, Throwable cause, ErrorContext context) {
        super(formatMessage(errorCode, message), cause);
        this.errorCode = errorCode;
        this.errorContext = context;
    }

    private static String formatMessage(ErrorCode errorCode, String message) {
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