package com.turaf.experiment.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.experiment.domain.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    private EventMapper eventMapper;
    private ObjectMapper objectMapper;
    private static final String TEST_EVENT_BUS = "test-event-bus";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        eventMapper = new EventMapper(objectMapper);
    }

    @Test
    void shouldMapProblemCreatedEvent() throws Exception {
        // Given
        ProblemCreated event = new ProblemCreated(
            UUID.randomUUID().toString(),
            "problem-123",
            "org-456",
            "User authentication is slow",
            "user-789"
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then
        assertNotNull(entry);
        assertEquals(TEST_EVENT_BUS, entry.eventBusName());
        assertEquals("turaf.experiment-service", entry.source());
        assertEquals("ProblemCreated", entry.detailType());
        assertNotNull(entry.detail());
        assertNotNull(entry.time());

        // Verify detail content
        Map<String, Object> detail = objectMapper.readValue(entry.detail(), Map.class);
        assertEquals(event.getEventId(), detail.get("eventId"));
        assertEquals("ProblemCreated", detail.get("eventType"));
        assertEquals("problem-123", detail.get("problemId"));
        assertEquals("org-456", detail.get("organizationId"));
        assertEquals("User authentication is slow", detail.get("title"));
        assertEquals("user-789", detail.get("createdBy"));
    }

    @Test
    void shouldMapHypothesisCreatedEvent() throws Exception {
        // Given
        HypothesisCreated event = new HypothesisCreated(
            UUID.randomUUID().toString(),
            "hypothesis-123",
            "org-456",
            "problem-789",
            "Implementing caching will reduce login time",
            "user-101"
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then
        assertNotNull(entry);
        assertEquals("HypothesisCreated", entry.detailType());

        // Verify detail content
        Map<String, Object> detail = objectMapper.readValue(entry.detail(), Map.class);
        assertEquals("hypothesis-123", detail.get("hypothesisId"));
        assertEquals("org-456", detail.get("organizationId"));
        assertEquals("problem-789", detail.get("problemId"));
        assertEquals("Implementing caching will reduce login time", detail.get("statement"));
        assertEquals("user-101", detail.get("createdBy"));
    }

    @Test
    void shouldMapExperimentCreatedEvent() throws Exception {
        // Given
        ExperimentCreated event = new ExperimentCreated(
            UUID.randomUUID().toString(),
            "experiment-123",
            "org-456",
            "hypothesis-789",
            "Cache Implementation Test",
            "user-101"
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then
        assertNotNull(entry);
        assertEquals("ExperimentCreated", entry.detailType());

        // Verify detail content
        Map<String, Object> detail = objectMapper.readValue(entry.detail(), Map.class);
        assertEquals("experiment-123", detail.get("experimentId"));
        assertEquals("org-456", detail.get("organizationId"));
        assertEquals("hypothesis-789", detail.get("hypothesisId"));
        assertEquals("Cache Implementation Test", detail.get("name"));
        assertEquals("user-101", detail.get("createdBy"));
    }

    @Test
    void shouldMapExperimentStartedEvent() throws Exception {
        // Given
        Instant startedAt = Instant.now();
        ExperimentStarted event = new ExperimentStarted(
            UUID.randomUUID().toString(),
            "experiment-123",
            "org-456",
            "hypothesis-789",
            startedAt
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then
        assertNotNull(entry);
        assertEquals("ExperimentStarted", entry.detailType());

        // Verify detail content
        Map<String, Object> detail = objectMapper.readValue(entry.detail(), Map.class);
        assertEquals("experiment-123", detail.get("experimentId"));
        assertEquals("org-456", detail.get("organizationId"));
        assertEquals("hypothesis-789", detail.get("hypothesisId"));
        assertNotNull(detail.get("startedAt"));
    }

    @Test
    void shouldMapExperimentCompletedEvent() throws Exception {
        // Given
        Instant completedAt = Instant.now();
        ExperimentCompleted event = new ExperimentCompleted(
            UUID.randomUUID().toString(),
            "experiment-123",
            "org-456",
            "hypothesis-789",
            completedAt
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then
        assertNotNull(entry);
        assertEquals("ExperimentCompleted", entry.detailType());

        // Verify detail content
        Map<String, Object> detail = objectMapper.readValue(entry.detail(), Map.class);
        assertEquals("experiment-123", detail.get("experimentId"));
        assertEquals("org-456", detail.get("organizationId"));
        assertEquals("hypothesis-789", detail.get("hypothesisId"));
        assertNotNull(detail.get("completedAt"));
    }

    @Test
    void shouldMapExperimentCancelledEvent() throws Exception {
        // Given
        Instant cancelledAt = Instant.now();
        ExperimentCancelled event = new ExperimentCancelled(
            UUID.randomUUID().toString(),
            "experiment-123",
            "org-456",
            "hypothesis-789",
            cancelledAt
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then
        assertNotNull(entry);
        assertEquals("ExperimentCancelled", entry.detailType());

        // Verify detail content
        Map<String, Object> detail = objectMapper.readValue(entry.detail(), Map.class);
        assertEquals("experiment-123", detail.get("experimentId"));
        assertEquals("org-456", detail.get("organizationId"));
        assertEquals("hypothesis-789", detail.get("hypothesisId"));
        assertNotNull(detail.get("cancelledAt"));
    }

    @Test
    void shouldIncludeEventMetadataInAllEvents() throws Exception {
        // Given
        ProblemCreated event = new ProblemCreated(
            UUID.randomUUID().toString(),
            "problem-123",
            "org-456",
            "Test Problem",
            "user-789"
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then
        Map<String, Object> detail = objectMapper.readValue(entry.detail(), Map.class);
        
        // Verify all events have required metadata
        assertTrue(detail.containsKey("eventId"));
        assertTrue(detail.containsKey("occurredAt"));
        assertTrue(detail.containsKey("eventType"));
        
        assertNotNull(detail.get("eventId"));
        assertNotNull(detail.get("occurredAt"));
        assertEquals("ProblemCreated", detail.get("eventType"));
    }

    @Test
    void shouldUseCorrectEventSource() {
        // Given
        ProblemCreated event = new ProblemCreated(
            UUID.randomUUID().toString(),
            "problem-123",
            "org-456",
            "Test Problem",
            "user-789"
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then
        assertEquals("turaf.experiment-service", entry.source());
    }

    @Test
    void shouldSetEventBusName() {
        // Given
        String customEventBus = "custom-event-bus";
        ProblemCreated event = new ProblemCreated(
            UUID.randomUUID().toString(),
            "problem-123",
            "org-456",
            "Test Problem",
            "user-789"
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, customEventBus);

        // Then
        assertEquals(customEventBus, entry.eventBusName());
    }

    @Test
    void shouldSetTimestampOnEntry() {
        // Given
        ProblemCreated event = new ProblemCreated(
            UUID.randomUUID().toString(),
            "problem-123",
            "org-456",
            "Test Problem",
            "user-789"
        );

        Instant before = Instant.now();

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        Instant after = Instant.now();

        // Then
        assertNotNull(entry.time());
        assertTrue(!entry.time().isBefore(before));
        assertTrue(!entry.time().isAfter(after));
    }

    @Test
    void shouldProduceValidJsonDetail() throws Exception {
        // Given
        ExperimentStarted event = new ExperimentStarted(
            UUID.randomUUID().toString(),
            "experiment-123",
            "org-456",
            "hypothesis-789",
            Instant.now()
        );

        // When
        PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, TEST_EVENT_BUS);

        // Then - Should not throw exception
        Map<String, Object> detail = objectMapper.readValue(entry.detail(), Map.class);
        assertNotNull(detail);
        assertFalse(detail.isEmpty());
    }
}
