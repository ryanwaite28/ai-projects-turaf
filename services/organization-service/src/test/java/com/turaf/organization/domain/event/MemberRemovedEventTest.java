package com.turaf.organization.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemberRemoved domain event.
 */
class MemberRemovedEventTest {
    
    @Test
    void shouldCreateEventWithAllFields() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String removedBy = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        
        // When
        MemberRemoved event = new MemberRemoved(
            eventId,
            orgId,
            userId,
            removedBy,
            timestamp
        );
        
        // Then
        assertNotNull(event);
        assertEquals(eventId, event.getEventId());
        assertEquals(orgId, event.getOrganizationId());
        assertEquals("MemberRemoved", event.getEventType());
        assertEquals(userId, event.getUserId());
        assertEquals(removedBy, event.getRemovedBy());
        assertEquals(timestamp, event.getTimestamp());
    }
    
    @Test
    void shouldHaveCorrectEventType() {
        // Given
        MemberRemoved event = new MemberRemoved(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        // When
        String eventType = event.getEventType();
        
        // Then
        assertEquals("MemberRemoved", eventType);
    }
    
    @Test
    void shouldThrowExceptionForNullEventId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new MemberRemoved(
                null,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                Instant.now()
            )
        );
    }
    
    @Test
    void shouldThrowExceptionForNullOrganizationId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new MemberRemoved(
                UUID.randomUUID().toString(),
                null,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                Instant.now()
            )
        );
    }
    
    @Test
    void shouldThrowExceptionForNullUserId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new MemberRemoved(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                null,
                UUID.randomUUID().toString(),
                Instant.now()
            )
        );
    }
}
