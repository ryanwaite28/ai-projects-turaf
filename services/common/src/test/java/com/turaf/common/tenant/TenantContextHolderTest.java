package com.turaf.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class TenantContextHolderTest {
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void testSetAndGetContext() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        TenantContext retrieved = TenantContextHolder.getContext();
        
        assertThat(retrieved).isEqualTo(context);
        assertThat(retrieved.getOrganizationId()).isEqualTo("org-123");
        assertThat(retrieved.getUserId()).isEqualTo("user-456");
    }
    
    @Test
    void testSetContext_Null_ThrowsException() {
        assertThatThrownBy(() -> TenantContextHolder.setContext(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Tenant context cannot be null");
    }
    
    @Test
    void testGetContext_NoContextSet_ThrowsException() {
        assertThatThrownBy(() -> TenantContextHolder.getContext())
            .isInstanceOf(TenantException.class)
            .hasMessageContaining("No tenant context available");
    }
    
    @Test
    void testGetContextOptional_NoContextSet_ReturnsEmpty() {
        Optional<TenantContext> context = TenantContextHolder.getContextOptional();
        
        assertThat(context).isEmpty();
    }
    
    @Test
    void testGetContextOptional_ContextSet_ReturnsContext() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        Optional<TenantContext> retrieved = TenantContextHolder.getContextOptional();
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualTo(context);
    }
    
    @Test
    void testGetOrganizationId() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        String organizationId = TenantContextHolder.getOrganizationId();
        
        assertThat(organizationId).isEqualTo("org-123");
    }
    
    @Test
    void testGetUserId() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        String userId = TenantContextHolder.getUserId();
        
        assertThat(userId).isEqualTo("user-456");
    }
    
    @Test
    void testHasContext_NoContext() {
        assertThat(TenantContextHolder.hasContext()).isFalse();
    }
    
    @Test
    void testHasContext_WithContext() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        assertThat(TenantContextHolder.hasContext()).isTrue();
    }
    
    @Test
    void testClear() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        TenantContextHolder.clear();
        
        assertThat(TenantContextHolder.hasContext()).isFalse();
        assertThatThrownBy(() -> TenantContextHolder.getContext())
            .isInstanceOf(TenantException.class);
    }
    
    @Test
    void testThreadIsolation() throws InterruptedException {
        TenantContext mainContext = new TenantContext("org-main", "user-main");
        TenantContextHolder.setContext(mainContext);
        
        AtomicReference<String> threadOrgId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Thread thread = new Thread(() -> {
            try {
                // Thread should not have access to main thread's context
                assertThat(TenantContextHolder.hasContext()).isFalse();
                
                // Set different context in this thread
                TenantContext threadContext = new TenantContext("org-thread", "user-thread");
                TenantContextHolder.setContext(threadContext);
                
                threadOrgId.set(TenantContextHolder.getOrganizationId());
            } finally {
                TenantContextHolder.clear();
                latch.countDown();
            }
        });
        
        thread.start();
        latch.await();
        
        // Main thread should still have its own context
        assertThat(TenantContextHolder.getOrganizationId()).isEqualTo("org-main");
        
        // Thread had different context
        assertThat(threadOrgId.get()).isEqualTo("org-thread");
    }
}
