package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.error.ErrorContext;

/**
 * A generic exception for when an employee fails to enroll in something,
 * such as a training course or a benefit plan.
 * <p>
 * This exception is used to signal business-level enrollment failures.
 */
public final class EnrollmentException extends HRMException {
    public enum EnrollmentFailureReason {
        CAPACITY_FULL,
        PREREQUISITES_NOT_MET,
        ALREADY_ENROLLED,
        ELIGIBILITY_CRITERIA_NOT_MET,
        ENROLLMENT_PERIOD_CLOSED,
        UNKNOWN,
    }

    private final String employeeId;
    private final String enrollmentTarget;
    private final EnrollmentFailureReason reason;

    /**
     * Constructs an EnrollmentException with basic message.
     *
     * @param message The detail message explaining the enrollment failure
     */
    public EnrollmentException(String message) {
        super(ErrorCode.SYSTEM_ERROR, message);
        this.employeeId = null;
        this.enrollmentTarget = null;
        this.reason = EnrollmentFailureReason.UNKNOWN;
    }

    /**
     * Constructs an EnrollmentException with employee and target information.
     *
     * @param employeeId The ID of the employee attempting enrollment
     * @param enrollmentTarget The target being enrolled in (e.g., course name, benefit plan)
     * @param reason The reason for enrollment failure
     * @param message Additional details about the failure
     */
    public EnrollmentException(
        String employeeId,
        String enrollmentTarget,
        EnrollmentFailureReason reason,
        String message
    ) {
        super(
            ErrorCode.SYSTEM_ERROR,
            formatMessage(employeeId, enrollmentTarget, reason, message)
        );
        this.employeeId = employeeId;
        this.enrollmentTarget = enrollmentTarget;
        this.reason = reason;
    }

    /**
     * Constructs an EnrollmentException with context.
     *
     * @param employeeId The ID of the employee attempting enrollment
     * @param enrollmentTarget The target being enrolled in
     * @param reason The reason for enrollment failure
     * @param message Additional details about the failure
     * @param context The error context
     */
    public EnrollmentException(
        String employeeId,
        String enrollmentTarget,
        EnrollmentFailureReason reason,
        String message,
        ErrorContext context
    ) {
        super(
            ErrorCode.SYSTEM_ERROR,
            formatMessage(employeeId, enrollmentTarget, reason, message),
            context
        );
        this.employeeId = employeeId;
        this.enrollmentTarget = enrollmentTarget;
        this.reason = reason;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEnrollmentTarget() {
        return enrollmentTarget;
    }

    public EnrollmentFailureReason getReason() {
        return reason;
    }

    private static String formatMessage(
        String employeeId,
        String enrollmentTarget,
        EnrollmentFailureReason reason,
        String message
    ) {
        return String.format("Employee %s failed to enroll in %s: %s - %s",
            employeeId, enrollmentTarget, reason, message);
    }
}
