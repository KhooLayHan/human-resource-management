package org.bhel.hrm.server.daos;

import java.util.List;
import java.util.Optional;

/**
 * A generic Data Access Object (DAO) interface defining the standard CRUD
 * operations for all domain entities.
 *
 * @param <T> The type of the domain entity (e.g., Employee, User).
 * @param <ID> The type of the entity's primary key (e.g., Integer).
 */
public interface DAO<T, ID> {
    /**
     * Retrieves an entity by its ID.
     *
     * @param id The ID of the entity.
     * @return An {@link Optional} containing the entity if found, otherwise an empty {@link Optional}.
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves all entities of this type.
     *
     * @return A {@link List} of all entities.
     */
    List<T> findAll();

    /**
     * Saves a given entity. Use the returned instance for further operations
     * as the save operation might have changed the entity instance completely.
     * If the entity is new, it will be inserted. If it exists, it will be updated.
     *
     * @param entity The entity to save.
     */
    void save(T entity);

    /**
     * Deletes an entity by its ID.
     *
     * @param id The ID of the entity to delete.
     */
    void deleteById(ID id);

    /**
     * Returns the total number of entities.
     *
     * @return The count of entities.
     */
    long count();
}
