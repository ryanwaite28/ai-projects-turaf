package com.turaf.metrics.infrastructure.events;

import com.turaf.common.event.DomainEvent;
import com.turaf.metrics.domain.event.MetricBatchRecorded;
import com.turaf.metrics.domain.event.MetricRecorded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private SpringEventPublisher springEventPublisher;

    @BeforeEach
    void setUp() {
        springEventPublisher = new SpringEventPublisher(applicationEventPublisher);
    }

    @Test
    void shouldPublishMetricRecordedEvent() {
        // Given
        MetricRecorded event = new MetricRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            "metric-id",
            "response_time",
            125.5,
            "GAUGE",
            Instant.now()
        );

        // When
        springEventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void shouldPublishMetricBatchRecordedEvent() {
        // Given
        MetricBatchRecorded event = new MetricBatchRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            100
        );

        // When
        springEventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void shouldPublishAnyDomainEvent() {
        // Given
        DomainEvent event = new MetricRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            "metric-id",
            "metric",
            100.0,
            "COUNTER",
            Instant.now()
        );

        // When
        springEventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void shouldPublishMultipleEvents() {
        // Given
        MetricRecorded event1 = new MetricRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            "metric-1",
            "metric1",
            100.0,
            "GAUGE",
            Instant.now()
        );

        MetricRecorded event2 = new MetricRecorded(
            UUID.randomUUID().toString(),
            "org-123",
            "exp-456",
            "metric-2",
            "metric2",
            200.0,
            "COUNTER",
            Instant.now()
        );

        // When
        springEventPublisher.publish(event1);
        springEventPublisher.publish(event2);

        // Then
        verify(applicationEventPublisher).publishEvent(event1);
        verify(applicationEventPublisher).publishEvent(event2);
    }
}
