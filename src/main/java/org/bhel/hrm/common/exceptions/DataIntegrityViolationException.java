package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * A {@link DataAccessException} thrown when a database constraint is violated
 * (e.g., a unique constraint or foreign key constraint).
 */
public final class DataIntegrityViolationException extends DataAccessException {
    /**
     * Constructs a new DataIntegrityViolationException with the specified detail message and cause.
     *
     * @param message The detail message explaining the data integrity violation error
     * @param cause The underlying cause of the data violation
     */
    public DataIntegrityViolationException(String message, Throwable cause) {
        super(ErrorCode.DB_DUPLICATE_ENTRY, message, cause);
    }

    public DataIntegrityViolationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public DataIntegrityViolationException(ErrorCode errorCode, String message, Throwable cause, ErrorContext context) {
        super(errorCode, message, cause, context);
    }
}
