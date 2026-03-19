package com.turaf.common.tenant;

import java.util.Objects;

/**
 * Immutable context object that holds tenant-specific information for the current request.
 * This includes the organization ID (tenant identifier) and the user ID making the request.
 * 
 * The tenant context is stored in a ThreadLocal and is automatically set by the TenantFilter
 * for each incoming request.
 */
public final class TenantContext {
    
    private final String organizationId;
    private final String userId;
    
    /**
     * Creates a new tenant context.
     *
     * @param organizationId The organization ID (tenant identifier)
     * @param userId The user ID making the request
     * @throws NullPointerException if organizationId or userId is null
     */
    public TenantContext(String organizationId, String userId) {
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
    }
    
    /**
     * Gets the organization ID (tenant identifier).
     *
     * @return The organization ID
     */
    public String getOrganizationId() {
        return organizationId;
    }
    
    /**
     * Gets the user ID making the request.
     *
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantContext that = (TenantContext) o;
        return organizationId.equals(that.organizationId) && userId.equals(that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(organizationId, userId);
    }
    
    @Override
    public String toString() {
        return "TenantContext{" +
                "organizationId='" + organizationId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
