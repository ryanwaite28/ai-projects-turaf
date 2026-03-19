package com.turaf.organization.infrastructure.persistence;

import com.turaf.organization.domain.Organization;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.OrganizationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of OrganizationRepository using Spring Data JPA.
 * Bridges domain layer and infrastructure layer.
 */
@Component
@Transactional
public class OrganizationRepositoryImpl implements OrganizationRepository {
    
    private final OrganizationJpaRepository jpaRepository;
    
    public OrganizationRepositoryImpl(OrganizationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Organization save(Organization organization) {
        OrganizationJpaEntity entity = OrganizationJpaEntity.fromDomain(organization);
        OrganizationJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> findById(OrganizationId id) {
        return jpaRepository.findById(id.getValue())
            .map(OrganizationJpaEntity::toDomain);
    }
    
    @Override
    public void delete(Organization organization) {
        jpaRepository.deleteById(organization.getId().getValue());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsById(OrganizationId id) {
        return jpaRepository.existsById(id.getValue());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug)
            .map(OrganizationJpaEntity::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlug(slug);
    }
}
