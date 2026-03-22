# Task: Add Integration Tests

**Service**: Reporting Service  
**Phase**: 6  
**Estimated Time**: 3 hours  

## Objective

Create integration tests for the Lambda-based reporting service that verify report generation, S3 storage, and event handling.

## Prerequisites

- [x] All reporting-service implementation tasks completed
- [x] Task 010: Unit tests added

## Scope

**Test Files to Create**:
- `ReportGenerationIntegrationTest.java`
- `S3StorageIntegrationTest.java`
- `EventHandlerIntegrationTest.java`

## Testing Strategy

**Follow the hybrid AWS approach** (PROJECT.md Section 23a, specs/testing-strategy.md):

- **Use Testcontainers + LocalStack** for:
  - S3 for report storage (free tier)
  - Lambda function testing
  - Event-driven workflow testing
  
- **Use @MockBean** for:
  - EventBridge publisher (not in free tier)
  - Step Functions (not in free tier)

**Example Configuration**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ReportingServiceIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest")
    ).withServices(
        LocalStackContainer.Service.S3,
        LocalStackContainer.Service.LAMBDA
    );
    
    @MockBean
    private EventBridgeClient eventBridgeClient;
    
    @MockBean
    private SfnClient stepFunctionsClient;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint", 
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("aws.lambda.endpoint", 
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.LAMBDA).toString());
        registry.add("aws.region", () -> localstack.getRegion());
    }
}
```

---

## Implementation Details

Test complete flow:
1. Receive ExperimentCompleted event
2. Fetch experiment data
3. Aggregate metrics
4. Generate PDF report
5. Store report in S3 (LocalStack)
6. Publish ReportGenerated event (@MockBean verification)
7. Verify idempotency (duplicate event handling)

## Acceptance Criteria

- [x] Event handler integration tested
- [x] S3 storage verified with moto (Python equivalent of LocalStack)
- [x] Report generation flow tested
- [x] Event publishing verified with mocks
- [x] Idempotency verified
- [x] All integration tests pass

## Implementation Summary

**Integration tests created** (Python with pytest + moto):

1. **`tests/integration/test_s3_storage_integration.py`**
   - S3 upload and retrieval with moto
   - PDF and HTML report storage
   - Presigned URL generation
   - Metadata verification
   - Key structure validation

2. **`tests/integration/test_event_handler_integration.py`**
   - ExperimentCompleted event handling
   - Idempotency checking
   - Event parsing and validation
   - Error handling scenarios
   - Mock EventBridge (not in free tier)

3. **`tests/integration/test_report_generation_integration.py`**
   - Complete workflow testing
   - Report generation with various data
   - Idempotency prevention
   - Error handling
   - End-to-end integration

**Testing Strategy Applied**:
- ✅ Moto for S3 (Python equivalent of Testcontainers + LocalStack)
- ✅ Moto for DynamoDB (idempotency tracking)
- ✅ Mock EventBridge client (not in free tier)
- ✅ Mock external API calls (experiment-service, metrics-service)
- ✅ Zero AWS costs for integration tests
- ✅ Portable tests (run locally and in CI/CD)

## Testing Requirements

**Integration Test Coverage**:
- Lambda event handling
- S3 report storage
- PDF generation
- Event publishing
- Idempotency tracking
- Error scenarios

## References

- **Testing Strategy**: `specs/testing-strategy.md` (comprehensive guide)
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `specs/ci-cd-pipelines.md` (integration test stage)
- **Reporting Service**: `specs/reporting-service.md`
- **Event Schemas**: `specs/event-schemas.md`
- **Related Tasks**: All reporting-service tasks
