package com.turaf.organization.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrganizationUpdated domain event.
 */
class OrganizationUpdatedEventTest {
    
    @Test
    void shouldCreateEventWithAllFields() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        String fieldName = "name";
        String newValue = "Updated Name";
        Instant timestamp = Instant.now();
        
        // When
        OrganizationUpdated event = new OrganizationUpdated(
            eventId,
            orgId,
            fieldName,
            newValue,
            timestamp
        );
        
        // Then
        assertNotNull(event);
        assertEquals(eventId, event.getEventId());
        assertEquals(orgId, event.getOrganizationId());
        assertEquals("OrganizationUpdated", event.getEventType());
        assertEquals(fieldName, event.getFieldName());
        assertEquals(newValue, event.getNewValue());
        assertEquals(timestamp, event.getTimestamp());
    }
    
    @Test
    void shouldHaveCorrectEventType() {
        // Given
        OrganizationUpdated event = new OrganizationUpdated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "name",
            "New Name",
            Instant.now()
        );
        
        // When
        String eventType = event.getEventType();
        
        // Then
        assertEquals("OrganizationUpdated", eventType);
    }
    
    @Test
    void shouldThrowExceptionForNullEventId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new OrganizationUpdated(
                null,
                UUID.randomUUID().toString(),
                "name",
                "New Name",
                Instant.now()
            )
        );
    }
    
    @Test
    void shouldThrowExceptionForNullOrganizationId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new OrganizationUpdated(
                UUID.randomUUID().toString(),
                null,
                "name",
                "New Name",
                Instant.now()
            )
        );
    }
}
