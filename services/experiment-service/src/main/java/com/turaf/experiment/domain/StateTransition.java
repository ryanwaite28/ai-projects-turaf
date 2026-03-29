package com.turaf.experiment.domain;

import com.turaf.common.domain.ValueObject;

import java.util.List;
import java.util.Objects;

public class StateTransition extends ValueObject {
    private final ExperimentStatus from;
    private final ExperimentStatus to;

    public StateTransition(ExperimentStatus from, ExperimentStatus to) {
        this.from = Objects.requireNonNull(from, "from status cannot be null");
        this.to = Objects.requireNonNull(to, "to status cannot be null");
    }

    public ExperimentStatus getFrom() {
        return from;
    }

    public ExperimentStatus getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateTransition that = (StateTransition) o;
        return from == that.from && to == that.to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }

    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(from, to);
    }
}
