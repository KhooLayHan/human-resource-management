package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * Thrown when the data provided by the client fails a business validation rule
 * (e.g., a leave application with a start date after the end date).
 * <p>
 * This exception indicates invalid input that violates domain constraints.
 */
public final class InvalidInputException extends HRMException {
    private final String fieldName;
    private final Object invalidValue;

    /**
     * Constructs an InvalidInputException with the default validation error code.
     *
     * @param message The detail message explaining the validation failure
     */
    public InvalidInputException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * Constructs an InvalidInputException with field information.
     *
     * @param fieldName The name of the field that failed validation
     * @param invalidValue The invalid value that was provided
     * @param message The validation error message
     */
    public InvalidInputException(String fieldName, Object invalidValue, String message) {
        super(
            ErrorCode.VALIDATION_FAILED,
            formatMessage(fieldName, message)
        );
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    /**
     * Constructs an InvalidInputException with context.
     *
     * @param message The validation error message
     * @param context The error context
     */
    public InvalidInputException(String message, ErrorContext context) {
        super(ErrorCode.VALIDATION_FAILED, message, context);
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * Constructs an InvalidInputException with error code and context.
     *
     * @param errorCode The specific validation error code
     * @param message The validation error message
     * @param context The error context
     */
    public InvalidInputException(ErrorCode errorCode, String message, ErrorContext context) {
        super(errorCode, message, context);
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * Constructs an InvalidInputException with field information and context.
     *
     * @param fieldName The name of the field that failed validation
     * @param invalidValue The invalid value that was provided
     * @param message The validation error message
     * @param context The error context
     */
    public InvalidInputException(
        String fieldName,
        Object invalidValue,
        String message,
        ErrorContext context
    ) {
        super(
            ErrorCode.VALIDATION_FAILED,
            formatMessage(fieldName, message),
            context
        );
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    private static String formatMessage(String fieldName, String message) {
        return String.format("Invalid value for field '%s': %s", fieldName, message);
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }
}
