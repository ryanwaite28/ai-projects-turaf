package com.turaf.organization.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an Organization identifier.
 * Immutable and self-validating.
 */
public final class OrganizationId {
    
    private final String value;
    
    private OrganizationId(String value) {
        this.value = Objects.requireNonNull(value, "Organization ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Organization ID cannot be blank");
        }
    }
    
    /**
     * Create an OrganizationId from a string value.
     *
     * @param value The ID value
     * @return OrganizationId instance
     */
    public static OrganizationId of(String value) {
        return new OrganizationId(value);
    }
    
    /**
     * Generate a new random OrganizationId.
     *
     * @return New OrganizationId instance
     */
    public static OrganizationId generate() {
        return new OrganizationId(UUID.randomUUID().toString());
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationId that = (OrganizationId) o;
        return Objects.equals(value, that.value);
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
