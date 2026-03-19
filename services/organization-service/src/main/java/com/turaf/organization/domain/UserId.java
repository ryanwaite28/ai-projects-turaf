package com.turaf.organization.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a User identifier.
 * Immutable and self-validating.
 */
public final class UserId {
    
    private final String value;
    
    private UserId(String value) {
        this.value = Objects.requireNonNull(value, "User ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be blank");
        }
    }
    
    /**
     * Create a UserId from a string value.
     *
     * @param value The ID value
     * @return UserId instance
     */
    public static UserId of(String value) {
        return new UserId(value);
    }
    
    /**
     * Generate a new random UserId.
     *
     * @return New UserId instance
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
