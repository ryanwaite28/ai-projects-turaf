package com.turaf.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for OrganizationMemberJpaEntity.
 * Provides CRUD operations and custom queries.
 */
@Repository
public interface OrganizationMemberJpaRepository extends JpaRepository<OrganizationMemberJpaEntity, String> {
    
    /**
     * Find all members of an organization.
     *
     * @param organizationId The organization ID
     * @return List of members
     */
    List<OrganizationMemberJpaEntity> findByOrganizationId(String organizationId);
    
    /**
     * Find a specific member in an organization.
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return Optional containing the member if found
     */
    Optional<OrganizationMemberJpaEntity> findByOrganizationIdAndUserId(String organizationId, String userId);
    
    /**
     * Check if a user is a member of an organization.
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return true if the user is a member
     */
    boolean existsByOrganizationIdAndUserId(String organizationId, String userId);
    
    /**
     * Count members in an organization.
     *
     * @param organizationId The organization ID
     * @return Number of members
     */
    long countByOrganizationId(String organizationId);
}
