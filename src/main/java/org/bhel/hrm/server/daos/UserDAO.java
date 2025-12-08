package org.bhel.hrm.server.daos;

import org.bhel.hrm.server.domain.User;

import java.util.Optional;

/**
 * Data Access Object interface for User entities.
 * Extends the generic DAO for standard CRUD operations and adds
 * user-specific query methods.
 */
public interface UserDAO extends DAO<User, Integer> {
    /**
     * Finds a user by their unique username. This is the primary method
     * used for authentication lookups.
     *
     * @param username The username of the user to find.
     * @return An {@link Optional} containing the User if found, otherwise an empty Optional.
     */
    Optional<User> findByUsername(String username);
}
