package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;

/**
 * Exception thrown for network and communication errors including connection failures,
 * timeouts, I/O errors, and server communication problems.
 */
public final class NetworkException extends TransientDataAccessException {

    public NetworkException(ErrorCode errorCode, String message) {
        super(errorCode, message, null);
    }

    public NetworkException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}