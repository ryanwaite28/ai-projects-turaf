# Frontend ↔ BFF API Alignment Plan

Correct all frontend Angular services/models to match actual BFF API endpoints, remove the reports REST feature (Lambda-based per architecture), add missing BFF metrics endpoints, and update outdated documentation.

**Created**: 2026-04-01  
**Status**: Active  
**Related Docs**: `docs/api/api-discrepancy-report.md`, `specs/angular-frontend.md`, `PROJECT.md` (Section 40)

---

## Findings Summary

The existing `docs/api/api-discrepancy-report.md` is **partially outdated** — it claims Problem, Hypothesis, and Organization member endpoints are missing from BFF, but they now exist. The actual current discrepancies (frontend ↔ BFF) are documented below.

---

## Phase 1: Auth Service Fixes

**Files**: `frontend/src/app/features/auth/services/auth.service.ts`, `frontend/src/app/models/user.model.ts`

| # | Issue | Fix |
|---|-------|-----|
| 1.1 | `LoginResponse` model has `{ user, token }` but BFF `LoginResponseDto` returns `{ user, accessToken, refreshToken, expiresIn, tokenType }` | Update `LoginResponse` interface to match BFF response shape |
| 1.2 | `refreshToken()` calls `POST /auth/refresh` — BFF has no endpoint | Add `POST /auth/refresh` endpoint to BFF `AuthController` + `IdentityServiceClient` |
| 1.3 | `requestPasswordReset()` and `resetPassword()` call `/auth/password-reset/*` — BFF has no endpoints | Add `POST /auth/password-reset/request` and `POST /auth/password-reset/confirm` to BFF (proxy to Identity Service) |
| 1.4 | No `getCurrentUser()` method in frontend auth service | Add `getCurrentUser()` calling `GET /auth/me` |

---

## Phase 2: Experiment Service Fixes

**Files**: `frontend/src/app/features/experiments/services/experiments.service.ts`, `frontend/src/app/models/experiment.model.ts`

| # | Issue | Fix |
|---|-------|-----|
| 2.1 | `getExperiments()` sends pagination params (`page`, `limit`, `search`, `sortBy`, `sortOrder`) but BFF requires `organizationId` as mandatory `@RequestParam` and doesn't accept those params | Change frontend to send `organizationId`; remove unsupported params. Adjust return type from `PaginatedExperimentsResponse` to `Experiment[]` (BFF returns `Flux<ExperimentDto>`) |
| 2.2 | `getExperiment(id)` doesn't send `organizationId` but BFF requires it | Add `organizationId` param |
| 2.3 | `deleteExperiment(id)` doesn't send `organizationId` but BFF requires it | Add `organizationId` param |
| 2.4 | Frontend uses generic `POST /{id}/transition` with body `{ action }` but BFF has specific endpoints (`/start`, `/complete`, `/cancel`) | Replace `transitionState()` and individual wrapper methods with direct calls to BFF-specific endpoints: `POST /{id}/start`, `POST /{id}/complete`, `POST /{id}/cancel`. Per PROJECT.md, experiment lifecycle uses `start` and `complete`; BFF also has `cancel` |
| 2.5 | `pauseExperiment()`, `resumeExperiment()`, `failExperiment()` have no BFF endpoints | Remove these methods (not in PROJECT.md service spec) |
| 2.6 | BFF `startExperiment`, `completeExperiment`, `cancelExperiment` require `organizationId` as `@RequestParam` | Ensure frontend sends `organizationId` on state transition calls |

---

## Phase 3: Problems Service Fixes

**Files**: `frontend/src/app/features/problems/services/problems.service.ts`, `frontend/src/app/models/problem.model.ts`

| # | Issue | Fix |
|---|-------|-----|
| 3.1 | `getProblems()` sends pagination/filter params (`page`, `limit`, `status`, `search`, `sortBy`, `sortOrder`) but BFF `GET /problems` accepts none | Remove unsupported query params from frontend call |
| 3.2 | Frontend expects `PaginatedProblemsResponse` but BFF returns `Flux<ProblemDto>` (array) | Change return type to `Problem[]` |

---

## Phase 4: Hypotheses Service Fixes

**Files**: `frontend/src/app/features/hypotheses/services/hypotheses.service.ts`, `frontend/src/app/models/hypothesis.model.ts`

| # | Issue | Fix |
|---|-------|-----|
| 4.1 | `getHypotheses()` sends many query params but BFF only accepts optional `problemId` | Remove unsupported params (`page`, `limit`, `status`, `search`, `sortBy`, `sortOrder`) |
| 4.2 | Frontend expects `PaginatedHypothesesResponse` but BFF returns `Flux<HypothesisDto>` (array) | Change return type to `Hypothesis[]` |

---

## Phase 5: Metrics Service Fixes (Frontend + BFF)

### 5A: BFF Additions

Per PROJECT.md, Metrics Service APIs are: `POST /metrics` and `GET /experiments/{id}/metrics`. The BFF currently has basic CRUD but the frontend needs to retrieve metrics by experiment. The BFF already has `GET /metrics/experiments/{experimentId}` which aligns with project intent.

**BFF files to update**: `MetricsController.java`

| # | Issue | Fix |
|---|-------|-----|
| 5A.1 | `MetricsController.recordMetric()` gets `organizationId` from `userContext.getOrganizationId()` — consistent, no issue | No change needed (frontend doesn't need to send orgId separately since auth context provides it) |

### 5B: Frontend Fixes

**Files**: `frontend/src/app/features/metrics/services/metrics.service.ts`, `frontend/src/app/models/metric.model.ts`

| # | Issue | Fix |
|---|-------|-----|
| 5B.1 | `getMetrics()` calls `GET /metrics` with many query params — BFF has no general listing endpoint | Replace with `getExperimentMetrics(experimentId)` calling `GET /metrics/experiments/{experimentId}` (matches BFF `MetricsController`) |
| 5B.2 | `getTimeSeriesData()` calls `GET /metrics/timeseries` — no BFF endpoint | Remove method (not in PROJECT.md metrics service spec; future enhancement) |
| 5B.3 | `getAggregatedMetrics()` calls `GET /metrics/aggregated` — no BFF endpoint | Remove method |
| 5B.4 | `getMetricsSummary()` calls `GET /metrics/summary/{experimentId}` — no BFF endpoint | Remove method |
| 5B.5 | `batchCreateMetrics()` calls `POST /metrics/batch` — no BFF endpoint | Remove method |
| 5B.6 | `createMetric()` sends `CreateMetricRequest` — BFF expects `RecordMetricRequest` | Rename frontend type to `RecordMetricRequest` to match BFF DTO semantics |
| 5B.7 | `deleteMetric(id)` works — BFF has `DELETE /metrics/{id}` | No change needed |

---

## Phase 6: Dashboard Service Fixes

**Files**: `frontend/src/app/features/dashboard/services/dashboard.service.ts`, `frontend/src/app/models/dashboard.model.ts`

| # | Issue | Fix |
|---|-------|-----|
| 6.1 | `getDashboardData()` calls `GET /dashboard` — BFF has `GET /dashboard/overview` | Change to call `/dashboard/overview` |
| 6.2 | `getDashboardStats()` calls `GET /dashboard/stats` — no BFF endpoint | Remove method |
| 6.3 | `getRecentExperiments()` calls `GET /dashboard/recent-experiments` — no BFF endpoint | Remove method |
| 6.4 | `getDashboardMetrics()` calls `GET /dashboard/metrics` — no BFF endpoint | Remove method |
| 6.5 | No methods for BFF's `GET /dashboard/experiments/{id}/full` and `GET /dashboard/organizations/{id}/summary` | Add `getExperimentFull(id, organizationId)` and `getOrganizationSummary(id)` methods |
| 6.6 | `DashboardData`, `DashboardStats`, `RecentExperiment`, `DashboardMetrics` models don't match BFF DTOs (`DashboardOverviewDto`, `ExperimentFullDto`, `OrganizationSummaryDto`) | Replace models to match BFF response shapes |

---

## Phase 7: Reports Feature Removal

Per PROJECT.md and user confirmation: the Reporting Service is a **Lambda** triggered by `ExperimentCompleted` events. It is not a REST service. The frontend should not have REST-based report CRUD.

**Files to modify/remove**:
- `frontend/src/app/features/reports/services/reports.service.ts` — Remove REST CRUD methods
- `frontend/src/app/models/report.model.ts` — Simplify to read-only report viewing (reports come from S3 via experiment context, not a REST API)
- `frontend/src/app/features/reports/` — Adjust components to view-only (reports generated async by Lambda)
- Routing in `app.routes.ts` — Keep reports route but components should display reports from experiment detail context, not standalone CRUD

**Approach**: Replace the reports service with a method to retrieve a report associated with an experiment (via BFF's `GET /dashboard/experiments/{id}/full` which includes metrics, or a future BFF endpoint for report retrieval from S3). For now, stub the service with a TODO comment noting the Lambda-based architecture.

---

## Phase 8: Update Outdated Documentation

### 8.1 Update `docs/api/api-discrepancy-report.md`

Mark resolved issues:
- Section 4.1 (Missing Problem Endpoints) → **RESOLVED** — `ProblemController` exists in BFF
- Section 4.2 (Missing Hypothesis Endpoints) → **RESOLVED** — `HypothesisController` exists in BFF
- Section 2.3 (Missing Member Management) → **RESOLVED** — `OrganizationController` has member endpoints
- Section 2.2 (HTTP method mismatch) → **RESOLVED** — Frontend uses PUT, matches BFF
- Section 1.1/1.2 (Login/Register response mismatch) → **RESOLVED** — BFF `IdentityServiceClient` now returns `LoginResponseDto`
- Section 1.3 (Get current user mismatch) → **RESOLVED** — BFF calls `/identity/users/me`
- Section 1.4 (Logout header mismatch) → **RESOLVED** — BFF uses `X-User-Id` from UserContext

Add new section documenting remaining gaps and fixes applied by this plan.

### 8.2 Update `specs/angular-frontend.md`

- Update API service examples to match corrected frontend code
- Remove report CRUD references; note Lambda-based report generation
- Update dashboard service example to use `/dashboard/overview`

---

## Implementation Order

1. **Phase 1** — Auth (model + service fixes, BFF refresh/password-reset endpoints)
2. **Phase 2** — Experiments (organizationId params, specific state transition endpoints)
3. **Phase 3** — Problems (remove unsupported params, fix return types)
4. **Phase 4** — Hypotheses (remove unsupported params, fix return types)
5. **Phase 5** — Metrics (frontend aligned to BFF endpoints, remove non-existent calls)
6. **Phase 6** — Dashboard (align to BFF overview/full/summary endpoints)
7. **Phase 7** — Reports (remove REST CRUD, align with Lambda architecture)
8. **Phase 8** — Documentation updates

---

## Files Changed (Summary)

### Frontend (modify)
- `frontend/src/app/models/user.model.ts`
- `frontend/src/app/models/experiment.model.ts`
- `frontend/src/app/models/problem.model.ts`
- `frontend/src/app/models/hypothesis.model.ts`
- `frontend/src/app/models/metric.model.ts`
- `frontend/src/app/models/dashboard.model.ts`
- `frontend/src/app/models/report.model.ts`
- `frontend/src/app/features/auth/services/auth.service.ts`
- `frontend/src/app/features/experiments/services/experiments.service.ts`
- `frontend/src/app/features/problems/services/problems.service.ts`
- `frontend/src/app/features/hypotheses/services/hypotheses.service.ts`
- `frontend/src/app/features/metrics/services/metrics.service.ts`
- `frontend/src/app/features/dashboard/services/dashboard.service.ts`
- `frontend/src/app/features/reports/services/reports.service.ts`

### BFF (modify/add)
- `services/bff-api/.../controllers/AuthController.java` — Add refresh + password-reset endpoints
- `services/bff-api/.../clients/IdentityServiceClient.java` — Add refresh + password-reset client methods

### Documentation (modify)
- `docs/api/api-discrepancy-report.md`
- `specs/angular-frontend.md`
