package com.turaf.common.security;

import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.common.tenant.TenantException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for handling authorization checks across all microservices.
 * Ensures users can only access data within their organization.
 */
@Service
public class AuthorizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);
    
    /**
     * Validates that the authenticated user's organization matches the current tenant context.
     * This prevents users from accessing data from other organizations.
     *
     * @param principal The authenticated user principal
     * @throws UnauthorizedException if the organization IDs don't match
     * @throws TenantException if no tenant context is available
     */
    public void validateTenantAccess(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("No authenticated user found");
        }
        
        String userOrgId = principal.getOrganizationId();
        String contextOrgId = TenantContextHolder.getOrganizationId();
        
        if (userOrgId == null || contextOrgId == null) {
            logger.error("Missing organization ID - User: {}, Context: {}", userOrgId, contextOrgId);
            throw new UnauthorizedException("Organization context is not properly set");
        }
        
        if (!userOrgId.equals(contextOrgId)) {
            logger.warn("Organization mismatch - User org: {}, Context org: {}, User: {}", 
                userOrgId, contextOrgId, principal.getUserId());
            throw new UnauthorizedException(
                "User organization does not match tenant context. Access denied."
            );
        }
        
        logger.debug("Authorization validated for user {} in organization {}", 
            principal.getUserId(), userOrgId);
    }
    
    /**
     * Validates that a specific organization ID matches the current tenant context.
     * Useful for operations where organizationId is provided in the request.
     *
     * @param organizationId The organization ID to validate
     * @throws UnauthorizedException if the organization IDs don't match
     */
    public void validateOrganizationAccess(String organizationId) {
        if (organizationId == null) {
            throw new UnauthorizedException("Organization ID cannot be null");
        }
        
        String contextOrgId = TenantContextHolder.getOrganizationId();
        
        if (!organizationId.equals(contextOrgId)) {
            logger.warn("Organization access denied - Requested: {}, Context: {}", 
                organizationId, contextOrgId);
            throw new UnauthorizedException(
                "Cannot access resources from a different organization"
            );
        }
    }
    
    /**
     * Checks if the current user has access to the tenant context.
     * Returns true if access is valid, false otherwise.
     *
     * @param principal The authenticated user principal
     * @return true if access is valid, false otherwise
     */
    public boolean hasValidTenantAccess(UserPrincipal principal) {
        try {
            validateTenantAccess(principal);
            return true;
        } catch (UnauthorizedException | TenantException e) {
            return false;
        }
    }
}
