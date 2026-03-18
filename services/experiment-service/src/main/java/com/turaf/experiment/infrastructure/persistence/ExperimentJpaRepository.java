package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperimentJpaRepository extends JpaRepository<ExperimentJpaEntity, String> {
    
    List<ExperimentJpaEntity> findByHypothesisId(String hypothesisId);
    
    List<ExperimentJpaEntity> findByStatus(ExperimentStatus status);
    
    List<ExperimentJpaEntity> findByOrganizationId(String organizationId);
    
    List<ExperimentJpaEntity> findByOrganizationIdAndStatus(String organizationId, ExperimentStatus status);
    
    List<ExperimentJpaEntity> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
}
