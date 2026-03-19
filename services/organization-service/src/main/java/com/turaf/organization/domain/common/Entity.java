package com.turaf.organization.domain.common;

import java.util.Objects;

/**
 * Base class for all entities in DDD.
 * Entities have a distinct identity that runs through time and different representations.
 * They are defined by their identity, not by their attributes.
 *
 * @param <ID> The type of the entity's identifier
 */
public abstract class Entity<ID> {
    
    private final ID id;
    
    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "Entity ID cannot be null");
    }
    
    public ID getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return Objects.equals(id, entity.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
