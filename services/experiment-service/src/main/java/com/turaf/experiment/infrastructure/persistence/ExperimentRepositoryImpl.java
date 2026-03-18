package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.Experiment;
import com.turaf.experiment.domain.ExperimentId;
import com.turaf.experiment.domain.ExperimentRepository;
import com.turaf.experiment.domain.ExperimentStatus;
import com.turaf.experiment.domain.HypothesisId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ExperimentRepositoryImpl implements ExperimentRepository {
    
    private final ExperimentJpaRepository jpaRepository;

    public ExperimentRepositoryImpl(ExperimentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Experiment> findById(ExperimentId id) {
        return jpaRepository.findById(id.getValue())
            .map(ExperimentJpaEntity::toDomain);
    }

    @Override
    public Experiment save(Experiment experiment) {
        ExperimentJpaEntity entity = ExperimentJpaEntity.fromDomain(experiment);
        ExperimentJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Experiment experiment) {
        jpaRepository.deleteById(experiment.getId().getValue());
    }

    @Override
    public List<Experiment> findByHypothesisId(HypothesisId hypothesisId) {
        return jpaRepository.findByHypothesisId(hypothesisId.getValue())
            .stream()
            .map(ExperimentJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Experiment> findByStatus(ExperimentStatus status) {
        return jpaRepository.findByStatus(status)
            .stream()
            .map(ExperimentJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Experiment> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
            .stream()
            .map(ExperimentJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Experiment> findByOrganizationIdAndStatus(String organizationId, ExperimentStatus status) {
        return jpaRepository.findByOrganizationIdAndStatus(organizationId, status)
            .stream()
            .map(ExperimentJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Experiment> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(ExperimentJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
}
