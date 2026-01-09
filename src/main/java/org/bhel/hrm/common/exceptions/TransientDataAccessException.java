package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;

public sealed class TransientDataAccessException extends DataAccessException permits NetworkException {
    /**
     * Constructs a TransientDataAccessException with the specified detail message and cause.
     *
     * @param message The detail message explaining the lock not getting acquired
     * @param cause The underlying cause of not acquiring the lock problem
     */
    public TransientDataAccessException(String message, Throwable cause) {
        super(ErrorCode.DB_LOCK_TIMEOUT, message, cause);
    }

    public TransientDataAccessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }


}
