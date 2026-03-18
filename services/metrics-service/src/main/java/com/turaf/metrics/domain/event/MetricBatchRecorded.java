package com.turaf.metrics.domain.event;

import com.turaf.common.event.DomainEvent;

import java.time.Instant;

public class MetricBatchRecorded implements DomainEvent {

    private final String eventId;
    private final String organizationId;
    private final String experimentId;
    private final int metricCount;
    private final Instant occurredAt;

    public MetricBatchRecorded(String eventId, String organizationId, String experimentId, int metricCount) {
        this.eventId = eventId;
        this.organizationId = organizationId;
        this.experimentId = experimentId;
        this.metricCount = metricCount;
        this.occurredAt = Instant.now();
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

    public String getOrganizationId() {
        return organizationId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public int getMetricCount() {
        return metricCount;
    }
}
