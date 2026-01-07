package org.bhel.hrm.common.error;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Structured context information for errors.
 * Provides rich diagnostic information for logging, monitoring, and debugging.
 */
public class ErrorContext implements Serializable {
    private final String errorId;
    private final LocalDateTime timestamp;
    private final String operation;
    private final String userId;
    private final String sessionId;
    private final String ipAddress;
    private final Map<String, Object> additionalData;

    private ErrorContext(ErrorContextBuilder builder) {
        this.errorId = builder.errorId;
        this.timestamp = builder.timestamp;
        this.operation = builder.operation;
        this.userId = builder.userId;
        this.sessionId = builder.sessionId;
        this.ipAddress = builder.ipAddress;
        this.additionalData = builder.additionalData;
    }

    public String getErrorId() {
        return errorId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getOperation() {
        return operation;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData == null ? new HashMap<>() : new HashMap<>(additionalData);
    }

    public Object getAdditionalData(String key) {
        return additionalData.get(key);
    }

    @Override
    public String toString() {
        return String.format(
            "ErrorContext{errorId='%s', timestamp=%s, operation='%s', userId='%s', sessionId='%s', ipAddress='%s', additionalData=%s}",
            errorId, timestamp, operation, userId, sessionId, ipAddress, additionalData
        );
    }

    /**
     * Creates a new ErrorResponseBuilder instance.
     *
     * @return A builder object.
     */
    public static ErrorContextBuilder builder() {
        return new ErrorContextBuilder();
    }

    /**
     * ErrorResponseBuilder for ErrorContext.
     */
    public static class ErrorContextBuilder {
        private String errorId;
        private final LocalDateTime timestamp;
        private String operation;
        private String userId;
        private String sessionId;
        private String ipAddress;
        private final Map<String, Object> additionalData;

        private ErrorContextBuilder() {
            this.errorId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
            this.additionalData = new HashMap<>();
        }

        public ErrorContextBuilder errorId(String errorId) {
            this.errorId = errorId;
            return this;
        }

        public ErrorContextBuilder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public ErrorContextBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public ErrorContextBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public ErrorContextBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public ErrorContextBuilder addData(String key, Object value) {
            this.additionalData.put(key, value);
            return this;
        }

        public ErrorContextBuilder addData(Map<String, Object> data) {
            this.additionalData.putAll(data);
            return this;
        }

        public ErrorContext build() {
            return new ErrorContext(this);
        }
    }

    /**
     * Factory method for common use cases.
     *
     * @param operation The particular operation.
     * @return A static ErrorContext with operation field.
     */
    public static ErrorContext forOperation(String operation) {
        return builder()
            .operation(operation)
            .build();
    }

    /**
     * Factory method with user context.
     *
     * @param operation The particular operation.
     * @param userId The user's ID.
     * @return A static ErrorContext with user context.
     */
    public static ErrorContext forUser(String operation, String userId) {
        return builder()
            .operation(operation)
            .userId(userId)
            .build();
    }
}
