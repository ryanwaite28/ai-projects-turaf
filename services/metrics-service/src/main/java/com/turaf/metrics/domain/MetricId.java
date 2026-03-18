package com.turaf.metrics.domain;

import java.util.Objects;
import java.util.UUID;

public class MetricId {

    private final String value;

    private MetricId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MetricId cannot be null or blank");
        }
        this.value = value;
    }

    public static MetricId of(String value) {
        return new MetricId(value);
    }

    public static MetricId generate() {
        return new MetricId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricId metricId = (MetricId) o;
        return Objects.equals(value, metricId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
