# MiniStack AWS Services Audit & Setup

Audit all AWS service usage across the project, provision missing resources in MiniStack's init script, and fix configuration gaps so all MiniStack-emulable services work for local development.

## Audit Findings

### AWS Services Currently Used

| AWS Service | Used By | In init-aws.sh? | MiniStack Config? |
|---|---|---|---|
| **EventBridge** | identity, org, experiment, metrics, communications, reporting-svc | ✅ | ✅ |
| **SQS** | communications-service, ws-gateway | ✅ | ✅ |
| **S3** | reporting-service (boto3) | ✅ buckets created | ❌ No endpoint override in boto3 code |
| **SNS** | init-aws.sh topics only | ✅ | N/A |
| **Secrets Manager** | init-aws.sh secrets only | ✅ | N/A |
| **DynamoDB** | notification-service (idempotency), reporting-service (idempotency) | ❌ **Missing** | ❌ No tables provisioned |
| **SES** | notification-service (ses_client.py) | ❌ **Missing** | ❌ No identities verified |

### Refactoring Notes
- The Java common module's DynamoDB-based `IdempotencyService` and `DatabaseIdempotencyService` were **removed**. The common module now has only an `IdempotencyChecker` interface (no AWS dependency). **No Java/Spring services use DynamoDB.**
- Only the **Python Lambda services** (notification-service, reporting-service) use DynamoDB via boto3 for idempotency.
- notification-service and reporting-service are **Lambda functions** (not in docker-compose) — they need their AWS resources provisioned but don't need docker-compose entries.

### Config Gaps
- `SqsConfig.java` reads `${aws.sqs.endpoint:}` but `application.yml` only sets `aws.endpoint` — SQS endpoint override is **not wired** in docker profile.

## Plan

### 1. Update `init-aws.sh` — Add DynamoDB tables
Create DynamoDB tables used by notification-service and reporting-service idempotency:
- `processed_notification_events` (PK: `eventId` String)
- `processed_events` (PK: `eventId` String)
Both with TTL enabled on `ttl` attribute.

### 2. Update `init-aws.sh` — Add SES identity verification
Verify the email identity used by notification-service:
- `aws ses verify-email-identity --email-address notifications@turaf.com`

### 3. Fix SQS endpoint config in communications-service
`SqsConfig.java` reads `aws.sqs.endpoint` but `application.yml` only defines `aws.endpoint`. Two options:
- **(Chosen)** Add `aws.sqs.endpoint: ${AWS_ENDPOINT:}` to `application.yml` under the `aws.sqs` section, or
- Change `SqsConfig.java` to read `aws.endpoint` instead.

The cleaner fix: update `SqsConfig.java` to use `aws.endpoint` (consistent with `EventBridgeConfig.java` pattern), so all AWS configs use the same property.

### 4. Update `init-aws.sh` summary
Update the summary section at the bottom to list new DynamoDB tables and SES identities.

### 5. Update documentation
- `docs/LOCAL_DEVELOPMENT.md` — Add DynamoDB and SES to the list of provisioned services
- `PROJECT.md` — Update AWS service testing strategy to note DynamoDB is emulated by MiniStack
- `.windsurf/rules/rules.md` — Add DynamoDB to emulated services list

### 6. Create changelog entry

## Out of Scope
- **notification-service and reporting-service are not containerized** — they are Lambda functions. No docker-compose changes needed for them.
- **No Java/Spring DynamoDB changes needed** — the common module was refactored to remove DynamoDB dependency.
