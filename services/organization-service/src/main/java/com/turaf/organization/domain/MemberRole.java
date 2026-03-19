package com.turaf.organization.domain;

/**
 * Enum representing the role of a member within an organization.
 */
public enum MemberRole {
    /**
     * Administrator role with full permissions.
     */
    ADMIN,
    
    /**
     * Regular member role with limited permissions.
     */
    MEMBER;
    
    /**
     * Check if this role has administrative privileges.
     *
     * @return true if this is an admin role
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * Check if this role can manage members.
     *
     * @return true if this role can manage members
     */
    public boolean canManageMembers() {
        return this == ADMIN;
    }
    
    /**
     * Check if this role can manage organization settings.
     *
     * @return true if this role can manage settings
     */
    public boolean canManageSettings() {
        return this == ADMIN;
    }
}
