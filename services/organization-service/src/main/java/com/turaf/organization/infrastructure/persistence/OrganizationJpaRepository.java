package com.turaf.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
