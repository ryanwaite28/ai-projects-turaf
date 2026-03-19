package com.turaf.common.tenant;

import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TenantInterceptorTest {
    
    @Mock
    private Type type;
    
    private TenantInterceptor interceptor;
    
    private static class TestEntity implements TenantAware {
        private String organizationId;
        
        @Override
        public String getOrganizationId() {
            return organizationId;
        }
        
        @Override
        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }
    }
    
    @BeforeEach
    void setUp() {
        interceptor = new TenantInterceptor();
    }
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void testOnSave_SetsOrganizationId() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        TestEntity entity = new TestEntity();
        Object[] state = new Object[1];
        String[] propertyNames = {"organizationId"};
        Type[] types = {type};
        
        boolean modified = interceptor.onSave(entity, "id-1", state, propertyNames, types);
        
        assertThat(modified).isTrue();
        assertThat(entity.getOrganizationId()).isEqualTo("org-123");
        assertThat(state[0]).isEqualTo("org-123");
    }
    
    @Test
    void testOnSave_DoesNotOverrideExistingOrganizationId() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        TestEntity entity = new TestEntity();
        entity.setOrganizationId("org-existing");
        
        Object[] state = new Object[1];
        String[] propertyNames = {"organizationId"};
        Type[] types = {type};
        
        boolean modified = interceptor.onSave(entity, "id-1", state, propertyNames, types);
        
        assertThat(modified).isFalse();
        assertThat(entity.getOrganizationId()).isEqualTo("org-existing");
    }
    
    @Test
    void testOnSave_NoTenantContext_DoesNotFail() {
        TestEntity entity = new TestEntity();
        Object[] state = new Object[1];
        String[] propertyNames = {"organizationId"};
        Type[] types = {type};
        
        // Should not throw exception, just log warning
        boolean modified = interceptor.onSave(entity, "id-1", state, propertyNames, types);
        
        assertThat(modified).isFalse();
        assertThat(entity.getOrganizationId()).isNull();
    }
    
    @Test
    void testOnSave_NonTenantAwareEntity_NoChange() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        Object entity = new Object();
        Object[] state = new Object[0];
        String[] propertyNames = new String[0];
        Type[] types = new Type[0];
        
        boolean modified = interceptor.onSave(entity, "id-1", state, propertyNames, types);
        
        assertThat(modified).isFalse();
    }
    
    @Test
    void testOnFlushDirty_ValidatesOrganizationId() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        TestEntity entity = new TestEntity();
        entity.setOrganizationId("org-123");
        
        Object[] currentState = new Object[1];
        Object[] previousState = new Object[1];
        String[] propertyNames = {"organizationId"};
        Type[] types = {type};
        
        // Should not throw exception when organizationId matches
        boolean modified = interceptor.onFlushDirty(entity, "id-1", currentState, previousState, propertyNames, types);
        
        assertThat(modified).isFalse();
    }
    
    @Test
    void testOnFlushDirty_DifferentOrganizationId_ThrowsException() {
        TenantContext context = new TenantContext("org-123", "user-456");
        TenantContextHolder.setContext(context);
        
        TestEntity entity = new TestEntity();
        entity.setOrganizationId("org-different");
        
        Object[] currentState = new Object[1];
        Object[] previousState = new Object[1];
        String[] propertyNames = {"organizationId"};
        Type[] types = {type};
        
        assertThatThrownBy(() -> 
            interceptor.onFlushDirty(entity, "id-1", currentState, previousState, propertyNames, types))
            .isInstanceOf(TenantException.class)
            .hasMessageContaining("Attempted to modify entity belonging to different organization");
    }
}
