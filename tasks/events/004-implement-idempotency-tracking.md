# Task: Implement Idempotency Tracking

**Service**: Events Infrastructure  
**Phase**: 6  
**Estimated Time**: 2 hours  

## Objective

Implement shared idempotency tracking infrastructure using DynamoDB to prevent duplicate event processing.

## Prerequisites

- [x] Task 001: Event envelope implemented

## Scope

**Files to Create**:
- `services/common/src/main/java/com/turaf/common/events/IdempotencyService.java`
- `services/common/src/main/java/com/turaf/common/events/IdempotencyRecord.java`
- `infrastructure/terraform/modules/messaging/idempotency-table.tf`

## Implementation Details

### Idempotency Service

```java
@Component
public class IdempotencyService {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    
    public IdempotencyService(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.idempotency-table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }
    
    public boolean isProcessed(String eventId) {
        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of("eventId", AttributeValue.builder().s(eventId).build()))
            .build();
        
        GetItemResponse response = dynamoDbClient.getItem(request);
        return response.hasItem();
    }
    
    public void markProcessed(String eventId, String eventType, String handler) {
        PutItemRequest request = PutItemRequest.builder()
            .tableName(tableName)
            .item(Map.of(
                "eventId", AttributeValue.builder().s(eventId).build(),
                "eventType", AttributeValue.builder().s(eventType).build(),
                "handler", AttributeValue.builder().s(handler).build(),
                "processedAt", AttributeValue.builder().s(Instant.now().toString()).build(),
                "ttl", AttributeValue.builder().n(String.valueOf(getTtl())).build()
            ))
            .conditionExpression("attribute_not_exists(eventId)")
            .build();
        
        try {
            dynamoDbClient.putItem(request);
        } catch (ConditionalCheckFailedException e) {
            log.warn("Event already processed: {}", eventId);
        }
    }
    
    private long getTtl() {
        // 30 days from now
        return Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond();
    }
    
    public IdempotencyRecord getRecord(String eventId) {
        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of("eventId", AttributeValue.builder().s(eventId).build()))
            .build();
        
        GetItemResponse response = dynamoDbClient.getItem(request);
        
        if (!response.hasItem()) {
            return null;
        }
        
        Map<String, AttributeValue> item = response.item();
        return new IdempotencyRecord(
            item.get("eventId").s(),
            item.get("eventType").s(),
            item.get("handler").s(),
            Instant.parse(item.get("processedAt").s())
        );
    }
}
```

### DynamoDB Table

```hcl
resource "aws_dynamodb_table" "idempotency" {
  name         = "turaf-event-idempotency-${var.environment}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"
  
  attribute {
    name = "eventId"
    type = "S"
  }
  
  ttl {
    attribute_name = "ttl"
    enabled        = true
  }
  
  tags = {
    Name        = "turaf-event-idempotency-${var.environment}"
    Environment = var.environment
  }
}
```

## Acceptance Criteria

- [x] Idempotency service implemented
- [x] DynamoDB table created
- [x] TTL configured for automatic cleanup
- [x] Conditional writes prevent duplicates
- [x] Record retrieval works
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test isProcessed check
- Test markProcessed
- Test duplicate prevention
- Test TTL calculation

**Test Files to Create**:
- `IdempotencyServiceTest.java`

## References

- Specification: `specs/event-flow.md` (Idempotency section)
- Related Tasks: 005-add-event-validation
