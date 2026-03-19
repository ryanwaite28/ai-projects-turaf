package com.turaf.common.tenant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TenantContextTest {
    
    @Test
    void testContextCreation() {
        TenantContext context = new TenantContext("org-123", "user-456");
        
        assertThat(context.getOrganizationId()).isEqualTo("org-123");
        assertThat(context.getUserId()).isEqualTo("user-456");
    }
    
    @Test
    void testContextCreation_NullOrganizationId_ThrowsException() {
        assertThatThrownBy(() -> new TenantContext(null, "user-456"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Organization ID cannot be null");
    }
    
    @Test
    void testContextCreation_NullUserId_ThrowsException() {
        assertThatThrownBy(() -> new TenantContext("org-123", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("User ID cannot be null");
    }
    
    @Test
    void testContextEquality_SameValues() {
        TenantContext context1 = new TenantContext("org-123", "user-456");
        TenantContext context2 = new TenantContext("org-123", "user-456");
        
        assertThat(context1).isEqualTo(context2);
        assertThat(context1.hashCode()).isEqualTo(context2.hashCode());
    }
    
    @Test
    void testContextEquality_DifferentOrganizationId() {
        TenantContext context1 = new TenantContext("org-123", "user-456");
        TenantContext context2 = new TenantContext("org-789", "user-456");
        
        assertThat(context1).isNotEqualTo(context2);
    }
    
    @Test
    void testContextEquality_DifferentUserId() {
        TenantContext context1 = new TenantContext("org-123", "user-456");
        TenantContext context2 = new TenantContext("org-123", "user-789");
        
        assertThat(context1).isNotEqualTo(context2);
    }
    
    @Test
    void testToString() {
        TenantContext context = new TenantContext("org-123", "user-456");
        
        assertThat(context.toString()).contains("org-123");
        assertThat(context.toString()).contains("user-456");
        assertThat(context.toString()).contains("TenantContext");
    }
}
