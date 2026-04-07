# Communications Event Schemas Specification

**Event Bus**: AWS EventBridge  
**Event Format**: JSON  
**Versioning**: Semantic versioning in event payload

---

## Overview

This specification defines the event schemas for the Communications domain. All events follow the standard event envelope pattern used across the Turaf platform and are published to the EventBridge event bus for consumption by other services.

---

## Event Envelope Standard

All events follow this envelope structure:

```json
{
  "version": "1.0",
  "id": "uuid-v4",
  "source": "communications-service",
  "detailType": "MessageDelivered",
  "time": "2026-03-21T20:30:00Z",
  "detail": {
    // Event-specific payload
  }
}
```

**Fields**:
- `version`: Event schema version (semantic versioning)
- `id`: Unique event identifier (UUID v4)
- `source`: Service that published the event
- `detailType`: Event type name
- `time`: ISO 8601 timestamp when event occurred
- `detail`: Event-specific payload

---

## Event Schemas

### MessageDeliveredEvent

**Purpose**: Published when a message is successfully persisted to the database.

**Source**: `communications-service`  
**Detail Type**: `MessageDelivered`  
**Version**: `1.0`

**Schema**:
```json
{
  "version": "1.0",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "source": "communications-service",
  "detailType": "MessageDelivered",
  "time": "2026-03-21T20:30:00Z",
  "detail": {
    "messageId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "senderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "conversationType": "DIRECT",
    "recipientIds": [
      "b2c3d4e5-f6a7-8901-bcde-f12345678901"
    ],
    "content": "Hello, how are you?",
    "deliveredAt": "2026-03-21T20:30:00Z",
    "metadata": {
      "organizationId": "org-123",
      "platform": "web"
    }
  }
}
```

**Payload Fields**:
- `messageId` (string, UUID): Unique message identifier
- `conversationId` (string, UUID): Conversation the message belongs to
- `senderId` (string, UUID): User who sent the message
- `conversationType` (string, enum): "DIRECT" or "GROUP"
- `recipientIds` (array of strings): User IDs of all participants except sender
- `content` (string): Message content (max 10,000 characters)
- `deliveredAt` (string, ISO 8601): Timestamp when message was persisted
- `metadata` (object, optional): Additional context

**Consumers**:
- **Notification Service**: Send push notifications to recipients
- **Analytics Service**: Track messaging activity and engagement
- **Audit Service**: Log communication for compliance

**EventBridge Rule Example**:
```json
{
  "source": ["communications-service"],
  "detail-type": ["MessageDelivered"]
}
```

---

### ConversationCreatedEvent

**Purpose**: Published when a new conversation is created.

**Source**: `communications-service`  
**Detail Type**: `ConversationCreated`  
**Version**: `1.0`

**Schema**:
```json
{
  "version": "1.0",
  "id": "660f9511-f3ac-52e5-c827-557766551111",
  "source": "communications-service",
  "detailType": "ConversationCreated",
  "time": "2026-03-21T20:25:00Z",
  "detail": {
    "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "type": "GROUP",
    "name": "Project Alpha Team",
    "participantIds": [
      "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "c3d4e5f6-a7b8-9012-cdef-123456789012"
    ],
    "createdBy": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "createdAt": "2026-03-21T20:25:00Z",
    "metadata": {
      "organizationId": "org-123"
    }
  }
}
```

**Payload Fields**:
- `conversationId` (string, UUID): Unique conversation identifier
- `type` (string, enum): "DIRECT" or "GROUP"
- `name` (string, optional): Conversation name (required for GROUP)
- `participantIds` (array of strings): User IDs of all participants
- `createdBy` (string, UUID): User who created the conversation
- `createdAt` (string, ISO 8601): Timestamp when conversation was created
- `metadata` (object, optional): Additional context

**Consumers**:
- **Notification Service**: Notify participants about new conversation
- **Analytics Service**: Track conversation creation patterns

---

### ParticipantAddedEvent

**Purpose**: Published when a user is added to a conversation.

**Source**: `communications-service`  
**Detail Type**: `ParticipantAdded`  
**Version**: `1.0`

**Schema**:
```json
{
  "version": "1.0",
  "id": "770fa622-g4bd-63f6-d938-668877662222",
  "source": "communications-service",
  "detailType": "ParticipantAdded",
  "time": "2026-03-21T20:35:00Z",
  "detail": {
    "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userId": "d4e5f6a7-b8c9-0123-def1-234567890123",
    "addedBy": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "role": "MEMBER",
    "addedAt": "2026-03-21T20:35:00Z",
    "metadata": {
      "conversationType": "GROUP",
      "conversationName": "Project Alpha Team"
    }
  }
}
```

**Payload Fields**:
- `conversationId` (string, UUID): Conversation identifier
- `userId` (string, UUID): User who was added
- `addedBy` (string, UUID): User who added the participant
- `role` (string, enum): "MEMBER" or "ADMIN"
- `addedAt` (string, ISO 8601): Timestamp when participant was added
- `metadata` (object, optional): Additional context

**Consumers**:
- **Notification Service**: Notify the added user
- **Analytics Service**: Track participant growth

---

### ParticipantRemovedEvent

**Purpose**: Published when a user is removed from a conversation.

**Source**: `communications-service`  
**Detail Type**: `ParticipantRemoved`  
**Version**: `1.0`

**Schema**:
```json
{
  "version": "1.0",
  "id": "880fb733-h5ce-74g7-e049-779988773333",
  "source": "communications-service",
  "detailType": "ParticipantRemoved",
  "time": "2026-03-21T20:40:00Z",
  "detail": {
    "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userId": "d4e5f6a7-b8c9-0123-def1-234567890123",
    "removedBy": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "reason": "ADMIN_REMOVED",
    "removedAt": "2026-03-21T20:40:00Z",
    "metadata": {
      "conversationType": "GROUP"
    }
  }
}
```

**Payload Fields**:
- `conversationId` (string, UUID): Conversation identifier
- `userId` (string, UUID): User who was removed
- `removedBy` (string, UUID): User who removed the participant (can be same as userId for voluntary leave)
- `reason` (string, enum): "ADMIN_REMOVED", "VOLUNTARY_LEAVE", "CONVERSATION_DELETED"
- `removedAt` (string, ISO 8601): Timestamp when participant was removed
- `metadata` (object, optional): Additional context

**Consumers**:
- **Notification Service**: Notify the removed user
- **Analytics Service**: Track participant churn

---

## Event Versioning Strategy

### Version Format

Events use semantic versioning: `MAJOR.MINOR`

- **MAJOR**: Incremented for breaking changes (field removal, type changes)
- **MINOR**: Incremented for backward-compatible additions (new optional fields)

### Version Evolution Example

**Version 1.0** (Initial):
```json
{
  "version": "1.0",
  "detail": {
    "messageId": "...",
    "conversationId": "...",
    "senderId": "...",
    "content": "..."
  }
}
```

**Version 1.1** (Added optional field):
```json
{
  "version": "1.1",
  "detail": {
    "messageId": "...",
    "conversationId": "...",
    "senderId": "...",
    "content": "...",
    "attachments": []  // New optional field
  }
}
```

**Version 2.0** (Breaking change):
```json
{
  "version": "2.0",
  "detail": {
    "messageId": "...",
    "conversationId": "...",
    "senderId": "...",
    "body": {  // Changed from "content" to structured "body"
      "text": "...",
      "format": "plain"
    }
  }
}
```

### Consumer Compatibility

Consumers should:
1. Check the `version` field
2. Handle multiple versions gracefully
3. Ignore unknown fields (forward compatibility)
4. Provide default values for missing optional fields (backward compatibility)

```java
public void handleMessageDelivered(MessageDeliveredEvent event) {
    String version = event.getVersion();
    
    if (version.startsWith("1.")) {
        // Handle v1.x events
        processV1Message(event);
    } else if (version.startsWith("2.")) {
        // Handle v2.x events
        processV2Message(event);
    } else {
        logger.warn("Unknown event version: {}", version);
    }
}
```

---

## Event Publishing

### Java (Spring Boot)

```java
@Component
public class EventBridgePublisher {
    private final EventBridgeClient eventBridgeClient;
    private final String eventBusName;
    
    public void publishMessageDelivered(Message message, List<String> recipientIds) {
        MessageDeliveredEvent event = MessageDeliveredEvent.builder()
            .messageId(message.getId())
            .conversationId(message.getConversationId())
            .senderId(message.getSenderId())
            .conversationType(message.getConversationType())
            .recipientIds(recipientIds)
            .content(message.getContent())
            .deliveredAt(message.getCreatedAt())
            .build();
        
        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
            .source("communications-service")
            .detailType("MessageDelivered")
            .detail(objectMapper.writeValueAsString(event))
            .eventBusName(eventBusName)
            .build();
        
        PutEventsRequest request = PutEventsRequest.builder()
            .entries(entry)
            .build();
        
        eventBridgeClient.putEvents(request);
    }
}
```

---

## Event Consumption

### EventBridge Rule Configuration

```json
{
  "Name": "communications-to-notifications",
  "EventPattern": {
    "source": ["communications-service"],
    "detail-type": [
      "MessageDelivered",
      "ConversationCreated",
      "ParticipantAdded"
    ]
  },
  "Targets": [
    {
      "Arn": "arn:aws:lambda:us-east-1:123456789:function:notification-handler",
      "Id": "1"
    }
  ]
}
```

### Lambda Consumer (Python)

```python
import json

def lambda_handler(event, context):
    detail_type = event['detail-type']
    detail = event['detail']
    version = detail.get('version', '1.0')
    
    if detail_type == 'MessageDelivered':
        handle_message_delivered(detail, version)
    elif detail_type == 'ConversationCreated':
        handle_conversation_created(detail, version)
    
    return {'statusCode': 200}

def handle_message_delivered(detail, version):
    message_id = detail['messageId']
    recipient_ids = detail['recipientIds']
    
    # Send notifications to recipients
    for recipient_id in recipient_ids:
        send_push_notification(recipient_id, detail['content'])
```

---

## Event Validation

### JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "MessageDeliveredEvent",
  "type": "object",
  "required": ["version", "id", "source", "detailType", "time", "detail"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^[0-9]+\\.[0-9]+$"
    },
    "id": {
      "type": "string",
      "format": "uuid"
    },
    "source": {
      "type": "string",
      "const": "communications-service"
    },
    "detailType": {
      "type": "string",
      "const": "MessageDelivered"
    },
    "time": {
      "type": "string",
      "format": "date-time"
    },
    "detail": {
      "type": "object",
      "required": ["messageId", "conversationId", "senderId", "conversationType", "recipientIds", "content", "deliveredAt"],
      "properties": {
        "messageId": {
          "type": "string",
          "format": "uuid"
        },
        "conversationId": {
          "type": "string",
          "format": "uuid"
        },
        "senderId": {
          "type": "string",
          "format": "uuid"
        },
        "conversationType": {
          "type": "string",
          "enum": ["DIRECT", "GROUP"]
        },
        "recipientIds": {
          "type": "array",
          "items": {
            "type": "string",
            "format": "uuid"
          },
          "minItems": 1
        },
        "content": {
          "type": "string",
          "minLength": 1,
          "maxLength": 10000
        },
        "deliveredAt": {
          "type": "string",
          "format": "date-time"
        }
      }
    }
  }
}
```

---

## Idempotency

All events include a unique `id` field (UUID v4) to enable idempotent processing:

```java
@Component
public class IdempotentEventHandler {
    private final ProcessedEventRepository processedEventRepository;
    
    public void handleEvent(DomainEvent event) {
        String eventId = event.getId();
        
        // Check if already processed
        if (processedEventRepository.existsById(eventId)) {
            logger.info("Event already processed: {}", eventId);
            return;
        }
        
        // Process event
        processEvent(event);
        
        // Mark as processed
        processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
    }
}
```

---

## Monitoring

### CloudWatch Metrics

- `EventsPublished`: Count of events published per detail type
- `EventPublishFailures`: Count of failed event publications
- `EventProcessingLatency`: Time from event creation to processing

### CloudWatch Logs

```json
{
  "timestamp": "2026-03-21T20:30:00Z",
  "level": "INFO",
  "service": "communications-service",
  "event": "MessageDelivered",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "messageId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

---

## References

- **PROJECT.md**: Section 7 (Domain Events), Section 28 (Event Architecture)
- **Event Flow**: `specs/event-flow.md`
- **Event Schemas (General)**: `specs/event-schemas.md`
- **Communications Service**: `specs/communications-service.md`
- **AWS EventBridge**: https://docs.aws.amazon.com/eventbridge/
