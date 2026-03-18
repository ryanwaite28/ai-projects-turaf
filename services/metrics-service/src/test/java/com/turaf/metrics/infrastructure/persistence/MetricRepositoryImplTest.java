package com.turaf.metrics.infrastructure.persistence;

import com.turaf.metrics.domain.Metric;
import com.turaf.metrics.domain.MetricId;
import com.turaf.metrics.domain.MetricRepository;
import com.turaf.metrics.domain.MetricType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(MetricRepositoryImpl.class)
@ActiveProfiles("test")
class MetricRepositoryImplTest {

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private MetricJpaRepository jpaRepository;

    @AfterEach
    void cleanup() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldSaveMetric() {
        // Given
        Metric metric = createMetric("org-123", "exp-456", "response_time", 125.5, MetricType.GAUGE);

        // When
        metricRepository.save(metric);

        // Then
        Optional<Metric> saved = metricRepository.findById(metric.getId());
        assertTrue(saved.isPresent());
        assertEquals(metric.getId(), saved.get().getId());
        assertEquals(metric.getName(), saved.get().getName());
        assertEquals(metric.getValue(), saved.get().getValue());
        assertEquals(metric.getType(), saved.get().getType());
    }

    @Test
    void shouldSaveMetricWithTags() {
        // Given
        Metric metric = createMetric("org-123", "exp-456", "cpu_usage", 75.0, MetricType.GAUGE);
        metric.addTag("region", "us-east-1");
        metric.addTag("environment", "production");

        // When
        metricRepository.save(metric);

        // Then
        Optional<Metric> saved = metricRepository.findById(metric.getId());
        assertTrue(saved.isPresent());
        assertEquals(2, saved.get().getTags().size());
        assertEquals("us-east-1", saved.get().getTag("region"));
        assertEquals("production", saved.get().getTag("environment"));
    }

    @Test
    void shouldSaveAllMetrics() {
        // Given
        Metric metric1 = createMetric("org-123", "exp-456", "metric1", 100.0, MetricType.COUNTER);
        Metric metric2 = createMetric("org-123", "exp-456", "metric2", 200.0, MetricType.GAUGE);
        Metric metric3 = createMetric("org-123", "exp-456", "metric3", 300.0, MetricType.HISTOGRAM);

        // When
        metricRepository.saveAll(Arrays.asList(metric1, metric2, metric3));

        // Then
        Optional<Metric> saved1 = metricRepository.findById(metric1.getId());
        Optional<Metric> saved2 = metricRepository.findById(metric2.getId());
        Optional<Metric> saved3 = metricRepository.findById(metric3.getId());

        assertTrue(saved1.isPresent());
        assertTrue(saved2.isPresent());
        assertTrue(saved3.isPresent());
    }

    @Test
    void shouldFindByExperimentIdWithinTimeRange() {
        // Given
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        Metric metric1 = createMetricWithTimestamp("org-123", "exp-456", "metric1", 100.0, now.minus(30, ChronoUnit.MINUTES));
        Metric metric2 = createMetricWithTimestamp("org-123", "exp-456", "metric2", 200.0, now);
        Metric metric3 = createMetricWithTimestamp("org-123", "exp-456", "metric3", 300.0, now.plus(30, ChronoUnit.MINUTES));
        Metric metricOutside = createMetricWithTimestamp("org-123", "exp-456", "metric4", 400.0, now.minus(2, ChronoUnit.HOURS));

        metricRepository.saveAll(Arrays.asList(metric1, metric2, metric3, metricOutside));

        // When
        List<Metric> results = metricRepository.findByExperimentId("exp-456", start, end);

        // Then
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(m -> m.getId().equals(metric1.getId())));
        assertTrue(results.stream().anyMatch(m -> m.getId().equals(metric2.getId())));
        assertTrue(results.stream().anyMatch(m -> m.getId().equals(metric3.getId())));
        assertFalse(results.stream().anyMatch(m -> m.getId().equals(metricOutside.getId())));
    }

    @Test
    void shouldFindByExperimentIdAndNameWithinTimeRange() {
        // Given
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        Metric metric1 = createMetricWithTimestamp("org-123", "exp-456", "response_time", 100.0, now.minus(30, ChronoUnit.MINUTES));
        Metric metric2 = createMetricWithTimestamp("org-123", "exp-456", "response_time", 200.0, now);
        Metric metric3 = createMetricWithTimestamp("org-123", "exp-456", "error_rate", 300.0, now);

        metricRepository.saveAll(Arrays.asList(metric1, metric2, metric3));

        // When
        List<Metric> results = metricRepository.findByExperimentIdAndName("exp-456", "response_time", start, end);

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> m.getName().equals("response_time")));
    }

    @Test
    void shouldFindByOrganizationIdWithinTimeRange() {
        // Given
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        Metric metric1 = createMetricWithTimestamp("org-123", "exp-1", "metric1", 100.0, now);
        Metric metric2 = createMetricWithTimestamp("org-123", "exp-2", "metric2", 200.0, now);
        Metric metric3 = createMetricWithTimestamp("org-456", "exp-3", "metric3", 300.0, now);

        metricRepository.saveAll(Arrays.asList(metric1, metric2, metric3));

        // When
        List<Metric> results = metricRepository.findByOrganizationId("org-123", start, end);

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> m.getOrganizationId().equals("org-123")));
    }

    @Test
    void shouldReturnMetricsOrderedByTimestampDescending() {
        // Given
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        Metric metric1 = createMetricWithTimestamp("org-123", "exp-456", "metric", 100.0, now.minus(30, ChronoUnit.MINUTES));
        Metric metric2 = createMetricWithTimestamp("org-123", "exp-456", "metric", 200.0, now);
        Metric metric3 = createMetricWithTimestamp("org-123", "exp-456", "metric", 300.0, now.minus(15, ChronoUnit.MINUTES));

        metricRepository.saveAll(Arrays.asList(metric1, metric2, metric3));

        // When
        List<Metric> results = metricRepository.findByExperimentId("exp-456", start, end);

        // Then
        assertEquals(3, results.size());
        assertTrue(results.get(0).getTimestamp().isAfter(results.get(1).getTimestamp()) ||
                   results.get(0).getTimestamp().equals(results.get(1).getTimestamp()));
        assertTrue(results.get(1).getTimestamp().isAfter(results.get(2).getTimestamp()) ||
                   results.get(1).getTimestamp().equals(results.get(2).getTimestamp()));
    }

    @Test
    void shouldAggregateMetricsByExperiment() {
        // Given
        Metric metric1 = createMetric("org-123", "exp-456", "response_time", 100.0, MetricType.GAUGE);
        Metric metric2 = createMetric("org-123", "exp-456", "response_time", 200.0, MetricType.GAUGE);
        Metric metric3 = createMetric("org-123", "exp-456", "response_time", 300.0, MetricType.GAUGE);

        metricRepository.saveAll(Arrays.asList(metric1, metric2, metric3));

        // When
        Map<String, Double> aggregations = metricRepository.aggregateByExperiment("exp-456", "response_time");

        // Then
        assertNotNull(aggregations);
        assertEquals(200.0, aggregations.get("average"), 0.01);
        assertEquals(100.0, aggregations.get("min"), 0.01);
        assertEquals(300.0, aggregations.get("max"), 0.01);
        assertEquals(600.0, aggregations.get("sum"), 0.01);
        assertEquals(3.0, aggregations.get("count"), 0.01);
    }

    @Test
    void shouldReturnZeroAggregationsWhenNoMetricsFound() {
        // When
        Map<String, Double> aggregations = metricRepository.aggregateByExperiment("non-existent", "metric");

        // Then
        assertNotNull(aggregations);
        assertEquals(0.0, aggregations.get("average"));
        assertEquals(0.0, aggregations.get("min"));
        assertEquals(0.0, aggregations.get("max"));
        assertEquals(0.0, aggregations.get("sum"));
        assertEquals(0.0, aggregations.get("count"));
    }

    @Test
    void shouldDeleteByExperimentId() {
        // Given
        Metric metric1 = createMetric("org-123", "exp-456", "metric1", 100.0, MetricType.COUNTER);
        Metric metric2 = createMetric("org-123", "exp-456", "metric2", 200.0, MetricType.GAUGE);
        Metric metric3 = createMetric("org-123", "exp-789", "metric3", 300.0, MetricType.HISTOGRAM);

        metricRepository.saveAll(Arrays.asList(metric1, metric2, metric3));

        // When
        metricRepository.deleteByExperimentId("exp-456");

        // Then
        Optional<Metric> deleted1 = metricRepository.findById(metric1.getId());
        Optional<Metric> deleted2 = metricRepository.findById(metric2.getId());
        Optional<Metric> notDeleted = metricRepository.findById(metric3.getId());

        assertFalse(deleted1.isPresent());
        assertFalse(deleted2.isPresent());
        assertTrue(notDeleted.isPresent());
    }

    @Test
    void shouldHandleEmptyTimeRange() {
        // Given
        Instant now = Instant.now();
        Metric metric = createMetricWithTimestamp("org-123", "exp-456", "metric", 100.0, now);
        metricRepository.save(metric);

        // When
        List<Metric> results = metricRepository.findByExperimentId(
            "exp-456",
            now.plus(1, ChronoUnit.HOURS),
            now.plus(2, ChronoUnit.HOURS)
        );

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldPreserveMetricTypeOnSaveAndRetrieve() {
        // Given
        Metric counter = createMetric("org-123", "exp-456", "counter", 1.0, MetricType.COUNTER);
        Metric gauge = createMetric("org-123", "exp-456", "gauge", 2.0, MetricType.GAUGE);
        Metric histogram = createMetric("org-123", "exp-456", "histogram", 3.0, MetricType.HISTOGRAM);

        metricRepository.saveAll(Arrays.asList(counter, gauge, histogram));

        // When
        Optional<Metric> savedCounter = metricRepository.findById(counter.getId());
        Optional<Metric> savedGauge = metricRepository.findById(gauge.getId());
        Optional<Metric> savedHistogram = metricRepository.findById(histogram.getId());

        // Then
        assertTrue(savedCounter.isPresent());
        assertEquals(MetricType.COUNTER, savedCounter.get().getType());

        assertTrue(savedGauge.isPresent());
        assertEquals(MetricType.GAUGE, savedGauge.get().getType());

        assertTrue(savedHistogram.isPresent());
        assertEquals(MetricType.HISTOGRAM, savedHistogram.get().getType());
    }

    @Test
    void shouldHandleNegativeValues() {
        // Given
        Metric metric = createMetric("org-123", "exp-456", "temperature", -50.5, MetricType.GAUGE);

        // When
        metricRepository.save(metric);

        // Then
        Optional<Metric> saved = metricRepository.findById(metric.getId());
        assertTrue(saved.isPresent());
        assertEquals(-50.5, saved.get().getValue());
    }

    @Test
    void shouldHandleVeryLargeValues() {
        // Given
        Metric metric = createMetric("org-123", "exp-456", "large_value", 1.7976931348623157E308, MetricType.COUNTER);

        // When
        metricRepository.save(metric);

        // Then
        Optional<Metric> saved = metricRepository.findById(metric.getId());
        assertTrue(saved.isPresent());
        assertEquals(1.7976931348623157E308, saved.get().getValue());
    }

    @Test
    void shouldFindByIdReturnEmptyWhenNotFound() {
        // When
        Optional<Metric> result = metricRepository.findById(MetricId.generate());

        // Then
        assertFalse(result.isPresent());
    }

    // Helper methods
    private Metric createMetric(String orgId, String expId, String name, Double value, MetricType type) {
        return new Metric(
            MetricId.generate(),
            orgId,
            expId,
            name,
            value,
            type,
            Instant.now()
        );
    }

    private Metric createMetricWithTimestamp(String orgId, String expId, String name, Double value, Instant timestamp) {
        return new Metric(
            MetricId.generate(),
            orgId,
            expId,
            name,
            value,
            MetricType.GAUGE,
            timestamp
        );
    }
}
