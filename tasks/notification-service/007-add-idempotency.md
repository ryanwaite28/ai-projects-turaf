# Task: Add Idempotency

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 2 hours  

## Objective

Implement idempotency tracking using DynamoDB to prevent duplicate notifications.

## Prerequisites

- [x] Task 002: Event handlers implemented

## Scope

**Files to Create**:
- `repositories/processed_event_repository.py`

## Implementation Details

### Idempotency Service

```python
class IdempotencyService:
    def __init__(self):
        self.dynamo_db_client = boto3.client('dynamodb')
        self.table_name = os.getenv("IDEMPOTENCY_TABLE_NAME")

    def is_processed(self, event_id):
        response = self.dynamo_db_client.get_item(
            TableName=self.table_name,
            Key={'eventId': {'S': event_id}}
        )
        return 'Item' in response

    def mark_processed(self, event_id):
        self.dynamo_db_client.put_item(
            TableName=self.table_name,
            Item={
                'eventId': {'S': event_id},
                'processedAt': {'S': datetime.now().isoformat()},
                'ttl': {'N': str(int(datetime.now().timestamp()) + 30 * 24 * 60 * 60)}
            }
        )
            .build();
        
        GetItemResponse response = dynamoDbClient.getItem(request);
        return response.hasItem();
    }
    
    public void markProcessed(String eventId) {
        PutItemRequest request = PutItemRequest.builder()
            .tableName(tableName)
            .item(Map.of(
                "eventId", AttributeValue.builder().s(eventId).build(),
                "processedAt", AttributeValue.builder().s(Instant.now().toString()).build(),
                "ttl", AttributeValue.builder().n(String.valueOf(getTtl())).build()
            ))
            .build();
        
        dynamoDbClient.putItem(request);
    }
    
    private long getTtl() {
        return Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond();
    }
}
```

## Acceptance Criteria

- [x] Idempotency check works
- [x] Events marked as processed
- [x] TTL configured
- [x] Duplicate processing prevented
- [x] Unit tests pass

**Note**: Idempotency service was already implemented in Task 002 with full DynamoDB integration, TTL support, and comprehensive unit tests.

## Testing Requirements

**Unit Tests**:
- Test idempotency check
- Test mark processed

**Test Files to Create**:
- `test_idempotency.py`

## References

- Specification: `specs/notification-service.md` (Idempotency section)
- Related Tasks: 008-add-unit-tests
