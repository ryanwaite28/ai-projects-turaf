package com.turaf.common.security;

/**
 * Represents the authenticated user principal for multi-tenant operations.
 * Contains user and organization context extracted from JWT tokens.
 */
public class UserPrincipal {

    private final String userId;
    private final String organizationId;

    public UserPrincipal(String userId, String organizationId) {
        this.userId = userId;
        this.organizationId = organizationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId='" + userId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                '}';
    }
}
