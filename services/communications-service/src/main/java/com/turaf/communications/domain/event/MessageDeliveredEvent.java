package com.turaf.communications.domain.event;

import com.turaf.common.domain.DomainEvent;
import com.turaf.communications.domain.model.ConversationType;
import lombok.Getter;
import java.time.Instant;
import java.util.List;

@Getter
public class MessageDeliveredEvent implements DomainEvent {
    private final String eventId;
    private final String messageId;
    private final String conversationId;
    private final String senderId;
    private final String organizationId;
    private final ConversationType conversationType;
    private final List<String> recipientIds;
    private final String content;
    private final Instant deliveredAt;
    private final String correlationId;
    
    public MessageDeliveredEvent(
        String eventId,
        String messageId,
        String conversationId,
        String senderId,
        String organizationId,
        ConversationType conversationType,
        List<String> recipientIds,
        String content,
        Instant deliveredAt
    ) {
        this.eventId = eventId;
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.organizationId = organizationId;
        this.conversationType = conversationType;
        this.recipientIds = recipientIds;
        this.content = content;
        this.deliveredAt = deliveredAt;
        this.correlationId = eventId;
    }
    
    @Override
    public String getEventType() {
        return "MessageDelivered";
    }
    
    @Override
    public Instant getOccurredAt() {
        return deliveredAt;
    }
}
