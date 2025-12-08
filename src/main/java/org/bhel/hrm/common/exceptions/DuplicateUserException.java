package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * Thrown when attempting to create a user with a username that already exists.
 * <p>
 * This exception indicates a violation of the unique username constraint in the system.
 */
public final class DuplicateUserException extends HRMException {
    private final String identifier;
    private final String identifierType;

    /**
     * Constructs a DuplicateUserException with a default message.
     *
     * @param message The detail message
     */
    public DuplicateUserException(String message) {
        super(ErrorCode.USER_ALREADY_EXISTS, message);
        this.identifier = null;
        this.identifierType = null;
    }

    /**
     * Constructs a DuplicateUserException for a specific identifier.
     *
     * @param identifier The identifier value that already exists
     * @param identifierType The type of identifier (e.g., "email", "username", "employee ID")
     */
    public DuplicateUserException(String identifier, String identifierType) {
        super(
            ErrorCode.USER_ALREADY_EXISTS,
            formatMessage(identifierType, identifier)
        );
        this.identifier = identifier;
        this.identifierType = identifierType;
    }

    /**
     * Constructs a DuplicateUserException with context.
     *
     * @param identifier The identifier value that already exists
     * @param identifierType The type of identifier
     * @param context The error context
     */
    public DuplicateUserException(String identifier, String identifierType, ErrorContext context) {
        super(
            ErrorCode.USER_ALREADY_EXISTS,
            formatMessage(identifierType, identifier),
            context
        );
        this.identifier = identifier;
        this.identifierType = identifierType;
    }

    /**
     * Constructs a DuplicateUserException with context.
     *
     * @param message Custom error message
     * @param context The error context
     */
    public DuplicateUserException(String message, ErrorContext context) {
        super(ErrorCode.USER_ALREADY_EXISTS, message, context);
        this.identifier = null;
        this.identifierType = null;
    }

    private static String formatMessage(String identifierType, String identifier) {
        return String.format("A user with %s [%s] already exists", identifierType, identifier);
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getIdentifierType() {
        return identifierType;
    }
}
