package com.turaf.experiment.domain;

import com.turaf.common.domain.ValueObject;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class HypothesisId extends ValueObject {
    private final String value;

    public HypothesisId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("HypothesisId cannot be null or empty");
        }
        this.value = value;
    }

    public static HypothesisId generate() {
        return new HypothesisId(UUID.randomUUID().toString());
    }

    public static HypothesisId of(String value) {
        return new HypothesisId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HypothesisId that = (HypothesisId) o;
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

    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(value);
    }
}
