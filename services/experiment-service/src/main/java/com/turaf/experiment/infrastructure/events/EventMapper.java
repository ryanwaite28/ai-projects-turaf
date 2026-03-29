package com.turaf.experiment.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.common.domain.DomainEvent;
import com.turaf.experiment.domain.event.*;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventMapper {
    
    private final ObjectMapper objectMapper;
    private static final String EVENT_SOURCE = "turaf.experiment-service";

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
        if (event instanceof ProblemCreated) {
            return "ProblemCreated";
        } else if (event instanceof HypothesisCreated) {
            return "HypothesisCreated";
        } else if (event instanceof ExperimentCreated) {
            return "ExperimentCreated";
        } else if (event instanceof ExperimentStarted) {
            return "ExperimentStarted";
        } else if (event instanceof ExperimentCompleted) {
            return "ExperimentCompleted";
        } else if (event instanceof ExperimentCancelled) {
            return "ExperimentCancelled";
        }
        return event.getClass().getSimpleName();
    }

    private String serializeEvent(DomainEvent event) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventId", event.getEventId());
            eventData.put("occurredAt", event.getOccurredAt());
            eventData.put("eventType", event.getClass().getSimpleName());
            
            if (event instanceof ProblemCreated) {
                ProblemCreated e = (ProblemCreated) event;
                eventData.put("problemId", e.getProblemId());
                eventData.put("organizationId", e.getOrganizationId());
                eventData.put("title", e.getTitle());
                eventData.put("createdBy", e.getCreatedBy());
            } else if (event instanceof HypothesisCreated) {
                HypothesisCreated e = (HypothesisCreated) event;
                eventData.put("hypothesisId", e.getHypothesisId());
                eventData.put("organizationId", e.getOrganizationId());
                eventData.put("problemId", e.getProblemId());
                eventData.put("statement", e.getStatement());
                eventData.put("createdBy", e.getCreatedBy());
            } else if (event instanceof ExperimentCreated) {
                ExperimentCreated e = (ExperimentCreated) event;
                eventData.put("experimentId", e.getExperimentId());
                eventData.put("organizationId", e.getOrganizationId());
                eventData.put("hypothesisId", e.getHypothesisId());
                eventData.put("name", e.getName());
                eventData.put("createdBy", e.getCreatedBy());
            } else if (event instanceof ExperimentStarted) {
                ExperimentStarted e = (ExperimentStarted) event;
                eventData.put("experimentId", e.getExperimentId());
                eventData.put("organizationId", e.getOrganizationId());
                eventData.put("hypothesisId", e.getHypothesisId());
                eventData.put("startedAt", e.getOccurredAt());
            } else if (event instanceof ExperimentCompleted) {
                ExperimentCompleted e = (ExperimentCompleted) event;
                eventData.put("experimentId", e.getExperimentId());
                eventData.put("organizationId", e.getOrganizationId());
                eventData.put("hypothesisId", e.getHypothesisId());
                eventData.put("completedAt", e.getOccurredAt());
            } else if (event instanceof ExperimentCancelled) {
                ExperimentCancelled e = (ExperimentCancelled) event;
                eventData.put("experimentId", e.getExperimentId());
                eventData.put("organizationId", e.getOrganizationId());
                eventData.put("hypothesisId", e.getHypothesisId());
                eventData.put("cancelledAt", e.getOccurredAt());
            }
            
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
