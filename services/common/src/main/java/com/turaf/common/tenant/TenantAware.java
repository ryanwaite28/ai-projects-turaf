package com.turaf.common.tenant;

/**
 * Interface for entities that are tenant-aware.
 * Entities implementing this interface will have their organizationId automatically set
 * by the TenantInterceptor when saved.
 * 
 * This ensures that all data is properly scoped to the correct tenant (organization).
 */
public interface TenantAware {
    
    /**
     * Gets the organization ID (tenant identifier) for this entity.
     *
     * @return The organization ID
     */
    String getOrganizationId();
    
    /**
     * Sets the organization ID (tenant identifier) for this entity.
     * This is typically called automatically by the TenantInterceptor.
     *
     * @param organizationId The organization ID to set
     */
    void setOrganizationId(String organizationId);
}
