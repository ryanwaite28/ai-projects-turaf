package com.turaf.common.tenant;

import java.util.Optional;

/**
 * Thread-local holder for the current tenant context.
 * Provides static methods to set, get, and clear the tenant context for the current thread.
 * 
 * The context is automatically set by TenantFilter at the start of each request
 * and cleared at the end of the request to prevent memory leaks.
 * 
 * This class follows the same pattern as Spring Security's SecurityContextHolder.
 */
public final class TenantContextHolder {
    
    private static final ThreadLocal<TenantContext> contextHolder = new ThreadLocal<>();
    
    private TenantContextHolder() {
        // Prevent instantiation
    }
    
    /**
     * Sets the tenant context for the current thread.
     *
     * @param context The tenant context to set
     * @throws NullPointerException if context is null
     */
    public static void setContext(TenantContext context) {
        if (context == null) {
            throw new NullPointerException("Tenant context cannot be null");
        }
        contextHolder.set(context);
    }
    
    /**
     * Gets the tenant context for the current thread.
     *
     * @return The tenant context
     * @throws TenantException if no context is available
     */
    public static TenantContext getContext() {
        TenantContext context = contextHolder.get();
        if (context == null) {
            throw new TenantException("No tenant context available. Ensure TenantFilter is configured.");
        }
        return context;
    }
    
    /**
     * Gets the tenant context as an Optional.
     * Useful when the context might not be available (e.g., in async operations).
     *
     * @return Optional containing the context if available, empty otherwise
     */
    public static Optional<TenantContext> getContextOptional() {
        return Optional.ofNullable(contextHolder.get());
    }
    
    /**
     * Gets the organization ID from the current tenant context.
     *
     * @return The organization ID
     * @throws TenantException if no context is available
     */
    public static String getOrganizationId() {
        return getContext().getOrganizationId();
    }
    
    /**
     * Gets the user ID from the current tenant context.
     *
     * @return The user ID
     * @throws TenantException if no context is available
     */
    public static String getUserId() {
        return getContext().getUserId();
    }
    
    /**
     * Checks if a tenant context is currently set.
     *
     * @return true if context is available, false otherwise
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
    
    /**
     * Clears the tenant context for the current thread.
     * This should be called at the end of each request to prevent memory leaks.
     */
    public static void clear() {
        contextHolder.remove();
    }
}
