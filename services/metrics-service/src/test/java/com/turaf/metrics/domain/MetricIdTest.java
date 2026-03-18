package com.turaf.metrics.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricIdTest {

    @Test
    void shouldCreateMetricIdWithValidValue() {
        // Given
        String value = "metric-123";

        // When
        MetricId metricId = MetricId.of(value);

        // Then
        assertNotNull(metricId);
        assertEquals(value, metricId.getValue());
    }

    @Test
    void shouldGenerateMetricId() {
        // When
        MetricId metricId = MetricId.generate();

        // Then
        assertNotNull(metricId);
        assertNotNull(metricId.getValue());
        assertFalse(metricId.getValue().isEmpty());
    }

    @Test
    void shouldGenerateUniqueMetricIds() {
        // When
        MetricId id1 = MetricId.generate();
        MetricId id2 = MetricId.generate();

        // Then
        assertNotEquals(id1, id2);
        assertNotEquals(id1.getValue(), id2.getValue());
    }

    @Test
    void shouldThrowExceptionWhenCreatingWithNullValue() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> MetricId.of(null));
    }

    @Test
    void shouldThrowExceptionWhenCreatingWithBlankValue() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> MetricId.of(""));
        assertThrows(IllegalArgumentException.class, () -> MetricId.of("   "));
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        String value = "metric-123";
        MetricId id1 = MetricId.of(value);
        MetricId id2 = MetricId.of(value);

        // When & Then
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        MetricId id1 = MetricId.of("metric-123");
        MetricId id2 = MetricId.of("metric-456");

        // When & Then
        assertNotEquals(id1, id2);
    }

    @Test
    void shouldReturnValueAsString() {
        // Given
        String value = "metric-123";
        MetricId metricId = MetricId.of(value);

        // When
        String result = metricId.toString();

        // Then
        assertEquals(value, result);
    }
}
