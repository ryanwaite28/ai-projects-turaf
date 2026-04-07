# API Integration Review and Correction Plan

Comprehensive review of API contracts and integrations across Frontend → BFF → Microservices to ensure proper alignment and correct API calling patterns.

---

## Metadata

**Created**: 2026-03-31  
**Status**: Active  
**Related Specs**: 
- `specs/bff-api.md`
- `specs/angular-frontend.md`
- `specs/architecture.md`
- `specs/identity-service.md`
- `specs/organization-service.md`
- `specs/experiment-service.md`
- `specs/metrics-service.md`

**Related Tasks**: N/A (This is a review and correction task)

---

## Objectives

1. **Verify API Contract Alignment**: Ensure endpoints, HTTP methods, request/response structures match across all layers
2. **Validate Request/Response DTOs**: Confirm data models are consistent between frontend, BFF, and microservices
3. **Check Authentication Flow**: Verify JWT token handling and header propagation
4. **Review Error Handling**: Ensure consistent error responses across all layers
5. **Validate Query Parameters**: Check parameter naming and usage consistency
6. **Correct Mismatches**: Fix any discrepancies found during review

---

## Scope

### Services to Review

**Frontend (Angular)**:
- Auth Service
- Experiments Service
- Problems Service
- Hypotheses Service
- Metrics Service
- Reports Service
- Organization Service
- Dashboard Service

**BFF API (Spring Boot)**:
- AuthController
- ExperimentController
- OrganizationController
- MetricsController
- DashboardController
- Service Clients (IdentityServiceClient, ExperimentServiceClient, etc.)

**Microservices (Spring Boot)**:
- Identity Service (AuthController, UserController)
- Organization Service (OrganizationController, MembershipController)
- Experiment Service (ExperimentController, HypothesisController, ProblemController)
- Metrics Service (MetricController)

---

## Review Areas

### 1. Authentication & Authorization APIs

**Endpoints to Review**:
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`

**Check Points**:
- [ ] Frontend → BFF endpoint paths match
- [ ] BFF → Identity Service endpoint paths match
- [ ] Request DTOs align (LoginRequest, RegisterRequest)
- [ ] Response DTOs align (LoginResponse with user + tokens)
- [ ] JWT token structure (access + refresh tokens)
- [ ] Authorization header format (`Bearer {token}`)
- [ ] User context propagation (`X-User-Id`, `X-Organization-Id` headers)
- [ ] Error responses (401, 403 handling)

**Known Issues to Investigate**:
- Identity Service returns `LoginResponseDto` with user + tokens
- BFF expects `UserDto` from Identity Service
- Frontend expects `LoginResponse` with user + token
- Mismatch in response structure needs resolution

---

### 2. Organization APIs

**Endpoints to Review**:
- `GET /api/v1/organizations`
- `POST /api/v1/organizations`
- `GET /api/v1/organizations/{id}`
- `PUT /api/v1/organizations/{id}`
- `DELETE /api/v1/organizations/{id}`
- `GET /api/v1/organizations/{id}/members`
- `POST /api/v1/organizations/{id}/members`

**Check Points**:
- [ ] Frontend → BFF endpoint paths match
- [ ] BFF → Organization Service endpoint paths match
- [ ] Request DTOs (CreateOrganizationRequest, UpdateOrganizationRequest)
- [ ] Response DTOs (OrganizationDto, MemberDto)
- [ ] Query parameters for filtering/pagination
- [ ] Authorization checks (organization membership validation)

---

### 3. Experiment APIs

**Endpoints to Review**:
- `GET /api/v1/experiments`
- `POST /api/v1/experiments`
- `GET /api/v1/experiments/{id}`
- `PUT /api/v1/experiments/{id}`
- `DELETE /api/v1/experiments/{id}`
- `POST /api/v1/experiments/{id}/start`
- `POST /api/v1/experiments/{id}/complete`
- `POST /api/v1/experiments/{id}/cancel`

**Check Points**:
- [ ] Frontend → BFF endpoint paths match
- [ ] BFF → Experiment Service endpoint paths match
- [ ] Request DTOs (CreateExperimentRequest, UpdateExperimentRequest)
- [ ] Response DTOs (ExperimentDto)
- [ ] Query parameters (`organizationId`, `hypothesisId`, `status`, pagination)
- [ ] State transition endpoints (start, complete, cancel, pause, resume, fail)
- [ ] organizationId handling (query param vs request body)

**Known Issues to Investigate**:
- Frontend uses generic `transitionState` with action parameter
- BFF uses specific endpoints (`/start`, `/complete`)
- Experiment Service has `/cancel` endpoint not in BFF
- Frontend has pause/resume/fail actions not in BFF
- Need to align state transition approach

---

### 4. Problem & Hypothesis APIs

**Endpoints to Review**:
- `GET /api/v1/problems`
- `POST /api/v1/problems`
- `GET /api/v1/problems/{id}`
- `PUT /api/v1/problems/{id}`
- `DELETE /api/v1/problems/{id}`
- `GET /api/v1/hypotheses`
- `POST /api/v1/hypotheses`
- `GET /api/v1/hypotheses/{id}`
- `PUT /api/v1/hypotheses/{id}`
- `DELETE /api/v1/hypotheses/{id}`

**Check Points**:
- [ ] Verify if BFF has Problem/Hypothesis controllers
- [ ] Check if frontend services exist
- [ ] Verify Experiment Service has ProblemController and HypothesisController
- [ ] Determine if BFF should proxy these endpoints
- [ ] Check request/response DTOs alignment

---

### 5. Metrics APIs

**Endpoints to Review**:
- `POST /api/v1/metrics`
- `GET /api/v1/metrics/{id}`
- `GET /api/v1/experiments/{id}/metrics`

**Check Points**:
- [ ] Frontend → BFF endpoint paths match
- [ ] BFF → Metrics Service endpoint paths match
- [ ] Request DTOs (RecordMetricRequest)
- [ ] Response DTOs (MetricDto)
- [ ] Query parameters for filtering (metricName, startDate, endDate)
- [ ] Experiment-specific metrics retrieval

---

### 6. Dashboard/Orchestration APIs

**Endpoints to Review**:
- `GET /api/v1/dashboard/overview`
- `GET /api/v1/experiments/{id}/full`
- `GET /api/v1/organizations/{id}/summary`

**Check Points**:
- [ ] Verify BFF orchestration endpoints exist
- [ ] Check frontend dashboard service calls
- [ ] Validate aggregated response structures
- [ ] Verify parallel service calls implementation
- [ ] Check error handling for partial failures

---

### 7. Reports APIs

**Endpoints to Review**:
- `GET /api/v1/reports`
- `GET /api/v1/reports/{id}`
- `POST /api/v1/reports/generate`

**Check Points**:
- [ ] Verify if BFF has report endpoints
- [ ] Check frontend reports service
- [ ] Determine report generation flow
- [ ] Verify S3 URL handling for report downloads

---

## Review Process

### Phase 1: Discovery & Documentation (Read-Only)

**Step 1.1: Map Frontend API Calls**
- Read all frontend service files
- Document endpoint paths, HTTP methods, request/response types
- Create API contract matrix for frontend

**Step 1.2: Map BFF Controllers**
- Read all BFF controller files
- Document exposed endpoints, request/response DTOs
- Note any missing endpoints vs spec

**Step 1.3: Map BFF Service Clients**
- Read all BFF service client files
- Document internal ALB calls, headers, request/response types
- Note any discrepancies with microservice APIs

**Step 1.4: Map Microservice Controllers**
- Read all microservice controller files
- Document actual endpoints, request/response DTOs
- Compare with BFF expectations

**Step 1.5: Create Discrepancy Report**
- List all mismatches found
- Categorize by severity (critical, high, medium, low)
- Prioritize fixes

### Phase 2: Validation & Analysis

**Step 2.1: Validate DTOs**
- Compare DTO field names and types across layers
- Check for missing fields or type mismatches
- Verify JSON serialization compatibility

**Step 2.2: Validate Authentication Flow**
- Trace JWT token from frontend → BFF → microservices
- Verify header propagation
- Check token validation logic

**Step 2.3: Validate Query Parameters**
- Check parameter naming consistency
- Verify required vs optional parameters
- Validate pagination parameters

**Step 2.4: Validate Error Handling**
- Check error response formats
- Verify HTTP status codes
- Validate error propagation from microservices → BFF → frontend

### Phase 3: Corrections & Implementation

**Step 3.1: Fix Critical Issues**
- Authentication/authorization mismatches
- Broken endpoint paths
- Missing required endpoints

**Step 3.2: Fix High Priority Issues**
- DTO field mismatches
- Query parameter inconsistencies
- State transition endpoint alignment

**Step 3.3: Fix Medium Priority Issues**
- Missing optional endpoints
- Inconsistent error responses
- Documentation updates

**Step 3.4: Update Tests**
- Update integration tests for corrected APIs
- Add missing test coverage
- Verify end-to-end flows

### Phase 4: Documentation & Verification

**Step 4.1: Update API Documentation**
- Update OpenAPI/Swagger specs
- Update spec files in `/specs`
- Document any architectural decisions

**Step 4.2: Create API Contract Tests**
- Implement contract tests between layers
- Add automated validation
- Set up CI checks

**Step 4.3: Final Verification**
- Run all tests
- Perform manual smoke tests
- Verify all critical flows work end-to-end

---

## Expected Findings

Based on initial review, likely issues include:

1. **Authentication Response Mismatch**: Identity Service returns `LoginResponseDto` with tokens, but BFF expects `UserDto`
2. **Experiment State Transitions**: Frontend uses generic transition endpoint, BFF uses specific endpoints
3. **Missing Endpoints**: Problem/Hypothesis endpoints may not be in BFF
4. **Query Parameter Inconsistencies**: `organizationId` passed as query param vs header vs body
5. **DTO Field Mismatches**: Field names or types may differ between layers
6. **Missing Orchestration Endpoints**: Dashboard aggregation endpoints may not be implemented
7. **Header Propagation**: User context headers may not be consistently propagated

---

## Success Criteria

- [ ] All frontend API calls successfully reach correct microservice endpoints
- [ ] Request/response DTOs are aligned across all layers
- [ ] Authentication flow works end-to-end
- [ ] All CRUD operations work for each domain entity
- [ ] Error responses are consistent and properly handled
- [ ] Query parameters work as expected
- [ ] Integration tests pass for all API flows
- [ ] No 404, 400, or 500 errors due to API mismatches

---

## Deliverables

1. **API Contract Matrix**: Spreadsheet/document showing all endpoints across layers
2. **Discrepancy Report**: List of all mismatches found with severity ratings
3. **Code Fixes**: PRs for all corrections needed
4. **Updated Tests**: Integration tests covering corrected APIs
5. **Updated Documentation**: Specs and API docs reflecting actual implementation
6. **ADR (if needed)**: Document any architectural decisions made during corrections

---

## Risks & Mitigation

**Risk**: Breaking existing functionality during corrections  
**Mitigation**: Comprehensive test coverage before changes, feature flags for major changes

**Risk**: Discovering fundamental architectural misalignment  
**Mitigation**: Escalate to architectural review, create ADR for resolution

**Risk**: Time-consuming manual review process  
**Mitigation**: Use automated tools where possible (OpenAPI diff, contract testing)

---

## Notes

- This is a comprehensive review covering all API layers
- Focus on contract alignment and correctness
- Prioritize authentication and core CRUD operations
- Document all findings for future reference
- Consider implementing automated contract testing to prevent future drift
