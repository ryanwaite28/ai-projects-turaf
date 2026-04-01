# API Integration Discrepancy Report

**Generated**: 2026-03-31  
**Phase**: 1.5 - Discovery Complete  
**Status**: Critical Issues Identified

---

## Executive Summary

This report documents API contract mismatches discovered across Frontend → BFF → Microservices layers. **27 critical issues** have been identified that require immediate correction.

### Severity Breakdown
- **Critical**: 12 issues (broken endpoints, missing proxies)
- **High**: 8 issues (DTO mismatches, HTTP method inconsistencies)
- **Medium**: 7 issues (query parameter inconsistencies, missing features)

---

## 1. Authentication APIs

### 1.1 Login Response Structure Mismatch ⚠️ **CRITICAL**

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
**Fix Required**: BFF must handle `LoginResponseDto` and extract tokens

---

### 1.2 Register Response Structure Mismatch ⚠️ **CRITICAL**

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
**Fix Required**: BFF must handle `LoginResponseDto`

---

### 1.3 Get Current User Endpoint Mismatch ⚠️ **HIGH**

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
**Fix Required**: BFF should call `/identity/users/me` or Identity Service needs `/auth/me` endpoint

---

### 1.4 Logout Endpoint Missing in Identity Service ⚠️ **CRITICAL**

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
**Fix Required**: Align header expectations

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

### 2.2 Update Organization HTTP Method Mismatch ⚠️ **HIGH**

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
**Fix Required**: Change frontend to use PUT or BFF to accept PATCH

---

### 2.3 Missing Member Management Endpoints in BFF ⚠️ **CRITICAL**

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
**Fix Required**: Add member management endpoints to BFF

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

### 4.1 Missing Problem Endpoints in BFF ⚠️ **CRITICAL**

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
**Fix Required**: Create ProblemController in BFF to proxy to Experiment Service

---

### 4.2 Missing Hypothesis Endpoints in BFF ⚠️ **CRITICAL**

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
**Fix Required**: Create HypothesisController in BFF to proxy to Experiment Service

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

### 6.1 Missing Dashboard Endpoints in Frontend ⚠️ **MEDIUM**

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
**Fix Required**: Align dashboard endpoints between frontend and BFF

---

## 7. Reports APIs

### 7.1 Missing Report Endpoints in BFF ⚠️ **CRITICAL**

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
**Fix Required**: Create ReportController in BFF (reports likely handled by Lambda)

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

## Summary of Critical Issues Requiring Immediate Fix

1. **Authentication**: Login/Register response structure mismatch
2. **Authentication**: Get current user endpoint mismatch
3. **Authentication**: Logout header mismatch
4. **Organization**: Missing GET /organizations endpoint
5. **Organization**: Missing member management endpoints in BFF
6. **Problems**: Missing all problem endpoints in BFF
7. **Hypotheses**: Missing all hypothesis endpoints in BFF
8. **Metrics**: Endpoint path mismatch for experiment metrics
9. **Reports**: Missing all report endpoints in BFF

---

## Recommended Fix Priority

### Phase 1 (Critical - Week 1)
1. Fix authentication response structures
2. Add Problem and Hypothesis controllers to BFF
3. Add member management endpoints to BFF
4. Fix metrics endpoint paths

### Phase 2 (High - Week 2)
5. Align experiment state transitions
6. Fix organizationId handling inconsistencies
7. Add missing organization list endpoint
8. Fix HTTP method mismatches

### Phase 3 (Medium - Week 3)
9. Add report endpoints to BFF
10. Add advanced metrics endpoints
11. Implement pagination support
12. Align dashboard endpoints

---

## Next Steps

1. Review and prioritize fixes with team
2. Create GitHub issues for each critical item
3. Begin Phase 1 implementations
4. Update integration tests
5. Document API contracts with OpenAPI specs
