package com.turaf.organization.infrastructure.tenant;

/**
 * Holds tenant context information for the current request.
 */
public class TenantContext {
    
    private String organizationId;
    private String userId;
    
    public TenantContext() {
    }
    
    public TenantContext(String organizationId, String userId) {
        this.organizationId = organizationId;
        this.userId = userId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
