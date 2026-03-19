package com.turaf.bff.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContextHolder {
    
    public static UserContext getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserContext) {
            return (UserContext) authentication.getPrincipal();
        }
        
        return null;
    }
    
    public static String getCurrentUserId() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getUserId() : null;
    }
    
    public static String getCurrentOrganizationId() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getOrganizationId() : null;
    }
    
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }
}
