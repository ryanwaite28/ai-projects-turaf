package com.turaf.experiment.domain;

import com.turaf.common.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

public class ProblemId extends ValueObject {
    private final String value;

    public ProblemId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProblemId cannot be null or empty");
        }
        this.value = value;
    }

    public static ProblemId generate() {
        return new ProblemId(UUID.randomUUID().toString());
    }

    public static ProblemId of(String value) {
        return new ProblemId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProblemId problemId = (ProblemId) o;
        return Objects.equals(value, problemId.value);
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
