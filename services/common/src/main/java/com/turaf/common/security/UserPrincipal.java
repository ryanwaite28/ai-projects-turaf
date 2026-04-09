package com.turaf.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents the authenticated user principal for multi-tenant operations.
 * Contains user and organization context extracted from JWT tokens.
 * Implements Principal and UserDetails so Spring Security can use it directly
 * as an @AuthenticationPrincipal in downstream service controllers.
 */
public class UserPrincipal implements Principal, UserDetails {

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

    // --- Principal ---

    @Override
    public String getName() {
        return userId;
    }

    // --- UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "UserPrincipal{userId='" + userId + "', organizationId='" + organizationId + "'}";
    }
}
