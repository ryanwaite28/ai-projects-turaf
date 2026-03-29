package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.Hypothesis;
import com.turaf.experiment.domain.HypothesisId;
import com.turaf.experiment.domain.HypothesisRepository;
import com.turaf.experiment.domain.ProblemId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class HypothesisRepositoryImpl implements HypothesisRepository {
    
    private final HypothesisJpaRepository jpaRepository;

    public HypothesisRepositoryImpl(HypothesisJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Hypothesis> findById(HypothesisId id) {
        return jpaRepository.findById(id.getValue())
            .map(HypothesisJpaEntity::toDomain);
    }

    @Override
    public Hypothesis save(Hypothesis hypothesis) {
        HypothesisJpaEntity entity = HypothesisJpaEntity.fromDomain(hypothesis);
        HypothesisJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Hypothesis hypothesis) {
        jpaRepository.deleteById(hypothesis.getId().getValue());
    }

    @Override
    public List<Hypothesis> findByProblemId(ProblemId problemId) {
        return jpaRepository.findByProblemId(problemId.getValue())
            .stream()
            .map(HypothesisJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Hypothesis> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
            .stream()
            .map(HypothesisJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    public List<Hypothesis> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(HypothesisJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
}
