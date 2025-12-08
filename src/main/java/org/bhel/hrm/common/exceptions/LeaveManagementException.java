package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * Thrown for any business rule violation related to leave management,
 * such as insufficient leave balance or overlapping leave dates.
 * <p>
 * This exception covers domain-specific errors in leave processing.
 */
public final class LeaveManagementException extends HRMException {
    public enum LeaveViolationType {
        INSUFFICIENT_BALANCE,
        OVERLAPPING_DATES,
        INVALID_DATE_RANGE,
        CANNOT_CANCEL,
        UNKNOWN
    }

    private final String employeeId;
    private final LeaveViolationType violationType;

    /**
     * Constructs a LeaveManagementException with specific error code.
     *
     * @param errorCode The specific leave management error code
     * @param message The detail message
     */
    public LeaveManagementException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.employeeId = null;
        this.violationType = LeaveViolationType.UNKNOWN;
    }

    /**
     * Constructs a LeaveManagementException with employee and violation details.
     *
     * @param employeeId The ID of the employee
     * @param violationType The type of leave rule violation
     * @param message The detail message
     */
    public LeaveManagementException(String employeeId, LeaveViolationType violationType, String message) {
        super(
            mapViolationToErrorCode(violationType),
            formatMessage(employeeId, message)
        );
        this.employeeId = employeeId;
        this.violationType = violationType;
    }

    /**
     * Constructs a LeaveManagementException with context.
     *
     * @param errorCode The specific leave management error code
     * @param message The detail message
     * @param context The error context
     */
    public LeaveManagementException(ErrorCode errorCode, String message, ErrorContext context) {
        super(errorCode, message, context);
        this.employeeId = null;
        this.violationType = LeaveViolationType.UNKNOWN;
    }

    /**
     * Constructs a LeaveManagementException with employee, violation, and context.
     *
     * @param employeeId The ID of the employee
     * @param violationType The type of leave rule violation
     * @param message The detail message
     * @param context The error context
     */
    public LeaveManagementException(
        String employeeId,
        LeaveViolationType violationType,
        String message,
        ErrorContext context
    ) {
        super(
            mapViolationToErrorCode(violationType),
            formatMessage(employeeId, message),
            context
        );
        this.employeeId = employeeId;
        this.violationType = violationType;
    }

    public static ErrorCode mapViolationToErrorCode(LeaveViolationType violationType) {
        if (violationType == null)
            violationType = LeaveViolationType.UNKNOWN;

        return switch (violationType) {
            case INSUFFICIENT_BALANCE -> ErrorCode.LEAVE_INSUFFICIENT_BALANCE;
            case OVERLAPPING_DATES -> ErrorCode.LEAVE_OVERLAPPING_DATES;
            case INVALID_DATE_RANGE -> ErrorCode.LEAVE_INVALID_DATE_RANGE;
            case CANNOT_CANCEL -> ErrorCode.LEAVE_CANNOT_CANCEL;
            case UNKNOWN -> ErrorCode.SYSTEM_ERROR;
        };
    }

    private static String formatMessage(
        String employeeId,
        String message
    ) {
        String id = employeeId != null ? employeeId : "UNKNOWN";
        String msg = message != null ? message : "No details provided";

        return String.format("Leave violation for employee %s: %s",
            id, msg);
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public LeaveViolationType getViolationType() {
        return violationType;
    }
}
