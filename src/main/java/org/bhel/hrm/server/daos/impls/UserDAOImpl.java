package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.UserDAO;
import org.bhel.hrm.server.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl extends AbstractDAO<User> implements UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);

    private final RowMapper<User> rowMapper = result -> new User(
        result.getInt("id"),
        result.getString("username"),
        result.getString("password_hash"),
        mapRole(result.getObject("role_id", Integer.class))
    );

    public UserDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = """
            SELECT
                id,
                username,
                password_hash,
                role_id
            FROM
                users
            WHERE
                id = ?
        """;

        return findOne(sql, stmt -> stmt.setInt(1, id), rowMapper);
    }

    @Override
    public List<User> findAll() {
        String sql = """
            SELECT
                id,
                username,
                password_hash,
                role_id
            FROM
                users
            ORDER BY
                username ASC
        """;

        return findMany(sql, stmt -> {}, rowMapper);
    }

    @Override
    public void save(User user) {

        if (user.getId() == 0)
            insert(user);
        else
            update(user);
    }

    @Override
    protected void insert(User user) {
        String sql = """
            INSERT INTO
                users (
                    username,
                    password_hash,
                    role_id
                )
            VALUES (
                ?,
                ?,
                ?
            )
        """;

        Connection conn = null;

        try {
            conn = dbManager.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setSaveParameters(stmt, user);
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next())
                        user.setId(generatedKeys.getInt(1)); // Sets the new ID back on the object
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting new user: " + user.getUsername(), e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    @Override
    protected void update(User user) {
        String sql = """
            UPDATE
                users
            SET
                username = ?,
                password_hash = ?,
                role_id = ?
            WHERE
                id = ?
        """;

        executeUpdate(sql, stmt -> {
            setSaveParameters(stmt, user);
            stmt.setInt(4, user.getId());
        });
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getUsername());
        stmt.setString(2, user.getPasswordHash());
        stmt.setInt(3, user.getRole() == UserDTO.Role.HR_STAFF ? 1 : 2);
    }

    @Override
    public void deleteById(Integer id) {
        String sql = """
            DELETE FROM
                users
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
                users
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
            throw new DataAccessException("Error counting users", e);
        }

        return 0;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = """
            SELECT
                id,
                username,
                password_hash,
                role_id
            FROM
                users
            WHERE
                username = ?
        """;

        return findOne(sql, stmt -> stmt.setString(1, username), rowMapper);
    }

    private static UserDTO.Role mapRole(Integer roleId) {
        if (roleId == null)
            throw new IllegalStateException("users.role_id is NULL");

        return switch (roleId) {
            case 1 -> UserDTO.Role.HR_STAFF;
            case 2 -> UserDTO.Role.EMPLOYEE;
            default -> throw new IllegalArgumentException("Unknown users.role_id=" + roleId);
        };
    }
}