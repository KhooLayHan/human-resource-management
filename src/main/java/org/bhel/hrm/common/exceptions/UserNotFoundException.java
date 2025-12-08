package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * Thrown when a requested user cannot be found in the system.
 * <p>
 * This exception indicates that an operation requiring a specific user failed
 * because no user with the specified identifier exists.
 */
public final class UserNotFoundException extends HRMException {
    private final String identifier;

    /**
     * Constructs a UserNotFoundException for the specified identifier.
     *
     * @param identifier The username that was not found
     */
    public UserNotFoundException(String identifier) {
        super(
            ErrorCode.USER_NOT_FOUND,
            formatMessage(identifier)
        );
        this.identifier = identifier;
    }

    /**
     * Constructs a UserNotFoundException with context.
     *
     * @param identifier The username or user ID that was not found
     * @param context The error context
     */
    public UserNotFoundException(String identifier, ErrorContext context) {
        super(
            ErrorCode.USER_NOT_FOUND,
            formatMessage(identifier),
            context
        );
        this.identifier = identifier;
    }

    /**
     * Constructs a UserNotFoundException with custom message.
     *
     * @param identifier The username or user ID that was not found
     * @param message Custom error message
     */
    public UserNotFoundException(String identifier, String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
        this.identifier = identifier;
    }

    private static String formatMessage(String identifier) {
        return String.format("User with identifier '%s' was not found",
            identifier);
    }

    public String getIdentifier() {
        return identifier;
    }
}
