package com.turaf.experiment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemJpaRepository extends JpaRepository<ProblemJpaEntity, String> {
    
    List<ProblemJpaEntity> findByOrganizationId(String organizationId);
    
    List<ProblemJpaEntity> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
}
