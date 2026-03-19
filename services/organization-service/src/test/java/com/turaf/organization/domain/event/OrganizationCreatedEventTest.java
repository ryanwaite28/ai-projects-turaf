package com.turaf.organization.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationCreatedEventTest {
    
    @Test
    void shouldCreateEventWithValidData() {
        String eventId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        
        OrganizationCreated event = new OrganizationCreated(
            eventId, orgId, "Test Org", "test-org", "user-123", timestamp
        );
        
        assertEquals(eventId, event.getEventId());
        assertEquals("OrganizationCreated", event.getEventType());
        assertEquals(orgId, event.getOrganizationId());
        assertEquals("Test Org", event.getName());
        assertEquals("test-org", event.getSlug());
        assertEquals(timestamp, event.getTimestamp());
    }
    
    @Test
    void shouldRejectNullEventId() {
        assertThrows(NullPointerException.class, () ->
            new OrganizationCreated(null, "org-1", "Test", "test", "user-1", Instant.now())
        );
    }
}
