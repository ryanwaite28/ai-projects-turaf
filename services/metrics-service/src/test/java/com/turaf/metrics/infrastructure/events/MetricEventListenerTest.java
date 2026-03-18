package com.turaf.metrics.infrastructure.events;

import com.turaf.metrics.domain.event.MetricBatchRecorded;
import com.turaf.metrics.domain.event.MetricRecorded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MetricEventListenerTest {

    private MetricEventListener metricEventListener;

    @BeforeEach
    void setUp() {
        metricEventListener = new MetricEventListener();
    }

    @Test
    void shouldHandleMetricRecordedEvent() {
        // Given
        MetricRecorded event = new MetricRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            "metric-id",
            "response_time",
            125.5,
            "GAUGE",
            Instant.now()
        );

        // When & Then
        assertDoesNotThrow(() -> metricEventListener.handleMetricRecorded(event));
    }

    @Test
    void shouldHandleMetricBatchRecordedEvent() {
        // Given
        MetricBatchRecorded event = new MetricBatchRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            100
        );

        // When & Then
        assertDoesNotThrow(() -> metricEventListener.handleMetricBatchRecorded(event));
    }

    @Test
    void shouldHandleMultipleMetricRecordedEvents() {
        // Given
        MetricRecorded event1 = new MetricRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            "metric-1",
            "metric1",
            100.0,
            "GAUGE",
            Instant.now()
        );

        MetricRecorded event2 = new MetricRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            "metric-2",
            "metric2",
            200.0,
            "COUNTER",
            Instant.now()
        );

        // When & Then
        assertDoesNotThrow(() -> {
            metricEventListener.handleMetricRecorded(event1);
            metricEventListener.handleMetricRecorded(event2);
        });
    }

    @Test
    void shouldHandleMultipleBatchRecordedEvents() {
        // Given
        MetricBatchRecorded event1 = new MetricBatchRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            50
        );

        MetricBatchRecorded event2 = new MetricBatchRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-789",
            100
        );

        // When & Then
        assertDoesNotThrow(() -> {
            metricEventListener.handleMetricBatchRecorded(event1);
            metricEventListener.handleMetricBatchRecorded(event2);
        });
    }

    @Test
    void shouldHandleMixedEvents() {
        // Given
        MetricRecorded recordedEvent = new MetricRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            "metric-id",
            "metric",
            100.0,
            "GAUGE",
            Instant.now()
        );

        MetricBatchRecorded batchEvent = new MetricBatchRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            25
        );

        // When & Then
        assertDoesNotThrow(() -> {
            metricEventListener.handleMetricRecorded(recordedEvent);
            metricEventListener.handleMetricBatchRecorded(batchEvent);
        });
    }
}
