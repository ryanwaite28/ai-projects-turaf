package com.turaf.common.domain;

import java.util.List;
import java.util.Objects;

/**
 * Base class for all value objects.
 * Value objects are immutable objects that are defined by their attributes rather than an identity.
 * Two value objects are equal if all their attributes are equal.
 * 
 * Subclasses must implement getEqualityComponents() to define which attributes
 * determine equality.
 */
public abstract class ValueObject {
    
    /**
     * Returns the list of components that define equality for this value object.
     * All components returned by this method will be used to determine equality and hash code.
     *
     * @return List of objects that define this value object's equality
     */
    protected abstract List<Object> getEqualityComponents();
    
    /**
     * Value objects are equal if all their equality components are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueObject that = (ValueObject) o;
        return Objects.equals(getEqualityComponents(), that.getEqualityComponents());
    }
    
    /**
     * Hash code based on all equality components.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getEqualityComponents().toArray());
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "components=" + getEqualityComponents() +
                '}';
    }
}
