# MiniStack AWS Services Complete Setup

**Date**: March 29, 2026  
**Type**: Infrastructure Enhancement  
**Impact**: Local Development, Testing  
**Related Documents**: 
- [LOCAL_DEVELOPMENT.md](../docs/LOCAL_DEVELOPMENT.md)
- [PROJECT.md](../PROJECT.md)
- [.windsurf/rules/rules.md](../.windsurf/rules/rules.md)

---

## Summary

Completed comprehensive audit of AWS service usage across the project and provisioned all missing resources in MiniStack for local development. Added DynamoDB tables for Lambda service idempotency tracking and SES email identity verification. Fixed SQS endpoint configuration inconsistency in communications-service.

---

## Changes Made

### 1. DynamoDB Tables Added to MiniStack

**File**: `infrastructure/docker/ministack/init-aws.sh`

Added two DynamoDB tables for idempotency tracking used by Python Lambda services:

- **`processed_notification_events`** - Idempotency table for notification-service
  - Primary Key: `eventId` (String)
  - TTL enabled on `ttl` attribute
  - Billing mode: PAY_PER_REQUEST

- **`processed_events`** - Idempotency table for reporting-service
  - Primary Key: `eventId` (String)
  - TTL enabled on `ttl` attribute
  - Billing mode: PAY_PER_REQUEST

Both tables use TTL-based automatic cleanup (30-day retention) to prevent unbounded growth.

### 2. SES Email Identity Verification

**File**: `infrastructure/docker/ministack/init-aws.sh`

Added SES email identity verification for notification-service:

```bash
aws ses verify-email-identity --email-address notifications@turaf.com
```

This enables the notification-service Lambda function to send emails via MiniStack's SES emulation during local development and testing.

### 3. SQS Endpoint Configuration Fix

**File**: `services/communications-service/src/main/java/com/turaf/communications/infrastructure/config/SqsConfig.java`

**Issue**: `SqsConfig.java` was reading from `${aws.sqs.endpoint:}` but `application.yml` only defined `aws.endpoint`, creating a configuration mismatch.

**Fix**: Changed `SqsConfig.java` to use `${aws.endpoint:}` for consistency with `EventBridgeConfig.java` pattern. All AWS service configs now use the same `aws.endpoint` property.

**Before**:
```java
@Value("${aws.sqs.endpoint:}")
private String endpoint;
```

**After**:
```java
@Value("${aws.endpoint:}")
private String endpoint;
```

### 4. Documentation Updates

Updated three key documentation files to reflect DynamoDB and SES support:

**`docs/LOCAL_DEVELOPMENT.md`**:
- Added DynamoDB and SES to MiniStack services table
- Added DynamoDB tables and SES identities to pre-created resources section

**`PROJECT.md`**:
- Added DynamoDB and SES to emulated services list in AWS Service Testing Strategy
- Updated DynamoDB description to note idempotency tracking usage

**`.windsurf/rules/rules.md`**:
- Added DynamoDB and SES to integration testing MiniStack services list

### 5. Init Script Summary Update

**File**: `infrastructure/docker/ministack/init-aws.sh`

Updated the summary section to include newly provisioned resources:

```bash
echo "  - DynamoDB Tables: processed_notification_events, processed_events (with TTL enabled)"
echo "  - SES Identities: notifications@turaf.com"
```

---

## Rationale

### Why DynamoDB for Lambda Services?

The Python Lambda services (notification-service, reporting-service) use DynamoDB for idempotency tracking because:

1. **Lambda-native pattern** - DynamoDB is the standard choice for Lambda idempotency
2. **TTL support** - Automatic cleanup of old records without manual maintenance
3. **Serverless alignment** - Fits Lambda's serverless execution model
4. **boto3 integration** - Native Python SDK support

### Why Not Java Services?

The Java/Spring services were refactored to remove DynamoDB dependencies. The common module now provides only an `IdempotencyChecker` interface with no AWS dependency. This allows services to choose their idempotency implementation (PostgreSQL, DynamoDB, etc.) without coupling to AWS.

### Why SES for Notifications?

The notification-service uses SES for email delivery because:

1. **Production parity** - Matches AWS production environment
2. **MiniStack support** - Full SES emulation available locally
3. **Testing capability** - Enables email flow testing without external services

---

## Testing Impact

### Local Development

All AWS services used by the project are now fully emulated by MiniStack:

- ✅ **S3** - Object storage (reports, artifacts)
- ✅ **EventBridge** - Event bus and routing
- ✅ **SQS** - Message queues (standard and FIFO)
- ✅ **SNS** - Pub/sub messaging
- ✅ **DynamoDB** - Idempotency tracking for Lambda services
- ✅ **SES** - Email notifications
- ✅ **Secrets Manager** - Database passwords

### Integration Tests

Integration tests can now:

- Test Lambda service idempotency behavior with real DynamoDB
- Test email notification flows with SES emulation
- Verify end-to-end event-driven workflows locally
- Run without any AWS costs or external dependencies

---

## Migration Notes

### No Breaking Changes

This change is **non-breaking**:

- Existing services continue to work unchanged
- No code changes required in Java services
- Lambda services already had DynamoDB code, just needed tables provisioned
- SQS config fix is backward-compatible (uses same environment variable)

### Verification Steps

To verify the changes work:

```bash
# 1. Stop and remove existing containers
docker-compose down -v

# 2. Start MiniStack
docker-compose up -d ministack

# 3. Wait for initialization
sleep 10

# 4. Verify DynamoDB tables exist
aws --endpoint-url=http://localhost:4566 dynamodb list-tables

# Expected output includes:
# - processed_notification_events
# - processed_events

# 5. Verify SES identity
aws --endpoint-url=http://localhost:4566 ses list-identities

# Expected output includes:
# - notifications@turaf.com
```

---

## Future Considerations

### Lambda Service Containerization

Currently, notification-service and reporting-service are Lambda functions not included in docker-compose. Future work could:

1. Add them as containerized services for local development
2. Use AWS SAM Local or similar for Lambda emulation
3. Keep them as Lambda-only and test via direct invocation

This decision is deferred pending requirements for local Lambda testing.

### Idempotency Strategy Alignment

The project now has two idempotency approaches:

- **Lambda services** - DynamoDB-based (notification-service, reporting-service)
- **Spring services** - Interface-based, implementation TBD

Future work should align on a consistent strategy or document the rationale for the split.

---

## Related Changes

- **ADR**: None (infrastructure enhancement, not architectural decision)
- **Specs**: No spec changes required
- **Tasks**: No task updates required

---

**Author**: Development Team  
**Reviewers**: N/A (Solo project)  
**Status**: Completed
