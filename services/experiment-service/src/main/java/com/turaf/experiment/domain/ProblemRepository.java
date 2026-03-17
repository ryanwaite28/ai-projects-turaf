package com.turaf.experiment.domain;

import com.turaf.common.domain.Repository;

import java.util.List;

public interface ProblemRepository extends Repository<Problem, ProblemId> {
    List<Problem> findByOrganizationId(String organizationId);
}
