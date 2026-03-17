package com.turaf.experiment.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public class ProblemCreated implements DomainEvent {
    private final String eventId;
    private final String problemId;
    private final String organizationId;
    private final String title;
    private final String description;
    private final String createdBy;
    private final Instant timestamp;

    public ProblemCreated(String eventId, String problemId, String organizationId,
                          String title, String description, String createdBy, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId);
        this.problemId = Objects.requireNonNull(problemId);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.title = Objects.requireNonNull(title);
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
        return "ProblemCreated";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    public String getProblemId() {
        return problemId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
