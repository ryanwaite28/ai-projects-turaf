package com.turaf.metrics.infrastructure.persistence;

import com.turaf.metrics.domain.Metric;
import com.turaf.metrics.domain.MetricId;
import com.turaf.metrics.domain.MetricRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional
public class MetricRepositoryImpl implements MetricRepository {

    private final MetricJpaRepository jpaRepository;

    public MetricRepositoryImpl(MetricJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Metric save(Metric metric) {
        MetricJpaEntity entity = MetricJpaEntity.fromDomain(metric);
        MetricJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<Metric> saveAll(List<Metric> metrics) {
        List<MetricJpaEntity> entities = metrics.stream()
            .map(MetricJpaEntity::fromDomain)
            .collect(Collectors.toList());
        List<MetricJpaEntity> saved = jpaRepository.saveAll(entities);
        return saved.stream()
            .map(MetricJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Metric> findById(MetricId id) {
        return jpaRepository.findById(id.getValue())
            .map(MetricJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Metric> findByExperimentId(String experimentId, Instant start, Instant end) {
        return jpaRepository.findByExperimentIdAndTimestampBetweenOrderByTimestampDesc(
                experimentId, start, end)
            .stream()
            .map(MetricJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Metric> findByExperimentIdAndName(String experimentId, String name, Instant start, Instant end) {
        return jpaRepository.findByExperimentIdAndNameAndTimestampBetweenOrderByTimestampDesc(
                experimentId, name, start, end)
            .stream()
            .map(MetricJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Metric> findByOrganizationId(String organizationId, Instant start, Instant end) {
        return jpaRepository.findByOrganizationIdAndTimestampBetweenOrderByTimestampDesc(
                organizationId, start, end)
            .stream()
            .map(MetricJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> aggregateByExperiment(String experimentId, String metricName) {
        Map<String, Double> aggregations = new HashMap<>();
        
        Double avg = jpaRepository.calculateAverage(experimentId, metricName);
        Double min = jpaRepository.calculateMin(experimentId, metricName);
        Double max = jpaRepository.calculateMax(experimentId, metricName);
        Double sum = jpaRepository.calculateSum(experimentId, metricName);
        Long count = jpaRepository.countByExperimentIdAndName(experimentId, metricName);
        
        aggregations.put("average", avg != null ? avg : 0.0);
        aggregations.put("min", min != null ? min : 0.0);
        aggregations.put("max", max != null ? max : 0.0);
        aggregations.put("sum", sum != null ? sum : 0.0);
        aggregations.put("count", count != null ? count.doubleValue() : 0.0);
        
        return aggregations;
    }

    @Override
    public void delete(Metric metric) {
        jpaRepository.deleteById(metric.getId().getValue());
    }

    @Override
    public void deleteByExperimentId(String experimentId) {
        jpaRepository.deleteByExperimentId(experimentId);
    }
}
