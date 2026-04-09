# Lambda Services Documentation Alignment

Align documentation and tests to correctly reflect that Reporting and Notification services are event-driven Lambda functions, not REST microservices, following event ownership best practices.

**Created**: 2026-04-08  
**Status**: In Progress — Implementation complete; documentation/test alignment pending  
**Updated**: 2026-04-09  
**Related Documents**:
- PROJECT.md (Sections 40, 39, 23b)
- specs/reporting-service.md
- specs/notification-service.md
- specs/event-schemas.md
- specs/architecture-testing.md

---

## Problem Statement

The reporting and notification services are correctly implemented as event-driven Lambda functions, but:

1. **ReportController in BFF API** was created as a stub to make tests pass, violating event-driven architecture
2. **Architecture tests** expect REST endpoints for report creation instead of testing event flows
3. **Documentation** may contain outdated references to these services as "microservices"
4. ~~**ReportGenerationService stub**~~ — **RESOLVED (2026-04-09)**: `generate_report()` was a stub that returned fake metadata. Now wires the full pipeline: DataFetchingService → DataAggregationService → TemplateEngine → PdfGenerationService → S3StorageService → EventPublisher. See `.windsurf/tasks/reporting-service/001-wire-report-generation-pipeline.md`.
5. ~~**Notification handler stubs**~~ — **RESOLVED (2026-04-09)**: All three handlers (`ExperimentCompleted`, `ReportGenerated`, `MemberAdded`) were stubs that returned 200 without sending any notifications. Now fully implemented with RecipientService, EmailService, and WebhookService. Also fixed incorrect event envelope extraction (`detail.payload.*` pattern). See `.windsurf/tasks/notification-service/001-implement-event-handlers.md`.

This misalignment contradicts the project's intent to showcase **principal-level event-driven architecture**.

---

## Event Ownership Analysis

### Current Correct Implementation

**ExperimentCompleted Event**:
- **Owner**: Experiment Service (microservice)
- **Consumers**: Reporting Lambda, Notification Lambda
- **Flow**: Experiment Service → EventBridge → Lambdas

**ReportGenerated Event**:
- **Owner**: Reporting Service (Lambda)
- **Consumers**: Notification Lambda
- **Flow**: Reporting Lambda → EventBridge → Notification Lambda

### Violation in Current Tests

**ReportController** allows BFF API to create reports via REST:
- Bypasses event-driven architecture
- Violates single responsibility (Reporting Lambda owns report generation)
- Tests validate fake architecture, not real implementation

---

## Recommended Solution

### 1. ReportController Refactoring

**Keep READ operations** (query pattern):
```java
GET  /api/v1/reports              // List reports (query S3/DynamoDB)
GET  /api/v1/reports/{id}         // Get report metadata
GET  /api/v1/reports/{id}/download // Download from S3
```

**Remove WRITE operations**:
```java
❌ POST   /api/v1/reports          // Remove - use events
❌ DELETE /api/v1/reports/{id}     // Remove - use events
```

**Rationale**: BFF can query reports, but only Reporting Lambda creates them via events.

### 2. Architecture Test Updates

**Current (Incorrect)**:
```gherkin
# Direct REST creation - bypasses Lambda
Given path '/api/v1/reports'
And request { type: 'EXPERIMENT', format: 'PDF', experimentId: '#(experimentId)' }
When method POST
Then status 201
```

**Corrected (Event-Driven)**:
```gherkin
# Complete experiment - triggers ExperimentCompleted event
Given path '/api/v1/experiments', experimentId, 'complete'
When method POST
Then status 200

# Wait for async report generation (Lambda processes event)
* def reportReady = waitHelper.waitForS3Object(reportBucket, reportKey, 30)
* match reportReady == true

# Query generated report via BFF
Given path '/api/v1/reports'
And param experimentId = experimentId
When method GET
Then status 200
And match response[0].status == 'COMPLETED'
```

### 3. Documentation Updates

**Files to Update**:

1. **specs/bff-api.md** - Add report query endpoints, clarify no write operations
2. **specs/architecture-testing.md** - Update report test scenarios to use event flows
3. **tasks/architecture-tests/010-implement-report-tests.md** - Rewrite test implementation
4. **Any outdated references** - Search for "reporting microservice" → "Reporting Lambda"

**Files Already Correct**:
- ✅ specs/reporting-service.md - Correctly describes Lambda
- ✅ specs/notification-service.md - Correctly describes Lambda
- ✅ services/reporting-service/README.md - Correctly describes Lambda
- ✅ services/notification-service/README.md - Correctly describes Lambda

---

## Implementation Plan

### Phase 1: Documentation Audit (Read-Only)

1. **Search for incorrect references**:
   - "reporting microservice" → should be "Reporting Lambda" or "Reporting Service (Lambda)"
   - "notification microservice" → should be "Notification Lambda" or "Notification Service (Lambda)"
   - Any REST API descriptions for report/notification creation

2. **Identify affected files**:
   - PROJECT.md
   - specs/*.md
   - tasks/**/*.md
   - docs/**/*.md
   - .windsurf/plans/**/*.md

3. **Categorize by action**:
   - **Update**: Files with incorrect terminology but relevant content
   - **Archive**: Outdated plans/docs that are no longer relevant
   - **No change**: Files already correct

### Phase 2: Update Documentation

1. **Update PROJECT.md** (if needed):
   - Verify event ownership is clear
   - Ensure Lambda services are distinguished from microservices

2. **Update specs/bff-api.md**:
   - Add report query endpoints (GET only)
   - Document event-driven report creation flow
   - Clarify BFF does not create reports

3. **Update specs/architecture-testing.md**:
   - Update report test scenarios to use event flows
   - Add waiting strategies for Lambda execution
   - Document S3 polling for report completion

4. **Update task files**:
   - tasks/architecture-tests/010-implement-report-tests.md
   - Any other tasks referencing report/notification APIs

### Phase 3: Code Alignment

1. **Refactor ReportController**:
   - Remove `createReport()` method
   - Remove `deleteReport()` method
   - Keep `getReports()`, `getReport()`, `downloadReport()`
   - Add S3 integration for querying reports
   - Add DynamoDB integration for report metadata (if needed)

2. **Update ReportDto**:
   - Ensure fields match S3 metadata structure
   - Add fields for S3 location, presigned URLs

3. **Create S3 query service**:
   - ReportQueryService to fetch reports from S3
   - Handle presigned URL generation
   - Query report metadata

### Phase 4: Test Updates

1. **Update Karate feature files**:
   - features/reports/report-generation.feature
   - features/reports/report-management.feature
   - Remove POST /api/v1/reports scenarios
   - Add event-driven scenarios
   - Add S3 polling helpers

2. **Update WaitHelper.java**:
   - Add `waitForS3Object()` method
   - Add `waitForEventBridgeEvent()` method (if needed)
   - Add `waitForLambdaExecution()` method (if needed)

3. **Update AwsHelper.java**:
   - Add S3 client methods
   - Add methods to check S3 object existence
   - Add methods to generate presigned URLs

### Phase 5: Verification

1. **Run architecture tests** to verify event-driven flow
2. **Verify Lambda invocations** via EventBridge
3. **Verify S3 report storage** and retrieval
4. **Update test documentation** with new patterns

---

## Files to Modify

### Documentation (Phase 1-2)
- [ ] PROJECT.md (verify/update if needed)
- [ ] specs/bff-api.md (add report query endpoints)
- [ ] specs/architecture-testing.md (update test scenarios)
- [ ] tasks/architecture-tests/010-implement-report-tests.md
- [ ] Search results from grep (TBD based on audit)

### Code (Phase 3)
- [ ] services/bff-api/src/main/java/com/turaf/bff/controllers/ReportController.java
- [ ] services/bff-api/src/main/java/com/turaf/bff/dto/ReportDto.java
- [ ] services/bff-api/src/main/java/com/turaf/bff/dto/CreateReportRequest.java (DELETE)
- [ ] services/bff-api/src/main/java/com/turaf/bff/services/ReportQueryService.java (NEW)

### Tests (Phase 4)
- [ ] services/architecture-tests/src/test/resources/features/reports/report-generation.feature
- [ ] services/architecture-tests/src/test/resources/features/reports/report-management.feature
- [ ] services/architecture-tests/src/test/java/com/turaf/architecture/helpers/WaitHelper.java
- [ ] services/architecture-tests/src/test/java/com/turaf/architecture/helpers/AwsHelper.java

---

## Success Criteria

1. ✅ All references to "reporting/notification microservices" corrected to "Lambda functions"
2. ✅ ReportController only provides query operations (no creation)
3. ✅ Architecture tests validate event-driven report generation
4. ✅ Tests successfully wait for Lambda execution and S3 storage
5. ✅ Documentation clearly explains event ownership
6. ✅ BFF API spec documents report query endpoints only
7. ✅ **ReportGenerationService executes the full pipeline** (data fetch → aggregate → render → PDF → S3 → event) — completed 2026-04-09
8. ✅ **Notification handlers send real emails and webhooks** — all three handlers fully implemented 2026-04-09
9. ✅ **Event envelope extraction corrected** — handlers now read from `detail.payload.*` not `detail.*`

---

## Risks and Mitigations

**Risk**: Tests may be slower due to async Lambda execution  
**Mitigation**: Implement efficient polling with exponential backoff, reasonable timeouts (30s)

**Risk**: S3/EventBridge may not be available in local test environment  
**Mitigation**: Use MiniStack for local emulation (already configured)

**Risk**: Breaking existing tests  
**Mitigation**: Update tests incrementally, verify each change

---

## Notes

- This aligns with PROJECT.md's intent to showcase "principal-level event-driven architecture"
- Follows industry best practice: event ownership by single service
- Demonstrates proper separation between command (events) and query (REST) operations
- Showcases both microservices (Java Spring Boot) and serverless (Python Lambda) in one platform
