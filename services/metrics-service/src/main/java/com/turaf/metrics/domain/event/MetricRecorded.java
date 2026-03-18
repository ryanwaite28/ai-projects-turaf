package com.turaf.metrics.domain.event;

import com.turaf.common.event.DomainEvent;

import java.time.Instant;

public class MetricRecorded implements DomainEvent {

    private final String eventId;
    private final String organizationId;
    private final String experimentId;
    private final String metricId;
    private final String metricName;
    private final Double value;
    private final String metricType;
    private final Instant timestamp;
    private final Instant occurredAt;

    public MetricRecorded(String eventId, String organizationId, String experimentId,
                         String metricId, String metricName, Double value,
                         String metricType, Instant timestamp) {
        this.eventId = eventId;
        this.organizationId = organizationId;
        this.experimentId = experimentId;
        this.metricId = metricId;
        this.metricName = metricName;
        this.value = value;
        this.metricType = metricType;
        this.timestamp = timestamp;
        this.occurredAt = Instant.now();
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "MetricRecorded";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getMetricId() {
        return metricId;
    }

    public String getMetricName() {
        return metricName;
    }

    public Double getValue() {
        return value;
    }

    public String getMetricType() {
        return metricType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
