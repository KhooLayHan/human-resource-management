package org.bhel.hrm.common.error;

import org.bhel.hrm.common.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Configuration-driven mapping of database errors to application exceptions.
 * At the moment, only supports MySQL database vendor and custom mapping rules.
 */
public class ExceptionMappingConfig {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMappingConfig.class);

    public enum DatabaseVendor { MYSQL, POSTGRESQL }

    private final Map<Integer, ExceptionMapping> errorMappings;
    private final Map<String, ContextBasedMapping> contextMappings;
    private final DatabaseVendor vendor;

    public ExceptionMappingConfig() {
        this(DatabaseVendor.MYSQL);
    }

    public ExceptionMappingConfig(DatabaseVendor vendor) {
        this.vendor = vendor;
        this.errorMappings = new HashMap<>();
        this.contextMappings = new HashMap<>();

        initializeMappings();
    }

    private void initializeMappings() {
        switch (vendor) {
            case MYSQL -> initializeMySQLMappings();
            case POSTGRESQL -> initializePostGreSQLMappings();
        }
    }

    private void initializeMySQLMappings() {
        // --- 1. Connection / Resource Failure Errors ---

        // Access denied error
        addMapping(
            1045,
            ErrorCode.AUTH_INVALID_CREDENTIALS,
            (msg, ex) -> new DataAccessResourceFailureException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                msg,
                ex
            )
        );

        // Connection failure error
        addMapping(
            2013,
            ErrorCode.DB_CONNECTION_FAILED,
            DataAccessResourceFailureException::new
        );

        // --- 2. Data Integrity / Constraint Violation Errors ---

        // Duplicate entry
        addMapping(
            1062,
            ErrorCode.DB_DUPLICATE_ENTRY,
            DataIntegrityViolationException::new
        );

        // Foreign key constraint fails
        addMapping(
            1451,
            ErrorCode.DB_FOREIGN_KEY_VIOLATION,
            (msg, ex) -> new DataIntegrityViolationException(
                ErrorCode.DB_FOREIGN_KEY_VIOLATION,
                msg,
                ex
            )
        );

        addMapping(
            1452,
            ErrorCode.DB_FOREIGN_KEY_VIOLATION,
            (msg, ex) -> new DataIntegrityViolationException(
                ErrorCode.DB_FOREIGN_KEY_VIOLATION,
                msg,
                ex
            )
        );

        // Column cannot be null / missing defaults
        addMapping(
            1048,
            ErrorCode.DB_COLUMN_IS_NULL,
            (msg, ex) -> new DataIntegrityViolationException(
                ErrorCode.DB_COLUMN_IS_NULL,
                msg,
                ex
            )
        );

        addMapping(
            1364,
            ErrorCode.DB_COLUMN_IS_NULL,
            (msg, ex) -> new DataIntegrityViolationException(
                ErrorCode.DB_COLUMN_IS_NULL,
                msg,
                ex
            )
        );

        // --- 3. MySQL Grammar Errors ---

        // Syntax error
        addMapping(
            1064,
            ErrorCode.DB_QUERY_ERROR,
            IncorrectSqlGrammarException::new
        );

        // --- 4. Locking Errors ---

        // Deadlock found
        addMapping(
            1213,
            ErrorCode.DB_DEADLOCK,
            CannotAcquireLockException::new
        );

        // Lock wait timeout
        addMapping(
            1205,
            ErrorCode.DB_LOCK_TIMEOUT,
            (msg, ex) -> new CannotAcquireLockException(
                ErrorCode.DB_LOCK_TIMEOUT,
                msg,
                ex
            )
        );

        // --- 5. Context-specific Errors ---
        addContextMapping(
            "registration",
            1062,
            ErrorCode.USER_ALREADY_EXISTS,
            (msg, ex) -> new DataIntegrityViolationException(
                ErrorCode.USER_ALREADY_EXISTS,
                msg,
                ex
            )
        );

        addContextMapping(
            "employee.create",
            1062,
            ErrorCode.EMPLOYEE_DUPLICATE_ID,
            (msg, ex) -> new DataIntegrityViolationException(
                ErrorCode.EMPLOYEE_DUPLICATE_ID,
                msg,
                ex
            )
        );

        addContextMapping(
            "employee.delete",
            1451,
            ErrorCode.EMPLOYEE_HAS_DEPENDENCIES,
            (msg, ex) -> new DataIntegrityViolationException(
                ErrorCode.EMPLOYEE_HAS_DEPENDENCIES,
                msg,
                ex
            )
        );
    }

    private void initializePostGreSQLMappings() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public DataAccessException translate(SQLException ex, ErrorContext context) {
        int errorCode = ex.getErrorCode();
        String operation = context.getOperation();

        // First try context-specific first
        ContextBasedMapping contextMapping = findContextMapping(operation, errorCode);
        if (contextMapping != null) {
            return contextMapping.createException(
                contextMapping.errorCode.getDefaultMessage(), ex
            );
        }

        // Fallback to general error code mapping
        ExceptionMapping mapping = errorMappings.get(errorCode);
        if (mapping != null) {
            return mapping.createException(
                mapping.errorCode.getDefaultMessage() + " during " + operation,
                ex
            );
        }

        // Default fallback
        return new DataAccessException(
            ErrorCode.SYSTEM_ERROR.getDefaultMessage() + " during " + operation,
            ex
        );
    }

    // Adds a custom mapping for a specific error code
    public void addMapping(
        int errorCode,
        ErrorCode appErrorCode,
        BiFunction<String, SQLException, DataAccessException> factory
    ) {
        errorMappings.put(
            errorCode,
            new ExceptionMapping(appErrorCode, factory)
        );
    }

    // Adds a context-specific error code
    public void addContextMapping(
        String context,
        int dbErrorCode,
        ErrorCode appErrorCode,
        BiFunction<String, SQLException, DataAccessException> factory
    ) {
        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException("Context cannot be null or empty");
        }

        String normalizedContext = context.toLowerCase();

        // Warns about potential substring conflicts
        for (String existingContext : contextMappings.keySet()) {
            if (
                normalizedContext.contains(existingContext) ||
                existingContext.contains(normalizedContext)
            ) {
                logger.warn(
                    "Context '{}' may conflict with existing context '{}'. " +
                    "Ensure this is intentional. Longest match will be used.",
                    normalizedContext, existingContext
                );
            }
        }

        contextMappings.put(
            normalizedContext,
            new ContextBasedMapping(dbErrorCode, appErrorCode, factory)
        );
    }

    private ContextBasedMapping findContextMapping(String operation, int errorCode) {
        if (operation == null)
            return null;

        String key = operation.toLowerCase();

        // Step 1: Try exact match first (highest priority)
        ContextBasedMapping exactMatch = contextMappings.get(key);
        if (exactMatch != null && exactMatch.dbErrorCode == errorCode) {
            logger.debug("Found exact context match: '{}'", key);
            return exactMatch;
        }

        // Step 2: Find all matching contexts and sort by length (longest first)
        List<Map.Entry<String, ContextBasedMapping>> matches = new ArrayList<>();

        for (Map.Entry<String, ContextBasedMapping> entry : contextMappings.entrySet()) {
            String contextKey = entry.getKey();
            ContextBasedMapping mapping = entry.getValue();

            // Check if this context is a prefix of the operation AND error code matches
            if (key.startsWith(contextKey) && mapping.dbErrorCode == errorCode) {
                matches.add(entry);
            }
        }

        // If no matches found, return null
        if (matches.isEmpty()) {
            logger.debug("No context match found for operation='{}', errorCode={}", operation, errorCode);
            return null;
        }

        // Sort by key length (longest first) for most specific match
        // If lengths are equal, sort alphabetically for determinism
        matches.sort((e1, e2) -> {
            int lengthCompare = Integer.compare(
                e2.getKey().length(),
                e1.getKey().length()
            );

            if (lengthCompare != 0) {
                return lengthCompare;
            }

            // Same length - sort alphabetically for determinism
            return e1.getKey().compareTo(e2.getKey());
        });

        // Return the longest (most specific) match
        Map.Entry<String, ContextBasedMapping> bestMatch = matches.getFirst();
        logger.debug("Found prefix match: '{}' for operation '{}'",
            bestMatch.getKey(), operation);

        return bestMatch.getValue();
    }

    private record ExceptionMapping(
        ErrorCode errorCode,
        BiFunction<String, SQLException, DataAccessException> exceptionFactory
    ) {
        DataAccessException createException(String message, SQLException ex) {
            return exceptionFactory.apply(message, ex);
        }
    }

    private record ContextBasedMapping(
        int dbErrorCode,
        ErrorCode errorCode,
        BiFunction<String, SQLException, DataAccessException> exceptionFactory
    ) {
        DataAccessException createException(String message, SQLException ex) {
            return exceptionFactory.apply(message, ex);
        }
    }
}
