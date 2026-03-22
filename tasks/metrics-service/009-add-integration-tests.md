# Task: Add Integration Tests

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 3 hours  

## Objective

Create integration tests that verify the complete metrics management flow from API endpoints through to database.

## Prerequisites

- [x] All metrics-service implementation tasks completed
- [x] Task 008: Unit tests added

## Scope

**Test Files to Create**:
- `MetricControllerIntegrationTest.java`
- `MetricsFlowIntegrationTest.java`

## Testing Strategy

**Follow the hybrid AWS approach** (PROJECT.md Section 23a, specs/testing-strategy.md):

- **Use Testcontainers + LocalStack** for:
  - DynamoDB for metrics storage (free tier)
  - Time-series data operations
  - Batch processing tests
  
- **Use @MockBean** for:
  - CloudWatch metrics (limited in LocalStack free tier)
  - EventBridge publisher (not in free tier)

**Example Configuration**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MetricsServiceIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest")
    ).withServices(LocalStackContainer.Service.DYNAMODB);
    
    @MockBean
    private CloudWatchClient cloudWatchClient;
    
    @MockBean
    private EventBridgeClient eventBridgeClient;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint", 
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString());
        registry.add("aws.region", () -> localstack.getRegion());
    }
}
```

---

## Implementation Details

Test complete flow:
1. Record single metric (DynamoDB via LocalStack)
2. Record batch metrics (DynamoDB via LocalStack)
3. Retrieve metrics with filters (DynamoDB queries)
4. Aggregate metrics (verify CloudWatch calls with @MockBean)
5. Verify time-series queries (DynamoDB range queries)
6. Verify events published (EventBridge @MockBean verification)

## Acceptance Criteria

- [x] All API endpoints tested end-to-end
- [x] Complete metrics flow tested
- [x] Aggregation verified
- [x] Batch processing verified
- [x] All integration tests pass

## Testing Requirements

**Integration Test Coverage**:
- Metric recording
- Batch processing
- Time-range queries
- Aggregation
- Event publishing
- Tenant isolation

## References

- **Testing Strategy**: `specs/testing-strategy.md` (comprehensive guide)
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `specs/ci-cd-pipelines.md` (integration test stage)
- **Metrics Service**: `specs/metrics-service.md`
- **Event Schemas**: `specs/event-schemas.md`
- **Related Tasks**: All metrics-service tasks
