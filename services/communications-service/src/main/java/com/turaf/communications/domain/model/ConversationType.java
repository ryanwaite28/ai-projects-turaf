package com.turaf.communications.domain.model;

public enum ConversationType {
    DIRECT,
    GROUP;
    
    public void validateParticipantCount(int count) {
        if (this == DIRECT && count != 2) {
            throw new IllegalArgumentException("Direct conversations must have exactly 2 participants");
        }
        if (this == GROUP && count < 2) {
            throw new IllegalArgumentException("Group conversations must have at least 2 participants");
        }
    }
}
