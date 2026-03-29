package com.turaf.experiment.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public class HypothesisCreated implements DomainEvent {
    private final String eventId;
    private final String hypothesisId;
    private final String organizationId;
    private final String problemId;
    private final String statement;
    private final String expectedOutcome;
    private final String createdBy;
    private final Instant timestamp;
    private final String correlationId;

    public HypothesisCreated(String eventId, String hypothesisId, String organizationId,
                             String problemId, String statement, String expectedOutcome,
                             String createdBy, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId);
        this.hypothesisId = Objects.requireNonNull(hypothesisId);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.problemId = Objects.requireNonNull(problemId);
        this.statement = Objects.requireNonNull(statement);
        this.expectedOutcome = expectedOutcome;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.correlationId = eventId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "HypothesisCreated";
    }

    @Override
    public Instant getOccurredAt() {
        return timestamp;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    public String getHypothesisId() {
        return hypothesisId;
    }

    public String getProblemId() {
        return problemId;
    }

    public String getStatement() {
        return statement;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
