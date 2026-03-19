package com.turaf.organization.application;

import com.turaf.organization.application.dto.AddMemberRequest;
import com.turaf.organization.application.dto.MemberDto;
import com.turaf.organization.application.exception.MemberAlreadyExistsException;
import com.turaf.organization.application.exception.MemberNotFoundException;
import com.turaf.organization.application.exception.OrganizationNotFoundException;
import com.turaf.organization.domain.*;
import com.turaf.organization.domain.common.DomainEvent;
import com.turaf.organization.domain.event.MemberAdded;
import com.turaf.organization.domain.event.MemberRemoved;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MembershipService.
 */
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {
    
    @Mock
    private OrganizationRepository organizationRepository;
    
    @Mock
    private OrganizationMemberRepository memberRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    private MembershipService membershipService;
    
    private OrganizationId testOrgId;
    private Organization testOrg;
    private UserId testUserId;
    private UserId addedBy;
    
    @BeforeEach
    void setUp() {
        membershipService = new MembershipService(
            organizationRepository,
            memberRepository,
            eventPublisher
        );
        
        testOrgId = OrganizationId.generate();
        testUserId = UserId.generate();
        addedBy = UserId.generate();
        testOrg = new Organization(testOrgId, "Test Org", "test-org", addedBy);
    }
    
    @Test
    void shouldAddMemberSuccessfully() {
        // Given
        AddMemberRequest request = new AddMemberRequest(
            testUserId.getValue(),
            "MEMBER",
            "user@example.com",
            "Test User"
        );
        
        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(testOrg));
        when(memberRepository.existsByOrganizationIdAndUserId(testOrgId, testUserId)).thenReturn(false);
        when(memberRepository.save(any(OrganizationMember.class))).thenAnswer(invocation -> {
            OrganizationMember member = invocation.getArgument(0);
            return member;
        });
        
        // When
        MemberDto result = membershipService.addMember(testOrgId, request, addedBy);
        
        // Then
        assertNotNull(result);
        assertEquals(testOrgId.getValue(), result.getOrganizationId());
        assertEquals(testUserId.getValue(), result.getUserId());
        assertEquals("MEMBER", result.getRole());
        assertEquals(addedBy.getValue(), result.getAddedBy());
        
        verify(organizationRepository).findById(testOrgId);
        verify(memberRepository).existsByOrganizationIdAndUserId(testOrgId, testUserId);
        verify(memberRepository).save(any(OrganizationMember.class));
        verify(eventPublisher).publish(any(MemberAdded.class));
    }
    
    @Test
    void shouldThrowExceptionWhenOrganizationNotFoundOnAddMember() {
        // Given
        AddMemberRequest request = new AddMemberRequest(testUserId.getValue(), "MEMBER");
        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(
            OrganizationNotFoundException.class,
            () -> membershipService.addMember(testOrgId, request, addedBy)
        );
        
        verify(organizationRepository).findById(testOrgId);
        verify(memberRepository, never()).save(any(OrganizationMember.class));
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }
    
    @Test
    void shouldThrowExceptionWhenMemberAlreadyExists() {
        // Given
        AddMemberRequest request = new AddMemberRequest(testUserId.getValue(), "MEMBER");
        
        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(testOrg));
        when(memberRepository.existsByOrganizationIdAndUserId(testOrgId, testUserId)).thenReturn(true);
        
        // When/Then
        assertThrows(
            MemberAlreadyExistsException.class,
            () -> membershipService.addMember(testOrgId, request, addedBy)
        );
        
        verify(organizationRepository).findById(testOrgId);
        verify(memberRepository).existsByOrganizationIdAndUserId(testOrgId, testUserId);
        verify(memberRepository, never()).save(any(OrganizationMember.class));
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }
    
    @Test
    void shouldPublishMemberAddedEvent() {
        // Given
        AddMemberRequest request = new AddMemberRequest(
            testUserId.getValue(),
            "ADMIN",
            "admin@example.com",
            "Admin User"
        );
        
        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(testOrg));
        when(memberRepository.existsByOrganizationIdAndUserId(testOrgId, testUserId)).thenReturn(false);
        when(memberRepository.save(any(OrganizationMember.class))).thenAnswer(invocation -> {
            OrganizationMember member = invocation.getArgument(0);
            return member;
        });
        
        // When
        membershipService.addMember(testOrgId, request, addedBy);
        
        // Then
        ArgumentCaptor<MemberAdded> eventCaptor = ArgumentCaptor.forClass(MemberAdded.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        MemberAdded event = eventCaptor.getValue();
        assertEquals(testOrgId.getValue(), event.getOrganizationId());
        assertEquals(testUserId.getValue(), event.getUserId());
        assertEquals("admin@example.com", event.getUserEmail());
        assertEquals("Admin User", event.getUserName());
        assertEquals(MemberRole.ADMIN, event.getRole());
    }
    
    @Test
    void shouldGetAllMembers() {
        // Given
        UserId user1 = UserId.generate();
        UserId user2 = UserId.generate();
        
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
        
        when(memberRepository.findByOrganizationId(testOrgId))
            .thenReturn(Arrays.asList(member1, member2));
        
        // When
        List<MemberDto> result = membershipService.getMembers(testOrgId);
        
        // Then
        assertEquals(2, result.size());
        assertEquals(user1.getValue(), result.get(0).getUserId());
        assertEquals(user2.getValue(), result.get(1).getUserId());
        
        verify(memberRepository).findByOrganizationId(testOrgId);
    }
    
    @Test
    void shouldGetSpecificMember() {
        // Given
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            testUserId,
            MemberRole.MEMBER,
            addedBy
        );
        
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.of(member));
        
        // When
        MemberDto result = membershipService.getMember(testOrgId, testUserId);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId.getValue(), result.getUserId());
        assertEquals("MEMBER", result.getRole());
        
        verify(memberRepository).findByOrganizationIdAndUserId(testOrgId, testUserId);
    }
    
    @Test
    void shouldThrowExceptionWhenMemberNotFound() {
        // Given
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(
            MemberNotFoundException.class,
            () -> membershipService.getMember(testOrgId, testUserId)
        );
        
        verify(memberRepository).findByOrganizationIdAndUserId(testOrgId, testUserId);
    }
    
    @Test
    void shouldRemoveMemberSuccessfully() {
        // Given
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            testUserId,
            MemberRole.MEMBER,
            addedBy
        );
        
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.of(member));
        
        // When
        membershipService.removeMember(testOrgId, testUserId, addedBy);
        
        // Then
        verify(memberRepository).findByOrganizationIdAndUserId(testOrgId, testUserId);
        verify(memberRepository).delete(member);
        verify(eventPublisher).publish(any(MemberRemoved.class));
    }
    
    @Test
    void shouldThrowExceptionWhenRemovingNonexistentMember() {
        // Given
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(
            MemberNotFoundException.class,
            () -> membershipService.removeMember(testOrgId, testUserId, addedBy)
        );
        
        verify(memberRepository).findByOrganizationIdAndUserId(testOrgId, testUserId);
        verify(memberRepository, never()).delete(any(OrganizationMember.class));
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }
    
    @Test
    void shouldPublishMemberRemovedEvent() {
        // Given
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            testUserId,
            MemberRole.MEMBER,
            addedBy
        );
        
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.of(member));
        
        // When
        membershipService.removeMember(testOrgId, testUserId, addedBy);
        
        // Then
        ArgumentCaptor<MemberRemoved> eventCaptor = ArgumentCaptor.forClass(MemberRemoved.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        MemberRemoved event = eventCaptor.getValue();
        assertEquals(testOrgId.getValue(), event.getOrganizationId());
        assertEquals(testUserId.getValue(), event.getUserId());
        assertEquals(addedBy.getValue(), event.getRemovedBy());
    }
    
    @Test
    void shouldUpdateMemberRole() {
        // Given
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            testUserId,
            MemberRole.MEMBER,
            addedBy
        );
        
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.of(member));
        when(memberRepository.save(any(OrganizationMember.class))).thenAnswer(invocation -> {
            OrganizationMember updated = invocation.getArgument(0);
            return updated;
        });
        
        // When
        MemberDto result = membershipService.updateMemberRole(testOrgId, testUserId, MemberRole.ADMIN);
        
        // Then
        assertNotNull(result);
        assertEquals("ADMIN", result.getRole());
        
        verify(memberRepository).findByOrganizationIdAndUserId(testOrgId, testUserId);
        verify(memberRepository).save(any(OrganizationMember.class));
    }
    
    @Test
    void shouldCheckIfUserIsMember() {
        // Given
        when(memberRepository.existsByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(true);
        
        // When
        boolean result = membershipService.isMember(testOrgId, testUserId);
        
        // Then
        assertTrue(result);
        verify(memberRepository).existsByOrganizationIdAndUserId(testOrgId, testUserId);
    }
    
    @Test
    void shouldReturnFalseWhenUserIsNotMember() {
        // Given
        when(memberRepository.existsByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(false);
        
        // When
        boolean result = membershipService.isMember(testOrgId, testUserId);
        
        // Then
        assertFalse(result);
        verify(memberRepository).existsByOrganizationIdAndUserId(testOrgId, testUserId);
    }
    
    @Test
    void shouldCheckIfUserIsAdmin() {
        // Given
        OrganizationMember adminMember = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            testUserId,
            MemberRole.ADMIN,
            addedBy
        );
        
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.of(adminMember));
        
        // When
        boolean result = membershipService.isAdmin(testOrgId, testUserId);
        
        // Then
        assertTrue(result);
        verify(memberRepository).findByOrganizationIdAndUserId(testOrgId, testUserId);
    }
    
    @Test
    void shouldReturnFalseWhenUserIsNotAdmin() {
        // Given
        OrganizationMember regularMember = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrgId,
            testUserId,
            MemberRole.MEMBER,
            addedBy
        );
        
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.of(regularMember));
        
        // When
        boolean result = membershipService.isAdmin(testOrgId, testUserId);
        
        // Then
        assertFalse(result);
        verify(memberRepository).findByOrganizationIdAndUserId(testOrgId, testUserId);
    }
    
    @Test
    void shouldReturnFalseWhenCheckingAdminForNonMember() {
        // Given
        when(memberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
            .thenReturn(Optional.empty());
        
        // When
        boolean result = membershipService.isAdmin(testOrgId, testUserId);
        
        // Then
        assertFalse(result);
        verify(memberRepository).findByOrganizationIdAndUserId(testOrgId, testUserId);
    }
    
    @Test
    void shouldGetMemberCount() {
        // Given
        when(memberRepository.countByOrganizationId(testOrgId)).thenReturn(5L);
        
        // When
        long count = membershipService.getMemberCount(testOrgId);
        
        // Then
        assertEquals(5L, count);
        verify(memberRepository).countByOrganizationId(testOrgId);
    }
}
