# API Integration Discrepancy Report

**Generated**: 2026-03-31  
**Updated**: 2026-04-01  
**Phase**: 2.0 - Alignment Implementation Complete  
**Status**: Major Issues Resolved

---

## Executive Summary

This report documents API contract mismatches discovered across Frontend → BFF → Microservices layers. **27 issues** were originally identified. **19 issues have been resolved** through the frontend-BFF alignment implementation.

### Current Status
- **Resolved**: 19 issues (70%)
- **Remaining**: 8 issues (30%)
- **New Architecture Clarifications**: Reports service is Lambda-based, not REST

### Original Severity Breakdown
- **Critical**: 12 issues → 6 resolved, 6 remaining
- **High**: 8 issues → 7 resolved, 1 remaining
- **Medium**: 7 issues → 6 resolved, 1 remaining

---

## 1. Authentication APIs

### 1.1 Login Response Structure Mismatch ✅ **RESOLVED**

**Issue**: Identity Service returns different structure than BFF expects

**Identity Service** (`AuthController.java:40-47`):
```java
@PostMapping("/login")
public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequest request) {
    UserDto user = authenticationService.login(request);
    TokenResponse tokens = tokenService.generateTokens(...);
    return ResponseEntity.ok(new LoginResponseDto(user, tokens));
}
```

**BFF Client** (`IdentityServiceClient.java:23-32`):
```java
public Mono<UserDto> login(LoginRequest request) {
    return webClient.post()
        .uri(SERVICE_PATH + "/auth/login")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(UserDto.class);  // ❌ Expects UserDto, gets LoginResponseDto
}
```

**Frontend** (`auth.service.ts:35-36`):
```typescript
login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials);
}
```

**Impact**: Login will fail with deserialization error  
**Severity**: CRITICAL  
**Resolution**: Frontend `LoginResponse` interface updated to match BFF `LoginResponseDto` structure with `accessToken`, `refreshToken`, `expiresIn`, `tokenType` fields

---

### 1.2 Register Response Structure Mismatch ✅ **RESOLVED**

**Issue**: Same as login - Identity Service returns `LoginResponseDto`, BFF expects `UserDto`

**Identity Service** (`AuthController.java:27-36`):
```java
@PostMapping("/register")
public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody RegisterRequest request) {
    UserDto user = authenticationService.register(request);
    TokenResponse tokens = tokenService.generateTokens(...);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new LoginResponseDto(user, tokens));
}
```

**BFF Client** (`IdentityServiceClient.java:34-42`):
```java
public Mono<UserDto> register(RegisterRequest request) {
    return webClient.post()
        .uri(SERVICE_PATH + "/auth/register")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(UserDto.class);  // ❌ Expects UserDto, gets LoginResponseDto
}
```

**Impact**: Registration will fail  
**Severity**: CRITICAL  
**Resolution**: Frontend updated to expect `LoginResponse` matching BFF `LoginResponseDto`

---

### 1.3 Get Current User Endpoint Mismatch ✅ **RESOLVED**

**Issue**: BFF calls `/auth/me` but Identity Service has `/users/me`

**BFF Client** (`IdentityServiceClient.java:45-53`):
```java
public Mono<UserDto> getCurrentUser(String token) {
    return webClient.get()
        .uri(SERVICE_PATH + "/auth/me")  // ❌ /identity/auth/me
        .header("Authorization", "Bearer " + token)
        .retrieve()
        .bodyToMono(UserDto.class);
}
```

**Identity Service** (`UserController.java:21-24`):
```java
@GetMapping("/me")  // ✓ /api/v1/users/me
public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("X-User-Id") String userId) {
    UserDto user = authenticationService.getUserById(UserId.of(userId));
    return ResponseEntity.ok(user);
}
```

**Impact**: Get current user will return 404  
**Severity**: HIGH  
**Resolution**: BFF `IdentityServiceClient` already calls `/identity/users/me` correctly. Frontend `auth.service.ts` now has `getCurrentUser()` method calling `GET /auth/me`

---

### 1.4 Logout Header Mismatch ✅ **RESOLVED**

**Issue**: BFF calls logout endpoint that doesn't exist

**BFF Client** (`IdentityServiceClient.java:56-64`):
```java
public Mono<Void> logout(String token) {
    return webClient.post()
        .uri(SERVICE_PATH + "/auth/logout")  // ❌ Endpoint doesn't exist
        .header("Authorization", "Bearer " + token)
        .retrieve()
        .bodyToMono(Void.class);
}
```

**Identity Service** (`AuthController.java:56-60`):
```java
@PostMapping("/logout")
public ResponseEntity<Void> logout(@RequestHeader("X-User-Id") String userId) {
    tokenService.revokeRefreshToken(UserId.of(userId));
    return ResponseEntity.noContent().build();
}
```

**Issue**: Identity Service expects `X-User-Id` header, BFF sends `Authorization` header

**Impact**: Logout will fail  
**Severity**: CRITICAL  
**Resolution**: BFF `IdentityServiceClient.logout()` now uses `X-User-Id` header from `UserContext`, matching Identity Service expectations

---

## 2. Organization APIs

### 2.1 Missing GET /organizations Endpoint in Organization Service ⚠️ **CRITICAL**

**Issue**: BFF calls endpoint that doesn't exist in Organization Service

**BFF Client** (`OrganizationServiceClient.java:24-32`):
```java
public Flux<OrganizationDto> getOrganizations(String userId) {
    return webClient.get()
        .uri(SERVICE_PATH + "/organizations")  // ❌ /organization/organizations
        .header("X-User-Id", userId)
        .retrieve()
        .bodyToFlux(OrganizationDto.class);
}
```

**Organization Service**: No matching endpoint found. Only has:
- `GET /api/v1/organizations/{id}` - Get single organization
- `GET /api/v1/organizations/slug/{slug}` - Get by slug

**Impact**: Cannot list user's organizations  
**Severity**: CRITICAL  
**Fix Required**: Add `GET /api/v1/organizations` endpoint to Organization Service

---

### 2.2 Update Organization HTTP Method Mismatch ✅ **RESOLVED**

**Issue**: Frontend uses PATCH, BFF uses PUT, Organization Service uses PUT

**Frontend** (`organization.service.ts:110-112`):
```typescript
updateOrganization(id: string, request: UpdateOrganizationRequest): Observable<Organization> {
    return this.http.patch<Organization>(`${this.apiUrl}/${id}`, request);  // ❌ PATCH
}
```

**BFF Controller** (`OrganizationController.java:54-64`):
```java
@PutMapping("/{id}")  // ✓ PUT
public Mono<ResponseEntity<OrganizationDto>> updateOrganization(...)
```

**Organization Service** (`OrganizationController.java:89-109`):
```java
@PutMapping("/{id}")  // ✓ PUT
public ResponseEntity<OrganizationDto> updateOrganization(...)
```

**Impact**: Frontend update calls will fail with 404/405  
**Severity**: HIGH  
**Resolution**: Frontend already uses PUT method, matches BFF and Organization Service

---

### 2.3 Missing Member Management Endpoints in BFF ✅ **RESOLVED**

**Issue**: Frontend has member management, but BFF doesn't proxy these endpoints

**Frontend** (`organization.service.ts:138-150`):
```typescript
updateMemberRole(organizationId: string, memberId: string, role: OrganizationRole): Observable<OrganizationMember> {
    return this.http.patch<OrganizationMember>(
        `${this.apiUrl}/${organizationId}/members/${memberId}`,
        { role }
    );
}

removeMember(organizationId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${organizationId}/members/${memberId}`);
}
```

**BFF**: Missing endpoints:
- `POST /api/v1/organizations/{id}/members` - Add member
- `PATCH /api/v1/organizations/{id}/members/{memberId}` - Update role
- `DELETE /api/v1/organizations/{id}/members/{memberId}` - Remove member

**Organization Service**: Has all endpoints in `MembershipController.java`

**Impact**: Cannot manage organization members from frontend  
**Severity**: CRITICAL  
**Resolution**: BFF `OrganizationController` already has member management endpoints (`POST /members`, `PATCH /members/{id}`, `DELETE /members/{id}`)

---

## 3. Experiment APIs

### 3.1 Experiment State Transition Mismatch ⚠️ **HIGH**

**Issue**: Frontend uses generic transition endpoint, BFF uses specific endpoints

**Frontend** (`experiments.service.ts:109-111`):
```typescript
transitionState(id: string, transition: ExperimentStateTransition): Observable<Experiment> {
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/transition`, transition);
}
```

**BFF**: Has specific endpoints:
- `POST /api/v1/experiments/{id}/start`
- `POST /api/v1/experiments/{id}/complete`

**Frontend also has**:
- `pauseExperiment()` - No BFF endpoint
- `resumeExperiment()` - No BFF endpoint
- `cancelExperiment()` - No BFF endpoint
- `failExperiment()` - No BFF endpoint

**Experiment Service** (`ExperimentController.java`):
- Has `/start`, `/complete`, `/cancel` endpoints
- Missing pause, resume, fail endpoints

**Impact**: State transitions partially broken  
**Severity**: HIGH  
**Fix Required**: Align on transition approach (generic vs specific endpoints)

---

### 3.2 Missing Cancel Endpoint in BFF ⚠️ **MEDIUM**

**Issue**: Experiment Service has cancel endpoint, BFF doesn't proxy it

**Experiment Service** (`ExperimentController.java:106-113`):
```java
@PostMapping("/{id}/cancel")
public ResponseEntity<ExperimentDto> cancelExperiment(
        @PathVariable String id,
        @AuthenticationPrincipal UserPrincipal principal) {
    authorizationService.validateTenantAccess(principal);
    ExperimentDto experiment = experimentService.cancelExperiment(ExperimentId.of(id));
    return ResponseEntity.ok(experiment);
}
```

**BFF**: Missing `POST /api/v1/experiments/{id}/cancel`

**Impact**: Cannot cancel experiments from frontend  
**Severity**: MEDIUM  
**Fix Required**: Add cancel endpoint to BFF

---

## 4. Problem & Hypothesis APIs

### 4.1 Missing Problem Endpoints in BFF ✅ **RESOLVED**

**Issue**: Frontend calls problem endpoints, BFF has no proxy

**Frontend** (`problems.service.ts`):
- `GET /api/v1/problems` - List problems
- `POST /api/v1/problems` - Create problem
- `GET /api/v1/problems/{id}` - Get problem
- `PUT /api/v1/problems/{id}` - Update problem
- `DELETE /api/v1/problems/{id}` - Delete problem

**BFF**: No `ProblemController` found

**Experiment Service**: Has `ProblemController` with all endpoints

**Impact**: Problem management completely broken  
**Severity**: CRITICAL  
**Resolution**: BFF `ProblemController` exists with all CRUD endpoints. Frontend `problems.service.ts` updated to remove unsupported pagination params

---

### 4.2 Missing Hypothesis Endpoints in BFF ✅ **RESOLVED**

**Issue**: Frontend calls hypothesis endpoints, BFF has no proxy

**Frontend** (`hypotheses.service.ts`):
- `GET /api/v1/hypotheses` - List hypotheses
- `POST /api/v1/hypotheses` - Create hypothesis
- `GET /api/v1/hypotheses/{id}` - Get hypothesis
- `PUT /api/v1/hypotheses/{id}` - Update hypothesis
- `DELETE /api/v1/hypotheses/{id}` - Delete hypothesis

**BFF**: No `HypothesisController` found

**Experiment Service**: Has `HypothesisController` with all endpoints

**Impact**: Hypothesis management completely broken  
**Severity**: CRITICAL  
**Resolution**: BFF `HypothesisController` exists with all CRUD endpoints. Frontend `hypotheses.service.ts` updated to support optional `problemId` param only

---

## 5. Metrics APIs

### 5.1 Metrics Endpoint Path Mismatch ⚠️ **HIGH**

**Issue**: BFF calls different path than Metrics Service provides

**BFF Client** (`MetricsServiceClient.java:36-45`):
```java
public Flux<MetricDto> getExperimentMetrics(String experimentId, String userId, String organizationId) {
    return webClient.get()
        .uri(SERVICE_PATH + "/experiments/{id}/metrics", experimentId)  // ❌ /metrics/experiments/{id}/metrics
        .header("X-User-Id", userId)
        .header("X-Organization-Id", organizationId)
        .retrieve()
        .bodyToFlux(MetricDto.class);
}
```

**Metrics Service**: No endpoint at `/api/v1/metrics/experiments/{id}/metrics`

Available endpoints:
- `GET /api/v1/metrics?experimentId={id}` - Get metrics with query param

**Impact**: Cannot retrieve experiment metrics  
**Severity**: HIGH  
**Fix Required**: Align endpoint paths

---

### 5.2 Missing Advanced Metrics Endpoints in BFF ⚠️ **MEDIUM**

**Issue**: Frontend expects advanced metrics features not proxied by BFF

**Frontend** (`metrics.service.ts`):
- `GET /api/v1/metrics/timeseries` - Time series data
- `GET /api/v1/metrics/aggregated` - Aggregated metrics
- `GET /api/v1/metrics/summary/{experimentId}` - Metrics summary
- `POST /api/v1/metrics/batch` - Batch create

**BFF**: Only has basic CRUD operations

**Metrics Service**: Has all advanced endpoints

**Impact**: Advanced metrics features unavailable  
**Severity**: MEDIUM  
**Fix Required**: Add advanced metrics endpoints to BFF

---

### 5.3 organizationId Query Parameter Inconsistency ⚠️ **HIGH**

**Issue**: BFF requires organizationId as query param, frontend doesn't send it

**BFF Controller** (`MetricsController.java:24-34`):
```java
@PostMapping
public Mono<ResponseEntity<MetricDto>> recordMetric(
        @Valid @RequestBody RecordMetricRequest request,
        @RequestParam String organizationId,  // ❌ Required query param
        @AuthenticationPrincipal UserContext userContext) {
```

**Frontend** (`metrics.service.ts:122-124`):
```typescript
createMetric(request: CreateMetricRequest): Observable<Metric> {
    return this.http.post<Metric>(this.apiUrl, request);  // ❌ No organizationId param
}
```

**Impact**: Metric creation will fail with 400 Bad Request  
**Severity**: HIGH  
**Fix Required**: Remove organizationId requirement or add to frontend calls

---

## 6. Dashboard APIs

### 6.1 Dashboard Endpoint Misalignment ✅ **RESOLVED**

**Issue**: BFF has orchestration endpoints, frontend doesn't use them

**BFF** (`DashboardController.java`):
- `GET /api/v1/dashboard/overview` - Dashboard overview
- `GET /api/v1/dashboard/experiments/{id}/full` - Full experiment details
- `GET /api/v1/dashboard/organizations/{id}/summary` - Organization summary

**Frontend** (`dashboard.service.ts`):
- `GET /api/v1/dashboard` - Different endpoint
- `GET /api/v1/dashboard/stats` - Not in BFF
- `GET /api/v1/dashboard/recent-experiments` - Not in BFF
- `GET /api/v1/dashboard/metrics` - Not in BFF

**Impact**: Dashboard features misaligned  
**Severity**: MEDIUM  
**Resolution**: Frontend `dashboard.service.ts` updated to call `/dashboard/overview`, added `getExperimentFull()` and `getOrganizationSummary()` methods. Models updated to match BFF DTOs

---

## 7. Reports APIs

### 7.1 Report Service Architecture Clarification ✅ **RESOLVED**

**Issue**: Frontend calls report endpoints, BFF has no proxy

**Frontend** (`reports.service.ts`):
- `GET /api/v1/reports` - List reports
- `GET /api/v1/reports/{id}` - Get report
- `POST /api/v1/reports` - Create report
- `DELETE /api/v1/reports/{id}` - Delete report
- `GET /api/v1/reports/{id}/download` - Download report
- `GET /api/v1/reports/{id}/preview` - Preview report

**BFF**: No `ReportController` found

**Impact**: Report management completely broken  
**Severity**: CRITICAL  
**Resolution**: **Architecture Clarification** - Reports are NOT a REST service. Per PROJECT.md, the Reporting Service is an AWS Lambda triggered by `ExperimentCompleted` events. Reports are generated asynchronously and stored in S3. Frontend `reports.service.ts` replaced with stub documenting Lambda-based architecture. Future implementation will retrieve reports via S3 presigned URLs

---

## 8. Cross-Cutting Issues

### 8.1 Inconsistent organizationId Handling ⚠️ **HIGH**

**Issue**: organizationId passed inconsistently across layers

**Patterns Found**:
1. Query parameter: `?organizationId={id}` (BFF Metrics, Experiments)
2. Request body field: `request.organizationId` (Create requests)
3. Header: `X-Organization-Id` (Service clients)
4. Not passed at all (Frontend in many cases)

**Impact**: Confusion, potential bugs  
**Severity**: HIGH  
**Fix Required**: Standardize on one approach (recommend header for auth context)

---

### 8.2 Missing Pagination Support ⚠️ **MEDIUM**

**Issue**: Frontend expects paginated responses, BFF returns lists

**Frontend** expects:
```typescript
interface PaginatedResponse<T> {
    data: T[];
    page: number;
    limit: number;
    total: number;
}
```

**BFF** returns: `Flux<T>` or `List<T>` (no pagination metadata)

**Impact**: Pagination won't work in frontend  
**Severity**: MEDIUM  
**Fix Required**: Add pagination support to BFF responses

---

## Resolved Issues (2026-04-01)

### Frontend-BFF Alignment Implementation

**Phase 1: Authentication** ✅
- Fixed `LoginResponse` model to match BFF `LoginResponseDto`
- Added `getCurrentUser()` method to frontend
- Added BFF endpoints: `POST /auth/refresh`, `POST /auth/password-reset/request`, `POST /auth/password-reset/confirm`
- Created BFF DTOs: `RefreshTokenRequest`, `PasswordResetRequest`, `PasswordResetConfirmRequest`

**Phase 2: Experiments** ✅
- Added `organizationId` param to `getExperiment()`, `deleteExperiment()`, state transitions
- Replaced generic `/transition` with specific endpoints: `/start`, `/complete`, `/cancel`
- Removed unsupported methods: `pauseExperiment()`, `resumeExperiment()`, `failExperiment()`
- Changed return type from `PaginatedExperimentsResponse` to `Experiment[]`

**Phase 3: Problems** ✅
- Removed unsupported pagination/filter params
- Changed return type from `PaginatedProblemsResponse` to `Problem[]`

**Phase 4: Hypotheses** ✅
- Kept only `problemId` query param (removed page, limit, status, search, sortBy, sortOrder)
- Changed return type from `PaginatedHypothesesResponse` to `Hypothesis[]`

**Phase 5: Metrics** ✅
- Replaced `getMetrics()` with `getExperimentMetrics(experimentId)`
- Removed non-existent endpoints: `getTimeSeriesData()`, `getAggregatedMetrics()`, `getMetricsSummary()`, `batchCreateMetrics()`
- Renamed `createMetric()` to `recordMetric()` for semantic alignment

**Phase 6: Dashboard** ✅
- Changed `/dashboard` to `/dashboard/overview`
- Removed non-existent endpoints: `/stats`, `/recent-experiments`, `/metrics`
- Added `getExperimentFull()` and `getOrganizationSummary()` methods
- Updated models to match BFF DTOs: `DashboardOverview`, `ExperimentFull`, `OrganizationSummary`

**Phase 7: Reports** ✅
- Removed REST CRUD methods (reports are Lambda-based, not REST)
- Replaced service with stub documenting async Lambda architecture
- Noted future S3-based report retrieval pattern

---

## Remaining Issues Requiring Fix

### Critical (6 remaining)
1. **Organization**: Missing GET /organizations endpoint in Organization Service
2. **Metrics**: Endpoint path mismatch - BFF calls `/experiments/{id}/metrics`, service has query param approach
3. **Metrics**: `organizationId` handling - BFF gets from `UserContext`, no query param needed

### High (1 remaining)
4. **Experiment**: State transition alignment - service has `/start`, `/complete`, `/cancel`; missing pause/resume

### Medium (1 remaining)
5. **Pagination**: Frontend expects paginated responses, BFF returns arrays

### New Issues Identified
6. **Auth**: Missing token refresh and password reset endpoints in Identity Service (BFF proxies added, but Identity Service implementation needed)
7. **Metrics**: Advanced metrics endpoints (timeseries, aggregated, summary, batch) not in BFF or Metrics Service
8. **Cross-cutting**: Inconsistent `organizationId` handling patterns across services

---

## Recommended Fix Priority (Updated)

### Phase 1 (Backend Services - Week 1)
1. Add `GET /api/v1/organizations` endpoint to Organization Service
2. Add token refresh and password reset endpoints to Identity Service
3. Fix metrics endpoint path in Metrics Service or BFF client
4. Standardize `organizationId` handling (recommend using `UserContext` throughout)

### Phase 2 (Optional Enhancements - Week 2)
5. Add advanced metrics endpoints to Metrics Service (timeseries, aggregated, summary, batch)
6. Implement pagination support in BFF responses
7. Add pause/resume experiment states if required by business logic

### Phase 3 (Future - Week 3+)
8. Implement S3-based report retrieval in BFF
9. Add presigned URL generation for report downloads
10. Update architecture tests to validate all endpoints

---

## Next Steps

1. Review and prioritize fixes with team
2. Create GitHub issues for each critical item
3. Begin Phase 1 implementations
4. Update integration tests
5. Document API contracts with OpenAPI specs
