package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;

/**
 * A {@link DataAccessException} thrown when a database operation fails
 * because a lock could not be acquired (e.g., due to a timeout or deadlock).
 */
public final class CannotAcquireLockException extends DataAccessException {
    /**
     * Constructs a CannotAcquireLockException with the specified detail message and cause.
     *
     * @param message The detail message explaining the lock not getting acquired
     * @param cause The underlying cause of not acquiring the lock problem
     */
    public CannotAcquireLockException(String message, Throwable cause) {
        super(ErrorCode.DB_DEADLOCK, message, cause);
    }

    public CannotAcquireLockException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
