package com.turaf.communications.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations", schema = "communications_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Conversation {
    
    @Id
    private String id;
    
    @Column(name = "organization_id", nullable = false)
    private String organizationId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;
    
    private String name;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    public static Conversation createDirect(String user1Id, String user2Id) {
        Conversation conversation = new Conversation();
        conversation.id = UUID.randomUUID().toString();
        conversation.type = ConversationType.DIRECT;
        conversation.createdAt = Instant.now();
        conversation.updatedAt = Instant.now();
        
        conversation.addParticipant(user1Id, ParticipantRole.MEMBER);
        conversation.addParticipant(user2Id, ParticipantRole.MEMBER);
        
        conversation.validateInvariants();
        return conversation;
    }
    
    public static Conversation createGroup(String name, List<String> userIds, String creatorId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Group conversation name is required");
        }
        
        Conversation conversation = new Conversation();
        conversation.id = UUID.randomUUID().toString();
        conversation.type = ConversationType.GROUP;
        conversation.name = name;
        conversation.createdAt = Instant.now();
        conversation.updatedAt = Instant.now();
        
        conversation.addParticipant(creatorId, ParticipantRole.ADMIN);
        userIds.stream()
            .filter(userId -> !userId.equals(creatorId))
            .forEach(userId -> conversation.addParticipant(userId, ParticipantRole.MEMBER));
        
        conversation.validateInvariants();
        return conversation;
    }
    
    public void addParticipant(String userId, ParticipantRole role) {
        if (isParticipant(userId)) {
            throw new IllegalArgumentException("User is already a participant");
        }
        
        Participant participant = new Participant(
            UUID.randomUUID().toString(),
            this,
            userId,
            role,
            Instant.now()
        );
        participants.add(participant);
        this.updatedAt = Instant.now();
    }
    
    public void removeParticipant(String userId) {
        participants.removeIf(p -> p.getUserId().equals(userId));
        this.updatedAt = Instant.now();
        validateInvariants();
    }
    
    public boolean isParticipant(String userId) {
        return participants.stream()
            .anyMatch(p -> p.getUserId().equals(userId));
    }
    
    public boolean isAdmin(String userId) {
        return participants.stream()
            .anyMatch(p -> p.getUserId().equals(userId) && p.getRole() == ParticipantRole.ADMIN);
    }
    
    public void validateInvariants() {
        type.validateParticipantCount(participants.size());
        
        long uniqueUserIds = participants.stream()
            .map(Participant::getUserId)
            .distinct()
            .count();
        
        if (uniqueUserIds != participants.size()) {
            throw new IllegalStateException("Duplicate participants not allowed");
        }
        
        if (type == ConversationType.GROUP) {
            boolean hasAdmin = participants.stream()
                .anyMatch(p -> p.getRole() == ParticipantRole.ADMIN);
            if (!hasAdmin) {
                throw new IllegalStateException("Group conversation must have at least one admin");
            }
        }
    }
}
