package com.turaf.experiment.domain;

import com.turaf.common.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

public class ExperimentId extends ValueObject {
    private final String value;

    public ExperimentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ExperimentId cannot be null or empty");
        }
        this.value = value;
    }

    public static ExperimentId generate() {
        return new ExperimentId(UUID.randomUUID().toString());
    }

    public static ExperimentId of(String value) {
        return new ExperimentId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentId that = (ExperimentId) o;
        return Objects.equals(value, that.value);
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
