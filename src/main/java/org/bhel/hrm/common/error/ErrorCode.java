package org.bhel.hrm.common.error;

/**
 * Centralized error codes for the HRM application.
 * Format: MODULE-CATEGORY-SEQUENCE
 * Example: HRM-AUTH-001
 */
public enum ErrorCode {
    // --- Authentication & Authorization ---

    AUTH_INVALID_CREDENTIALS("HRM-AUTH-001", "Invalid username or password", 401),
    AUTH_TOKEN_EXPIRED("HRM-AUTH-002", "Authenticated token has expired", 401),
    AUTH_INSUFFICIENT_PERMISSIONS("HRM-AUTH-003", "Insufficient permissions to perform this action", 403),
    AUTH_ACCOUNT_LOCKED("HRM-AUTH-004", "Account has been locked due to multiple failed attempts", 403),

    // --- User Management (User) ---

    USER_NOT_FOUND("HRM-USER-001", "User not found", 404),
    USER_ALREADY_EXISTS("HRM-USER-002", "User with this email or employee ID already exists", 409),
    USER_INVALID_INPUT("HRM-USER-003", "Invalid user data provided", 400),
    USER_CANNOT_SELF_DELETE("HRM-USER-004", "Cannot delete your own account", 400),

    // --- User Management (Employee) ---

    EMPLOYEE_NOT_FOUND("HRM-EMP-001", "Employee not found", 404),
    EMPLOYEE_DUPLICATE_ID("HRM-EMP-002", "Employee ID already exists", 409),
    EMPLOYEE_INVALID_DEPARTMENT("HRM-EMP-003", "Invalid department assignment", 400),
    EMPLOYEE_HAS_DEPENDENCIES("HRM-EMP-004", "Cannot delete employee with active records", 409),

    // --- Leave Management ---

    LEAVE_INSUFFICIENT_BALANCE("HRM-LEAVE-001", "Insufficient leave balance", 400),
    LEAVE_OVERLAPPING_DATES("HRM-LEAVE-002", "Leave dates overlap with existing request", 409),
    LEAVE_INVALID_DATE_RANGE("HRM-LEAVE-003", "Invalid date range for leave request", 400),
    LEAVE_CANNOT_CANCEL("HRM-LEAVE-004", "Cannot cancel leave request in current status", 400),

    // --- Payroll ---

    PAYROLL_ALREADY_PROCESSED("HRM-PAY-001", "Payroll for this period has already been processed", 409),
    PAYROLL_INVALID_PERIOD("HRM-PAY-002", "Invalid payroll period", 400),
    PAYROLL_CALCULATION_ERROR("HRM-PAY-003", "Error calculating payroll", 500),

    // --- Database ---

    DB_CONNECTION_FAILED("HRM-DB-001", "Database connection failed", 503),
    DB_COLUMN_IS_NULL("HRM-DB-002", "Column entry violates null constraint ", 409),
    DB_DUPLICATE_ENTRY("HRM-DB-003", "Duplicate entry violates unique constraint", 409),
    DB_FOREIGN_KEY_VIOLATION("HRM-DB-004", "Operation violates referential integrity", 409),
    DB_DEADLOCK("HRM-DB-005", "Database deadlock detected", 409),
    DB_LOCK_TIMEOUT("HRM-DB-006", "Database lock timeout", 408),
    DB_QUERY_ERROR("HRM-DB-007", "Database query error", 500),

    // --- Validation ---

    VALIDATION_FAILED("HRM-VAL-001", "Input validation failed", 400),
    VALIDATION_REQUIRED_FIELD("HRM-VAL-002", "Required field is missing", 400),
    VALIDATION_INVALID_FORMAT("HRM-VAL-003", "Invalid data format", 400),
    VALIDATION_OUT_OF_RANGE("HRM-VAL-004", "Value is out of acceptable range", 400),

    // --- Security ---
    SECURITY_SSL_INITIALIZATION_FAILED("HRM-SEC-001", "SSL context initialization failed", 500),

    SECURITY_KEYSTORE_NOT_CONFIGURED("HRM-SEC-002", "Keystore configuration missing or invalid", 500),
    SECURITY_KEYSTORE_NOT_FOUND("HRM-SEC-003", "Keystore file not found", 500),
    SECURITY_KEYSTORE_LOAD_FAILED("HRM-SEC-004", "Keystore loading failed", 500),

    SECURITY_KEY_MANAGER_INIT_FAILED("HRM-SEC-005", "KeyManager initialization failed", 500),
    SECURITY_TRUST_MANAGER_INIT_FAILED("HRM-SEC-006", "TrustManager initialization failed", 500),

    SECURITY_ENCRYPTION_FAILED("HRM-SEC-007", "Data encryption failed", 500),
    SECURITY_DECRYPTION_FAILED("HRM-SEC-008", "Data encryption failed", 500),
    SECURITY_INVALID_ENCRYPTED_DATA("HRM-SEC-009", "Data encryption corrupted or failed", 400),

    SECURITY_KEY_NOT_CONFIGURED("HRM-SEC-010", "Encryption key not configured", 500),
    SECURITY_HANDSHAKE_FAILED("HRM-SEC-011", "SSL/TLS handshake failed", 500),
    SECURITY_CERTIFICATE_INVALID("HRM-SEC-012", "Invalid or expired certificate", 500),

    // --- Network Communication ---

    NETWORK_CONNECTION_FAILED("HRM-NET-001", "Network connection failed to establish", 503),
    NETWORK_TIMEOUT("HRM-NET-002", "Network operation timed out", 408),
    NETWORK_IO_ERROR("HRM-NET-003", "Network I/O error occurred", 500),
    NETWORK_NO_RESPONSE("HRM-NET-004", "No response received from remote server", 504),
    NETWORK_SERVER_START_FAILED("HRM-NET-005", "Network server failure", 500),
    NETWORK_CONNECTION_REFUSED("HRM-NET-006", "Connection refused by remote server", 503),
    NETWORK_HOST_UNREACHABLE("HRM-NET-007", "Remote host is unreachable", 503),

    // --- System ---

    SYSTEM_ERROR("HRM-SYS-001", "An unexpected system error occurred", 500),
    SYSTEM_SERVICE_UNAVAILABLE("HRM-SYS-002", "Service temporarily unavailable", 503),
    SYSTEM_MAINTENANCE("HRM-SYS-003", "System is under maintenance", 503),
    SYSTEM_TIMEOUT("HRM-SYS-004", "Operation timed out", 408),

    // --- File Operations ---

    FILE_NOT_FOUND("HRM-FILE-001", "File not found", 404),
    FILE_UPLOAD_FAILED("HRM-FILE-002", "File upload failed", 500),
    FILE_INVALID_TYPE("HRM-FILE-003", "Invalid file type", 400),
    FILE_SIZE_EXCEEDED("HRM-FILE-004", "File size exceeds maximum length", 413);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    ErrorCode(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Retrieves a formatted error code with a message.
     *
     * @return A formatted message string.
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s", code, defaultMessage);
    }

    /**
     * Finds the ErrorCode by the code string.
     *
     * @param code The error code to find
     * @return A static ErrorCode
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code))
                return errorCode;
        }

        return SYSTEM_ERROR;
    }

    @Override
    public String toString() {
        return code + ": " + defaultMessage;
    }
}
