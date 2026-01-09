package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.domain.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class EmployeeDAOImpl extends AbstractDAO<Employee> implements EmployeeDAO {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeDAOImpl.class);

    private final RowMapper<Employee> rowMapper = result -> new Employee(
        result.getInt("id"),
        result.getInt("user_id"),
        result.getString("first_name"),
        result.getString("last_name"),
        result.getString("ic_passport")
    );

    /**
     * Creates a new EmployeeDAOImpl configured with the provided DatabaseManager.
     *
     * @param dbManager the DatabaseManager used to obtain database connections for DAO operations
     */
    public EmployeeDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    /**
     * Finds the employee with the specified id.
     *
     * @param id the employee's id
     * @return an Optional containing the Employee with the given id, or empty if no matching record exists
     */
    @Override
    public Optional<Employee> findById(Integer id) {
        String sql = """
            SELECT
                id,
                user_id,
                first_name,
                last_name,
                ic_passport
            FROM
                employees
            WHERE
                id = ?
        """;

        return findOne(sql, stmt -> stmt.setInt(1, id), rowMapper);
    }

    /**
     * Retrieve all employees ordered by first name then last name.
     *
     * @return a list of Employee objects ordered by first name and last name; empty list if no employees are found
     */
    @Override
    public Optional<Employee> findByUserId(int userId) {
        String sql = """
            SELECT
                id,
                user_id,
                first_name,
                last_name,
                ic_passport
            FROM
                employees
            WHERE
                user_id = ?
        """;

        return findOne(sql, stmt -> stmt.setInt(1, userId), rowMapper);
    }

    @Override
    public List<Employee> findAll() {
        String sql = """
            SELECT
                id,
                user_id,
                first_name,
                last_name,
                ic_passport
            FROM
                employees
            ORDER BY
                last_name, first_name ASC
        """;

        return findMany(sql, stmt -> {}, rowMapper);
    }

    /**
     * Persists the given employee: creates a new record when the employee's id is zero,
     * otherwise updates the existing record identified by its id.
     *
     * @param employee the employee to persist; its `id` determines whether a new record is created or an existing one is updated
     */
    @Override
    public void save(Employee employee) {
        if (employee.getId() == 0)
            insert(employee);
        else
            update(employee);
    }

    /**
     * Inserts a new employee record into the employees table and sets the generated database id on the given Employee.
     *
     * @param employee the Employee to persist; on success its id is replaced with the generated id
     */
    @Override
    protected void insert(Employee employee) {
        String sql = """
            INSERT INTO
                employees (
                    user_id,
                    first_name,
                    last_name,
                    ic_passport
                )
            VALUES (
                ?,
                ?,
                ?,
                ?
            )
        """;

        Connection conn = null;

        try {
            conn = dbManager.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setSaveParameters(stmt, employee);
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next())
                        employee.setId(generatedKeys.getInt(1)); // Sets the new ID back on the object
                }
            }
        } catch (SQLException e) {
            logger.error("Error inserting new employee: {} {}", employee.getFirstName(), employee.getLastName(), e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Updates the database record for the given employee using its current field values.
     *
     * Updates the user_id, first_name, last_name, and ic_passport columns of the employees
     * table for the row identified by the employee's id.
     *
     * @param employee the Employee whose data will be written to the corresponding database row
     */
    @Override
    protected void update(Employee employee) {
        String sql = """
            UPDATE
                employees
            SET
                user_id = ?,
                first_name = ?,
                last_name = ?,
                ic_passport = ?
            WHERE
                id = ?
        """;

        executeUpdate(sql, stmt -> {
            setSaveParameters(stmt, employee);
            stmt.setInt(5, employee.getId());
        });
    }

    /**
     * Bind the employee's fields to the given PreparedStatement in the order expected by the
     * employees insert/update SQL (user_id, first_name, last_name, ic_passport).
     *
     * @param stmt     the PreparedStatement to bind parameters to; parameters 1–4 will be set
     * @param employee the Employee providing values for the parameters
     * @throws SQLException if a database access error occurs while setting parameters
     */
    @Override
    protected void setSaveParameters(PreparedStatement stmt, Employee employee) throws SQLException {
        stmt.setInt(1, employee.getUserId());
        stmt.setString(2, employee.getFirstName());
        stmt.setString(3, employee.getLastName());
        stmt.setString(4, employee.getIcPassport());
    }

    /**
     * Delete the employee record identified by the provided id.
     *
     * @param id the employee primary key to delete
     */
    @Override
    public void deleteById(Integer id) {
        String sql = """
            DELETE FROM
                employees
            WHERE
                id = ?
        """;

        executeUpdate(sql, stmt -> stmt.setInt(1, id));
    }

    /**
     * Retrieves the number of employee records in the database.
     *
     * @return the number of employee records, or 0 if the count cannot be obtained
     */
    @Override
    public long count() {
        String sql = """
            SELECT
                COUNT(*)
            FROM
                employees
        """;

        try (
            Connection conn = dbManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet result = stmt.executeQuery(sql)
        ) {
            if (result.next())
                return Math.toIntExact(result.getLong(1));

            logger.info("{}", stmt);
        } catch (SQLException e) {
            logger.error("Error counting employees", e);
        }

        return 0;
    }
}