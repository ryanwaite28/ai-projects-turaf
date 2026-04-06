# Project Alignment Plan: Full Stack Reconciliation

Comprehensive plan to identify and correct all discrepancies across documentation, specs, tasks, and implementation to achieve complete project alignment with PROJECT.md as the single source of truth.

**Created**: 2026-04-01
**Updated**: 2026-04-03 (infrastructure routing review: 36 discrepancies cataloged)
**Status**: Active
**Discrepancies Found**: 36 (8 critical, 16 medium, 12 low)
**Related Docs**: PROJECT.md, BEST_PRACTICES.md, .windsurf/rules/rules.md, DOCUMENTATION_INDEX.md
**Related Active Plans**: `frontend-bff-alignment-42182b.md`, `api-integration-review-75dcab.md`, `workflow-consolidation-c6c363.md`, `core-services-evaluation-plan.md`

---

## Discrepancy Inventory

### D1. User Model Field Mismatch (Critical)

The user entity definition is inconsistent across every layer:

| Layer | Fields | Location |
|-------|--------|----------|
| **DB migration V002** | `id (VARCHAR)`, `email`, `password`, `name` | `flyway-service/migrations/V002__identity_create_users_table.sql` |
| **Identity Service domain** | `email`, `password`, `name`, `organizationId` | `identity-service/.../domain/User.java` |
| **Identity Service RegisterRequest** | `email`, `password`, `name`, `organizationId` | `identity-service/.../dto/RegisterRequest.java` |
| **Identity Service spec** | `email`, `password_hash`, `name` (UUID id) | `specs/identity-service.md` |
| **BFF RegisterRequest** | `name`, `email`, `password` (no orgId) | `bff-api/.../dto/RegisterRequest.java` |
| **Frontend model (user.model.ts)** | `firstName`, `lastName`, `email` | `frontend/src/app/models/user.model.ts` |
| **Frontend RegisterComponent** | `firstName`, `lastName`, `email`, `organizationName` | `frontend/.../register/register.component.ts` |
| **Frontend core auth service** | `name`, `email`, `password` (inline interface) | `frontend/src/app/core/auth/services/auth.service.ts` |
| **Infra plan test curl** | `firstName`, `lastName` | `.windsurf/plans/completed/infrastructure-restructure/...` |

**Decision**: Rename `name` → `username`, add `first_name` + `last_name`. All layers updated.

### D2. Duplicate Auth Services in Frontend

Two competing `AuthService` implementations:
- `frontend/src/app/features/auth/services/auth.service.ts` — uses models from `user.model.ts` (firstName/lastName), NgRx-compatible
- `frontend/src/app/core/auth/services/auth.service.ts` — inline interfaces (name), BehaviorSubject-based, different return types

These have conflicting `User` interfaces and `RegisterRequest` definitions.

### D3. PROJECT.md Internal Inconsistencies

- **Section 9 & 26**: Say "React" / "React based web application" — should be **Angular**
- **Section numbering**: Two "Section 4" headings (Project Objectives + Product Concept)
- **Repository structure** (section 16): References `libs/` directory which doesn't exist
- **Architecture diagrams** (section 44): Show `APIGateway` node but architecture uses Public ALB → BFF pattern
- **Service list gap**: Section 11 lists `Communications Service` + `WebSocket Gateway` but they appear nowhere in sections 40's service specs

### D4. Missing API Endpoints in Identity Service

| Endpoint | Frontend calls it? | BFF forwards it? | Identity Service implements it? |
|----------|-------------------|-------------------|-------------------------------|
| `GET /auth/me` | ✅ | ✅ (as `/identity/users/me`) | ❌ Missing |
| `PUT /auth/password` | ❌ | ❌ | ❌ (spec says yes) |
| `POST /password-reset/request` | ✅ | ✅ | ❌ Missing |
| `POST /password-reset/confirm` | ✅ | ✅ | ❌ Missing |

### D5. BFF ↔ Identity Service Path Mismatch

- BFF `IdentityServiceClient` calls: `/identity/auth/login`, `/identity/auth/register`, `/identity/users/me`
- Identity Service `AuthController` maps: `/api/v1/auth/*`
- Internal ALB routes: `/identity/*` → Identity Service
- **Result**: BFF sends `/identity/auth/login` but Identity Service expects `/api/v1/auth/login` — these won't connect through internal ALB without path rewriting or alignment

### D6. BFF Missing Service Clients

`ServiceUrlsConfig` + `WebClientConfig` only configure 4 service clients:
- ✅ identity, organization, experiment, metrics

Missing clients for services defined in PROJECT.md:
- ❌ communications-service
- ❌ notification-service
- ❌ reporting-service
- ❌ ws-gateway

### D7. DB Migration vs Spec/Entity Discrepancies

| Issue | Migration | Spec / Entity |
|-------|-----------|---------------|
| ID type | `VARCHAR(36)` | `UUID` in spec |
| Password column | `password` | `password_hash` in spec |
| `user_roles` table | Not in any migration | Defined in spec |
| `organization_id` | Added in V011 | Part of domain entity constructor |

### D8. Frontend Architecture vs Rules

Rules (`.windsurf/rules/rules.md` §5.2) mandate:
- Standalone components (no NgModules)
- Signals for state management
- TailwindCSS + Lucide + shadcn/ui

Actual `RegisterComponent`:
- Uses class-based `@Component` with `templateUrl`/`styleUrls` (NgModule pattern)
- Uses NgRx store (not Signals)
- Uses SCSS (not TailwindCSS)

### D9. Hardcoded Values in Identity Service

- `AuthController.register()`: `tokenService.generateTokens(userId, "default-org")` — hardcoded org ID
- Tests use 3-arg `RegisterRequest` constructor but class has 4 args

### D10. Documentation Redundancy

Multiple overlapping docs with no clear ownership:
- `docs/FINAL-IMPLEMENTATION-SUMMARY.md` vs `docs/IMPLEMENTATION_SUMMARY.md` vs `docs/IMPLEMENTATION-TRACKER.md`
- `docs/RULES_SUMMARY.md` vs `.windsurf/rules/rules.md`
- `docs/DEPLOYMENT_RUNBOOK.md` vs referenced (but missing?) `docs/operations/RUNBOOKS.md`
- `infrastructure/` root has many loose `.md` files that belong in `infrastructure/docs/` or `docs/`
- `docs/DATABASE_SETUP.md` vs `docs/DATABASE_ARCHITECTURE_IMPLEMENTATION.md`
- Multiple audit/assessment files in `docs/audits/`

### D11. Missing `libs/` Directory

PROJECT.md references `libs/` for shared domain models and event schemas. This directory doesn't exist. The `common` module lives under `services/common/` instead.

### D12. Identity Service spec CORS URLs Wrong

Spec says:
- QA: `https://qa.turaf.com` (should be `https://app.qa.turafapp.com`)
- PROD: `https://app.turaf.com` (should be `https://app.turafapp.com`)

### D13. Infrastructure/Terraform Alignment

- `infrastructure/terraform/` exists (72 items) but needs verification against PROJECT.md service list
- Service-specific Terraform referenced in specs but locations need verification
- GitHub Actions workflows exist for all services but need content verification against actual deployment patterns

### D14. Problem Entity — Spec vs Implementation (Medium)

| Aspect | Spec (`specs/experiment-service.md`) | Implementation |
|--------|--------------------------------------|----------------|
| `CreateProblemRequest` fields | `title`, `description`, `affectedUsers`, `context` | `title`, `description` only |
| `Problem` domain entity | Has `affectedUsers`, `context` | Has `title`, `description` only |
| `ProblemCreated` event payload (spec `event-schemas.md`) | Includes `affectedUsers`, `context` | `ProblemCreated.java` has `title`, `description`, `createdBy` only |
| `ProblemDto` response | Includes `affectedUsers`, `context` | Has `title`, `description` only |
| DB migration V006 | N/A | `problems` table has `title`, `description` only — no `affected_users` or `context` columns |

**Decision needed**: Add `affectedUsers` + `context` to implementation, or remove from specs.

### D15. Metrics Service — Spec vs Implementation (Medium)

| Aspect | Spec (`specs/metrics-service.md`) | Implementation |
|--------|-----------------------------------|----------------|
| Metric fields | `name`, `value`, `unit`, `metadata` (JSON) | `name`, `value`, `type` (COUNTER/GAUGE/HISTOGRAM), `tags` (JSONB) |
| Timestamp field | `recordedAt` | `timestamp` |
| Request DTO | `RecordMetricRequest` with `unit`, `metadata` | Likely `RecordMetricRequest` with `type`, `tags` |

Spec and implementation use different field names and semantics. Need to reconcile.

### D16. Communications Service — Spec vs Implementation (Medium)

| Aspect | Spec (`specs/communications-service.md`) | Implementation |
|--------|------------------------------------------|----------------|
| `Message` entity | `@ManyToOne` JPA relationship to `Conversation` | Uses `String conversationId` (no JPA relationship) |
| `Conversation.messages` | `@OneToMany` list of messages | No `messages` field in `Conversation.java` |
| Architecture pattern | JPA entities directly annotated (shown in spec) | Actual entity also uses JPA annotations directly (consistent) |
| Style | Spec doesn't mention Lombok/MapStruct | Implementation uses Lombok + MapStruct (differs from other services using manual fromDomain/toDomain) |

### D17. Frontend Duplicate Guards (Critical)

Two competing auth guards:
- **`core/auth/guards/auth.guard.ts`** — functional `CanActivateFn`, uses `AuthService.isAuthenticated()` directly, redirects to `/auth/login`
- **`core/guards/auth.guard.ts`** — class-based `CanActivate`, uses NgRx `selectIsAuthenticated` from store, redirects to `/login`

`app.routes.ts` imports the **functional** guard from `core/auth/guards/`. The class-based one in `core/guards/` is unused by routing but still present and references `store/app.state` and `store/auth/auth.selectors`.

### D18. Frontend Duplicate Interceptors (Critical)

Two sets of interceptors:
- **`core/auth/interceptors/auth.interceptor.ts`** + `error.interceptor.ts` — functional (`HttpInterceptorFn`), used by `app.config.ts` via `withInterceptors()`
- **`core/interceptors/auth.interceptor.ts`** + `error.interceptor.ts` — class-based (`HttpInterceptor`), registered in `core.module.ts` via `HTTP_INTERCEPTORS` token

`app.config.ts` uses the functional ones. `CoreModule` registers the class-based ones. Both could be active if `CoreModule` is ever imported. The class-based ones reference `store/app.state`.

### D19. Frontend Missing Routes (Medium)

`app.routes.ts` defines routes for: auth, dashboard, problems, hypotheses, experiments, metrics, reports.

**Missing routes for implemented services**:
- ❌ No `organizations` route (organization-service exists with full CRUD)
- ❌ No `communications` or `messaging` route (communications-service + ws-gateway exist)

### D20. Frontend NgRx Store References Without Store Setup (Medium)

`app.config.ts` calls `provideStore()` and `provideEffects()` with **no arguments** — empty store. Yet 25+ component/guard/interceptor files import from `../../store/app.state`, `../../store/auth/auth.selectors`, etc. These store modules likely don't exist or are empty, causing runtime errors.

### D21. Docker Compose Issues (Low)

- **Unused volume**: `ministack_data` declared in `volumes:` section but not mounted by any service
- **BFF missing service URLs**: `bff-api` env vars don't include `IDENTITY_SERVICE_URL`, `ORGANIZATION_SERVICE_URL`, etc. — the BFF `ServiceUrlsConfig` needs these to route to downstream services
- **No frontend service**: No Angular dev server in docker-compose (expected for local dev?)
- **Communications health check path**: Uses `/communications/actuator/health` — non-standard prefix compared to other services using `/actuator/health`

### D22. GitHub Workflows — Missing Active Workflows (Medium)

- ❌ No active `service-ws-gateway.yml` — only archived per-env workflows exist in `archive/`
- ❌ No frontend deployment workflow (but `specs/frontend-deployment.md` spec exists)
- Archived per-env workflows (27 files) still in repo in `archive/` — should be cleaned up after consolidation
- Existing plan `workflow-consolidation-c6c363.md` addresses this partially but doesn't mention ws-gateway gap

### D23. V012 Migration Column Width Inconsistency (Low)

V012 adds `organization_id` to `conversations` as `VARCHAR(255)`. All other UUID-based ID columns use `VARCHAR(36)`. Should be `VARCHAR(36)` for consistency.

### D24. Tasks Folder Staleness (Medium)

- `TASK_SUMMARY.md` says 14 of 95 tasks completed, 81 remaining — but `COMPLETION_SUMMARY.md` says all 95 tasks "successfully generated". These contradict each other.
- `TASK_SUMMARY.md` still lists org/experiment/metrics/etc. services as "(0/N)" remaining, but code for all these services exists and is implemented
- Task completion tracking doesn't reflect actual implementation state
- No tasks exist for: V012 migration, ws-gateway deployment workflow, frontend-BFF alignment fixes

### D25. Active Plans Overlap (Low)

Multiple active plans cover overlapping scopes:
- `api-integration-review-75dcab.md` — API contracts across Frontend → BFF → Microservices
- `frontend-bff-alignment-42182b.md` — Frontend ↔ BFF endpoint alignment (detailed, 8 phases)
- `core-services-evaluation-plan.md` — Core services DDD/Clean Architecture evaluation
- `workflow-consolidation-c6c363.md` — GitHub Actions consolidation
- `common-module-reusability-cleanup-89feb0.md` — Common module cleanup
- `ministack-aws-services-audit-cb30c2.md` — MiniStack audit
- `ministack-lambda-deployment-5cd87b.md` — Lambda local deployment

These should be cross-referenced or consolidated to avoid duplicate work.

### D26. Communications Service Uses Different Architecture Pattern (Low)

Other Java services (identity, organization, experiment, metrics) follow Clean Architecture with:
- Domain entity → JPA entity (separate classes, `fromDomain`/`toDomain` mappers)
- Domain repository interface → JPA repository implementation
- Manual DTO mapping

Communications service uses:
- JPA entity annotations directly on domain models (`@Entity`, `@Table`)
- MapStruct for mapping
- Lombok for boilerplate

This inconsistency should be documented or reconciled.

### D27. Event Schema Spec vs Implementation (Medium)

The event envelope spec (`specs/event-schemas.md`) defines a standard envelope with `eventId`, `eventType`, `eventVersion`, `timestamp`, `sourceService`, `organizationId`, `payload`.

Actual event implementations:
- `ProblemCreated.java` — has `eventId`, `problemId`, `organizationId`, `title`, `description`, `createdBy`, `timestamp`, `correlationId` — flat structure, no `payload` wrapper, no `eventVersion`, no `sourceService`
- Notification/Reporting Lambda handlers expect EventBridge native `detail-type` and `detail` fields — different structure from the spec envelope

The `EventEnvelope.java` in common module may handle wrapping, but the spec and actual event classes don't match.

### D28. Specs Redundancy (Low)

- `specs/communications-domain-model.md` + `specs/communications-service.md` — overlapping communications specs
- `specs/communications-event-schemas.md` + `specs/event-schemas.md` — overlapping event schema docs
- `specs/event-flow.md` + `specs/event-schemas.md` — overlapping event documentation

### D29. No Internal/Private ALB (Critical)

PROJECT.md describes a **dual-ALB architecture**: public ALB → BFF, private/internal ALB → microservices. Terraform only creates **one public ALB** (`internal = false`) in `modules/compute/main.tf`. There is no internal ALB for BFF → microservice communication.

Without an internal ALB, the BFF has no way to reach backend microservices in AWS (Docker Compose uses Docker DNS, but that doesn't exist in ECS/Fargate).

### D30. No ALB Listener Rules or Target Groups (Critical)

The public ALB has HTTP (port 80) and HTTPS (port 443) listeners with **default 404 fixed responses**. There are:
- ❌ No path-based listener rules (e.g., `/api/*` → BFF target group)
- ❌ No target groups for any service
- ❌ No health check configurations for target groups

The `compute/main.tf` comment says "Service-specific resources (ECS services, task definitions, target groups, listener rules) are managed per-service via CI/CD pipelines" — but the CI/CD workflows need verification to confirm they actually create these resources.

### D31. No Service Discovery for BFF → Microservices (Critical)

The BFF's `ServiceUrlsConfig` expects URLs like `http://identity-service:8081` to reach microservices. In AWS, this requires either:
- AWS Cloud Map (service discovery) — **not configured** in any Terraform module
- Internal ALB with path-based routing — **not present** (D29)
- ECS Service Connect — **not configured**

No service discovery mechanism exists in Terraform. `grep` for `service_discovery`, `cloud_map`, `servicediscovery` returns zero results.

### D32. Lambda Trigger Architecture Mismatch (Medium)

| Service | Spec Says | Terraform Implements |
|---------|-----------|---------------------|
| Notification Lambda | EventBridge trigger (`ExperimentCompleted` event) | **SQS** event source mapping (`notifications_queue_arn`) |
| Reporting Lambda | EventBridge trigger (`ExperimentCompleted` event) | **SQS** event source mapping (`reports_queue_arn`) |
| Event Processor Lambda | EventBridge trigger (all `turaf.*` events) | EventBridge rule (✅ correct) |

Notification and Reporting specs describe direct EventBridge triggers, but Terraform uses SQS as an intermediary. This is a valid pattern (EventBridge → SQS → Lambda) but the EventBridge rules to forward events to these SQS queues are **missing**.

### D33. No Frontend Hosting Infrastructure (Medium)

No Terraform resources for Angular SPA hosting:
- ❌ No CloudFront distribution (despite S3 bucket policy referencing `cloudfront.amazonaws.com`)
- ❌ No S3 website hosting configuration
- ❌ No Route 53 DNS records for frontend domain
- ❌ No ACM certificate for frontend domain

`specs/frontend-deployment.md` exists but its hosting infrastructure is not implemented in Terraform.

### D34. ECS Security Group Single Port (Medium)

Security group `ecs_tasks` in `modules/security/main.tf` only allows port **8080** from ALB and VPC. But services run on different ports per docker-compose:
- Identity: 8081
- Organization: 8082
- Experiment: 8083
- Metrics: 8084
- Communications: 8085
- BFF: 8080
- WS Gateway: 3001

Only BFF (8080) would be reachable. Either all services need to run on 8080, or the SG needs per-service port rules, or services should use dynamic port mapping with ALB.

### D35. No EventBridge Event Bus Resource (Medium)

Lambda module references `var.event_bus_name` and services publish to EventBridge, but **no `aws_cloudwatch_event_bus` resource** is defined in any Terraform module. The default bus exists, but if the architecture uses a custom bus (e.g., `turaf-events-{env}`), it needs to be created.

Also: no EventBridge rules exist to forward domain events to the SQS queues that notification/reporting Lambdas consume from.

### D36. SQS Chat Queue Not FIFO (Medium)

WS Gateway's `SqsPublisherService` uses `MessageGroupId` and `MessageDeduplicationId` — these parameters **require FIFO queues**. But Terraform creates `turaf-chat-messages-{env}` as a **standard queue** (no `.fifo` suffix, no `fifo_queue = true`). Messages sent with these parameters will fail.

---

## Implementation Plan

### Phase 1: Source of Truth Alignment (PROJECT.md + Specs)

**Goal**: Fix PROJECT.md, then cascade corrections to specs.

**1.1 Fix PROJECT.md**
- [ ] Replace "React" with "Angular" in sections 9, 26, and anywhere else
- [ ] Fix duplicate section 4 numbering
- [ ] Update User entity in domain model (section 6) to include `username`, `firstName`, `lastName`
- [ ] Update Identity Service API spec (section 40) — register request/response with new fields
- [ ] Remove `libs/` reference or document it as `services/common/`
- [ ] Update architecture diagrams (section 44) — replace `APIGateway` with `PublicALB → BFF`
- [ ] Add Communications Service + WebSocket Gateway to section 40 service specs
- [ ] Create changelog entry

**1.2 Update Identity Service Spec**
- [ ] Update `specs/identity-service.md` register request/response: `username`, `firstName`, `lastName`
- [ ] Fix DB schema definition: `id UUID`, `password_hash`, add `username`, `first_name`, `last_name`
- [ ] Fix CORS URLs to use `turafapp.com`
- [ ] Document missing endpoints: `/me`, `/password-reset/*`
- [ ] Add status metadata header

**1.3 Update Related Specs**
- [ ] `specs/bff-api.md` — update RegisterRequest, add missing service clients
- [ ] `specs/angular-frontend.md` — update User model, registration form, note architecture (standalone vs NgModule)
- [ ] `specs/domain-model.md` — update User entity fields
- [ ] `specs/communications-service.md` — verify alignment
- [ ] `specs/ws-gateway.md` — verify alignment

**Tests**: N/A (documentation only)

---

### Phase 2: Database & Identity Service Implementation

**Goal**: Align database schema and Identity Service implementation with corrected specs.

**2.1 New Flyway Migration**
- [ ] Create `V013__identity_update_users_table.sql`:
  - Rename `name` → `username`
  - Add `first_name VARCHAR(100) NOT NULL`
  - Add `last_name VARCHAR(100) NOT NULL`
  - Migrate existing data: split `name` or default values
- [ ] Create `V014__identity_create_user_roles_table.sql` (if user_roles are needed now)

**2.2 Identity Service Domain Updates**
- [ ] Update `User.java` domain entity: add `username`, `firstName`, `lastName`; remove single `name`
- [ ] Update `UserId`, value objects if needed
- [ ] Update `RegisterRequest.java`: add `username`, `firstName`, `lastName`; remove `name`, remove `organizationId` (or keep as optional)
- [ ] Update `UserDto.java` and other DTOs
- [ ] Update `AuthenticationService.register()` method
- [ ] Remove hardcoded `"default-org"` in `AuthController`

**2.3 Add Missing Identity Endpoints**
- [ ] Implement `GET /api/v1/auth/me` (or `/api/v1/users/me`)
- [ ] Implement `POST /api/v1/auth/password-reset/request`
- [ ] Implement `POST /api/v1/auth/password-reset/confirm`
- [ ] Implement `PUT /api/v1/auth/password`

**2.4 Fix Path Convention**
- [ ] Decide: Does Identity Service use `/api/v1/auth/*` or `/auth/*`?
- [ ] Align BFF client paths with Identity Service actual routes and internal ALB routing

**2.5 Update Tests**
- [ ] Update `AuthenticationServiceTest.java` — fix constructor args, test new fields
- [ ] Update/add integration tests for new endpoints
- [ ] Ensure all existing tests pass with new schema

---

### Phase 3: BFF API Alignment

**Goal**: BFF correctly proxies to all microservices with correct DTOs and paths.

**3.1 Update BFF DTOs**
- [ ] Update `RegisterRequest.java`: add `username`, `firstName`, `lastName`
- [ ] Update `UserDto.java` and `LoginResponseDto.java`
- [ ] Verify all other DTOs match downstream services

**3.2 Fix Service Client Paths**
- [ ] `IdentityServiceClient` — fix URI paths to match Identity Service actual routes
- [ ] Verify organization, experiment, metrics client paths

**3.3 Add Missing Service Clients**
- [ ] Add `CommunicationsServiceClient`
- [ ] Add `NotificationServiceClient` (if BFF needs direct access)
- [ ] Add `ReportingServiceClient` (if BFF needs direct access)
- [ ] Update `ServiceUrlsConfig` and `WebClientConfig` with new service beans

**3.4 Update Tests**
- [ ] Update `AuthControllerTest.java` — fix DTOs, add tests for new endpoints
- [ ] Add tests for new service clients

---

### Phase 4: Frontend Alignment

**Goal**: Single auth service, consistent models, aligned with backend. Eliminate all duplicate code.

**4.1 Consolidate Duplicate Auth Services (D2)**
- [ ] Remove `frontend/src/app/core/auth/services/auth.service.ts` (or merge)
- [ ] Keep `frontend/src/app/features/auth/services/auth.service.ts` as canonical
- [ ] Update all imports across the app

**4.2 Consolidate Duplicate Guards (D17)**
- [ ] Remove `frontend/src/app/core/guards/auth.guard.ts` (class-based, unused by routing)
- [ ] Keep `frontend/src/app/core/auth/guards/auth.guard.ts` (functional, used by `app.routes.ts`)
- [ ] Remove `frontend/src/app/core/guards/role.guard.ts` if unused
- [ ] Update all imports

**4.3 Consolidate Duplicate Interceptors (D18)**
- [ ] Remove `frontend/src/app/core/interceptors/auth.interceptor.ts` (class-based, registered in `CoreModule`)
- [ ] Remove `frontend/src/app/core/interceptors/error.interceptor.ts` (class-based)
- [ ] Keep functional interceptors in `frontend/src/app/core/auth/interceptors/` (used by `app.config.ts`)
- [ ] Remove or update `core.module.ts` to stop registering deleted interceptors
- [ ] Remove associated `.spec.ts` files for deleted interceptors

**4.4 Fix NgRx Store Setup (D20)**
- [ ] Either: create proper store modules (`store/app.state.ts`, `store/auth/auth.selectors.ts`, etc.) that components reference
- [ ] Or: remove NgRx store references from components and use service-based state (aligns with functional guard pattern)
- [ ] Ensure `provideStore()` and `provideEffects()` in `app.config.ts` are configured correctly

**4.5 Add Missing Routes (D19)**
- [ ] Add `organizations` route to `app.routes.ts` (organization management)
- [ ] Add `communications` or `messaging` route (conversations/messaging UI)
- [ ] Create placeholder modules/components if features don't exist yet

**4.6 Update User Model**
- [ ] Update `user.model.ts`: add `username` field, keep `firstName`/`lastName`, align `LoginResponse` with backend shape
- [ ] Remove any inline interface definitions that conflict

**4.7 Update Registration Component**
- [ ] Add `username` field to register form
- [ ] Ensure form fields map correctly to `RegisterRequest` (username, firstName, lastName, email, password)
- [ ] Remove `organizationName` from form (or align with backend if org creation at register is supported)

**4.8 Apply Frontend-BFF Alignment Fixes**
- [ ] Implement fixes from `frontend-bff-alignment-42182b.md` Phases 1-8 (auth, experiments, problems, hypotheses, metrics, dashboard, reports, docs)
- [ ] This addresses all endpoint mismatches, unsupported params, wrong return types

**4.9 Evaluate Architecture Modernization** (may defer)
- [ ] Assess effort to migrate to standalone components
- [ ] Assess effort to migrate from NgRx to Signals
- [ ] Assess effort to adopt TailwindCSS
- [ ] Document decision: modernize now or defer (create ADR if deferred)

**4.10 Update Tests**
- [ ] Update `auth.service.spec.ts` — fix model fields
- [ ] Update `register.component.spec.ts` — fix form fields and expectations
- [ ] Remove tests for deleted duplicate files
- [ ] Verify all frontend tests pass

---

### Phase 5: Cross-Service Implementation Verification

**Goal**: Verify every microservice correctly references its DB schema columns, API contracts, and event schemas.

**5.1 Experiment Service — Problem Entity Fix (D14)**
- [ ] Decision: add `affectedUsers` + `context` to Problem entity or remove from specs
- [ ] If adding: create migration V015, update `Problem.java`, `ProblemJpaEntity.java`, `CreateProblemRequest.java`, `ProblemDto.java`, `ProblemCreated.java`
- [ ] If removing: update `specs/experiment-service.md` and `specs/event-schemas.md`
- [ ] Verify remaining experiment entity alignments (V006-V008)
- [ ] Verify BFF client paths and DTOs

**5.2 Metrics Service — Field Reconciliation (D15)**
- [ ] Decision: adopt spec fields (`unit`, `metadata`, `recordedAt`) or implementation fields (`type`, `tags`, `timestamp`)
- [ ] Update losing side (spec or code) to match
- [ ] Verify V009 migration matches final entity
- [ ] Verify BFF client DTOs match

**5.3 Communications Service (D16, D23, D26)**
- [ ] Fix V012 `organization_id` column type: `VARCHAR(255)` → `VARCHAR(36)` (new migration V016)
- [ ] Decide: reconcile Message entity (JPA relationship vs String FK) — update spec or code
- [ ] Decide: align architecture pattern with other services (Clean Architecture) or document exception
- [ ] Verify V010 + V012 fully match entity definitions

**5.4 Organization Service**
- [ ] Verify Flyway migrations (V004, V005) match entity definitions
- [ ] Verify API endpoints match `specs/organization-service.md`
- [ ] Verify BFF client paths and DTOs

**5.5 Event Schema Alignment (D27)**
- [ ] Reconcile event envelope spec with actual event implementations
- [ ] Verify `EventEnvelope.java` wrapping behavior matches spec
- [ ] Ensure Lambda consumers (notification, reporting) can parse the envelope correctly
- [ ] Update `specs/event-schemas.md` ProblemCreated payload to match actual fields

**5.6 Notification Service, Reporting Service, WS Gateway**
- [ ] Verify each service's implementation against its spec
- [ ] Verify event consumption patterns
- [ ] Verify tests are present and passing

---

### Phase 6: Documentation Cleanup & Consolidation

**Goal**: Eliminate redundant docs, consolidate, add cross-references.

**6.1 Remove/Consolidate Redundant Docs (D10)**
- [ ] Merge `docs/FINAL-IMPLEMENTATION-SUMMARY.md` + `docs/IMPLEMENTATION_SUMMARY.md` + `docs/IMPLEMENTATION-TRACKER.md` → single status doc or archive old ones
- [ ] Merge `docs/RULES_SUMMARY.md` into `.windsurf/rules/rules.md` or delete if redundant
- [ ] Consolidate `docs/DATABASE_SETUP.md` + `docs/DATABASE_ARCHITECTURE_IMPLEMENTATION.md`
- [ ] Move loose `.md` files from `infrastructure/` root to `infrastructure/docs/` or `docs/`
- [ ] Verify `docs/operations/RUNBOOKS.md` exists (referenced but may be missing)
- [ ] Archive completed audit files in `docs/audits/`
- [ ] Archive `docs/pre-reset-inventory.md` (stale DEV environment snapshot from March 2026)
- [ ] Archive `docs/PRODUCTION-DEPLOYMENT-COMPLETE.md` (premature "100% COMPLETE" status)

**6.2 Consolidate Redundant Specs (D28)**
- [ ] Merge `specs/communications-domain-model.md` into `specs/communications-service.md`
- [ ] Merge `specs/communications-event-schemas.md` into `specs/event-schemas.md`
- [ ] Review `specs/event-flow.md` overlap with `specs/event-schemas.md` — merge or cross-reference

**6.3 Fix Tasks Folder Staleness (D24)**
- [ ] Reconcile `tasks/TASK_SUMMARY.md` vs `tasks/COMPLETION_SUMMARY.md` contradiction
- [ ] Update task completion status to reflect actual implementation state
- [ ] Add missing tasks for: V012 migration, ws-gateway workflow, frontend-BFF alignment

**6.4 Consolidate/Cross-Reference Active Plans (D25)**
- [ ] Cross-reference this plan with `frontend-bff-alignment-42182b.md` (Phase 4 overlap)
- [ ] Cross-reference with `api-integration-review-75dcab.md` (Phase 3/5 overlap)
- [ ] Cross-reference with `workflow-consolidation-c6c363.md` (Phase 7 overlap)
- [ ] Cross-reference with `core-services-evaluation-plan.md` (Phase 5 overlap)
- [ ] Archive plans that are fully superseded by this one

**6.5 Update DOCUMENTATION_INDEX.md**
- [ ] Reflect all moved/merged/deleted docs
- [ ] Ensure every current doc is indexed
- [ ] Add status metadata to all docs missing it

**6.6 Update Cross-References**
- [ ] Specs → related tasks
- [ ] Tasks → related specs
- [ ] Plans → related specs and tasks
- [ ] ADRs → related specs

**6.7 Create ADR for User Model Change**
- [ ] `docs/adr/ADR-009-user-model-restructure.md` — document username + firstName + lastName decision

---

### Phase 7: Infrastructure & CI/CD Alignment

**Goal**: Ensure Terraform, GitHub Actions, docker-compose, and scripts implement the full request path: Frontend ↔ Public ALB ↔ BFF ↔ Private ALB ↔ Microservices, Lambdas.

**7.1 Create Internal ALB for Service-to-Service Communication (D29, D31)**
- [ ] Add `aws_lb.internal` to `modules/compute/main.tf` — `internal = true`, placed in private subnets
- [ ] Add internal ALB security group to `modules/security/main.tf` — allow traffic only from ECS tasks SG
- [ ] Add internal ALB listeners (HTTP on port 80)
- [ ] Add path-based listener rules for each microservice:
  - `/identity/*` → identity-service target group
  - `/organization/*` → organization-service target group
  - `/experiment/*` → experiment-service target group
  - `/metrics/*` → metrics-service target group
  - `/communications/*` → communications-service target group
- [ ] **OR**: Implement AWS Cloud Map service discovery (simpler for ECS Fargate) as an alternative to internal ALB
- [ ] Update BFF `ServiceUrlsConfig` to use internal ALB DNS name or Cloud Map service names
- [ ] Output internal ALB DNS name from compute module

**7.2 Add Public ALB Listener Rules and Target Groups (D30)**
- [ ] Add target group for BFF API service (port 8080, health check `/actuator/health`)
- [ ] Add listener rule on public ALB: `/api/*` → BFF target group
- [ ] Add listener rule for WebSocket Gateway: `/ws/*` → ws-gateway target group (port 3001)
- [ ] **OR**: Verify CI/CD pipelines create these dynamically and document the pattern
- [ ] Add target group health check configurations

**7.3 Fix ECS Security Group Port Configuration (D34)**
- [ ] Option A: Standardize all services to port 8080 (simplest — update docker-compose and service configs)
- [ ] Option B: Add per-service ingress rules for ports 8081-8085, 3001
- [ ] Option C: Use dynamic port mapping with ALB (ALB assigns random host port, SG allows full range from ALB)
- [ ] Decision: recommend Option A (single port) for ECS simplicity — services differentiated by ALB path routing

**7.4 Add EventBridge and Fix Lambda Triggers (D32, D35)**
- [ ] Add `aws_cloudwatch_event_bus` resource in `modules/messaging/main.tf` (custom bus `turaf-events-{env}`)
- [ ] Add EventBridge rules to forward events to notification SQS queue:
  - Rule: `source: ["turaf.experiment-service"]`, `detail-type: ["ExperimentCompleted"]` → SQS notification queue
- [ ] Add EventBridge rules to forward events to reports SQS queue:
  - Rule: `source: ["turaf.experiment-service"]`, `detail-type: ["ExperimentCompleted"]` → SQS reports queue
- [ ] Add SQS queue policies to allow EventBridge to send messages
- [ ] Verify event processor Lambda EventBridge rule pattern matches actual event sources

**7.5 Fix SQS FIFO Queue for Chat Messages (D36)**
- [ ] Change `turaf-chat-messages-{env}` to FIFO queue: name → `turaf-chat-messages-{env}.fifo`, add `fifo_queue = true`, `content_based_deduplication = false`
- [ ] Split into two FIFO queues per ws-gateway spec: direct messages queue + group messages queue
- [ ] Update DLQ or create FIFO DLQ for chat queue

**7.6 Add Frontend Hosting Infrastructure (D33)**
- [ ] Add CloudFront distribution to `modules/storage/main.tf` or new `modules/cdn/` module
- [ ] Configure S3 origin for Angular SPA (with OAC)
- [ ] Add custom error responses for SPA routing (403/404 → /index.html)
- [ ] Add ACM certificate for frontend domain (if not already created)
- [ ] Add Route 53 alias record pointing to CloudFront
- [ ] Connect to `cloudfront_distribution_arn` variable already referenced in S3 bucket policy

**7.7 Docker Compose Fixes (D21)**
- [ ] Remove unused `ministack_data` volume declaration
- [ ] Add BFF service URL env vars: `IDENTITY_SERVICE_URL=http://identity-service:8081`, `ORGANIZATION_SERVICE_URL=http://organization-service:8082`, etc.
- [ ] Normalize communications-service health check to `/actuator/health` (or document the `/communications/` prefix)
- [ ] Consider adding frontend dev server service

**7.8 GitHub Actions Workflows (D22)**
- [ ] Create active `service-ws-gateway.yml` workflow (currently only archived per-env versions exist)
- [ ] Create frontend deployment workflow per `specs/frontend-deployment.md` (S3 sync + CloudFront invalidation)
- [ ] Clean up or formally archive the 27 per-env workflow files in `archive/`
- [ ] Verify CI/CD workflows create ECS services, task definitions, target groups, and listener rules (per compute module comment)
- [ ] Verify `ci.yml` runs lint + test + build for all services
- [ ] Verify `database-migrations.yml` triggers Flyway correctly
- [ ] Verify `infrastructure.yml` applies Terraform correctly per environment
- [ ] Verify branch → environment mapping matches PROJECT.md

**7.9 Scripts Verification**
- [ ] Verify `scripts/setup-ecr-repositories.sh` includes all services
- [ ] Verify `scripts/local-dev-setup.sh` works with current docker-compose
- [ ] Verify `docker-compose.yml` includes all services and dependencies

**7.10 Update Tests**
- [ ] Architecture tests (`services/architecture-tests/`) — update Karate feature files for new User model
- [ ] Integration test script (`scripts/integration-test.sh`) — verify correctness

---

### Phase 8: Windsurf Workflow & Rules Optimization

**Goal**: Ensure AI-assisted development maintains consistency going forward.

**8.1 Update Rules**
- [ ] Update `.windsurf/rules/rules.md` — add explicit User model field standard
- [ ] Add rule: "All RegisterRequest/UserDTO changes must propagate to: migration → entity → DTO → BFF DTO → frontend model → tests"
- [ ] Add rule: "No inline interface definitions in Angular — all models in `models/` directory"
- [ ] Update frontend architecture rules to reflect actual state (NgRx vs Signals decision)

**8.2 Create Windsurf Skills/Workflows**
- [ ] Create `.windsurf/workflows/cross-layer-change.md` — checklist for changes that span DB → backend → BFF → frontend
- [ ] Create `.windsurf/workflows/new-endpoint.md` — checklist for adding a new API endpoint across all layers
- [ ] Create `.windsurf/workflows/verify-alignment.md` — checklist to verify project alignment

**8.3 Update Project Workflow**
- [ ] Update `.windsurf/workflows/project.md` with lessons learned from this alignment
- [ ] Add "alignment check" step to implementation workflow

---

## Phase Execution Order & Dependencies

```
Phase 1 (Docs/Specs)
    ↓
Phase 2 (DB + Identity Service)
    ↓
Phase 3 (BFF API)  ←── depends on Phase 2 DTOs
    ↓
Phase 4 (Frontend)  ←── depends on Phase 3 API shape
    ↓
Phase 5 (Cross-Service Verification)  ←── can partially parallel with Phase 4
    ↓
Phase 6 (Doc Cleanup)  ←── can partially parallel with Phase 5
    ↓
Phase 7 (Infra/CI/CD)  ←── depends on Phase 5 service list
    ↓
Phase 8 (Workflows/Rules)  ←── final phase, captures all learnings
```

Each phase is independently committable and testable. Phases 5-6 can run in parallel. Phase 8 can start alongside Phase 7.

---

## Success Criteria

- [ ] All `RegisterRequest`/`User` DTOs consistent across DB → Identity Service → BFF → Frontend
- [ ] All Identity Service endpoints implemented and tested
- [ ] BFF correctly proxies to all microservices
- [ ] Frontend has single auth service, single guard, single interceptor set — no duplicates
- [ ] Frontend NgRx store properly configured or removed
- [ ] Frontend routes cover all implemented services (including organizations, communications)
- [ ] Problem entity fields aligned between spec and implementation
- [ ] Metrics entity fields aligned between spec and implementation
- [ ] Communications entity aligned with migration schema
- [ ] Event schemas aligned between spec envelope and actual event classes
- [ ] No redundant/conflicting documentation or specs
- [ ] All specs have status metadata and cross-references
- [ ] PROJECT.md accurately reflects implementation
- [ ] Tasks folder accurately reflects implementation status
- [ ] Active plans cross-referenced, no duplicated work items
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] GitHub Actions workflows match deployment architecture (including ws-gateway and frontend)
- [ ] Docker Compose fully functional for local development
- [ ] Terraform matches PROJECT.md service list
- [ ] Full request path works: Frontend → CloudFront → Public ALB → BFF → Internal ALB/Service Discovery → Microservices
- [ ] Internal ALB or Cloud Map enables BFF → microservice communication in AWS
- [ ] Public ALB has listener rules routing to BFF and WS Gateway
- [ ] ECS security groups allow traffic on all required service ports
- [ ] EventBridge event bus exists with rules forwarding to SQS queues for Lambda consumption
- [ ] SQS chat queue is FIFO to support ws-gateway message ordering
- [ ] CloudFront + S3 hosting configured for Angular SPA
- [ ] Windsurf workflows prevent future drift

---

## Discrepancy Index

| ID | Severity | Summary | Phase |
|----|----------|---------|-------|
| D1 | Critical | User model field mismatch across all layers | 1-4 |
| D2 | Critical | Duplicate auth services in frontend | 4 |
| D3 | Medium | PROJECT.md internal inconsistencies (React, sections, libs/) | 1 |
| D4 | Medium | Missing API endpoints in Identity Service | 2 |
| D5 | Medium | BFF ↔ Identity Service path mismatch | 2-3 |
| D6 | Medium | BFF missing service clients | 3 |
| D7 | Medium | DB migration vs spec/entity discrepancies | 2 |
| D8 | Low | Frontend architecture vs rules (NgModule vs standalone) | 4 |
| D9 | Low | Hardcoded values in Identity Service | 2 |
| D10 | Medium | Documentation redundancy | 6 |
| D11 | Low | Missing libs/ directory | 1 |
| D12 | Low | Identity Service spec CORS URLs wrong | 1 |
| D13 | Medium | Infrastructure/Terraform alignment | 7 |
| D14 | Medium | Problem entity spec vs implementation | 5 |
| D15 | Medium | Metrics service spec vs implementation | 5 |
| D16 | Medium | Communications service spec vs implementation | 5 |
| D17 | Critical | Frontend duplicate guards | 4 |
| D18 | Critical | Frontend duplicate interceptors | 4 |
| D19 | Medium | Frontend missing routes | 4 |
| D20 | Medium | Frontend NgRx store references without store setup | 4 |
| D21 | Low | Docker Compose issues | 7 |
| D22 | Medium | GitHub workflows missing ws-gateway + frontend | 7 |
| D23 | Low | V012 migration column width inconsistency | 5 |
| D24 | Medium | Tasks folder staleness | 6 |
| D25 | Low | Active plans overlap | 6 |
| D26 | Low | Communications service different architecture pattern | 5 |
| D27 | Medium | Event schema spec vs implementation | 5 |
| D28 | Low | Specs redundancy (communications, events) | 6 |
| D29 | Critical | No internal/private ALB for BFF → microservices | 7 |
| D30 | Critical | No ALB listener rules or target groups | 7 |
| D31 | Critical | No service discovery for BFF → microservices | 7 |
| D32 | Medium | Lambda trigger architecture mismatch (EventBridge vs SQS) | 7 |
| D33 | Medium | No frontend hosting infrastructure (CloudFront/S3) | 7 |
| D34 | Medium | ECS security group only allows port 8080 | 7 |
| D35 | Medium | No EventBridge event bus resource in Terraform | 7 |
| D36 | Medium | SQS chat queue not FIFO (ws-gateway requires FIFO) | 7 |
