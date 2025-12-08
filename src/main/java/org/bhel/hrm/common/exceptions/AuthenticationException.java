package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * Thrown when a user provides invalid credentials (e.g., unknown username or wrong password).
 * <p>
 * This exception indicates an authentication failure due to incorrect login details.
 * It is recommended not to disclose whether the username exists or which specific
 * credential was incorrect, to prevent user enumeration attacks.
 */
public final class AuthenticationException extends HRMException {
    private static final String MESSAGE = "Invalid username or password.";

    private final String username;

    /**
     * Constructs an AuthenticationException for the specified username.
     *
     * @param username The username that failed authentication; may be null
     */
    public AuthenticationException(String username) {
        super(
            ErrorCode.AUTH_INVALID_CREDENTIALS,
            MESSAGE
        );
        this.username = username;
    }

    /**
     * Constructs an AuthenticationException with the specified context.
     *
     * @param username The username that failed authentication; may be null
     * @param context The error context with additional information
     */
    public AuthenticationException(String username, ErrorContext context) {
        super(
            ErrorCode.AUTH_INVALID_CREDENTIALS,
            MESSAGE,
            context
        );
        this.username = username;
    }

    /**
     * Constructs an AuthenticationException with the specified username and cause.
     *
     * @param username The username that failed authentication; may be null
     * @param cause The underlying cause of the authentication failure
     */
    public AuthenticationException(String username, Throwable cause) {
        super(
            ErrorCode.AUTH_INVALID_CREDENTIALS,
            MESSAGE,
            cause,
            null
        );
        this.username = username;
    }

    /**
     * Constructs an AuthenticationException with cause and context.
     *
     * @param username The username that failed authentication; may be null
     * @param cause The underlying cause of the authentication failure
     * @param context The error context with additional information
     */
    public AuthenticationException(String username, Throwable cause, ErrorContext context) {
        super(
            ErrorCode.AUTH_INVALID_CREDENTIALS,
            MESSAGE,
            cause,
            context
        );
        this.username = username;
    }

    /**
     * Returns the username associated with this authentication failure.
     *
     * @return the username that failed authentication, or null
     */
    public String getUsername() {
        return username;
    }
}
