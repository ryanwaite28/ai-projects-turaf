package com.turaf.common.tenant;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Hibernate interceptor that automatically sets the organizationId on TenantAware entities
 * before they are saved to the database.
 * 
 * This ensures that all tenant-aware entities are properly scoped to the current organization
 * without requiring manual setting of the organizationId in every service method.
 * 
 * To use this interceptor, configure it in your JPA/Hibernate configuration.
 */
public class TenantInterceptor implements Interceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    
    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
            throws CallbackException {
        
        if (entity instanceof TenantAware) {
            TenantAware tenantAware = (TenantAware) entity;
            
            // Only set if not already set
            if (tenantAware.getOrganizationId() == null) {
                try {
                    String organizationId = TenantContextHolder.getOrganizationId();
                    tenantAware.setOrganizationId(organizationId);
                    
                    // Update the state array to reflect the change
                    for (int i = 0; i < propertyNames.length; i++) {
                        if ("organizationId".equals(propertyNames[i])) {
                            state[i] = organizationId;
                            logger.debug("Set organizationId={} on entity {}", organizationId, entity.getClass().getSimpleName());
                            return true;
                        }
                    }
                } catch (TenantException e) {
                    logger.warn("Could not set organizationId on entity - no tenant context available", e);
                    // Don't fail the save, just log the warning
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                                String[] propertyNames, Type[] types) throws CallbackException {
        // Prevent changing organizationId on update
        if (entity instanceof TenantAware) {
            TenantAware tenantAware = (TenantAware) entity;
            String currentOrgId = tenantAware.getOrganizationId();
            
            if (currentOrgId != null) {
                try {
                    String contextOrgId = TenantContextHolder.getOrganizationId();
                    if (!currentOrgId.equals(contextOrgId)) {
                        throw new TenantException(
                            "Attempted to modify entity belonging to different organization. " +
                            "Entity orgId: " + currentOrgId + ", Context orgId: " + contextOrgId
                        );
                    }
                } catch (TenantException e) {
                    logger.warn("Could not validate organizationId on update - no tenant context available", e);
                }
            }
        }
        
        return false;
    }
}
