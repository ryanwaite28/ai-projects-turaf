package com.turaf.organization.application;

import com.turaf.organization.application.dto.AddMemberRequest;
import com.turaf.organization.application.dto.MemberDto;
import com.turaf.organization.application.exception.MemberAlreadyExistsException;
import com.turaf.organization.application.exception.MemberNotFoundException;
import com.turaf.organization.application.exception.OrganizationNotFoundException;
import com.turaf.organization.domain.*;
import com.turaf.organization.domain.event.MemberAdded;
import com.turaf.organization.domain.event.MemberRemoved;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for organization membership management.
 * Handles adding, removing, and querying members.
 */
@Service
@Transactional
public class MembershipService {
    
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final EventPublisher eventPublisher;
    
    public MembershipService(
        OrganizationRepository organizationRepository,
        OrganizationMemberRepository memberRepository,
        EventPublisher eventPublisher
    ) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Add a member to an organization.
     *
     * @param organizationId The organization ID
     * @param request The add member request
     * @param addedBy The user adding the member
     * @return Member DTO
     * @throws OrganizationNotFoundException if organization not found
     * @throws MemberAlreadyExistsException if user is already a member
     */
    public MemberDto addMember(OrganizationId organizationId, AddMemberRequest request, UserId addedBy) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization with ID '" + organizationId.getValue() + "' not found"
            ));
        
        UserId userId = UserId.of(request.getUserId());
        
        if (memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new MemberAlreadyExistsException(
                "User '" + userId.getValue() + "' is already a member of this organization"
            );
        }
        
        MemberRole role = MemberRole.valueOf(request.getRole());
        
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            organizationId,
            userId,
            role,
            addedBy
        );
        
        OrganizationMember saved = memberRepository.save(member);
        
        MemberAdded event = new MemberAdded(
            UUID.randomUUID().toString(),
            organizationId.getValue(),
            userId.getValue(),
            request.getUserEmail(),
            request.getUserName(),
            role,
            addedBy.getValue(),
            Instant.now()
        );
        eventPublisher.publish(event);
        
        return MemberDto.fromDomain(saved);
    }
    
    /**
     * Get all members of an organization.
     *
     * @param organizationId The organization ID
     * @return List of member DTOs
     */
    @Transactional(readOnly = true)
    public List<MemberDto> getMembers(OrganizationId organizationId) {
        return memberRepository.findByOrganizationId(organizationId)
            .stream()
            .map(MemberDto::fromDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Get a specific member.
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return Member DTO
     * @throws MemberNotFoundException if member not found
     */
    @Transactional(readOnly = true)
    public MemberDto getMember(OrganizationId organizationId, UserId userId) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new MemberNotFoundException(
                "User '" + userId.getValue() + "' is not a member of organization '" + organizationId.getValue() + "'"
            ));
        return MemberDto.fromDomain(member);
    }
    
    /**
     * Remove a member from an organization.
     *
     * @param organizationId The organization ID
     * @param userId The user ID to remove
     * @param removedBy The user performing the removal
     * @throws MemberNotFoundException if member not found
     */
    public void removeMember(OrganizationId organizationId, UserId userId, UserId removedBy) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new MemberNotFoundException(
                "User '" + userId.getValue() + "' is not a member of organization '" + organizationId.getValue() + "'"
            ));
        
        memberRepository.delete(member);
        
        MemberRemoved event = new MemberRemoved(
            UUID.randomUUID().toString(),
            organizationId.getValue(),
            userId.getValue(),
            removedBy.getValue(),
            Instant.now()
        );
        eventPublisher.publish(event);
    }
    
    /**
     * Update a member's role.
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @param newRole The new role
     * @return Updated member DTO
     * @throws MemberNotFoundException if member not found
     */
    public MemberDto updateMemberRole(OrganizationId organizationId, UserId userId, MemberRole newRole) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new MemberNotFoundException(
                "User '" + userId.getValue() + "' is not a member of organization '" + organizationId.getValue() + "'"
            ));
        
        member.changeRole(newRole);
        OrganizationMember updated = memberRepository.save(member);
        
        return MemberDto.fromDomain(updated);
    }
    
    /**
     * Check if a user is a member of an organization.
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return true if user is a member
     */
    @Transactional(readOnly = true)
    public boolean isMember(OrganizationId organizationId, UserId userId) {
        return memberRepository.existsByOrganizationIdAndUserId(organizationId, userId);
    }
    
    /**
     * Check if a user is an admin of an organization.
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return true if user is an admin
     */
    @Transactional(readOnly = true)
    public boolean isAdmin(OrganizationId organizationId, UserId userId) {
        return memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .map(OrganizationMember::isAdmin)
            .orElse(false);
    }
    
    /**
     * Get the count of members in an organization.
     *
     * @param organizationId The organization ID
     * @return Member count
     */
    @Transactional(readOnly = true)
    public long getMemberCount(OrganizationId organizationId) {
        return memberRepository.countByOrganizationId(organizationId);
    }
}
