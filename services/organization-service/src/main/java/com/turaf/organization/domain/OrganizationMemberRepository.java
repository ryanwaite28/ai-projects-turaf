package com.turaf.organization.domain;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OrganizationMember entities.
 * Provides methods for querying and persisting organization members.
 */
public interface OrganizationMemberRepository {
    
    /**
     * Save an organization member.
     *
     * @param member The member to save
     * @return The saved member
     */
    OrganizationMember save(OrganizationMember member);
    
    /**
     * Find a member by ID.
     *
     * @param id The member ID
     * @return Optional containing the member if found
     */
    Optional<OrganizationMember> findById(String id);
    
    /**
     * Find all members of an organization.
     *
     * @param organizationId The organization ID
     * @return List of members
     */
    List<OrganizationMember> findByOrganizationId(OrganizationId organizationId);
    
    /**
     * Find a specific member in an organization.
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return Optional containing the member if found
     */
    Optional<OrganizationMember> findByOrganizationIdAndUserId(OrganizationId organizationId, UserId userId);
    
    /**
     * Delete an organization member.
     *
     * @param member The member to delete
     */
    void delete(OrganizationMember member);
    
    /**
     * Check if a user is a member of an organization.
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return true if the user is a member
     */
    boolean existsByOrganizationIdAndUserId(OrganizationId organizationId, UserId userId);
    
    /**
     * Count members in an organization.
     *
     * @param organizationId The organization ID
     * @return Number of members
     */
    long countByOrganizationId(OrganizationId organizationId);
}
