package com.turaf.metrics.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MetricRepository {
    
    void save(Metric metric);
    
    void saveAll(List<Metric> metrics);
    
    Optional<Metric> findById(MetricId id);
    
    List<Metric> findByExperimentId(String experimentId, Instant start, Instant end);
    
    List<Metric> findByExperimentIdAndName(String experimentId, String name, Instant start, Instant end);
    
    List<Metric> findByOrganizationId(String organizationId, Instant start, Instant end);
    
    Map<String, Double> aggregateByExperiment(String experimentId, String metricName);
    
    void deleteByExperimentId(String experimentId);
}
