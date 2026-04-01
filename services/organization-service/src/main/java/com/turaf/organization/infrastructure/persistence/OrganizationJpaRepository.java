package com.turaf.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for OrganizationJpaEntity.
 * Provides CRUD operations and custom queries.
 */
@Repository
public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, String> {
    
    /**
     * Find an organization by its slug.
     *
     * @param slug The organization slug
     * @return Optional containing the organization if found
     */
    Optional<OrganizationJpaEntity> findBySlug(String slug);
    
    /**
     * Check if an organization exists with the given slug.
     *
     * @param slug The organization slug
     * @return true if exists, false otherwise
     */
    boolean existsBySlug(String slug);
    
    /**
     * Find all organizations where the user is a member.
     * Uses the relationship through OrganizationMember.
     *
     * @param userId The user ID
     * @return List of organizations
     */
    List<OrganizationJpaEntity> findByMembersUserId(String userId);
}
