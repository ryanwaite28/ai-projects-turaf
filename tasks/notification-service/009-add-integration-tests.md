# Task: Add Integration Tests

**Service**: Notification Service  
**Phase**: 7  
**Estimated Time**: 3 hours  

## Objective

Create integration tests for the Lambda-based notification service that verify email delivery, webhook calls, and event handling.

## Prerequisites

- [x] All notification-service implementation tasks completed
- [x] Task 008: Unit tests added

## Scope

**Test Files to Create**:
- `EmailNotificationIntegrationTest.java`
- `WebhookNotificationIntegrationTest.java`
- `EventHandlerIntegrationTest.java`

## Testing Strategy

**Follow the hybrid AWS approach** (PROJECT.md Section 23a, specs/testing-strategy.md):

- **Use Testcontainers + LocalStack** for:
  - SES for email sending (use @MockBean - limited in free tier)
  - SNS for notifications (free tier)
  - Lambda function testing
  
- **Use @MockBean** for:
  - SES email client (limited in LocalStack free tier)
  - EventBridge consumer (not in free tier)
  - External webhook endpoints (use WireMock)

**Example Configuration**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest")
    ).withServices(
        LocalStackContainer.Service.SNS,
        LocalStackContainer.Service.LAMBDA
    );
    
    @MockBean
    private SesClient sesClient;
    
    @MockBean
    private EventBridgeClient eventBridgeClient;
    
    private static WireMockServer wireMockServer;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        
        registry.add("aws.sns.endpoint", 
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.SNS).toString());
        registry.add("webhook.base-url", () -> wireMockServer.baseUrl());
        registry.add("aws.region", () -> localstack.getRegion());
    }
}
```

---

## Implementation Details

Test complete flow:
1. Receive ReportGenerated event
2. Select recipients
3. Send email notifications (verify SES @MockBean calls)
4. Send webhook notifications (WireMock verification)
5. Verify idempotency (duplicate event handling)
6. Test error scenarios (failed delivery, retries)

## Acceptance Criteria

- [x] Event handler integration tested
- [x] Email sending verified with mocks
- [x] Webhook delivery tested with requests-mock (Python equivalent of WireMock)
- [x] Recipient selection verified
- [x] Idempotency verified
- [x] Error handling tested
- [x] All integration tests pass

## Implementation Summary

**Integration tests created** (Python with pytest + moto + requests-mock):

1. **`tests/integration/test_email_notification_integration.py`** (8 tests)
   - Email composition with real templates
   - SES client interaction (mocked - not in free tier)
   - Multiple recipient handling
   - Error handling scenarios
   - Template rendering with special characters

2. **`tests/integration/test_webhook_notification_integration.py`** (8 tests)
   - Webhook delivery with requests-mock (Python equivalent of WireMock)
   - Retry logic on failures
   - Event type filtering
   - Disabled webhook handling
   - Timeout error handling
   - Signature header verification
   - Multiple endpoint delivery

3. **`tests/integration/test_event_handler_integration.py`** (10 tests)
   - Complete event handling workflow
   - Event routing (ReportGenerated, ExperimentCompleted, MemberAdded)
   - Idempotency prevention
   - Unknown event type handling
   - Configuration validation
   - Error handling throughout workflow
   - Email + webhook orchestration

**Testing Strategy Applied**:
- ✅ Moto for DynamoDB (idempotency tracking - free tier)
- ✅ Mock SES client (limited in LocalStack free tier)
- ✅ requests-mock for webhooks (Python equivalent of WireMock)
- ✅ Mock external API calls (organization-service, user-service)
- ✅ Zero AWS costs for integration tests
- ✅ Portable tests (run locally and in CI/CD)

**Total: 26 integration tests**

## Testing Requirements

**Integration Test Coverage**:
- Lambda event handling
- Email template rendering
- SES email sending (mocked)
- Webhook delivery
- Recipient selection logic
- Idempotency tracking
- Retry mechanisms
- Error scenarios

## References

- **Testing Strategy**: `specs/testing-strategy.md` (comprehensive guide)
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `specs/ci-cd-pipelines.md` (integration test stage)
- **Notification Service**: `specs/notification-service.md`
- **Event Schemas**: `specs/event-schemas.md`
- **Related Tasks**: All notification-service tasks
