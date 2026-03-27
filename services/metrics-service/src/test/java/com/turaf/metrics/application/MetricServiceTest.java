package com.turaf.metrics.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.metrics.application.dto.MetricDto;
import com.turaf.metrics.application.dto.RecordMetricRequest;
import com.turaf.metrics.domain.*;
import com.turaf.metrics.domain.event.MetricBatchRecorded;
import com.turaf.metrics.domain.event.MetricRecorded;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private EventPublisher eventPublisher;

    private MetricService metricService;

    @BeforeEach
    void setUp() {
        metricService = new MetricService(metricRepository, eventPublisher);
        TenantContextHolder.setOrganizationId("org-123");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldRecordMetric() {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-456",
            "response_time",
            125.5,
            "GAUGE"
        );

        // When
        MetricDto result = metricService.recordMetric(request);

        // Then
        assertNotNull(result);
        assertEquals("org-123", result.getOrganizationId());
        assertEquals("exp-456", result.getExperimentId());
        assertEquals("response_time", result.getName());
        assertEquals(125.5, result.getValue());
        assertEquals("GAUGE", result.getType());
        assertNotNull(result.getId());
        assertNotNull(result.getTimestamp());

        verify(metricRepository).save(any(Metric.class));
        verify(eventPublisher).publish(any(MetricRecorded.class));
    }

    @Test
    void shouldRecordMetricWithCustomTimestamp() {
        // Given
        Instant customTimestamp = Instant.now().minus(1, ChronoUnit.HOURS);
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-456",
            "cpu_usage",
            75.0,
            "GAUGE",
            customTimestamp,
            null
        );

        // When
        MetricDto result = metricService.recordMetric(request);

        // Then
        assertEquals(customTimestamp, result.getTimestamp());
        verify(metricRepository).save(any(Metric.class));
    }

    @Test
    void shouldRecordMetricWithTags() {
        // Given
        Map<String, String> tags = new HashMap<>();
        tags.put("region", "us-east-1");
        tags.put("environment", "production");

        RecordMetricRequest request = new RecordMetricRequest(
            "exp-456",
            "error_rate",
            0.05,
            "COUNTER",
            null,
            tags
        );

        ArgumentCaptor<Metric> metricCaptor = ArgumentCaptor.forClass(Metric.class);

        // When
        MetricDto result = metricService.recordMetric(request);

        // Then
        verify(metricRepository).save(metricCaptor.capture());
        Metric savedMetric = metricCaptor.getValue();
        assertEquals(2, savedMetric.getTags().size());
        assertEquals("us-east-1", savedMetric.getTag("region"));
        assertEquals("production", savedMetric.getTag("environment"));

        assertEquals(2, result.getTags().size());
    }

    @Test
    void shouldPublishMetricRecordedEvent() {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-456",
            "metric",
            100.0,
            "COUNTER"
        );

        ArgumentCaptor<MetricRecorded> eventCaptor = ArgumentCaptor.forClass(MetricRecorded.class);

        // When
        metricService.recordMetric(request);

        // Then
        verify(eventPublisher).publish(eventCaptor.capture());
        MetricRecorded event = eventCaptor.getValue();

        assertNotNull(event.getEventId());
        assertEquals("org-123", event.getOrganizationId());
        assertEquals("exp-456", event.getExperimentId());
        assertEquals("metric", event.getMetricName());
        assertEquals(100.0, event.getValue());
        assertEquals("COUNTER", event.getMetricType());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void shouldRecordBatchMetrics() {
        // Given
        RecordMetricRequest request1 = new RecordMetricRequest("exp-456", "metric1", 100.0, "COUNTER");
        RecordMetricRequest request2 = new RecordMetricRequest("exp-456", "metric2", 200.0, "GAUGE");
        RecordMetricRequest request3 = new RecordMetricRequest("exp-456", "metric3", 300.0, "HISTOGRAM");

        List<RecordMetricRequest> requests = Arrays.asList(request1, request2, request3);

        // When
        List<MetricDto> results = metricService.recordBatch(requests);

        // Then
        assertEquals(3, results.size());
        assertEquals("metric1", results.get(0).getName());
        assertEquals("metric2", results.get(1).getName());
        assertEquals("metric3", results.get(2).getName());

        verify(metricRepository).saveAll(any(List.class));
        verify(eventPublisher).publish(any(MetricBatchRecorded.class));
    }

    @Test
    void shouldPublishBatchRecordedEvent() {
        // Given
        RecordMetricRequest request1 = new RecordMetricRequest("exp-456", "metric1", 100.0, "COUNTER");
        RecordMetricRequest request2 = new RecordMetricRequest("exp-456", "metric2", 200.0, "GAUGE");

        List<RecordMetricRequest> requests = Arrays.asList(request1, request2);

        ArgumentCaptor<MetricBatchRecorded> eventCaptor = ArgumentCaptor.forClass(MetricBatchRecorded.class);

        // When
        metricService.recordBatch(requests);

        // Then
        verify(eventPublisher).publish(eventCaptor.capture());
        MetricBatchRecorded event = eventCaptor.getValue();

        assertNotNull(event.getEventId());
        assertEquals("org-123", event.getOrganizationId());
        assertEquals("exp-456", event.getExperimentId());
        assertEquals(2, event.getMetricCount());
    }

    @Test
    void shouldNotPublishBatchEventWhenRequestsEmpty() {
        // Given
        List<RecordMetricRequest> emptyRequests = new ArrayList<>();

        // When
        List<MetricDto> results = metricService.recordBatch(emptyRequests);

        // Then
        assertTrue(results.isEmpty());
        verify(metricRepository).saveAll(any(List.class));
        verify(eventPublisher, never()).publish(any(MetricBatchRecorded.class));
    }

    @Test
    void shouldGetMetricsByExperimentId() {
        // Given
        String experimentId = "exp-456";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(
            createMetric("metric1", 100.0),
            createMetric("metric2", 200.0)
        );

        when(metricRepository.findByExperimentId(experimentId, start, end)).thenReturn(metrics);

        // When
        List<MetricDto> results = metricService.getMetrics(experimentId, null, start, end);

        // Then
        assertEquals(2, results.size());
        verify(metricRepository).findByExperimentId(experimentId, start, end);
        verify(metricRepository, never()).findByExperimentIdAndName(anyString(), anyString(), any(), any());
    }

    @Test
    void shouldGetMetricsByExperimentIdAndName() {
        // Given
        String experimentId = "exp-456";
        String name = "response_time";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(
            createMetric(name, 100.0),
            createMetric(name, 150.0)
        );

        when(metricRepository.findByExperimentIdAndName(experimentId, name, start, end)).thenReturn(metrics);

        // When
        List<MetricDto> results = metricService.getMetrics(experimentId, name, start, end);

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> m.getName().equals(name)));
        verify(metricRepository).findByExperimentIdAndName(experimentId, name, start, end);
        verify(metricRepository, never()).findByExperimentId(anyString(), any(), any());
    }

    @Test
    void shouldGetMetricsByExperimentIdWhenNameIsBlank() {
        // Given
        String experimentId = "exp-456";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(createMetric("metric1", 100.0));
        when(metricRepository.findByExperimentId(experimentId, start, end)).thenReturn(metrics);

        // When
        List<MetricDto> results = metricService.getMetrics(experimentId, "", start, end);

        // Then
        assertEquals(1, results.size());
        verify(metricRepository).findByExperimentId(experimentId, start, end);
    }

    @Test
    void shouldGetMetricsByOrganization() {
        // Given
        String organizationId = "org-123";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<Metric> metrics = Arrays.asList(
            createMetric("metric1", 100.0),
            createMetric("metric2", 200.0),
            createMetric("metric3", 300.0)
        );

        when(metricRepository.findByOrganizationId(organizationId, start, end)).thenReturn(metrics);

        // When
        List<MetricDto> results = metricService.getMetricsByOrganization(organizationId, start, end);

        // Then
        assertEquals(3, results.size());
        verify(metricRepository).findByOrganizationId(organizationId, start, end);
    }

    @Test
    void shouldReturnEmptyListWhenNoMetricsFound() {
        // Given
        String experimentId = "exp-456";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();

        when(metricRepository.findByExperimentId(experimentId, start, end)).thenReturn(new ArrayList<>());

        // When
        List<MetricDto> results = metricService.getMetrics(experimentId, null, start, end);

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldDeleteMetricsByExperiment() {
        // Given
        String experimentId = "exp-456";

        // When
        metricService.deleteMetricsByExperiment(experimentId);

        // Then
        verify(metricRepository).deleteByExperimentId(experimentId);
    }

    @Test
    void shouldUseCurrentTimestampWhenNotProvided() {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-456",
            "metric",
            100.0,
            "COUNTER"
        );
        request.setTimestamp(null);

        Instant before = Instant.now();

        // When
        MetricDto result = metricService.recordMetric(request);

        Instant after = Instant.now();

        // Then
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().isAfter(before.minus(1, ChronoUnit.SECONDS)));
        assertTrue(result.getTimestamp().isBefore(after.plus(1, ChronoUnit.SECONDS)));
    }

    @Test
    void shouldHandleAllMetricTypes() {
        // Given
        RecordMetricRequest counterRequest = new RecordMetricRequest("exp-456", "counter", 1.0, "COUNTER");
        RecordMetricRequest gaugeRequest = new RecordMetricRequest("exp-456", "gauge", 2.0, "GAUGE");
        RecordMetricRequest histogramRequest = new RecordMetricRequest("exp-456", "histogram", 3.0, "HISTOGRAM");

        // When
        MetricDto counter = metricService.recordMetric(counterRequest);
        MetricDto gauge = metricService.recordMetric(gaugeRequest);
        MetricDto histogram = metricService.recordMetric(histogramRequest);

        // Then
        assertEquals("COUNTER", counter.getType());
        assertEquals("GAUGE", gauge.getType());
        assertEquals("HISTOGRAM", histogram.getType());
    }

    @Test
    void shouldUseTenantContextForOrganizationId() {
        // Given
        TenantContextHolder.setOrganizationId("custom-org-789");
        RecordMetricRequest request = new RecordMetricRequest("exp-456", "metric", 100.0, "COUNTER");

        // When
        MetricDto result = metricService.recordMetric(request);

        // Then
        assertEquals("custom-org-789", result.getOrganizationId());

        TenantContextHolder.clear();
    }

    @Test
    void shouldRecordBatchWithDifferentMetricTypes() {
        // Given
        RecordMetricRequest counter = new RecordMetricRequest("exp-456", "counter", 1.0, "COUNTER");
        RecordMetricRequest gauge = new RecordMetricRequest("exp-456", "gauge", 2.0, "GAUGE");
        RecordMetricRequest histogram = new RecordMetricRequest("exp-456", "histogram", 3.0, "HISTOGRAM");

        List<RecordMetricRequest> requests = Arrays.asList(counter, gauge, histogram);

        // When
        List<MetricDto> results = metricService.recordBatch(requests);

        // Then
        assertEquals(3, results.size());
        assertEquals("COUNTER", results.get(0).getType());
        assertEquals("GAUGE", results.get(1).getType());
        assertEquals("HISTOGRAM", results.get(2).getType());
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
