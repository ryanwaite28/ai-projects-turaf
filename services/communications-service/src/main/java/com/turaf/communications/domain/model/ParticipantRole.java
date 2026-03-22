package com.turaf.communications.domain.model;

public enum ParticipantRole {
    MEMBER,
    ADMIN;
    
    public boolean canAddParticipants() {
        return this == ADMIN;
    }
    
    public boolean canRemoveParticipants() {
        return this == ADMIN;
    }
}
