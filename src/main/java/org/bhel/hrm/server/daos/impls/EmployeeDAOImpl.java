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

    public EmployeeDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

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

    @Override
    public void save(Employee employee) {
        if (employee.getId() == 0)
            insert(employee);
        else
            update(employee);
    }

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

    @Override
    protected void setSaveParameters(PreparedStatement stmt, Employee employee) throws SQLException {
        stmt.setInt(1, employee.getUserId());
        stmt.setString(2, employee.getFirstName());
        stmt.setString(3, employee.getLastName());
        stmt.setString(4, employee.getIcPassport());
    }

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
