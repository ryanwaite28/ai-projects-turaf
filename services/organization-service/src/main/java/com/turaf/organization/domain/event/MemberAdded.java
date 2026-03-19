package com.turaf.organization.domain.event;

import com.turaf.organization.domain.common.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a member is added to an organization.
 */
public class MemberAdded implements DomainEvent {
    
    private final String eventId;
    private final String organizationId;
    private final String userId;
    private final String userEmail;
    private final String userName;
    private final String role;
    private final String addedBy;
    private final Instant timestamp;
    
    public MemberAdded(String eventId, String organizationId, String userId,
                      String userEmail, String userName, String role,
                      String addedBy, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.userEmail = Objects.requireNonNull(userEmail, "User email cannot be null");
        this.userName = userName;
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.addedBy = Objects.requireNonNull(addedBy, "Added by cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }
    
    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return "MemberAdded";
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getOrganizationId() {
        return organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String getRole() {
        return role;
    }
    
    public String getAddedBy() {
        return addedBy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberAdded that = (MemberAdded) o;
        return Objects.equals(eventId, that.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
