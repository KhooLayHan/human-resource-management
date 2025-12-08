package org.bhel.hrm.common.exceptions;

import org.bhel.hrm.common.error.ErrorCode;

/**
 * A {@link DataAccessException} thrown when the SQL sent to the database
 * has incorrect syntax.
 */
public final class IncorrectSqlGrammarException extends DataAccessException {
    /**
     * Constructs a new IncorrectSqlGrammarException with the specified detail message and cause.
     *
     * @param message The detail message explaining the error of incorrect SQL grammar
     * @param cause The underlying cause of the incorrect SQL grammar
     */
    public IncorrectSqlGrammarException(String message, Throwable cause) {
        super(ErrorCode.DB_QUERY_ERROR, message, cause);
    }

    public IncorrectSqlGrammarException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
