package com.turaf.metrics.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.multitenancy.TenantContextHolder;
import com.turaf.metrics.application.dto.BatchRecordRequest;
import com.turaf.metrics.application.dto.RecordMetricRequest;
import com.turaf.metrics.domain.Metric;
import com.turaf.metrics.domain.MetricRepository;
import com.turaf.metrics.domain.event.MetricBatchRecorded;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchMetricServiceTest {

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private EventPublisher eventPublisher;

    private BatchMetricService batchMetricService;

    @BeforeEach
    void setUp() {
        batchMetricService = new BatchMetricService(metricRepository, eventPublisher);
        TenantContextHolder.setOrganizationId("org-123");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldRecordBatch() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(10);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        // When
        batchMetricService.recordBatch(request);

        // Then
        verify(metricRepository).saveAll(any(List.class));
        verify(eventPublisher).publish(any(MetricBatchRecorded.class));
    }

    @Test
    void shouldRecordBatchAndReturnCount() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(25);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        // When
        int count = batchMetricService.recordBatchAndReturnCount(request);

        // Then
        assertEquals(25, count);
        verify(metricRepository).saveAll(any(List.class));
        verify(eventPublisher).publish(any(MetricBatchRecorded.class));
    }

    @Test
    void shouldPublishBatchEvent() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(5);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        ArgumentCaptor<MetricBatchRecorded> eventCaptor = ArgumentCaptor.forClass(MetricBatchRecorded.class);

        // When
        batchMetricService.recordBatch(request);

        // Then
        verify(eventPublisher).publish(eventCaptor.capture());
        MetricBatchRecorded event = eventCaptor.getValue();

        assertNotNull(event.getEventId());
        assertEquals("org-123", event.getOrganizationId());
        assertEquals("exp-456", event.getExperimentId());
        assertEquals(5, event.getMetricCount());
    }

    @Test
    void shouldPartitionLargeBatches() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(2500);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        // When
        batchMetricService.recordBatch(request);

        // Then
        // Should be called 3 times: 1000 + 1000 + 500
        verify(metricRepository, times(3)).saveAll(any(List.class));
        verify(eventPublisher).publish(any(MetricBatchRecorded.class));
    }

    @Test
    void shouldHandleExactlyOneBatchSize() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(1000);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        // When
        batchMetricService.recordBatch(request);

        // Then
        verify(metricRepository, times(1)).saveAll(any(List.class));
    }

    @Test
    void shouldHandleExactlyTwoBatchSizes() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(2000);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        // When
        batchMetricService.recordBatch(request);

        // Then
        verify(metricRepository, times(2)).saveAll(any(List.class));
    }

    @Test
    void shouldHandleSmallBatch() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(1);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        // When
        batchMetricService.recordBatch(request);

        // Then
        verify(metricRepository, times(1)).saveAll(any(List.class));
        verify(eventPublisher).publish(any(MetricBatchRecorded.class));
    }

    @Test
    void shouldCreateMetricsWithCorrectOrganizationId() {
        // Given
        TenantContextHolder.setOrganizationId("custom-org-789");
        List<RecordMetricRequest> metrics = createMetricRequests(3);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        ArgumentCaptor<List<Metric>> metricsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        batchMetricService.recordBatch(request);

        // Then
        verify(metricRepository).saveAll(metricsCaptor.capture());
        List<Metric> savedMetrics = metricsCaptor.getValue();

        assertTrue(savedMetrics.stream().allMatch(m -> m.getOrganizationId().equals("custom-org-789")));

        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateMetricsWithCustomTimestamps() {
        // Given
        Instant customTimestamp = Instant.parse("2024-01-01T12:00:00Z");
        List<RecordMetricRequest> metrics = new ArrayList<>();
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-456",
            "metric",
            100.0,
            "GAUGE",
            customTimestamp,
            null
        );
        metrics.add(request);

        BatchRecordRequest batchRequest = new BatchRecordRequest("exp-456", metrics);

        ArgumentCaptor<List<Metric>> metricsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        batchMetricService.recordBatch(batchRequest);

        // Then
        verify(metricRepository).saveAll(metricsCaptor.capture());
        List<Metric> savedMetrics = metricsCaptor.getValue();

        assertEquals(customTimestamp, savedMetrics.get(0).getTimestamp());
    }

    @Test
    void shouldCreateMetricsWithTags() {
        // Given
        Map<String, String> tags = new HashMap<>();
        tags.put("region", "us-east-1");
        tags.put("environment", "production");

        List<RecordMetricRequest> metrics = new ArrayList<>();
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-456",
            "metric",
            100.0,
            "GAUGE",
            null,
            tags
        );
        metrics.add(request);

        BatchRecordRequest batchRequest = new BatchRecordRequest("exp-456", metrics);

        ArgumentCaptor<List<Metric>> metricsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        batchMetricService.recordBatch(batchRequest);

        // Then
        verify(metricRepository).saveAll(metricsCaptor.capture());
        List<Metric> savedMetrics = metricsCaptor.getValue();

        assertEquals(2, savedMetrics.get(0).getTags().size());
        assertEquals("us-east-1", savedMetrics.get(0).getTag("region"));
        assertEquals("production", savedMetrics.get(0).getTag("environment"));
    }

    @Test
    void shouldHandleMixedMetricTypes() {
        // Given
        List<RecordMetricRequest> metrics = new ArrayList<>();
        metrics.add(new RecordMetricRequest("exp-456", "counter", 1.0, "COUNTER"));
        metrics.add(new RecordMetricRequest("exp-456", "gauge", 2.0, "GAUGE"));
        metrics.add(new RecordMetricRequest("exp-456", "histogram", 3.0, "HISTOGRAM"));

        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        ArgumentCaptor<List<Metric>> metricsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        batchMetricService.recordBatch(request);

        // Then
        verify(metricRepository).saveAll(metricsCaptor.capture());
        List<Metric> savedMetrics = metricsCaptor.getValue();

        assertEquals(3, savedMetrics.size());
        assertEquals("COUNTER", savedMetrics.get(0).getType().name());
        assertEquals("GAUGE", savedMetrics.get(1).getType().name());
        assertEquals("HISTOGRAM", savedMetrics.get(2).getType().name());
    }

    @Test
    void shouldGenerateUniqueIdsForEachMetric() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(100);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        ArgumentCaptor<List<Metric>> metricsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        batchMetricService.recordBatch(request);

        // Then
        verify(metricRepository).saveAll(metricsCaptor.capture());
        List<Metric> savedMetrics = metricsCaptor.getValue();

        long uniqueIds = savedMetrics.stream()
            .map(m -> m.getId().getValue())
            .distinct()
            .count();

        assertEquals(100, uniqueIds);
    }

    @Test
    void shouldUseCurrentTimestampWhenNotProvided() {
        // Given
        List<RecordMetricRequest> metrics = new ArrayList<>();
        RecordMetricRequest request = new RecordMetricRequest("exp-456", "metric", 100.0, "GAUGE");
        request.setTimestamp(null);
        metrics.add(request);

        BatchRecordRequest batchRequest = new BatchRecordRequest("exp-456", metrics);

        ArgumentCaptor<List<Metric>> metricsCaptor = ArgumentCaptor.forClass(List.class);

        Instant before = Instant.now();

        // When
        batchMetricService.recordBatch(batchRequest);

        Instant after = Instant.now();

        // Then
        verify(metricRepository).saveAll(metricsCaptor.capture());
        List<Metric> savedMetrics = metricsCaptor.getValue();

        Instant timestamp = savedMetrics.get(0).getTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp.isAfter(before.minusSeconds(1)));
        assertTrue(timestamp.isBefore(after.plusSeconds(1)));
    }

    @Test
    void shouldHandleVeryLargeBatch() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(5000);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        // When
        batchMetricService.recordBatch(request);

        // Then
        // Should be called 5 times: 1000 * 5
        verify(metricRepository, times(5)).saveAll(any(List.class));
        verify(eventPublisher).publish(any(MetricBatchRecorded.class));
    }

    @Test
    void shouldHandleBatchWithPartialLastBatch() {
        // Given
        List<RecordMetricRequest> metrics = createMetricRequests(1250);
        BatchRecordRequest request = new BatchRecordRequest("exp-456", metrics);

        // When
        batchMetricService.recordBatch(request);

        // Then
        // Should be called 2 times: 1000 + 250
        verify(metricRepository, times(2)).saveAll(any(List.class));
    }

    @Test
    void shouldPreserveAllMetricData() {
        // Given
        List<RecordMetricRequest> metrics = new ArrayList<>();
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-456",
            "test_metric",
            123.45,
            "GAUGE"
        );
        metrics.add(request);

        BatchRecordRequest batchRequest = new BatchRecordRequest("exp-456", metrics);

        ArgumentCaptor<List<Metric>> metricsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        batchMetricService.recordBatch(batchRequest);

        // Then
        verify(metricRepository).saveAll(metricsCaptor.capture());
        List<Metric> savedMetrics = metricsCaptor.getValue();

        Metric savedMetric = savedMetrics.get(0);
        assertEquals("org-123", savedMetric.getOrganizationId());
        assertEquals("exp-456", savedMetric.getExperimentId());
        assertEquals("test_metric", savedMetric.getName());
        assertEquals(123.45, savedMetric.getValue());
        assertEquals("GAUGE", savedMetric.getType().name());
    }

    // Helper method
    private List<RecordMetricRequest> createMetricRequests(int count) {
        List<RecordMetricRequest> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            requests.add(new RecordMetricRequest(
                "exp-456",
                "metric_" + i,
                (double) i,
                "GAUGE"
            ));
        }
        return requests;
    }
}
