package com.turaf.metrics.domain;

import com.turaf.common.domain.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for Metric persistence operations.
 * Extends the base Repository interface and adds metric-specific queries.
 */
public interface MetricRepository extends Repository<Metric, MetricId> {
    
    /**
     * Saves multiple metrics in a batch.
     *
     * @param metrics The list of metrics to save
     * @return The list of saved metrics
     */
    List<Metric> saveAll(List<Metric> metrics);
    
    /**
     * Finds all metrics for a specific experiment within a time range.
     *
     * @param experimentId The experiment ID
     * @param start Start timestamp
     * @param end End timestamp
     * @return List of metrics
     */
    List<Metric> findByExperimentId(String experimentId, Instant start, Instant end);
    
    List<Metric> findByExperimentIdAndName(String experimentId, String name, Instant start, Instant end);
    
    List<Metric> findByOrganizationId(String organizationId, Instant start, Instant end);
    
    Map<String, Double> aggregateByExperiment(String experimentId, String metricName);
    
    void deleteByExperimentId(String experimentId);
}
