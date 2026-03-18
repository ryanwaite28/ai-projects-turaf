package com.turaf.experiment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HypothesisJpaRepository extends JpaRepository<HypothesisJpaEntity, String> {
    
    List<HypothesisJpaEntity> findByProblemId(String problemId);
    
    List<HypothesisJpaEntity> findByOrganizationId(String organizationId);
    
    List<HypothesisJpaEntity> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
}
