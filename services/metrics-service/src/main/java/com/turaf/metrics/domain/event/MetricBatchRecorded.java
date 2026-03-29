package com.turaf.metrics.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;

public class MetricBatchRecorded implements DomainEvent {

    private final String eventId;
    private final String organizationId;
    private final String experimentId;
    private final int metricCount;
    private final Instant occurredAt;
    private final String correlationId;

    public MetricBatchRecorded(String eventId, String organizationId, String experimentId, int metricCount) {
        this.eventId = eventId;
        this.organizationId = organizationId;
        this.experimentId = experimentId;
        this.metricCount = metricCount;
        this.occurredAt = Instant.now();
        this.correlationId = eventId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "MetricBatchRecorded";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public int getMetricCount() {
        return metricCount;
    }
}
