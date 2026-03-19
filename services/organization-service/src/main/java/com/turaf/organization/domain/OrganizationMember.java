package com.turaf.organization.domain;

import com.turaf.common.domain.Entity;
import com.turaf.common.tenant.TenantAware;

import java.time.Instant;
import java.util.Objects;

/**
 * OrganizationMember entity.
 * Represents a user's membership in an organization.
 */
public class OrganizationMember extends Entity<String> implements TenantAware {
    
    private String organizationId;
    private final UserId userId;
    private MemberRole role;
    private final Instant addedAt;
    private final UserId addedBy;
    private Instant updatedAt;
    
    /**
     * Create a new OrganizationMember.
     *
     * @param id Unique identifier for this membership
     * @param organizationId Organization identifier
     * @param userId User identifier
     * @param role Member role
     * @param addedBy User who added this member
     */
    public OrganizationMember(String id, String organizationId, UserId userId,
                             MemberRole role, UserId addedBy) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.role = Objects.requireNonNull(role, "Member role cannot be null");
        this.addedBy = Objects.requireNonNull(addedBy, "Added by user cannot be null");
        this.addedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Reconstruct an OrganizationMember from persistence.
     */
    public OrganizationMember(String id, String organizationId, UserId userId,
                             MemberRole role, UserId addedBy, Instant addedAt, Instant updatedAt) {
        super(id);
        this.organizationId = organizationId;
        this.userId = userId;
        this.role = role;
        this.addedBy = addedBy;
        this.addedAt = addedAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Change the member's role.
     *
     * @param newRole The new role
     * @throws IllegalArgumentException if newRole is null
     */
    public void changeRole(MemberRole newRole) {
        Objects.requireNonNull(newRole, "New role cannot be null");
        if (this.role != newRole) {
            this.role = newRole;
            this.updatedAt = Instant.now();
        }
    }
    
    /**
     * Check if this member is an administrator.
     *
     * @return true if member has admin role
     */
    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }
    
    /**
     * Check if this member can manage other members.
     *
     * @return true if member can manage members
     */
    public boolean canManageMembers() {
        return role.canManageMembers();
    }
    
    /**
     * Check if this member can manage organization settings.
     *
     * @return true if member can manage settings
     */
    public boolean canManageSettings() {
        return role.canManageSettings();
    }
    
    /**
     * Check if this member belongs to a specific organization.
     *
     * @param orgId Organization ID to check
     * @return true if member belongs to the organization
     */
    public boolean belongsToOrganization(String orgId) {
        return this.organizationId.equals(orgId);
    }
    
    // Getters
    
    /**
     * Gets the organization ID for this member.
     * Implements TenantAware interface.
     *
     * @return The organization ID
     */
    @Override
    public String getOrganizationId() {
        return organizationId;
    }
    
    /**
     * Sets the organization ID for this member.
     * Implements TenantAware interface for automatic tenant assignment.
     *
     * @param organizationId The organization ID to set
     */
    @Override
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    public UserId getUserId() {
        return userId;
    }
    
    public MemberRole getRole() {
        return role;
    }
    
    public Instant getAddedAt() {
        return addedAt;
    }
    
    public UserId getAddedBy() {
        return addedBy;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
