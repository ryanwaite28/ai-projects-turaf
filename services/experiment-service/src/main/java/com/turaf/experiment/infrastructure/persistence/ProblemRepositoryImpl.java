package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.Problem;
import com.turaf.experiment.domain.ProblemId;
import com.turaf.experiment.domain.ProblemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProblemRepositoryImpl implements ProblemRepository {
    
    private final ProblemJpaRepository jpaRepository;

    public ProblemRepositoryImpl(ProblemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Problem> findById(ProblemId id) {
        return jpaRepository.findById(id.getValue())
            .map(ProblemJpaEntity::toDomain);
    }

    @Override
    public Problem save(Problem problem) {
        ProblemJpaEntity entity = ProblemJpaEntity.fromDomain(problem);
        ProblemJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Problem problem) {
        jpaRepository.deleteById(problem.getId().getValue());
    }

    @Override
    public List<Problem> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
            .stream()
            .map(ProblemJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Problem> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(ProblemJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
}
