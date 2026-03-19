package com.turaf.organization.domain.common;

import java.util.Optional;

/**
 * Base repository interface for domain aggregates.
 * Repositories provide the illusion of an in-memory collection of all objects of a certain type.
 *
 * @param <T> The aggregate type
 * @param <ID> The aggregate's identifier type
 */
public interface Repository<T extends AggregateRoot<ID>, ID> {
    
    /**
     * Save an aggregate.
     *
     * @param aggregate The aggregate to save
     * @return The saved aggregate
     */
    T save(T aggregate);
    
    /**
     * Find an aggregate by its ID.
     *
     * @param id The aggregate ID
     * @return Optional containing the aggregate if found
     */
    Optional<T> findById(ID id);
    
    /**
     * Delete an aggregate.
     *
     * @param aggregate The aggregate to delete
     */
    void delete(T aggregate);
    
    /**
     * Check if an aggregate exists by ID.
     *
     * @param id The aggregate ID
     * @return true if exists, false otherwise
     */
    boolean existsById(ID id);
}
