package com.turaf.metrics.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetricTest {

    @Test
    void shouldCreateMetricWithValidData() {
        // Given
        MetricId id = MetricId.generate();
        String organizationId = "org-123";
        String experimentId = "exp-456";
        String name = "response_time";
        Double value = 125.5;
        MetricType type = MetricType.GAUGE;
        Instant timestamp = Instant.now();

        // When
        Metric metric = new Metric(id, organizationId, experimentId, name, value, type, timestamp);

        // Then
        assertNotNull(metric);
        assertEquals(id, metric.getId());
        assertEquals(organizationId, metric.getOrganizationId());
        assertEquals(experimentId, metric.getExperimentId());
        assertEquals(name, metric.getName());
        assertEquals(value, metric.getValue());
        assertEquals(type, metric.getType());
        assertEquals(timestamp, metric.getTimestamp());
        assertTrue(metric.getTags().isEmpty());
    }

    @Test
    void shouldCreateMetricWithTags() {
        // Given
        MetricId id = MetricId.generate();
        Map<String, String> tags = new HashMap<>();
        tags.put("region", "us-east-1");
        tags.put("environment", "production");

        // When
        Metric metric = new Metric(id, "org-123", "exp-456", "cpu_usage", 75.0, 
                                   MetricType.GAUGE, Instant.now(), tags);

        // Then
        assertEquals(2, metric.getTags().size());
        assertEquals("us-east-1", metric.getTag("region"));
        assertEquals("production", metric.getTag("environment"));
    }

    @Test
    void shouldAddTag() {
        // Given
        Metric metric = createValidMetric();

        // When
        metric.addTag("version", "1.0.0");

        // Then
        assertTrue(metric.hasTag("version"));
        assertEquals("1.0.0", metric.getTag("version"));
    }

    @Test
    void shouldRemoveTag() {
        // Given
        Metric metric = createValidMetric();
        metric.addTag("temp", "value");

        // When
        metric.removeTag("temp");

        // Then
        assertFalse(metric.hasTag("temp"));
        assertNull(metric.getTag("temp"));
    }

    @Test
    void shouldThrowExceptionWhenAddingTagWithNullKey() {
        // Given
        Metric metric = createValidMetric();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> metric.addTag(null, "value"));
    }

    @Test
    void shouldThrowExceptionWhenAddingTagWithBlankKey() {
        // Given
        Metric metric = createValidMetric();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> metric.addTag("", "value"));
        assertThrows(IllegalArgumentException.class, () -> metric.addTag("   ", "value"));
    }

    @Test
    void shouldThrowExceptionWhenAddingTagWithNullValue() {
        // Given
        Metric metric = createValidMetric();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> metric.addTag("key", null));
    }

    @Test
    void shouldThrowExceptionWhenOrganizationIdIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), null, "exp-123", "metric", 1.0, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenOrganizationIdIsBlank() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "", "exp-123", "metric", 1.0, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenExperimentIdIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", null, "metric", 1.0, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenExperimentIdIsBlank() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "", "metric", 1.0, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", null, 1.0, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", "", 1.0, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenNameExceeds100Characters() {
        // Given
        String longName = "a".repeat(101);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", longName, 1.0, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldAcceptNameWith100Characters() {
        // Given
        String maxName = "a".repeat(100);

        // When
        Metric metric = new Metric(MetricId.generate(), "org-123", "exp-123", maxName, 1.0, 
                                   MetricType.COUNTER, Instant.now());

        // Then
        assertEquals(maxName, metric.getName());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", "metric", null, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenValueIsNaN() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", "metric", Double.NaN, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenValueIsInfinite() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", "metric", Double.POSITIVE_INFINITY, 
                      MetricType.COUNTER, Instant.now()));
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", "metric", Double.NEGATIVE_INFINITY, 
                      MetricType.COUNTER, Instant.now()));
    }

    @Test
    void shouldAcceptNegativeValue() {
        // When
        Metric metric = new Metric(MetricId.generate(), "org-123", "exp-123", "metric", -50.5, 
                                   MetricType.GAUGE, Instant.now());

        // Then
        assertEquals(-50.5, metric.getValue());
    }

    @Test
    void shouldAcceptZeroValue() {
        // When
        Metric metric = new Metric(MetricId.generate(), "org-123", "exp-123", "metric", 0.0, 
                                   MetricType.COUNTER, Instant.now());

        // Then
        assertEquals(0.0, metric.getValue());
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", "metric", 1.0, 
                      null, Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenTimestampIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new Metric(MetricId.generate(), "org-123", "exp-123", "metric", 1.0, 
                      MetricType.COUNTER, null));
    }

    @Test
    void shouldSupportAllMetricTypes() {
        // Given
        MetricId id = MetricId.generate();
        Instant timestamp = Instant.now();

        // When & Then
        Metric counter = new Metric(id, "org-123", "exp-123", "counter", 1.0, 
                                    MetricType.COUNTER, timestamp);
        assertEquals(MetricType.COUNTER, counter.getType());

        Metric gauge = new Metric(id, "org-123", "exp-123", "gauge", 1.0, 
                                  MetricType.GAUGE, timestamp);
        assertEquals(MetricType.GAUGE, gauge.getType());

        Metric histogram = new Metric(id, "org-123", "exp-123", "histogram", 1.0, 
                                      MetricType.HISTOGRAM, timestamp);
        assertEquals(MetricType.HISTOGRAM, histogram.getType());
    }

    @Test
    void shouldReturnUnmodifiableTagsMap() {
        // Given
        Metric metric = createValidMetric();
        metric.addTag("key", "value");

        // When
        Map<String, String> tags = metric.getTags();

        // Then
        assertThrows(UnsupportedOperationException.class, () -> tags.put("new", "value"));
    }

    @Test
    void shouldHandleMultipleTags() {
        // Given
        Metric metric = createValidMetric();

        // When
        metric.addTag("region", "us-east-1");
        metric.addTag("environment", "production");
        metric.addTag("version", "1.0.0");

        // Then
        assertEquals(3, metric.getTags().size());
        assertTrue(metric.hasTag("region"));
        assertTrue(metric.hasTag("environment"));
        assertTrue(metric.hasTag("version"));
    }

    private Metric createValidMetric() {
        return new Metric(
            MetricId.generate(),
            "org-123",
            "exp-456",
            "test_metric",
            100.0,
            MetricType.GAUGE,
            Instant.now()
        );
    }
}
