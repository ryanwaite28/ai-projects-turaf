package com.turaf.organization.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationMemberTest {
    
    @Test
    void shouldCreateMemberWithValidData() {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.generate();
        UserId addedBy = UserId.generate();
        
        OrganizationMember member = new OrganizationMember(
            "member-1", orgId, userId, MemberRole.MEMBER, addedBy
        );
        
        assertNotNull(member);
        assertEquals(orgId, member.getOrganizationId());
        assertEquals(userId, member.getUserId());
        assertEquals(MemberRole.MEMBER, member.getRole());
        assertNotNull(member.getAddedAt());
    }
    
    @Test
    void shouldChangeRole() {
        OrganizationMember member = new OrganizationMember(
            "member-1",
            OrganizationId.generate(),
            UserId.generate(),
            MemberRole.MEMBER,
            UserId.generate()
        );
        
        member.changeRole(MemberRole.ADMIN);
        
        assertEquals(MemberRole.ADMIN, member.getRole());
        assertTrue(member.isAdmin());
    }
    
    @Test
    void shouldCheckIfAdmin() {
        OrganizationMember adminMember = new OrganizationMember(
            "member-1",
            OrganizationId.generate(),
            UserId.generate(),
            MemberRole.ADMIN,
            UserId.generate()
        );
        
        OrganizationMember regularMember = new OrganizationMember(
            "member-2",
            OrganizationId.generate(),
            UserId.generate(),
            MemberRole.MEMBER,
            UserId.generate()
        );
        
        assertTrue(adminMember.isAdmin());
        assertFalse(regularMember.isAdmin());
    }
    
    @Test
    void shouldCheckPermissions() {
        OrganizationMember admin = new OrganizationMember(
            "member-1",
            OrganizationId.generate(),
            UserId.generate(),
            MemberRole.ADMIN,
            UserId.generate()
        );
        
        assertTrue(admin.canManageMembers());
        assertTrue(admin.canManageSettings());
    }
    
    @Test
    void shouldRejectNullRole() {
        assertThrows(NullPointerException.class, () ->
            new OrganizationMember(
                "member-1",
                OrganizationId.generate(),
                UserId.generate(),
                null,
                UserId.generate()
            )
        );
    }
}
