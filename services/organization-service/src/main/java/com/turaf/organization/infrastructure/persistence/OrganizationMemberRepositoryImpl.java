package com.turaf.organization.infrastructure.persistence;

import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.OrganizationMember;
import com.turaf.organization.domain.OrganizationMemberRepository;
import com.turaf.organization.domain.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of OrganizationMemberRepository using Spring Data JPA.
 * Bridges domain layer and infrastructure layer.
 */
@Component
@Transactional
public class OrganizationMemberRepositoryImpl implements OrganizationMemberRepository {
    
    private final OrganizationMemberJpaRepository jpaRepository;
    
    public OrganizationMemberRepositoryImpl(OrganizationMemberJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public OrganizationMember save(OrganizationMember member) {
        OrganizationMemberJpaEntity entity = OrganizationMemberJpaEntity.fromDomain(member);
        OrganizationMemberJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizationMember> findById(String id) {
        return jpaRepository.findById(id)
            .map(OrganizationMemberJpaEntity::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrganizationMember> findByOrganizationId(OrganizationId organizationId) {
        return jpaRepository.findByOrganizationId(organizationId.getValue())
            .stream()
            .map(OrganizationMemberJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizationMember> findByOrganizationIdAndUserId(
        OrganizationId organizationId,
        UserId userId
    ) {
        return jpaRepository.findByOrganizationIdAndUserId(
            organizationId.getValue(),
            userId.getValue()
        ).map(OrganizationMemberJpaEntity::toDomain);
    }
    
    @Override
    public void delete(OrganizationMember member) {
        jpaRepository.deleteById(member.getId());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByOrganizationIdAndUserId(OrganizationId organizationId, UserId userId) {
        return jpaRepository.existsByOrganizationIdAndUserId(
            organizationId.getValue(),
            userId.getValue()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByOrganizationId(OrganizationId organizationId) {
        return jpaRepository.countByOrganizationId(organizationId.getValue());
    }
}
