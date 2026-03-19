package com.turaf.common.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class AggregateRootTest {
    
    private static class TestAggregate extends AggregateRoot<String> {
        private String name;
        
        public TestAggregate(String id, String name) {
            super(id);
            this.name = name;
        }
        
        public void doSomething() {
            registerEvent(new TestEvent("event-1", "TestEvent", Instant.now(), "org-1"));
        }
        
        public void doMultipleThings() {
            registerEvent(new TestEvent("event-1", "TestEvent1", Instant.now(), "org-1"));
            registerEvent(new TestEvent("event-2", "TestEvent2", Instant.now(), "org-1"));
        }
        
        public String getName() {
            return name;
        }
    }
    
    private static class TestEvent implements DomainEvent {
        private final String eventId;
        private final String eventType;
        private final Instant timestamp;
        private final String organizationId;
        
        public TestEvent(String eventId, String eventType, Instant timestamp, String organizationId) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.timestamp = timestamp;
            this.organizationId = organizationId;
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getEventType() {
            return eventType;
        }
        
        @Override
        public Instant getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String getOrganizationId() {
            return organizationId;
        }
    }
    
    private TestAggregate aggregate;
    
    @BeforeEach
    void setUp() {
        aggregate = new TestAggregate("agg-1", "Test Aggregate");
    }
    
    @Test
    void testAggregateCreation() {
        assertThat(aggregate.getId()).isEqualTo("agg-1");
        assertThat(aggregate.getName()).isEqualTo("Test Aggregate");
        assertThat(aggregate.getDomainEvents()).isEmpty();
    }
    
    @Test
    void testRegisterEvent() {
        aggregate.doSomething();
        
        assertThat(aggregate.getDomainEvents()).hasSize(1);
        assertThat(aggregate.getDomainEventCount()).isEqualTo(1);
        assertThat(aggregate.getDomainEvents().get(0).getEventType()).isEqualTo("TestEvent");
    }
    
    @Test
    void testRegisterMultipleEvents() {
        aggregate.doMultipleThings();
        
        assertThat(aggregate.getDomainEvents()).hasSize(2);
        assertThat(aggregate.getDomainEventCount()).isEqualTo(2);
    }
    
    @Test
    void testClearDomainEvents() {
        aggregate.doMultipleThings();
        assertThat(aggregate.getDomainEvents()).hasSize(2);
        
        aggregate.clearDomainEvents();
        
        assertThat(aggregate.getDomainEvents()).isEmpty();
        assertThat(aggregate.getDomainEventCount()).isZero();
    }
    
    @Test
    void testGetDomainEvents_ReturnsUnmodifiableList() {
        aggregate.doSomething();
        
        assertThatThrownBy(() -> aggregate.getDomainEvents().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }
    
    @Test
    void testRegisterNullEvent_ThrowsException() {
        assertThatThrownBy(() -> aggregate.registerEvent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Domain event cannot be null");
    }
}
