package com.turaf.organization.infrastructure.persistence;

import com.turaf.organization.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OrganizationMemberRepositoryImpl.
 * Tests repository operations against an in-memory database.
 */
@DataJpaTest
@Import({OrganizationMemberRepositoryImpl.class, OrganizationRepositoryImpl.class})
@ActiveProfiles("test")
class OrganizationMemberRepositoryImplTest {
    
    @Autowired
    private OrganizationMemberRepositoryImpl memberRepository;
    
    @Autowired
    private OrganizationRepositoryImpl organizationRepository;
    
    private OrganizationId testOrgId;
    
    @BeforeEach
    void setUp() {
        // Create a test organization
        testOrgId = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(testOrgId, "Test Org", "test-org", createdBy);
        organizationRepository.save(org);
    }
    
    @Test
    void shouldSaveAndRetrieveMember() {
        // Given
        String memberId = UUID.randomUUID().toString();
        UserId userId = UserId.generate();
        UserId addedBy = UserId.generate();
        OrganizationMember member = new OrganizationMember(
            memberId,
            testOrgId,
            userId,
            MemberRole.MEMBER,
            addedBy
        );
        
        // When
        OrganizationMember saved = memberRepository.save(member);
        Optional<OrganizationMember> retrieved = memberRepository.findById(memberId);
        
        // Then
        assertTrue(retrieved.isPresent());
        assertEquals(memberId, retrieved.get().getId());
        assertEquals(testOrgId, retrieved.get().getOrganizationId());
        assertEquals(userId, retrieved.get().getUserId());
        assertEquals(MemberRole.MEMBER, retrieved.get().getRole());
    }
    
    @Test
    void shouldFindMembersByOrganizationId() {
        // Given
        UserId user1 = UserId.generate();
        UserId user2 = UserId.generate();
        UserId addedBy = UserId.generate();
        
        OrganizationMember member1 = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            user1,
            MemberRole.ADMIN,
            addedBy
        );
        OrganizationMember member2 = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            user2,
            MemberRole.MEMBER,
            addedBy
        );
        
        memberRepository.save(member1);
        memberRepository.save(member2);
        
        // When
        List<OrganizationMember> members = memberRepository.findByOrganizationId(testOrgId);
        
        // Then
        assertEquals(2, members.size());
    }
    
    @Test
    void shouldFindMemberByOrganizationIdAndUserId() {
        // Given
        UserId userId = UserId.generate();
        UserId addedBy = UserId.generate();
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            userId,
            MemberRole.MEMBER,
            addedBy
        );
        memberRepository.save(member);
        
        // When
        Optional<OrganizationMember> found = memberRepository.findByOrganizationIdAndUserId(
            testOrgId,
            userId
        );
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(userId, found.get().getUserId());
    }
    
    @Test
    void shouldCheckMembershipExistence() {
        // Given
        UserId userId = UserId.generate();
        UserId addedBy = UserId.generate();
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            userId,
            MemberRole.MEMBER,
            addedBy
        );
        memberRepository.save(member);
        
        // When/Then
        assertTrue(memberRepository.existsByOrganizationIdAndUserId(testOrgId, userId));
        assertFalse(memberRepository.existsByOrganizationIdAndUserId(testOrgId, UserId.generate()));
    }
    
    @Test
    void shouldCountMembersByOrganizationId() {
        // Given
        UserId user1 = UserId.generate();
        UserId user2 = UserId.generate();
        UserId user3 = UserId.generate();
        UserId addedBy = UserId.generate();
        
        memberRepository.save(new OrganizationMember(
            UUID.randomUUID().toString(), testOrgId, user1, MemberRole.ADMIN, addedBy
        ));
        memberRepository.save(new OrganizationMember(
            UUID.randomUUID().toString(), testOrgId, user2, MemberRole.MEMBER, addedBy
        ));
        memberRepository.save(new OrganizationMember(
            UUID.randomUUID().toString(), testOrgId, user3, MemberRole.MEMBER, addedBy
        ));
        
        // When
        long count = memberRepository.countByOrganizationId(testOrgId);
        
        // Then
        assertEquals(3, count);
    }
    
    @Test
    void shouldUpdateMemberRole() {
        // Given
        String memberId = UUID.randomUUID().toString();
        UserId userId = UserId.generate();
        UserId addedBy = UserId.generate();
        OrganizationMember member = new OrganizationMember(
            memberId,
            testOrgId,
            userId,
            MemberRole.MEMBER,
            addedBy
        );
        memberRepository.save(member);
        
        // When
        member.changeRole(MemberRole.ADMIN);
        memberRepository.save(member);
        Optional<OrganizationMember> updated = memberRepository.findById(memberId);
        
        // Then
        assertTrue(updated.isPresent());
        assertEquals(MemberRole.ADMIN, updated.get().getRole());
    }
    
    @Test
    void shouldDeleteMember() {
        // Given
        String memberId = UUID.randomUUID().toString();
        UserId userId = UserId.generate();
        UserId addedBy = UserId.generate();
        OrganizationMember member = new OrganizationMember(
            memberId,
            testOrgId,
            userId,
            MemberRole.MEMBER,
            addedBy
        );
        memberRepository.save(member);
        
        // When
        memberRepository.delete(member);
        
        // Then
        assertFalse(memberRepository.findById(memberId).isPresent());
    }
    
    @Test
    void shouldEnforceUniqueOrganizationUserConstraint() {
        // Given
        UserId userId = UserId.generate();
        UserId addedBy = UserId.generate();
        OrganizationMember member1 = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            userId,
            MemberRole.MEMBER,
            addedBy
        );
        memberRepository.save(member1);
        
        // When/Then - Attempting to add same user again should fail
        OrganizationMember member2 = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            userId,
            MemberRole.ADMIN,
            addedBy
        );
        
        assertThrows(Exception.class, () -> memberRepository.save(member2));
    }
    
    @Test
    void shouldCascadeDeleteMembersWhenOrganizationDeleted() {
        // Given
        UserId userId = UserId.generate();
        UserId addedBy = UserId.generate();
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            userId,
            MemberRole.MEMBER,
            addedBy
        );
        memberRepository.save(member);
        
        // When
        Organization org = organizationRepository.findById(testOrgId).orElseThrow();
        organizationRepository.delete(org);
        
        // Then
        List<OrganizationMember> members = memberRepository.findByOrganizationId(testOrgId);
        assertTrue(members.isEmpty());
    }
}
