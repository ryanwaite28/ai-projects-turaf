package com.turaf.organization.infrastructure.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.organization.domain.MemberRole;
import com.turaf.organization.domain.event.MemberAdded;
import com.turaf.organization.domain.event.OrganizationCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventMapper.
 */
class EventMapperTest {
    
    private EventMapper eventMapper;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        eventMapper = new EventMapper();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void shouldSerializeOrganizationCreatedEvent() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        
        OrganizationCreated event = new OrganizationCreated(
            eventId,
            orgId,
            "Test Org",
            "test-org",
            userId,
            timestamp
        );
        
        // When
        String json = eventMapper.toJson(event);
        
        // Then
        assertNotNull(json);
        
        JsonNode root = objectMapper.readTree(json);
        assertEquals(eventId, root.get("eventId").asText());
        assertEquals("OrganizationCreated", root.get("eventType").asText());
        assertEquals(1, root.get("eventVersion").asInt());
        assertEquals("organization-service", root.get("sourceService").asText());
        assertEquals(orgId, root.get("organizationId").asText());
        assertNotNull(root.get("timestamp"));
        
        JsonNode payload = root.get("payload");
        assertNotNull(payload);
        assertEquals("Test Org", payload.get("name").asText());
        assertEquals("test-org", payload.get("slug").asText());
        assertEquals(userId, payload.get("createdBy").asText());
    }
    
    @Test
    void shouldSerializeMemberAddedEvent() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String addedBy = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        
        MemberAdded event = new MemberAdded(
            eventId,
            orgId,
            userId,
            "user@example.com",
            "Test User",
            MemberRole.MEMBER,
            addedBy,
            timestamp
        );
        
        // When
        String json = eventMapper.toJson(event);
        
        // Then
        assertNotNull(json);
        
        JsonNode root = objectMapper.readTree(json);
        assertEquals(eventId, root.get("eventId").asText());
        assertEquals("MemberAdded", root.get("eventType").asText());
        assertEquals(orgId, root.get("organizationId").asText());
        
        JsonNode payload = root.get("payload");
        assertNotNull(payload);
        assertEquals(userId, payload.get("userId").asText());
        assertEquals("user@example.com", payload.get("userEmail").asText());
        assertEquals("Test User", payload.get("userName").asText());
        assertEquals("MEMBER", payload.get("role").asText());
        assertEquals(addedBy, payload.get("addedBy").asText());
    }
    
    @Test
    void shouldIncludeAllEnvelopeFields() throws Exception {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        // When
        String json = eventMapper.toJson(event);
        JsonNode root = objectMapper.readTree(json);
        
        // Then - verify all required envelope fields
        assertTrue(root.has("eventId"), "Missing eventId");
        assertTrue(root.has("eventType"), "Missing eventType");
        assertTrue(root.has("eventVersion"), "Missing eventVersion");
        assertTrue(root.has("timestamp"), "Missing timestamp");
        assertTrue(root.has("sourceService"), "Missing sourceService");
        assertTrue(root.has("organizationId"), "Missing organizationId");
        assertTrue(root.has("payload"), "Missing payload");
    }
    
    @Test
    void shouldSerializeTimestampCorrectly() throws Exception {
        // Given
        Instant timestamp = Instant.parse("2024-01-15T10:30:00Z");
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            timestamp
        );
        
        // When
        String json = eventMapper.toJson(event);
        JsonNode root = objectMapper.readTree(json);
        
        // Then - timestamp should be in ISO-8601 format, not epoch
        String timestampStr = root.get("timestamp").asText();
        assertTrue(timestampStr.contains("T"), "Timestamp should be in ISO-8601 format");
        assertTrue(timestampStr.contains("Z"), "Timestamp should include timezone");
    }
    
    @Test
    void shouldSetEventVersionToOne() throws Exception {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        // When
        String json = eventMapper.toJson(event);
        JsonNode root = objectMapper.readTree(json);
        
        // Then
        assertEquals(1, root.get("eventVersion").asInt());
    }
    
    @Test
    void shouldSetSourceServiceCorrectly() throws Exception {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        // When
        String json = eventMapper.toJson(event);
        JsonNode root = objectMapper.readTree(json);
        
        // Then
        assertEquals("organization-service", root.get("sourceService").asText());
    }
    
    @Test
    void shouldProduceValidJson() throws Exception {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        // When
        String json = eventMapper.toJson(event);
        
        // Then - should be parseable as valid JSON
        assertDoesNotThrow(() -> objectMapper.readTree(json));
    }
}
