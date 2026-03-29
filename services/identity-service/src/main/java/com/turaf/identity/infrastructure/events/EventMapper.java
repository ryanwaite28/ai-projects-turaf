package com.turaf.identity.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.common.domain.DomainEvent;
import com.turaf.identity.domain.event.UserCreated;
import com.turaf.identity.domain.event.UserPasswordChanged;
import com.turaf.identity.domain.event.UserProfileUpdated;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventMapper {
    
    private final ObjectMapper objectMapper;
    private static final String EVENT_SOURCE = "turaf.identity-service";

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
        if (event instanceof UserCreated) {
            return "UserCreated";
        } else if (event instanceof UserPasswordChanged) {
            return "UserPasswordChanged";
        } else if (event instanceof UserProfileUpdated) {
            return "UserProfileUpdated";
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
            
            if (event instanceof UserCreated) {
                UserCreated e = (UserCreated) event;
                eventData.put("userId", e.getUserId());
                eventData.put("email", e.getEmail());
                eventData.put("name", e.getName());
            } else if (event instanceof UserPasswordChanged) {
                UserPasswordChanged e = (UserPasswordChanged) event;
                eventData.put("userId", e.getUserId());
            } else if (event instanceof UserProfileUpdated) {
                UserProfileUpdated e = (UserProfileUpdated) event;
                eventData.put("userId", e.getUserId());
            }
            
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
