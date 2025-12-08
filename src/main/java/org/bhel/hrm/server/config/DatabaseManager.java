package org.bhel.hrm.server.config;

import org.bhel.hrm.common.exceptions.HRMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final Configuration config;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    public DatabaseManager(Configuration config) {
        this.config = config;
        initializeDatabase();
    }

    /**
     * Gets a connection. If a transaction is active on the current thread,
     * returns the transaction's connection; otherwise, returns a new connection.
     *
     * @return A database connection; never null
     * @throws SQLException If a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        Connection conn = transactionConnection.get();
        if (conn != null)
            return conn; // Returns an existing transaction connection

        // Returns a new connection for a single, non-transactional operation
        return DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());
    }

    /**
     * A functional interface representing code that should be
     * executed within a single database transaction.
     */
    @FunctionalInterface
    public interface TransactionalTask {
        /**
         * Executes the transactional work.
         *
         * @throws HRMException If a business rule or data validation error occurs
         */
        void execute() throws HRMException;
    }

    /**
     * Executes a given task within a managed database transaction.
     * Handles connection lifecycle, commit, and rollback.
     *
     * @param task The block of code to execute transactionally; must not be null
     * @throws SQLException If a database error occurs during transaction management
     * @throws HRMException If the task throws an HRM specific exception.
     */
    public void executeInTransaction(TransactionalTask task) throws SQLException, HRMException {
        beginTransaction();
        try {
            task.execute();
            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
            switch (e) {
                case HRMException hrmException -> throw hrmException;
                case SQLException sqlException -> throw sqlException;
                default -> throw new HRMException("Unexpected error in transaction", e);
            }
        }
    }

    /**
     * Starts a new transaction on the current thread.
     *
     * @throws SQLException If a transaction is already active or connection fails
     */
    public void beginTransaction() throws SQLException {
        if (transactionConnection.get() != null)
            throw new SQLException("Transaction is already active on this thread.");

        Connection conn = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());
        try {
            conn.setAutoCommit(false);

            transactionConnection.set(conn);
            logger.debug("Transaction started for Thread [{}]", Thread.currentThread().getName());
        } catch (SQLException e) {
            try {
                conn.close();
            } catch (SQLException suppressed) {
                logger.warn("Error closing tx connection after begin failure.", suppressed);
            }
        }
    }

    /**
     * Commits the active transaction.
     *
     * @throws SQLException If a database error occurs during commit
     */
    public void commitTransaction() throws SQLException {
        Connection conn = transactionConnection.get();

        if (conn != null) {
            try {
                conn.commit();
                logger.debug("Transaction committed for Thread [{}]", Thread.currentThread().getName());
            } finally {
                closeTransactionConnection();
            }
        }
    }

    /**
     * Rolls back the active transaction if one is in progress.
     */
    public void rollbackTransaction() {
        Connection conn = transactionConnection.get();

        if (conn != null) {
            try {
                conn.rollback();
                logger.warn("Transaction rolled back for Thread [{}]", Thread.currentThread().getName());
            } catch (SQLException e) {
                logger.error("Error during transaction rollback.", e);
            } finally {
                closeTransactionConnection();
            }
        }
    }

    /**
     * Releases a non-transactional connection.
     *
     * @param conn The connection to release; may be null
     */
    public void releaseConnection(Connection conn) {
        Connection tx = transactionConnection.get();

        if (conn != null && conn != tx) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("Error closing connection.", e);
            }
        }
    }

    /**
     * Checks if a transaction is active on the current thread.
     *
     * @return {@code true} if a transaction is active, false otherwise
     */
    public boolean isTransactionActive() {
        return transactionConnection.get() != null;
    }

    /**
     * Closes the transactional connection and removes it from ThreadLocal.
     */
    private void closeTransactionConnection() {
        Connection conn = transactionConnection.get();

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("Error closing transaction connection.", e);
            } finally {
                transactionConnection.remove(); // Cleans up the ThreadLocal
            }
        }
    }

    /**
     * Initializes the database schema.
     */
    private void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            createHRMTables(stmt);
            logger.info("Database connection successful. Database schema initialized successfully.");
        } catch (SQLException e) {
            logger.error("FATAL: Database schema initialization failed!");
            logger.error(e.toString());
        }
    }

    /**
     * Creates all HRM (Human Resource Management) database tables and populates lookup tables with initial data.
     * <p>
     * This method creates the following table groups in order:
     * <ol>
     *   <li>User authentication tables: {@code user_roles}, {@code users}</li>
     *   <li>Employee information table: {@code employees}</li>
     *   <li>Leave management tables: {@code leave_application_types}, {@code leave_application_statuses}, {@code leave_applications}</li>
     *   <li>Training courses table: {@code training_courses}</li>
     *   <li>Benefits management table: {@code benefit_plans}</li>
     *   <li>Recruitment tables: {@code job_opening_statuses}, {@code job_openings}, {@code applicant_statuses}, {@code applicants}</li>
     * </ol>
     *
     * All tables use the {@code IF NOT EXISTS} clause to allow safe re-execution.
     * Lookup tables are populated with predefined values using {@code INSERT IGNORE} to prevent duplicate entries.
     *
     * @param stmt The SQL Statement object used to execute DDL commands
     * @throws SQLException If a database access error occurs or table creation fails
     */
    private void createHRMTables(Statement stmt) throws SQLException {
        // 1. Users and UserRoles table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS user_roles (
                id TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(20) NOT NULL UNIQUE,
                sort_order TINYINT NOT NULL DEFAULT 0
            )
        """);

        stmt.execute("""
            INSERT IGNORE INTO user_roles (id, name, sort_order) VALUES
            (1, 'hr_staff', 1),
            (2, 'employee', 2)
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role_id TINYINT UNSIGNED NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                CONSTRAINT fk_users_role_id
                    FOREIGN KEY (role_id) REFERENCES user_roles(id)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT
            );
        """);

        // 2. Employees Table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS employees (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                first_name VARCHAR(255) NOT NULL,
                last_name VARCHAR(255) NOT NULL,
                ic_passport VARCHAR(255) NOT NULL UNIQUE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                CONSTRAINT uk_employees_first_last UNIQUE (first_name, last_name),

                CONSTRAINT fk_employees_employee_id
                    FOREIGN KEY (user_id) REFERENCES users(id)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT
            )
        """);

        // 3. LeaveApplications, LeaveApplicationTypes, and LeaveApplicationStatuses Table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS leave_application_types (
                id TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(20) NOT NULL UNIQUE,
                sort_order TINYINT NOT NULL DEFAULT 0
            )
        """);

        stmt.execute("""
            INSERT IGNORE INTO leave_application_types (id, name, sort_order) VALUES
            (1, 'annual', 1),
            (2, 'sick', 2),
            (3, 'unpaid', 3)
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS leave_application_statuses (
                id TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(20) NOT NULL UNIQUE,
                sort_order TINYINT NOT NULL DEFAULT 0
            )
        """);

        stmt.execute("""
            INSERT IGNORE INTO leave_application_statuses (id, name, sort_order) VALUES
            (1, 'pending', 1),
            (2, 'approved', 2),
            (3, 'rejected', 3)
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS leave_applications (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id INT NOT NULL,
                start_date_time DATETIME NOT NULL,
                end_date_time DATETIME NOT NULL,
                type_id TINYINT UNSIGNED NOT NULL,
                status_id TINYINT UNSIGNED NOT NULL,
                reason TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        
                CONSTRAINT fk_leave_applications_employee_id
                    FOREIGN KEY (employee_id) REFERENCES employees(id)
                    ON UPDATE CASCADE
                    ON DELETE CASCADE,

                CONSTRAINT fk_leave_applications_type_id
                    FOREIGN KEY (type_id) REFERENCES leave_application_types(id)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT,

                CONSTRAINT fk_leave_applications_status_id
                    FOREIGN KEY (status_id) REFERENCES leave_application_statuses(id)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT
            )
        """);

        // 4. TrainingCourses Table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS training_courses (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                duration_in_hours INT,
                department VARCHAR(255),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """);

        // 5. BenefitPlans Table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS benefit_plans (
                id INT AUTO_INCREMENT PRIMARY KEY,
                plan_name VARCHAR(255) NOT NULL,
                provider VARCHAR(255),
                description TEXT,
                cost_per_month DECIMAL(10, 2),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """);

        // 6. JobOpenings, and JobOpeningStatuses Table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS job_opening_statuses (
                id TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(20) NOT NULL UNIQUE,
                sort_order TINYINT NOT NULL DEFAULT 0
            )
        """);

        stmt.execute("""
            INSERT IGNORE INTO job_opening_statuses (id, name, sort_order) VALUES
            (1, 'open', 1),
            (2, 'closed', 2),
            (3, 'on_hold', 3)
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS job_openings (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                department VARCHAR(255),
                status_id TINYINT UNSIGNED NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                CONSTRAINT fk_job_openings_status_id
                    FOREIGN KEY (status_id) REFERENCES job_opening_statuses(id)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT
            )
        """);

        // 7. Applicants, and ApplicantStatuses Table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS applicant_statuses (
                id TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(20) NOT NULL UNIQUE,
                sort_order TINYINT NOT NULL DEFAULT 0
            )
        """);

        stmt.execute("""
            INSERT IGNORE INTO applicant_statuses (id, name, sort_order) VALUES
            (1, 'new', 1),
            (2, 'screening', 2),
            (3, 'interviewing', 3),
            (4, 'offered', 4),
            (5, 'hired', 5),
            (6, 'rejected', 6)
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS applicants (
                id INT AUTO_INCREMENT PRIMARY KEY,
                job_opening_id INT NOT NULL,
                full_name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL,
                phone VARCHAR(50),
                status_id TINYINT UNSIGNED NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                CONSTRAINT fk_applicants_job_opening_id
                    FOREIGN KEY (job_opening_id) REFERENCES job_openings(id)
                    ON UPDATE CASCADE
                    ON DELETE CASCADE,

                CONSTRAINT fk_applicants_status_id
                    FOREIGN KEY (status_id) REFERENCES applicant_statuses(id)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT
            )
        """);
    }
}
