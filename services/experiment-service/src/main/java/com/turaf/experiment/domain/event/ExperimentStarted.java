package com.turaf.experiment.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public class ExperimentStarted implements DomainEvent {
    private final String eventId;
    private final String experimentId;
    private final String organizationId;
    private final String hypothesisId;
    private final Instant timestamp;

    public ExperimentStarted(String eventId, String experimentId, String organizationId,
                             String hypothesisId, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId);
        this.experimentId = Objects.requireNonNull(experimentId);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.hypothesisId = Objects.requireNonNull(hypothesisId);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "ExperimentStarted";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getHypothesisId() {
        return hypothesisId;
    }
}
