# Plan: Comprehensive Architecture Review

**Status**: Active  
**Created**: 2026-04-09  
**Related Docs**:
- `PROJECT.md` — authoritative source for all requirements
- `.windsurf/plans/active/fix-auth-architecture-and-tests.md`
- `.windsurf/plans/active/lambda-services-documentation-alignment-d83504.md`
- `.windsurf/tasks/reporting-service/001-wire-report-generation-pipeline.md`
- `.windsurf/tasks/notification-service/001-implement-event-handlers.md`
**Related Services**: bff-api, identity-service, organization-service, experiment-service, metrics-service, communications-service, ws-gateway, reporting-service, notification-service, infrastructure, cicd

---

## Problem / Goal

A full-stack architecture review of the Turaf platform covering:
1. **Application completeness** — every service (BFF, microservices, WS Gateway, Lambdas) is fully implemented with no stubs, correct business logic, and full test coverage
2. **Infrastructure correctness** — Terraform modules are internally consistent, properly named, and wired (no SQS/EventBridge conflicts, no orphaned catch-all rules, services have their own Terraform)
3. **CI/CD correctness** — GitHub Actions workflows build, test, and deploy each component correctly with no typos, placeholder steps, or missing test gates

The review is read-first: each phase audits the current state, documents gaps in task files under `.windsurf/tasks/`, and then fixes them. The goal is full production readiness aligned with `PROJECT.md`.

---

## Scope Map

| Component | Type | Language | Review Phase |
|---|---|---|---|
| `bff-api` | Spring Boot (ECS) | Java 21 | Phase 2 |
| `identity-service` | Spring Boot (ECS) | Java 21 | Phase 1 |
| `organization-service` | Spring Boot (ECS) | Java 21 | Phase 1 |
| `experiment-service` | Spring Boot (ECS) | Java 21 | Phase 1 |
| `metrics-service` | Spring Boot (ECS) | Java 21 | Phase 1 |
| `communications-service` | Spring Boot (ECS) | Java 21 | Phase 1 |
| `ws-gateway` | NestJS (ECS) | TypeScript | Phase 3 |
| `reporting-service` | Lambda | Python 3.11 | Phase 4 |
| `notification-service` | Lambda | Python 3.11 | Phase 4 |
| `infrastructure/terraform` | Terraform | HCL | Phase 5 |
| `.github/workflows` | GitHub Actions | YAML | Phase 6 |
| `architecture-tests` | Karate | Gherkin/Java | Phase 7 |

---

## Known Issues (Pre-identified During Review)

Document issues found during exploration — to be fixed during implementation phases.

### Infrastructure Issues

**Lambda Module (`infrastructure/terraform/modules/lambda/main.tf`)**
- Uses generic function names: `event-processor-{env}`, `notification-processor-{env}`, `report-generator-{env}` — should be `reporting-service-{env}` and `notification-service-{env}`; the generic `event-processor` has no matching service
- Has SQS `aws_lambda_event_source_mapping` resources for notification and report generators (lines 243–266) — these services are EventBridge-triggered (not SQS), conflicting with `eventbridge-rules.tf`
- Has a catch-all EventBridge rule (`event_processor`, lines 201–221) that matches all `turaf.*` sources — conflicts with specific rules in `eventbridge-rules.tf`
- Handler string `handler.main` is wrong — actual entry points are `lambda_handler.lambda_handler` (reporting) and `notification_handler.lambda_handler` (notification)

**EventBridge Rules (`infrastructure/terraform/modules/messaging/eventbridge-rules.tf`)**
- `MemberAdded` event (published by organization-service and consumed by notification-service) is not routed — rule exists for `OrganizationCreated`/`OrganizationUpdated` but not `MemberAdded`
- `input_transformer` on `experiment_completed_reporting` target transforms the event into a different shape than what `reporting-service` expects — the handler reads from the native EventBridge envelope, not the transformed shape

**Service-Level Terraform**
- Service deployment workflows (`service-identity.yml`, etc.) reference `./services/{service}/terraform` directories for ECS task/service resources
- These per-service Terraform directories do not exist — ECS service definitions are in `archived/` inside compute module
- Each microservice needs its own `terraform/` directory with ECS task definition + service + target group + ALB listener rule

### CI/CD Issues

**Lambda Workflows (`deploy-reporting-lambda.yml`, `deploy-notification-lambda.yml`)**
- Typo on line 119 and 213: `s3://${BUCKET_NAME}}` (double closing brace `}}`) — will fail at runtime
- Packaging zips `src/` subdirectory contents but handler entry point is `lambda_handler.lambda_handler` at module root — the zip structure is wrong; `lambda_handler.py` needs to be at the root of the zip, not under `src/`
- Checks for `handler.py` (line 101) but reporting service entry point is `src/lambda_handler.py` — this check never succeeds and the handler won't be at the correct path
- No Python unit tests run before packaging — code is not validated before deployment
- Lambda workflow for `deploy-dev` only triggers on `develop` branch (line 61) but `deploy-qa` and `deploy-prod` trigger on `main` — `develop` pushes skip DEV deployment for QA/PROD promotion

**ECS Service Workflows (`service-*.yml`)**
- No unit tests or integration tests run before Docker build — untested code ships to dev
- QA integration tests are placeholder: `# Integration tests will be added when service is implemented`
- PROD smoke tests are placeholder: `# Smoke tests will be added when service is implemented`
- `service-identity.yml` sets `needs: [security-scan]` for `deploy-dev`, but `security-scan` only runs on `main` — on `develop` push, `security-scan` is skipped and `deploy-dev` runs regardless (acceptable but should be explicit)

**Infrastructure Workflow (`infrastructure.yml`)**
- Bootstrap runs with `-backend=false` (no remote state) but applies real AWS resources — bootstrap results are not tracked in state
- `terraform-apply-dev` triggers on ANY `feature/*` branch push — infrastructure changes deploy to DEV on every feature branch, which may be undesirable
- No `terraform plan` output review step between plan and apply for any environment

### Application Issues

**BFF API**
- `ReportController` may still contain stub write operations (`POST /api/v1/reports`, `DELETE /api/v1/reports/{id}`) — these should be removed per the lambda-services alignment plan; only GET operations belong in BFF
- Downstream client for reports queries S3/DynamoDB directly or proxies to a non-existent report service — needs a `ReportQueryService` backed by S3

**WS Gateway (`services/ws-gateway`)**
- No Dockerfile found in the glob results — ECS deployment requires a Docker image; check if Dockerfile exists or is missing
- `node_modules/` is present in the repository (should be in `.gitignore`) — build artifacts committed to VCS

**Per-service microservices**
- Application service stubs in QA/PROD workflows are a signal that implementation may be incomplete in some services — verified per service in Phase 1

---

## Implementation Plan

### Phase 1 — Java Microservices Completeness Review

Review each Spring Boot service against PROJECT.md. For each service check:
- Domain: aggregate roots, entities, value objects, domain events, repositories (interfaces in domain layer)
- Application: use cases, application services, DTOs, exceptions
- Infrastructure: JPA entities, repository implementations, event publishers, security filters
- REST: all required endpoints per PROJECT.md, request validation, error responses, global exception handler
- Tests: domain unit tests, application service tests, integration tests with Testcontainers

#### 1.1 Identity Service
- [ ] Verify `User` aggregate has `UserCreated`, `UserPasswordChanged`, `UserProfileUpdated` domain events
- [ ] Verify `PasswordResetToken` domain logic is complete
- [ ] Verify `AuthController` covers: register, login, refresh-token, logout, password-reset request, password-reset confirm
- [ ] Verify `UserController` covers: get-profile, update-profile, change-password
- [ ] Verify integration tests cover auth flow end-to-end (register → login → refresh → logout)
- [ ] Verify `UserCreated` event is published to EventBridge after registration

#### 1.2 Organization Service
- [ ] Verify `Organization` aggregate has `OrganizationCreated`, `MemberAdded`, `MemberRemoved`, `OrganizationUpdated` domain events
- [ ] Verify `OrganizationController` covers: create, get, update, list-by-user, delete
- [ ] Verify `MembershipController` covers: add-member, remove-member, get-members, update-member-role
- [ ] Verify all events are published to EventBridge (especially `MemberAdded` for notification-service)
- [ ] Verify `notification-service` handles `MemberAdded` — confirm event detail.payload schema matches what `handlers/member_added.py` expects (`memberId`, `memberEmail`, `memberName`, `role`)
- [ ] Verify integration tests cover membership lifecycle

#### 1.3 Experiment Service
- [ ] Verify `Problem` aggregate (CRUD) and `ProblemController` covers: create, get, update, list, delete
- [ ] Verify `Hypothesis` aggregate and `HypothesisController` covers: create, get, update, list, link-to-problem, delete
- [ ] Verify `Experiment` aggregate state machine: DRAFT → RUNNING → COMPLETED/FAILED with domain events at each transition
- [ ] Verify `ExperimentController` covers: create, get, update, start, complete, fail, list, delete
- [ ] Verify `ExperimentCompleted` event payload matches what `reporting-service` handler expects (`experimentId`, `completedAt`, `result` in `detail.payload`)
- [ ] Verify integration tests cover full experiment lifecycle with event publishing

#### 1.4 Metrics Service
- [ ] Verify `Metric` entity design and `MetricRepository` interface
- [ ] Verify `MetricController` covers: record-metric, get-metrics, get-aggregated-metrics, batch-record
- [ ] Verify aggregation logic is in the correct layer (application or domain, not infrastructure)
- [ ] Verify `MetricsCalculated` event is published when aggregation runs
- [ ] Verify integration tests cover metric ingestion and retrieval by experiment

#### 1.5 Communications Service
- [ ] Verify `Conversation`, `Message`, `ReadState` domain entities
- [ ] Verify `ConversationController`, `MessageController`, `UnreadCountController` endpoints
- [ ] Verify SQS consumer for WS Gateway message publishing
- [ ] Verify read-state tracking updates correctly
- [ ] Verify integration tests cover full conversation + message flow

#### 1.6 Common Module
- [ ] Verify `DomainEvent` interface consistency across all services (field: `getOccurredAt()` not `getTimestamp()`)
- [ ] Verify `EventEnvelope` wraps all events consistently (matches Python handler `detail.payload.*` extraction)
- [ ] Verify `ServiceJwtAuthenticationFilter` is correctly used in all downstream services (not BFF pattern)
- [ ] Verify `TenantContextHolder` is populated by filter in all services

---

### Phase 2 — BFF API Completeness Review

The BFF is the single entry point from the frontend. It must proxy all microservice capabilities and own JWT validation.

#### 2.1 Route Coverage
- [ ] Auth routes: proxy to identity-service (register, login, refresh, logout, password-reset)
- [ ] Organization routes: proxy to organization-service (CRUD + membership management)
- [ ] Problem routes: proxy to experiment-service
- [ ] Hypothesis routes: proxy to experiment-service
- [ ] Experiment routes: proxy to experiment-service (lifecycle endpoints)
- [ ] Metrics routes: proxy to metrics-service
- [ ] Reports routes: GET-only to query S3 reports (NO create/delete — these are event-driven)
- [ ] Conversations routes: proxy to communications-service
- [ ] Messages routes: proxy to communications-service

#### 2.2 Report Query Implementation
- [ ] Remove `POST /api/v1/reports` from `ReportController` if present
- [ ] Remove `DELETE /api/v1/reports/{id}` from `ReportController` if present
- [ ] Implement `ReportQueryService` backed by S3 (`s3_client.list_objects_v2` filtered by org/experiment)
- [ ] Implement presigned URL generation for report download endpoint
- [ ] Verify `ReportDto` fields match S3 object metadata structure

#### 2.3 Downstream Clients
- [ ] Verify each `*ServiceClient` correctly propagates `Authorization` header (JWT) to downstream
- [ ] Verify `organizationId` is propagated in all requests (from JWT claim → `X-Organization-Id` header or similar)
- [ ] Verify RestClient timeout and retry configuration
- [ ] Verify error mapping from downstream 4xx/5xx to appropriate BFF error responses

#### 2.4 Security
- [ ] Verify JWT validation rejects expired tokens correctly
- [ ] Verify protected routes require valid JWT (unit + integration tests)
- [ ] Verify CORS config matches frontend origin requirements per PROJECT.md
- [ ] Verify rate limiting is active on auth endpoints

#### 2.5 Tests
- [ ] All `*ServiceClientTest` classes have tests for success, 4xx, 5xx, and timeout cases
- [ ] Integration tests cover auth flow through BFF → identity-service
- [ ] Integration tests cover an orchestration call (e.g., dashboard overview that calls multiple services)

---

### Phase 3 — WS Gateway Completeness Review

The WS Gateway is a NestJS service that handles WebSocket connections and bridges to SQS/Redis.

#### 3.1 Implementation
- [ ] Verify `Dockerfile` exists for WS Gateway — required for ECS deployment
- [ ] Verify `node_modules/` is in `.gitignore` (currently committed, causing repo bloat)
- [ ] Verify `ChatGateway` handles: `join-conversation`, `send-message`, `leave-conversation` events
- [ ] Verify `TypingGateway` handles: `typing-start`, `typing-stop` events
- [ ] Verify `SqsPublisherService` sends messages to correct SQS queue (communications-service input queue)
- [ ] Verify `RedisPubSubService` subscribes to per-conversation channels and broadcasts to connected clients
- [ ] Verify JWT auth guard validates token on WebSocket connection (handshake)
- [ ] Verify `organizationId` extracted from JWT and enforced on conversation membership check

#### 3.2 Tests
- [ ] Unit tests for `ChatGateway`, `TypingGateway`, `WsAuthGuard`, `SqsPublisherService`, `RedisPubSubService`
- [ ] E2E tests for WebSocket connection with valid/invalid JWT
- [ ] E2E test for full message flow: connect → join-conversation → send-message → SQS delivery
- [ ] Integration test for Redis pub/sub broadcast to multiple connected clients

---

### Phase 4 — Lambda Services Completeness Review

Implementation was fixed (2026-04-09) — this phase focuses on the remaining Lambda-specific concerns.

#### 4.1 Reporting Service
- [ ] Verify `lambda_handler.py` module path is correct — handler: `lambda_handler.lambda_handler` (not `handler.main`)
- [ ] Verify `ExperimentCompletedHandler.__init__` creates all pipeline dependencies — they should not be module-level singletons (risks stale connections across warm invocations)
- [ ] Verify `idempotency.py` DynamoDB table name matches Terraform output (`IDEMPOTENCY_TABLE_NAME` env var)
- [ ] Verify `S3StorageService` bucket name matches Terraform output (`REPORTS_BUCKET_NAME` env var)
- [ ] Verify `EventPublisher` event bus name matches Terraform output (`EVENT_BUS_NAME` env var)
- [ ] Verify timeout (currently?) is sufficient for full pipeline (data fetch + PDF generation can be slow — recommend 300s)
- [ ] Verify memory allocation is sufficient for WeasyPrint PDF generation (recommend 1024MB+)
- [ ] Run and verify all unit tests pass: `pytest tests/ -v`

#### 4.2 Notification Service
- [ ] Verify `notification_handler.py` handler: `notification_handler.lambda_handler`
- [ ] Verify `config.py` includes `IDENTITY_SERVICE_URL` for `UserClient` (currently uses `EXPERIMENT_SERVICE_URL` as placeholder)
- [ ] Verify idempotency table name (`processed_notification_events`) matches Terraform
- [ ] Verify `WebhookService._get_webhook_configs()` TODO is addressed (currently returns empty list — webhook delivery is a no-op until config source is implemented)
- [ ] Verify `SES_FROM_EMAIL` env var is set in Lambda Terraform and matches SES verified identity
- [ ] Run and verify all unit tests pass: `pytest tests/ -v`

---

### Phase 5 — Terraform Infrastructure Review

#### 5.1 Lambda Module (`infrastructure/terraform/modules/lambda/`)

**5.1.1 — Remove generic `event-processor` Lambda and catch-all EventBridge rule**
- Remove `aws_lambda_function.event_processor` resource (no matching service)
- Remove `aws_cloudwatch_event_rule.event_processor` (catch-all — conflicts with specific rules in messaging module)
- Remove `aws_cloudwatch_event_target.event_processor`
- Remove `aws_lambda_permission.event_processor_eventbridge`
- Remove `aws_cloudwatch_log_group.event_processor`
- Remove all associated variables: `enable_event_processor`, `event_processor_*`

**5.1.2 — Fix Lambda function names and handlers**
- Rename `notification_processor` → `notification_service` (function name: `turaf-notification-service-{env}`)
- Rename `report_generator` → `reporting_service` (function name: `turaf-reporting-service-{env}`)
- Fix handler: `python3.11` runtime → `lambda_handler.lambda_handler` (reporting) and `notification_handler.lambda_handler` (notification)
- Add missing env vars to reporting Lambda: `IDEMPOTENCY_TABLE_NAME`, `EXPERIMENT_SERVICE_URL`, `METRICS_SERVICE_URL`
- Add missing env vars to notification Lambda: `ORGANIZATION_SERVICE_URL`, `FRONTEND_URL`, `SES_FROM_EMAIL`, `IDEMPOTENCY_TABLE_NAME`

**5.1.3 — Remove SQS event source mappings from Lambda module**
- Remove `aws_lambda_event_source_mapping.notification_processor` — notification is EventBridge-triggered (defined in messaging module)
- Remove `aws_lambda_event_source_mapping.report_generator` — reporting is EventBridge-triggered
- Remove associated SQS ARN variables: `notifications_queue_arn`, `reports_queue_arn`

**5.1.4 — Update Lambda function URLs**
- Remove `aws_lambda_function_url.report_generator` (reports are not invoked via HTTP — they're event-driven)

**5.1.5 — Update outputs and variables**
- Update `outputs.tf` to reflect renamed functions and removed resources
- Update `variables.tf` to remove orphaned variables and add correct ones

#### 5.2 Messaging Module (`infrastructure/terraform/modules/messaging/`)

**5.2.1 — Add MemberAdded EventBridge rule**
- Add rule for `source: turaf.organization-service`, `detail-type: MemberAdded`
- Add target: notification Lambda
- Add `aws_lambda_permission` for EventBridge → notification Lambda for MemberAdded
- Add output: `member_added_rule_arn`

**5.2.2 — Fix `input_transformer` on `experiment_completed_reporting` target**
- The current transformer restructures the event into a different shape than what `reporting-service` expects
- Remove the `input_transformer` block — pass the native EventBridge event unchanged; the handler already reads `event.get('detail', {})` directly

**5.2.3 — Update messaging module variables**
- Rename `reporting_lambda_arn`/`reporting_lambda_name` to match new function names
- Rename `notification_lambda_arn`/`notification_lambda_name` to match new function names

#### 5.3 Per-Service Terraform Directories

Each ECS microservice workflow references `./services/{service}/terraform/` for ECS task definition, service, target group, and ALB listener rule. These directories must exist.

Services requiring per-service Terraform:
- `services/identity-service/terraform/`
- `services/organization-service/terraform/`
- `services/experiment-service/terraform/`
- `services/metrics-service/terraform/`
- `services/communications-service/terraform/`
- `services/bff-api/terraform/`
- `services/ws-gateway/terraform/`

Each per-service Terraform should contain:
- `main.tf` — ECS task definition, ECS service, target group, ALB listener rule
- `variables.tf` — `environment`, `image_tag`, `service_name`, `aws_region`, `cluster_arn`, `vpc_id`, `private_subnet_ids`, `internal_alb_listener_arn` (for microservices), `public_alb_listener_arn` (for BFF/WS)
- `outputs.tf` — ECS service ARN, task definition ARN
- `backend.tf` — S3 backend config (parameterized via `-backend-config` in CI)

The compute module's `archived/service-specific-resources/` contains the templates to adapt.

#### 5.4 Security Module (`infrastructure/terraform/modules/security/`)
- [ ] Verify IAM execution role for Lambda has permissions for: EventBridge put-events, S3 (reports bucket), DynamoDB (idempotency table), SES send-email, CloudWatch logs
- [ ] Verify IAM deployment role for GitHub Actions has permissions for: ECS register-task-definition, ECS update-service, ECR push, Lambda update-function-code, S3 put-object (artifacts bucket)
- [ ] Verify security groups: Lambda → internal ALB (for HTTP clients to microservices), Lambda → DynamoDB (VPC endpoint or NAT)

#### 5.5 Storage Module (`infrastructure/terraform/modules/storage/`)
- [ ] Verify S3 bucket for Lambda artifacts (`turaf-lambda-artifacts-{env}`) is defined (currently the Lambda workflow creates it with `aws s3 mb` — should be Terraform-managed)
- [ ] Verify S3 bucket for reports (`turaf-reports-{env}`) has correct lifecycle policy and server-side encryption
- [ ] Verify DynamoDB tables for idempotency (one per Lambda: `reporting-idempotency-{env}`, `notification-idempotency-{env}`) are defined with TTL attribute enabled

---

### Phase 6 — CI/CD Workflow Review

#### 6.1 Lambda Deployment Workflows (`deploy-reporting-lambda.yml`, `deploy-notification-lambda.yml`)

**6.1.1 — Fix Lambda packaging structure**
- Entry point for reporting: `lambda_handler.lambda_handler` → zip must have `lambda_handler.py` at root (not `src/lambda_handler.py`)
- Entry point for notification: `notification_handler.lambda_handler` → zip must have `notification_handler.py` at root
- Fix packaging steps: install deps to package dir, copy Python files from `src/` to package root (not as subdirectory), zip from package root
- Remove the dead `if [ -f "handler.py" ]` check

**6.1.2 — Fix S3 URL typo**
- Line 119 and 213: `s3://${BUCKET_NAME}}` → `s3://${BUCKET_NAME}` (remove extra `}`)

**6.1.3 — Fix DEV trigger condition**
- `deploy-dev` currently only triggers on `develop` branch — should also trigger on `main` to match service workflows
- Fix: change condition from `github.ref == 'refs/heads/develop'` to include `main`

**6.1.4 — Add unit tests before packaging**
- Add step: `pip install -r requirements-dev.txt && pytest tests/ -v --ignore=tests/integration` before packaging
- Fail the workflow if unit tests fail

**6.1.5 — Run integration tests in QA (not just a placeholder)**
- Add step: `pytest tests/integration/ -v` with MiniStack/LocalStack configured via env vars pointing to deployed QA AWS resources (or skip with explicit skip marker if integration infra not available in CI)

#### 6.2 ECS Service Workflows (`service-*.yml`)

**6.2.1 — Add unit tests before Docker build**
- Add step before `docker build`: run Maven unit tests (`mvn test -pl services/{service} -Dspring.profiles.active=test`)
- Fail build if tests fail

**6.2.2 — Implement QA integration tests**
- Replace placeholder comments with actual integration test execution
- Run `mvn verify -pl services/{service} -Dgroups=IntegrationTest` against deployed QA service
- Or run Karate architecture tests scoped to the service

**6.2.3 — Implement PROD smoke tests**
- Replace placeholder comments with health check + basic API contract smoke test
- At minimum: `curl -f https://{alb}/api/{service}/health` and one authenticated endpoint call

**6.2.4 — Fix `deploy-dev` dependency on `security-scan`**
- `security-scan` only runs on `main` — on `develop`, `security-scan` is skipped and `deploy-dev` still runs (acceptable)
- Make this explicit: add `if: always() && !contains(needs.*.result, 'failure')` to `deploy-dev` needs condition (already present in identity workflow but verify consistently across all service workflows)

#### 6.3 Infrastructure Workflow (`infrastructure.yml`)

**6.3.1 — Restrict DEV auto-apply to develop/main branches only**
- Remove `feature/*` from push triggers — infrastructure should not auto-apply on feature branches
- Add `workflow_dispatch` as the primary trigger for feature branch testing

**6.3.2 — Separate bootstrap from environment apply**
- Bootstrap should only run once (or explicitly on demand) — not on every push
- Gate bootstrap on a condition that checks if the backend bucket already exists

**6.3.3 — Add plan review step for QA and PROD**
- For QA/PROD: publish the `terraform plan` output as a PR comment or step summary before applying
- Add `environment` protection rules to require manual approval before QA/PROD apply (GitHub Environments feature)

#### 6.4 CI Workflow (`ci.yml`)
- [ ] Verify `ci.yml` runs all service unit tests on every PR
- [ ] Verify `ci.yml` runs Python unit tests for Lambda services
- [ ] Verify `ci.yml` runs WS Gateway tests (`npm test`)
- [ ] Verify `ci.yml` is a required check for PR merge

#### 6.5 Architecture Tests Workflow (`architecture-tests.yml`)
- [ ] Verify architecture tests run against a deployed environment (not local)
- [ ] Verify the Karate runner targets correct base URLs per environment
- [ ] Verify architecture tests are gated after QA deployment before PROD promotion

---

### Phase 7 — Architecture Tests Review

Karate tests in `services/architecture-tests/` validate the running stack end-to-end.

#### 7.1 Test Scenarios
- [ ] Authentication flow: register → login → refresh → logout
- [ ] Organization management: create org → add member → list members → remove member
- [ ] Experiment lifecycle: create problem → create hypothesis → create experiment → start → complete
- [ ] Report generation (event-driven): complete experiment → wait for S3 object → GET report via BFF
- [ ] Communications: create conversation → send message → check unread count → mark read
- [ ] WS Gateway: connect with JWT → join conversation → receive message (if WS testable in Karate)

#### 7.2 Event-Driven Test Patterns
- [ ] Verify `WaitHelper` has `waitForS3Object()` for polling report availability
- [ ] Verify `AwsHelper` can check S3 and EventBridge (for confirming Lambda invocation)
- [ ] Remove any remaining `POST /api/v1/reports` scenarios — replaced by event-driven flow

#### 7.3 Test Configuration
- [ ] Verify `KarateTestRunner` connects to correct environment URLs via config
- [ ] Verify test setup/teardown creates and cleans up test organizations and users
- [ ] Verify tests are idempotent (can be re-run without manual cleanup)

---

## Implementation Order

```
Phase 1 (Microservices)  ─┐
Phase 2 (BFF)             ├─► Phase 5 (Terraform) ─┐
Phase 3 (WS Gateway)      │                         ├─► Phase 6 (CI/CD) ─► Phase 7 (Arch Tests)
Phase 4 (Lambda Services) ─┘                         │
                                                      │
                          (Phase 5.3 per-service TF) ─┘
```

- Phases 1–4 can be worked in parallel (independent services)
- Phase 5 (Infrastructure) should incorporate findings from Phases 1–4 (correct names, env vars, etc.)
- Phase 6 (CI/CD) depends on Phase 5 Terraform being correct (correct function names, per-service TF dirs)
- Phase 7 (Architecture Tests) is the final validation gate; runs against the corrected stack

---

## Files to Create / Modify

### New Files

**Per-service Terraform (Phase 5.3)**
- `services/identity-service/terraform/main.tf` — ECS task def + service + target group + listener rule
- `services/identity-service/terraform/variables.tf`
- `services/identity-service/terraform/outputs.tf`
- `services/identity-service/terraform/backend.tf`
- *(repeat pattern for organization, experiment, metrics, communications, bff-api, ws-gateway)*

**Task files (created per-phase as gaps are found)**
- `.windsurf/tasks/infrastructure/001-fix-lambda-module.md`
- `.windsurf/tasks/infrastructure/002-add-member-added-event-rule.md`
- `.windsurf/tasks/infrastructure/003-per-service-terraform.md`
- `.windsurf/tasks/cicd/001-fix-lambda-packaging.md`
- `.windsurf/tasks/cicd/002-add-unit-tests-to-workflows.md`
- `.windsurf/tasks/cicd/003-fix-infrastructure-workflow.md`

### Modified Files

**Terraform Infrastructure**
- `infrastructure/terraform/modules/lambda/main.tf` — remove event-processor, rename functions, fix handlers, remove SQS mappings, remove function URL
- `infrastructure/terraform/modules/lambda/variables.tf` — remove orphaned vars, add correct ones
- `infrastructure/terraform/modules/lambda/outputs.tf` — update to reflect renamed resources
- `infrastructure/terraform/modules/messaging/eventbridge-rules.tf` — add MemberAdded rule, remove input_transformer from reporting target
- `infrastructure/terraform/modules/messaging/variables.tf` — rename lambda ARN/name variables

**CI/CD**
- `.github/workflows/deploy-reporting-lambda.yml` — fix packaging, fix S3 typo, add unit tests, fix trigger
- `.github/workflows/deploy-notification-lambda.yml` — fix packaging, fix S3 typo, add unit tests, fix trigger
- `.github/workflows/service-*.yml` — add unit test steps, implement QA/PROD test placeholders
- `.github/workflows/infrastructure.yml` — restrict feature branch triggers, separate bootstrap

**BFF API**
- `services/bff-api/src/main/java/com/turaf/bff/controllers/ReportController.java` — remove write operations, implement S3 query
- `services/bff-api/src/main/java/com/turaf/bff/services/ReportQueryService.java` — new: S3 list + presigned URL

**WS Gateway**
- `services/ws-gateway/.gitignore` — ensure `node_modules/` is excluded

---

## Consistency Checklist

- [ ] Changes align with PROJECT.md architecture (event-driven, DDD, multi-tenant)
- [ ] Lambda handlers use `detail.payload.*` extraction consistently
- [ ] EventBridge event sources match `turaf.{service-name}` convention
- [ ] All Terraform resource names use `turaf-{component}-{env}` naming convention
- [ ] IAM roles follow least-privilege principle per service
- [ ] CI/CD workflows run tests before deploying to any environment
- [ ] Architecture tests validate the full event-driven chain end-to-end
- [ ] Plan moved to `completed/` when all phases are done
