package com.turaf.metrics.application;

import com.turaf.metrics.application.dto.AggregatedMetricsDto;
import com.turaf.metrics.domain.Metric;
import com.turaf.metrics.domain.MetricId;
import com.turaf.metrics.domain.MetricRepository;
import com.turaf.metrics.domain.MetricType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregationServiceTest {

    @Mock
    private MetricRepository metricRepository;

    private AggregationService aggregationService;

    @BeforeEach
    void setUp() {
        aggregationService = new AggregationService(metricRepository);
    }

    @Test
    void shouldAggregateMetrics() {
        // Given
        String experimentId = "exp-123";
        String metricName = "response_time";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(
            createMetric(metricName, 100.0),
            createMetric(metricName, 200.0),
            createMetric(metricName, 300.0)
        );

        when(metricRepository.findByExperimentIdAndName(experimentId, metricName, start, end))
            .thenReturn(metrics);

        // When
        AggregatedMetricsDto result = aggregationService.aggregateMetrics(experimentId, metricName, start, end);

        // Then
        assertNotNull(result);
        assertEquals(metricName, result.getMetricName());
        assertEquals(3, result.getCount());
        assertEquals(600.0, result.getSum(), 0.01);
        assertEquals(200.0, result.getAverage(), 0.01);
        assertEquals(100.0, result.getMin(), 0.01);
        assertEquals(300.0, result.getMax(), 0.01);
        assertEquals(start, result.getStartTime());
        assertEquals(end, result.getEndTime());

        verify(metricRepository).findByExperimentIdAndName(experimentId, metricName, start, end);
    }

    @Test
    void shouldReturnEmptyAggregationWhenNoMetrics() {
        // Given
        String experimentId = "exp-123";
        String metricName = "response_time";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        when(metricRepository.findByExperimentIdAndName(experimentId, metricName, start, end))
            .thenReturn(Collections.emptyList());

        // When
        AggregatedMetricsDto result = aggregationService.aggregateMetrics(experimentId, metricName, start, end);

        // Then
        assertNotNull(result);
        assertNull(result.getMetricName());
        assertEquals(0, result.getCount());
        assertEquals(0.0, result.getSum());
        assertEquals(0.0, result.getAverage());
        assertEquals(0.0, result.getMin());
        assertEquals(0.0, result.getMax());
    }

    @Test
    void shouldAggregateAllMetrics() {
        // Given
        String experimentId = "exp-123";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(
            createMetric("response_time", 100.0),
            createMetric("response_time", 200.0),
            createMetric("error_rate", 0.01),
            createMetric("error_rate", 0.02),
            createMetric("cpu_usage", 75.0)
        );

        when(metricRepository.findByExperimentId(experimentId, start, end))
            .thenReturn(metrics);

        // When
        Map<String, AggregatedMetricsDto> results = aggregationService.aggregateAllMetrics(experimentId, start, end);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());

        AggregatedMetricsDto responseTime = results.get("response_time");
        assertNotNull(responseTime);
        assertEquals(2, responseTime.getCount());
        assertEquals(150.0, responseTime.getAverage(), 0.01);

        AggregatedMetricsDto errorRate = results.get("error_rate");
        assertNotNull(errorRate);
        assertEquals(2, errorRate.getCount());
        assertEquals(0.015, errorRate.getAverage(), 0.001);

        AggregatedMetricsDto cpuUsage = results.get("cpu_usage");
        assertNotNull(cpuUsage);
        assertEquals(1, cpuUsage.getCount());
        assertEquals(75.0, cpuUsage.getAverage(), 0.01);
    }

    @Test
    void shouldReturnEmptyMapWhenNoMetricsForAllAggregation() {
        // Given
        String experimentId = "exp-123";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        when(metricRepository.findByExperimentId(experimentId, start, end))
            .thenReturn(Collections.emptyList());

        // When
        Map<String, AggregatedMetricsDto> results = aggregationService.aggregateAllMetrics(experimentId, start, end);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldCalculateCorrectStatisticsForSingleMetric() {
        // Given
        String experimentId = "exp-123";
        String metricName = "single_metric";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Collections.singletonList(
            createMetric(metricName, 42.0)
        );

        when(metricRepository.findByExperimentIdAndName(experimentId, metricName, start, end))
            .thenReturn(metrics);

        // When
        AggregatedMetricsDto result = aggregationService.aggregateMetrics(experimentId, metricName, start, end);

        // Then
        assertEquals(1, result.getCount());
        assertEquals(42.0, result.getSum());
        assertEquals(42.0, result.getAverage());
        assertEquals(42.0, result.getMin());
        assertEquals(42.0, result.getMax());
    }

    @Test
    void shouldHandleNegativeValues() {
        // Given
        String experimentId = "exp-123";
        String metricName = "temperature";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(
            createMetric(metricName, -10.0),
            createMetric(metricName, -5.0),
            createMetric(metricName, 0.0),
            createMetric(metricName, 5.0)
        );

        when(metricRepository.findByExperimentIdAndName(experimentId, metricName, start, end))
            .thenReturn(metrics);

        // When
        AggregatedMetricsDto result = aggregationService.aggregateMetrics(experimentId, metricName, start, end);

        // Then
        assertEquals(4, result.getCount());
        assertEquals(-10.0, result.getSum(), 0.01);
        assertEquals(-2.5, result.getAverage(), 0.01);
        assertEquals(-10.0, result.getMin(), 0.01);
        assertEquals(5.0, result.getMax(), 0.01);
    }

    @Test
    void shouldHandleLargeNumberOfMetrics() {
        // Given
        String experimentId = "exp-123";
        String metricName = "large_dataset";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            metrics.add(createMetric(metricName, (double) i));
        }

        when(metricRepository.findByExperimentIdAndName(experimentId, metricName, start, end))
            .thenReturn(metrics);

        // When
        AggregatedMetricsDto result = aggregationService.aggregateMetrics(experimentId, metricName, start, end);

        // Then
        assertEquals(1000, result.getCount());
        assertEquals(500500.0, result.getSum(), 0.01);
        assertEquals(500.5, result.getAverage(), 0.01);
        assertEquals(1.0, result.getMin(), 0.01);
        assertEquals(1000.0, result.getMax(), 0.01);
    }

    @Test
    void shouldGetStatisticsFromRepository() {
        // Given
        String experimentId = "exp-123";
        String metricName = "response_time";

        Map<String, Double> expectedStats = new HashMap<>();
        expectedStats.put("average", 150.0);
        expectedStats.put("min", 100.0);
        expectedStats.put("max", 200.0);
        expectedStats.put("sum", 450.0);
        expectedStats.put("count", 3.0);

        when(metricRepository.aggregateByExperiment(experimentId, metricName))
            .thenReturn(expectedStats);

        // When
        Map<String, Double> result = aggregationService.getStatistics(experimentId, metricName);

        // Then
        assertNotNull(result);
        assertEquals(expectedStats, result);
        verify(metricRepository).aggregateByExperiment(experimentId, metricName);
    }

    @Test
    void shouldAggregateByOrganization() {
        // Given
        String organizationId = "org-123";
        String metricName = "response_time";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> allMetrics = Arrays.asList(
            createMetric(metricName, 100.0),
            createMetric(metricName, 200.0),
            createMetric("other_metric", 300.0)
        );

        when(metricRepository.findByOrganizationId(organizationId, start, end))
            .thenReturn(allMetrics);

        // When
        AggregatedMetricsDto result = aggregationService.aggregateByOrganization(
            organizationId, metricName, start, end
        );

        // Then
        assertNotNull(result);
        assertEquals(metricName, result.getMetricName());
        assertEquals(2, result.getCount());
        assertEquals(150.0, result.getAverage(), 0.01);
        assertEquals(100.0, result.getMin(), 0.01);
        assertEquals(200.0, result.getMax(), 0.01);
    }

    @Test
    void shouldReturnEmptyWhenNoOrganizationMetrics() {
        // Given
        String organizationId = "org-123";
        String metricName = "response_time";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        when(metricRepository.findByOrganizationId(organizationId, start, end))
            .thenReturn(Collections.emptyList());

        // When
        AggregatedMetricsDto result = aggregationService.aggregateByOrganization(
            organizationId, metricName, start, end
        );

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCount());
    }

    @Test
    void shouldHandleDecimalPrecision() {
        // Given
        String experimentId = "exp-123";
        String metricName = "precision_test";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(
            createMetric(metricName, 0.1),
            createMetric(metricName, 0.2),
            createMetric(metricName, 0.3)
        );

        when(metricRepository.findByExperimentIdAndName(experimentId, metricName, start, end))
            .thenReturn(metrics);

        // When
        AggregatedMetricsDto result = aggregationService.aggregateMetrics(experimentId, metricName, start, end);

        // Then
        assertEquals(3, result.getCount());
        assertEquals(0.6, result.getSum(), 0.0001);
        assertEquals(0.2, result.getAverage(), 0.0001);
        assertEquals(0.1, result.getMin(), 0.0001);
        assertEquals(0.3, result.getMax(), 0.0001);
    }

    @Test
    void shouldAggregateMultipleMetricsWithSameName() {
        // Given
        String experimentId = "exp-123";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(
            createMetric("metric", 1.0),
            createMetric("metric", 2.0),
            createMetric("metric", 3.0),
            createMetric("metric", 4.0),
            createMetric("metric", 5.0)
        );

        when(metricRepository.findByExperimentId(experimentId, start, end))
            .thenReturn(metrics);

        // When
        Map<String, AggregatedMetricsDto> results = aggregationService.aggregateAllMetrics(experimentId, start, end);

        // Then
        assertEquals(1, results.size());
        AggregatedMetricsDto result = results.get("metric");
        assertEquals(5, result.getCount());
        assertEquals(15.0, result.getSum(), 0.01);
        assertEquals(3.0, result.getAverage(), 0.01);
    }

    @Test
    void shouldPreserveTimeRangeInAggregation() {
        // Given
        String experimentId = "exp-123";
        String metricName = "metric";
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");

        List<Metric> metrics = Collections.singletonList(createMetric(metricName, 100.0));

        when(metricRepository.findByExperimentIdAndName(experimentId, metricName, start, end))
            .thenReturn(metrics);

        // When
        AggregatedMetricsDto result = aggregationService.aggregateMetrics(experimentId, metricName, start, end);

        // Then
        assertEquals(start, result.getStartTime());
        assertEquals(end, result.getEndTime());
    }

    // Helper method
    private Metric createMetric(String name, Double value) {
        return new Metric(
            MetricId.generate(),
            "org-123",
            "exp-456",
            name,
            value,
            MetricType.GAUGE,
            Instant.now()
        );
    }
}
