package com.turaf.experiment.domain;

import com.turaf.common.domain.Repository;

import java.util.List;

public interface ExperimentRepository extends Repository<Experiment, ExperimentId> {
    List<Experiment> findByHypothesisId(HypothesisId hypothesisId);
    List<Experiment> findByStatus(ExperimentStatus status);
    List<Experiment> findByOrganizationId(String organizationId);
    List<Experiment> findByOrganizationIdAndStatus(String organizationId, ExperimentStatus status);
}
