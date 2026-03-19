package com.turaf.organization.domain;

import com.turaf.organization.domain.common.Repository;

import java.util.Optional;

/**
 * Repository interface for Organization aggregate.
 * Defines domain-specific query methods beyond the base repository.
 */
public interface OrganizationRepository extends Repository<Organization, OrganizationId> {
    
    /**
     * Find an organization by its slug.
     *
     * @param slug The organization slug
     * @return Optional containing the organization if found
     */
    Optional<Organization> findBySlug(String slug);
    
    /**
     * Check if an organization exists with the given slug.
     *
     * @param slug The organization slug
     * @return true if exists, false otherwise
     */
    boolean existsBySlug(String slug);
}
