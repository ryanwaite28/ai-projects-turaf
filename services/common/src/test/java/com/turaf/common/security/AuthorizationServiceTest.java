package com.turaf.common.security;

import com.turaf.common.tenant.TenantContext;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.common.tenant.TenantException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AuthorizationServiceTest {
    
    private AuthorizationService authorizationService;
    
    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
    }
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void shouldValidateWhenOrganizationsMatch() {
        // Given
        String orgId = "org-123";
        TenantContextHolder.setContext(new TenantContext(orgId, "user-456"));
        UserPrincipal principal = new UserPrincipal("user-456", "test@example.com", "Test User", orgId);
        
        // When/Then - should not throw
        assertThatCode(() -> authorizationService.validateTenantAccess(principal))
            .doesNotThrowAnyException();
    }
    
    @Test
    void shouldThrowWhenOrganizationsDontMatch() {
        // Given
        TenantContextHolder.setContext(new TenantContext("org-123", "user-456"));
        UserPrincipal principal = new UserPrincipal("user-456", "test@example.com", "Test User", "org-999");
        
        // When/Then
        assertThatThrownBy(() -> authorizationService.validateTenantAccess(principal))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("does not match tenant context");
    }
    
    @Test
    void shouldThrowWhenPrincipalIsNull() {
        // Given
        TenantContextHolder.setContext(new TenantContext("org-123", "user-456"));
        
        // When/Then
        assertThatThrownBy(() -> authorizationService.validateTenantAccess(null))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("No authenticated user found");
    }
    
    @Test
    void shouldThrowWhenNoTenantContext() {
        // Given
        UserPrincipal principal = new UserPrincipal("user-456", "test@example.com", "Test User", "org-123");
        // No tenant context set
        
        // When/Then
        assertThatThrownBy(() -> authorizationService.validateTenantAccess(principal))
            .isInstanceOf(TenantException.class);
    }
    
    @Test
    void shouldValidateOrganizationAccess() {
        // Given
        String orgId = "org-123";
        TenantContextHolder.setContext(new TenantContext(orgId, "user-456"));
        
        // When/Then
        assertThatCode(() -> authorizationService.validateOrganizationAccess(orgId))
            .doesNotThrowAnyException();
    }
    
    @Test
    void shouldThrowWhenOrganizationAccessDenied() {
        // Given
        TenantContextHolder.setContext(new TenantContext("org-123", "user-456"));
        
        // When/Then
        assertThatThrownBy(() -> authorizationService.validateOrganizationAccess("org-999"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Cannot access resources from a different organization");
    }
    
    @Test
    void shouldReturnTrueForValidAccess() {
        // Given
        String orgId = "org-123";
        TenantContextHolder.setContext(new TenantContext(orgId, "user-456"));
        UserPrincipal principal = new UserPrincipal("user-456", "test@example.com", "Test User", orgId);
        
        // When
        boolean hasAccess = authorizationService.hasValidTenantAccess(principal);
        
        // Then
        assertThat(hasAccess).isTrue();
    }
    
    @Test
    void shouldReturnFalseForInvalidAccess() {
        // Given
        TenantContextHolder.setContext(new TenantContext("org-123", "user-456"));
        UserPrincipal principal = new UserPrincipal("user-456", "test@example.com", "Test User", "org-999");
        
        // When
        boolean hasAccess = authorizationService.hasValidTenantAccess(principal);
        
        // Then
        assertThat(hasAccess).isFalse();
    }
}
