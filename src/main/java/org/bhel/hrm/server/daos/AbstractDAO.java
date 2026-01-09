package org.bhel.hrm.server.daos;

import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An abstract base class for DAOs using the Template Method Pattern to encapsulate
 * common JDBC operations. This class handles boilerplate code for resource management
 * (connections, statements) and exception handling, allowing concrete DAO implementations
 * to focus solely on their specific SQL and parameter mapping logic.
 *
 * @param <T> The type of the domain entity this DAO manages.
 */
public abstract class AbstractDAO<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractDAO.class);

    protected final DatabaseManager dbManager;

    protected AbstractDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * A functional interface that defines the contract for mapping a row from a
     * {@link ResultSet} to an object of type R.
     * @param <R> The type of the object to be mapped.
     */
    @FunctionalInterface
    protected interface RowMapper<R> {
        R mapRow(ResultSet result) throws SQLException;
    }

    /**
     * A functional interface that defines the contract for setting parameter
     * values on a {@link PreparedStatement}.
     */
    @FunctionalInterface
    protected interface StatementSetter {
        void setValues(PreparedStatement stmt) throws SQLException;
    }

    /**
     * Template method for executing a query expected to return a single entity.
     *
     * @param sql The SQL query to execute.
     * @param setter A lambda expression to set the query parameters on the PreparedStatement.
     * @param mapper A lambda expression to map the ResultSet row to an entity.
     * @return An {@link Optional} containing the entity if found, otherwise an empty Optional.
     */
    protected Optional<T> findOne(String sql, StatementSetter setter, RowMapper<T> mapper) {
        List<T> results = findMany(sql, setter, mapper);

        return results.isEmpty()
            ? Optional.empty()
            : Optional.of(results.getFirst());
    }

    /**
     * Template method for executing a query expected to return a list of entities.
     *
     * @param sql The SQL query to execute.
     * @param setter A lambda expression to set the query parameters on the PreparedStatement.
     * @param mapper A lambda expression to map each ResultSet row to an entity.
     * @return A {@link List} of entities, which may be empty if no results are found.
     */
    protected List<T> findMany(String sql, StatementSetter setter, RowMapper<T> mapper) {
        List<T> results = new ArrayList<>();
        Connection conn = null;

        try {
            conn = dbManager.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setter.setValues(stmt);

                try (ResultSet result = stmt.executeQuery()) {
                    while (result.next())
                        results.add(mapper.mapRow(result));
                }

                logger.info("{}", stmt);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error executing query: " + sql, e);
        } finally {
            dbManager.releaseConnection(conn);
        }

        return results;
    }

    /**
     * Template method for executing an INSERT, UPDATE, or DELETE statement.
     *
     * @param sql The SQL statement to execute.
     * @param setter A lambda expression to set the parameters on the PreparedStatement.
     */
    protected void executeUpdate(String sql, StatementSetter setter) {
        Connection conn = null;

        try {
            conn = dbManager.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setter.setValues(stmt);
                stmt.executeUpdate();

                logger.info("{}", stmt);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error executing update: " + sql, e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Inserts a new entity into the data store.
     *
     * @param entity The entity to be inserted; must not be null.
     */
    protected abstract void insert(T entity);

    /**
     * Updates an existing entity in the data store.
     *
     * @param entity The entity to be updated; must not be null and must exist in the data store.
     */
    protected abstract void update(T entity);

    /**
     * An abstract "hook" method, part of the Template Method Pattern for saving entities.
     * Concrete DAO implementations must provide the logic for mapping an entity's fields
     * to the parameters of a PreparedStatement for an INSERT or UPDATE operation.
     *
     * @param stmt The PreparedStatement to which parameters will be set.
     * @param entity The entity containing the data to be saved.
     * @throws SQLException if a database access error occurs.
     */
    protected abstract void setSaveParameters(PreparedStatement stmt, T entity) throws SQLException;

    // In AbstractDAO<T>
    protected long countFromTable(String tableName) {
        String sql = "SELECT COUNT(*) AS total FROM " + tableName;
        return queryForLong(sql, "total", "Error counting rows in " + tableName);
    }

    protected long queryForLong(String sql, String column, String errorMessage) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(column) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Executes an INSERT and returns the generated key as int.
     * Throws DataAccessException if no rows affected or no key returned.
     */
    protected int executeInsertReturningId(String sql,
                                           SqlConsumer<PreparedStatement> binder,
                                           String noRowsMessage,
                                           String noIdMessage,
                                           String errorMessage) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                binder.accept(stmt);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new DataAccessException(noRowsMessage, null);
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                    throw new DataAccessException(noIdMessage, null);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /** Small functional interface so we can pass lambdas that throw SQLException */
    @FunctionalInterface
    protected interface SqlConsumer<P> {
        void accept(P p) throws SQLException;
    }

}
