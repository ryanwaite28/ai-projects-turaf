package com.turaf.organization.infrastructure.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TenantContextHolder.
 */
class TenantContextHolderTest {
    
    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
    }
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void shouldSetAndGetContext() {
        // Given
        TenantContext context = new TenantContext("org-123", "user-456");
        
        // When
        TenantContextHolder.setContext(context);
        
        // Then
        TenantContext retrieved = TenantContextHolder.getContext();
        assertNotNull(retrieved);
        assertEquals("org-123", retrieved.getOrganizationId());
        assertEquals("user-456", retrieved.getUserId());
    }
    
    @Test
    void shouldGetOrganizationId() {
        // Given
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        // When
        String orgId = TenantContextHolder.getOrganizationId();
        
        // Then
        assertEquals("org-123", orgId);
    }
    
    @Test
    void shouldGetUserId() {
        // Given
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        // When
        String userId = TenantContextHolder.getUserId();
        
        // Then
        assertEquals("user-456", userId);
    }
    
    @Test
    void shouldReturnNullWhenContextNotSet() {
        // When
        TenantContext context = TenantContextHolder.getContext();
        String orgId = TenantContextHolder.getOrganizationId();
        String userId = TenantContextHolder.getUserId();
        
        // Then
        assertNull(context);
        assertNull(orgId);
        assertNull(userId);
    }
    
    @Test
    void shouldClearContext() {
        // Given
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        // When
        TenantContextHolder.clear();
        
        // Then
        assertNull(TenantContextHolder.getContext());
        assertNull(TenantContextHolder.getOrganizationId());
        assertNull(TenantContextHolder.getUserId());
    }
    
    @Test
    void shouldHandleNullOrganizationId() {
        // Given
        TenantContext context = new TenantContext(null, "user-456");
        TenantContextHolder.setContext(context);
        
        // When
        String orgId = TenantContextHolder.getOrganizationId();
        
        // Then
        assertNull(orgId);
    }
    
    @Test
    void shouldHandleNullUserId() {
        // Given
        TenantContext context = new TenantContext("org-123", null);
        TenantContextHolder.setContext(context);
        
        // When
        String userId = TenantContextHolder.getUserId();
        
        // Then
        assertNull(userId);
    }
    
    @Test
    void shouldBeThreadLocal() throws InterruptedException {
        // Given
        TenantContext mainContext = new TenantContext("org-main", "user-main");
        TenantContextHolder.setContext(mainContext);
        
        // When - set different context in another thread
        Thread otherThread = new Thread(() -> {
            TenantContext otherContext = new TenantContext("org-other", "user-other");
            TenantContextHolder.setContext(otherContext);
            
            // Verify context in other thread
            assertEquals("org-other", TenantContextHolder.getOrganizationId());
            assertEquals("user-other", TenantContextHolder.getUserId());
        });
        
        otherThread.start();
        otherThread.join();
        
        // Then - main thread context should be unchanged
        assertEquals("org-main", TenantContextHolder.getOrganizationId());
        assertEquals("user-main", TenantContextHolder.getUserId());
    }
}
