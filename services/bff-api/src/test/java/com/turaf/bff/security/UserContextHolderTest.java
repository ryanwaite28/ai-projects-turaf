package com.turaf.bff.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class UserContextHolderTest {
    
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }
    
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testGetCurrentUser_Authenticated() {
        UserContext userContext = UserContext.builder()
            .userId("user-123")
            .organizationId("org-123")
            .email("test@example.com")
            .name("Test User")
            .build();
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userContext, null, userContext.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        UserContext result = UserContextHolder.getCurrentUser();
        
        assertNotNull(result);
        assertEquals("user-123", result.getUserId());
        assertEquals("org-123", result.getOrganizationId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
    }
    
    @Test
    void testGetCurrentUser_NotAuthenticated() {
        UserContext result = UserContextHolder.getCurrentUser();
        
        assertNull(result);
    }
    
    @Test
    void testGetCurrentUserId_Authenticated() {
        UserContext userContext = UserContext.builder()
            .userId("user-456")
            .organizationId("org-456")
            .build();
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userContext, null, userContext.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String userId = UserContextHolder.getCurrentUserId();
        
        assertEquals("user-456", userId);
    }
    
    @Test
    void testGetCurrentUserId_NotAuthenticated() {
        String userId = UserContextHolder.getCurrentUserId();
        
        assertNull(userId);
    }
    
    @Test
    void testGetCurrentOrganizationId_Authenticated() {
        UserContext userContext = UserContext.builder()
            .userId("user-789")
            .organizationId("org-789")
            .build();
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userContext, null, userContext.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String orgId = UserContextHolder.getCurrentOrganizationId();
        
        assertEquals("org-789", orgId);
    }
    
    @Test
    void testGetCurrentOrganizationId_NotAuthenticated() {
        String orgId = UserContextHolder.getCurrentOrganizationId();
        
        assertNull(orgId);
    }
    
    @Test
    void testIsAuthenticated_True() {
        UserContext userContext = UserContext.builder()
            .userId("user-999")
            .build();
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userContext, null, userContext.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        assertTrue(UserContextHolder.isAuthenticated());
    }
    
    @Test
    void testIsAuthenticated_False() {
        assertFalse(UserContextHolder.isAuthenticated());
    }
}
