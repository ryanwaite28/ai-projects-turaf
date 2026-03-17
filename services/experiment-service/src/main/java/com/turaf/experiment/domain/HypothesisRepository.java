package com.turaf.experiment.domain;

import com.turaf.common.domain.Repository;

import java.util.List;

public interface HypothesisRepository extends Repository<Hypothesis, HypothesisId> {
    List<Hypothesis> findByProblemId(ProblemId problemId);
    List<Hypothesis> findByOrganizationId(String organizationId);
}
