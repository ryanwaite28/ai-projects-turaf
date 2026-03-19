package com.turaf.organization.domain.event;

import com.turaf.organization.domain.MemberRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemberAdded domain event.
 */
class MemberAddedEventTest {
    
    @Test
    void shouldCreateEventWithAllFields() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String userEmail = "user@example.com";
        String userName = "Test User";
        MemberRole role = MemberRole.MEMBER;
        String addedBy = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        
        // When
        MemberAdded event = new MemberAdded(
            eventId,
            orgId,
            userId,
            userEmail,
            userName,
            role,
            addedBy,
            timestamp
        );
        
        // Then
        assertNotNull(event);
        assertEquals(eventId, event.getEventId());
        assertEquals(orgId, event.getOrganizationId());
        assertEquals("MemberAdded", event.getEventType());
        assertEquals(userId, event.getUserId());
        assertEquals(userEmail, event.getUserEmail());
        assertEquals(userName, event.getUserName());
        assertEquals(role, event.getRole());
        assertEquals(addedBy, event.getAddedBy());
        assertEquals(timestamp, event.getTimestamp());
    }
    
    @Test
    void shouldHaveCorrectEventType() {
        // Given
        MemberAdded event = new MemberAdded(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "user@example.com",
            "Test User",
            MemberRole.ADMIN,
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        // When
        String eventType = event.getEventType();
        
        // Then
        assertEquals("MemberAdded", eventType);
    }
    
    @Test
    void shouldSupportAdminRole() {
        // Given
        MemberAdded event = new MemberAdded(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "admin@example.com",
            "Admin User",
            MemberRole.ADMIN,
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        // When
        MemberRole role = event.getRole();
        
        // Then
        assertEquals(MemberRole.ADMIN, role);
    }
    
    @Test
    void shouldThrowExceptionForNullEventId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new MemberAdded(
                null,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user@example.com",
                "Test User",
                MemberRole.MEMBER,
                UUID.randomUUID().toString(),
                Instant.now()
            )
        );
    }
    
    @Test
    void shouldThrowExceptionForNullOrganizationId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new MemberAdded(
                UUID.randomUUID().toString(),
                null,
                UUID.randomUUID().toString(),
                "user@example.com",
                "Test User",
                MemberRole.MEMBER,
                UUID.randomUUID().toString(),
                Instant.now()
            )
        );
    }
}
