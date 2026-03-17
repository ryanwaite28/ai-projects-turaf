# Task: Add Idempotency

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Implement idempotency tracking using DynamoDB to prevent duplicate report generation.

## Prerequisites

- [x] Task 002: Event handler implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/main/java/com/turaf/reporting/service/IdempotencyService.java`

## Implementation Details

### Idempotency Service

```java
public class IdempotencyService {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    
    public IdempotencyService() {
        this.dynamoDbClient = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .build();
        this.tableName = System.getenv("IDEMPOTENCY_TABLE_NAME");
    }
    
    public boolean isProcessed(String eventId) {
        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of("eventId", AttributeValue.builder().s(eventId).build()))
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
        // 30 days from now
        return Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond();
    }
}
```

## Acceptance Criteria

- [ ] Idempotency check works
- [ ] Events marked as processed
- [ ] TTL configured
- [ ] Duplicate processing prevented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test idempotency check
- Test mark processed
- Test TTL calculation

**Test Files to Create**:
- `IdempotencyServiceTest.java`

## References

- Specification: `specs/reporting-service.md` (Idempotency section)
- Related Tasks: 010-add-unit-tests
