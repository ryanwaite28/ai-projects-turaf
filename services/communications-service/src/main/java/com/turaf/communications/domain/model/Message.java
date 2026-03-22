package com.turaf.communications.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages", schema = "communications_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Message {
    
    @Id
    private String id;
    
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;
    
    @Column(name = "sender_id", nullable = false)
    private String senderId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public static Message create(String conversationId, String senderId, String content) {
        validateContent(content);
        
        return new Message(
            UUID.randomUUID().toString(),
            conversationId,
            senderId,
            content,
            Instant.now()
        );
    }
    
    private static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (content.length() > 10000) {
            throw new IllegalArgumentException("Message content exceeds maximum length of 10,000 characters");
        }
    }
}
