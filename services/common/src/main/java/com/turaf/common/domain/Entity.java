package com.turaf.common.domain;

import java.util.Objects;

/**
 * Base class for all domain entities.
 * Entities are objects that have a distinct identity that runs through time and different representations.
 * Two entities are equal if they have the same ID, regardless of their other attributes.
 *
 * @param <ID> The type of the entity's identifier
 */
public abstract class Entity<ID> {
    
    protected ID id;
    
    /**
     * Protected constructor to enforce creation through subclasses.
     *
     * @param id The unique identifier for this entity
     * @throws NullPointerException if id is null
     */
    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "Entity ID cannot be null");
    }
    
    /**
     * Gets the unique identifier of this entity.
     *
     * @return The entity's ID
     */
    public ID getId() {
        return id;
    }
    
    /**
     * Entities are equal if they have the same ID.
     * This implements identity-based equality as per DDD principles.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return id.equals(entity.id);
    }
    
    /**
     * Hash code based on entity ID.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}
