package com.turaf.communications.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "read_state", schema = "communications_schema",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "conversation_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReadState {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;
    
    @Column(name = "last_read_message_id")
    private String lastReadMessageId;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    public static ReadState create(String userId, String conversationId) {
        return new ReadState(
            UUID.randomUUID().toString(),
            userId,
            conversationId,
            null,
            Instant.now()
        );
    }
    
    public void markAsRead(String messageId) {
        this.lastReadMessageId = messageId;
        this.updatedAt = Instant.now();
    }
}
