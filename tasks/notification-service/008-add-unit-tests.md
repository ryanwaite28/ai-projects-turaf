# Task: Add Unit Tests

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 3 hours  

## Objective

Create comprehensive unit tests for all notification service components.

## Prerequisites

- [x] All notification-service implementation tasks completed

## Scope

**Test Files to Create**:
- `EventRouterTest.java`
- `EmailServiceTest.java`
- `SesEmailSenderTest.java`
- `TemplateServiceTest.java`
- `WebhookServiceTest.java`
- `WebhookDeliveryServiceTest.java`
- `RecipientServiceTest.java`
- `IdempotencyServiceTest.java`

## Acceptance Criteria

- [ ] All components tested
- [ ] Code coverage > 80%
- [ ] All edge cases covered
- [ ] Mock AWS services properly

## Testing Requirements

**Unit Test Coverage**:
- Event handling and routing
- Email sending
- Template rendering
- Webhook delivery
- Recipient selection
- Idempotency

## References

- Specification: `specs/notification-service.md`
- Related Tasks: All notification-service tasks
