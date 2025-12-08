package org.bhel.hrm.common.error;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized error response object for API/RMI responses.
 * Provides consistent error information to clients.
 */
public class ErrorResponse implements Serializable {
    private final String errorId;
    private final String errorCode;
    private final String message;
    private final LocalDateTime timestamp;
    private final int httpStatus;
    private final Map<String, Object> details;

    public ErrorResponse(String errorId, ErrorCode errorCode, String message, LocalDateTime timestamp) {
        this.errorId = errorId;
        this.errorCode = errorCode.getCode();
        this.message = message;
        this.timestamp = timestamp;
        this.httpStatus = errorCode.getHttpStatus();
        this.details = new HashMap<>();
    }

    public String getErrorId() {
        return errorId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    public ErrorResponse addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    public ErrorResponse addValidationErrors(Map<String, String> validationErrors) {
        this.details.put("validationErrors", validationErrors);
        return this;
    }

    @Override
    public String toString() {
        return String.format(
            "ErrorResponse{errorId='%s', errorCode='%s', message='%s', timestamp=%s, httpStatus=%d, details=%s}",
            errorId, errorCode, message, timestamp, httpStatus, details
        );
    }

    /**
     * ErrorResponseBuilder for ErrorResponse.
     */
    public static class ErrorResponseBuilder {
        private String errorId;
        private ErrorCode errorCode;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, Object> details;

        public ErrorResponseBuilder() {
            this.timestamp = LocalDateTime.now();
            this.details = new HashMap<>();
        }

        public ErrorResponseBuilder errorId(String errorId) {
            this.errorId = errorId;
            return this;
        }

        public ErrorResponseBuilder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder addDetail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public ErrorResponse build() {
            ErrorResponse response = new ErrorResponse(errorId, errorCode, message, timestamp);
            response.details.putAll(this.details);
            return response;
        }
    }
}
