package com.turaf.common.domain;

import java.util.Optional;

/**
 * Base interface for all repositories in the domain layer.
 * Repositories provide the illusion of an in-memory collection of aggregate roots.
 * They encapsulate the logic required to access data sources and provide a clean separation
 * between the domain model and data mapping layers.
 * 
 * Repositories should only work with aggregate roots, not individual entities within an aggregate.
 *
 * @param <T> The type of aggregate root
 * @param <ID> The type of the aggregate root's identifier
 */
public interface Repository<T extends AggregateRoot<ID>, ID> {
    
    /**
     * Finds an aggregate root by its unique identifier.
     *
     * @param id The aggregate root's ID
     * @return Optional containing the aggregate root if found, empty otherwise
     */
    Optional<T> findById(ID id);
    
    /**
     * Saves an aggregate root.
     * This method handles both creation and updates.
     * After saving, domain events should be published and then cleared.
     *
     * @param aggregate The aggregate root to save
     * @return The saved aggregate root
     */
    T save(T aggregate);
    
    /**
     * Deletes an aggregate root.
     * This removes the aggregate and all entities within it from persistence.
     *
     * @param aggregate The aggregate root to delete
     */
    void delete(T aggregate);
    
    /**
     * Checks if an aggregate root exists with the given ID.
     *
     * @param id The aggregate root's ID
     * @return true if exists, false otherwise
     */
    default boolean existsById(ID id) {
        return findById(id).isPresent();
    }
}
