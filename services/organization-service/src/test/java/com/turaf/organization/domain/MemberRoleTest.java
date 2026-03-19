package com.turaf.organization.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemberRole enum.
 */
class MemberRoleTest {
    
    @Test
    void shouldHaveAdminRole() {
        // When
        MemberRole role = MemberRole.ADMIN;
        
        // Then
        assertNotNull(role);
        assertEquals("ADMIN", role.name());
    }
    
    @Test
    void shouldHaveMemberRole() {
        // When
        MemberRole role = MemberRole.MEMBER;
        
        // Then
        assertNotNull(role);
        assertEquals("MEMBER", role.name());
    }
    
    @Test
    void shouldIdentifyAdminRole() {
        // Given
        MemberRole adminRole = MemberRole.ADMIN;
        MemberRole memberRole = MemberRole.MEMBER;
        
        // When/Then
        assertTrue(adminRole.isAdmin());
        assertFalse(memberRole.isAdmin());
    }
    
    @Test
    void shouldCheckManageMembersPermission() {
        // Given
        MemberRole adminRole = MemberRole.ADMIN;
        MemberRole memberRole = MemberRole.MEMBER;
        
        // When/Then
        assertTrue(adminRole.canManageMembers());
        assertFalse(memberRole.canManageMembers());
    }
    
    @Test
    void shouldCheckManageSettingsPermission() {
        // Given
        MemberRole adminRole = MemberRole.ADMIN;
        MemberRole memberRole = MemberRole.MEMBER;
        
        // When/Then
        assertTrue(adminRole.canManageSettings());
        assertFalse(memberRole.canManageSettings());
    }
    
    @Test
    void shouldParseFromString() {
        // When
        MemberRole admin = MemberRole.valueOf("ADMIN");
        MemberRole member = MemberRole.valueOf("MEMBER");
        
        // Then
        assertEquals(MemberRole.ADMIN, admin);
        assertEquals(MemberRole.MEMBER, member);
    }
    
    @Test
    void shouldThrowExceptionForInvalidRole() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            MemberRole.valueOf("INVALID")
        );
    }
}
