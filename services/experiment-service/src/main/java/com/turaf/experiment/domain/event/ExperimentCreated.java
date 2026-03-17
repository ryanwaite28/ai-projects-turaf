package com.turaf.experiment.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public class ExperimentCreated implements DomainEvent {
    private final String eventId;
    private final String experimentId;
    private final String organizationId;
    private final String hypothesisId;
    private final String name;
    private final String description;
    private final String createdBy;
    private final Instant timestamp;

    public ExperimentCreated(String eventId, String experimentId, String organizationId,
                             String hypothesisId, String name, String description,
                             String createdBy, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId);
        this.experimentId = Objects.requireNonNull(experimentId);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.hypothesisId = Objects.requireNonNull(hypothesisId);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "ExperimentCreated";
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
