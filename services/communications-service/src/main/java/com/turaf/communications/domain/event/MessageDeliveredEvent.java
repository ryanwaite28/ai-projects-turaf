package com.turaf.communications.domain.event;

import com.turaf.communications.domain.model.ConversationType;
import lombok.Getter;
import java.time.Instant;
import java.util.List;

@Getter
public class MessageDeliveredEvent extends DomainEvent {
    private final String messageId;
    private final String conversationId;
    private final String senderId;
    private final ConversationType conversationType;
    private final List<String> recipientIds;
    private final String content;
    private final Instant deliveredAt;
    
    public MessageDeliveredEvent(
        String messageId,
        String conversationId,
        String senderId,
        ConversationType conversationType,
        List<String> recipientIds,
        String content,
        Instant deliveredAt
    ) {
        super();
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.conversationType = conversationType;
        this.recipientIds = recipientIds;
        this.content = content;
        this.deliveredAt = deliveredAt;
    }
}
