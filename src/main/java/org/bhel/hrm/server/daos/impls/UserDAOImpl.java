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

    /**
     * Create a UserDAOImpl backed by the given DatabaseManager.
     *
     * @param dbManager the DatabaseManager used to obtain database connections for DAO operations
     */
    public UserDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    /**
     * Retrieve a user by its database identifier.
     *
     * @param id the user's database id
     * @return an Optional containing the User with the given id, or empty if no matching user exists
     */
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

    /**
     * Retrieve all users from the database ordered by username ascending.
     *
     * @return a list of User objects for every row in the users table ordered by username ascending; an empty list if no users exist.
     */
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

    /**
     * Persist the given User to the database, inserting when the user's id is zero and updating otherwise.
     *
     * @param user the User to persist; its id will be populated after insertion
     */
    @Override
    public void save(User user) {
        if (user.getId() == 0)
            insert(user);
        else
            update(user);
    }

    /**
     * Persist the provided User as a new database row and update the User's id with the generated key.
     *
     * After a successful insert, the user's id field is set to the newly generated primary key.
     * On database error the method logs the exception and does not propagate it; the user's id will remain unchanged.
     *
     * @param user the User to insert; its id will be updated with the generated key after a successful insert
     */
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

    /**
     * Update the persisted database record for the given user using its id as the identifier.
     *
     * @param user the user whose username, password hash, and role should be written to the database; its `id` determines which row to update
     */
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

    /**
     * Populate the given PreparedStatement's parameters with the user's username, password hash, and role id.
     *
     * @param stmt the PreparedStatement where parameter indexes 1..3 will be set to username, password_hash, and role_id respectively
     * @param user the user whose values will be bound; role is mapped to `1` for `HR_STAFF` and `2` for other roles
     * @throws SQLException if an error occurs while setting statement parameters
     */
    @Override
    protected void setSaveParameters(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getUsername());
        stmt.setString(2, user.getPasswordHash());
        stmt.setInt(3, user.getRole() == UserDTO.Role.HR_STAFF ? 1 : 2);
    }

    /**
     * Delete the user with the specified id from the database.
     *
     * If no user matches the given id, the statement completes without affecting any rows.
     *
     * @param id the identifier of the user to delete
     */
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

    /**
     * Get the total number of users in the database.
     *
     * @return the total number of users; `0` if none are found or if a database error occurs
     */
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

    /**
     * Retrieve the user with the given username.
     *
     * @param username the username to match
     * @return an Optional containing the matching User, or empty if no user has that username
     */
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