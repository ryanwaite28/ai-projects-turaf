package com.turaf.organization.infrastructure.tenant;

/**
 * Thread-local holder for tenant context.
 * Provides access to current organization and user context.
 */
public class TenantContextHolder {
    
    private static final ThreadLocal<TenantContext> contextHolder = new ThreadLocal<>();
    
    /**
     * Set the tenant context for the current thread.
     *
     * @param context The tenant context
     */
    public static void setContext(TenantContext context) {
        contextHolder.set(context);
    }
    
    /**
     * Get the tenant context for the current thread.
     *
     * @return The tenant context, or null if not set
     */
    public static TenantContext getContext() {
        return contextHolder.get();
    }
    
    /**
     * Get the current organization ID.
     *
     * @return The organization ID, or null if not set
     */
    public static String getOrganizationId() {
        TenantContext context = contextHolder.get();
        return context != null ? context.getOrganizationId() : null;
    }
    
    /**
     * Get the current user ID.
     *
     * @return The user ID, or null if not set
     */
    public static String getUserId() {
        TenantContext context = contextHolder.get();
        return context != null ? context.getUserId() : null;
    }
    
    /**
     * Clear the tenant context for the current thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        contextHolder.remove();
    }
}
