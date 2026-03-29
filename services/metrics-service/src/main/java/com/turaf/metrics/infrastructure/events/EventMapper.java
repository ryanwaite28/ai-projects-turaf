package com.turaf.metrics.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.common.domain.DomainEvent;
import com.turaf.metrics.domain.event.MetricBatchRecorded;
import com.turaf.metrics.domain.event.MetricRecorded;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventMapper {
    
    private final ObjectMapper objectMapper;
    private static final String EVENT_SOURCE = "turaf.metrics-service";

    public EventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PutEventsRequestEntry toEventBridgeEntry(DomainEvent event, String eventBusName) {
        String detailType = getDetailType(event);
        String detail = serializeEvent(event);

        return PutEventsRequestEntry.builder()
            .eventBusName(eventBusName)
            .source(EVENT_SOURCE)
            .detailType(detailType)
            .detail(detail)
            .time(Instant.now())
            .build();
    }

    private String getDetailType(DomainEvent event) {
        if (event instanceof MetricRecorded) {
            return "MetricRecorded";
        } else if (event instanceof MetricBatchRecorded) {
            return "MetricBatchRecorded";
        }
        return event.getClass().getSimpleName();
    }

    private String serializeEvent(DomainEvent event) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventId", event.getEventId());
            eventData.put("occurredAt", event.getOccurredAt());
            eventData.put("eventType", event.getClass().getSimpleName());
            eventData.put("organizationId", event.getOrganizationId());
            
            if (event instanceof MetricRecorded) {
                MetricRecorded e = (MetricRecorded) event;
                eventData.put("experimentId", e.getExperimentId());
                eventData.put("metricId", e.getMetricId());
                eventData.put("metricName", e.getMetricName());
                eventData.put("value", e.getValue());
                eventData.put("metricType", e.getMetricType());
                eventData.put("timestamp", e.getTimestamp());
            } else if (event instanceof MetricBatchRecorded) {
                MetricBatchRecorded e = (MetricBatchRecorded) event;
                eventData.put("experimentId", e.getExperimentId());
                eventData.put("metricCount", e.getMetricCount());
            }
            
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
