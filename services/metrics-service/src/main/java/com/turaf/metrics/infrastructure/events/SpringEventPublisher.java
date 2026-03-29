package com.turaf.metrics.infrastructure.events;

import com.turaf.common.domain.DomainEvent;
import com.turaf.common.event.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishBatch(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        events.forEach(this::publish);
    }
}
