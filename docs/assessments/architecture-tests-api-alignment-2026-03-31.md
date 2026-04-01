# Architecture Tests API Alignment Review

**Date**: 2026-03-31  
**Reviewer**: AI Assistant  
**Status**: Alignment Review Complete  
**Related Documents**:
- [Architecture Testing Spec](../../specs/architecture-testing.md)
- [API Fixes Implementation Summary](../api/api-fixes-implementation-summary.md)
- [API Discrepancy Report](../api/api-discrepancy-report.md)

---

## Executive Summary

This document reviews the architecture-testing specification against the recent API integration fixes to ensure test scenarios accurately reflect the current API contracts. The review identified **8 critical misalignments** that must be corrected before implementing architecture tests.

### Key Findings

- ✅ **Authentication tests**: Require updates for LoginResponseDto structure
- ⚠️ **Organization tests**: Need new endpoints for listing organizations and member management
- ⚠️ **Problem/Hypothesis tests**: New endpoints added, tests need to be created
- ⚠️ **Metrics tests**: Endpoint paths changed, organizationId handling updated
- ⚠️ **Experiment tests**: Cancel endpoint added, needs test coverage
- ⚠️ **Report tests**: New endpoints added, tests need to be created

---

## Detailed Misalignments

### 1. Authentication Response Structure ⚠️ CRITICAL

**Current Spec** (lines 154-168):
```gherkin
Scenario: Successful login returns JWT token
  Given path '/api/v1/auth/login'
  And request testUser
  When method POST
  Then status 200
  And match response.token != null
  And match response.user.email == testUser.email
```

**Issue**: The spec expects a flat response with `token` and `user` fields, but the actual API now returns `LoginResponseDto` with nested structure.

**Actual API Response** (per API fixes):
```json
{
  "user": {
    "id": "...",
    "email": "...",
    "name": "..."
  },
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

**Required Fix**:
```gherkin
Scenario: Successful login returns JWT token and user data
  Given path '/api/v1/auth/login'
  And request testUser
  When method POST
  Then status 200
  And match response.accessToken != null
  And match response.refreshToken != null
  And match response.user.email == testUser.email
  And match response.tokenType == 'Bearer'
  And match response.expiresIn > 0
  
  # Verify token works for protected endpoint
  Given path '/api/v1/auth/me'
  And header Authorization = 'Bearer ' + response.accessToken
  When method GET
  Then status 200
  And match response.email == testUser.email
```

**Impact**: HIGH - Authentication is the foundation for all other tests

---

### 2. Get Current User Endpoint Path ⚠️ CRITICAL

**Current Spec** (line 163):
```gherkin
Given path '/api/v1/auth/me'
```

**Issue**: This is correct in the spec, but the example shows using `Authorization` header with token.

**Actual Implementation**: Uses `UserContext` from Spring Security, not manual token parsing.

**Required Fix**: The test should work as-is since the BFF's AuthController still accepts `Authorization: Bearer <token>` header and extracts UserContext from it. No change needed, but documentation should clarify this.

**Impact**: LOW - Test will work, but implementation detail differs

---

### 3. Organization Listing Endpoint ⚠️ NEW FEATURE

**Current Spec**: Missing test for listing user's organizations

**New API Endpoint** (per API fixes):
```
GET /api/v1/organizations
```

**Required Addition**:
```gherkin
Scenario: List user's organizations
  # Login
  Given path '/api/v1/auth/login'
  And request testUser
  When method POST
  Then status 200
  * def token = response.accessToken
  
  # List organizations
  Given path '/api/v1/organizations'
  And header Authorization = 'Bearer ' + token
  When method GET
  Then status 200
  And match response != null
  And match response[*].id != null
  And match response[*].name != null
```

**Impact**: MEDIUM - New functionality needs test coverage

---

### 4. Member Management Endpoints ⚠️ NEW FEATURES

**Current Spec** (lines 183-204): Has basic member add scenario but missing update and remove

**New API Endpoints** (per API fixes):
```
POST   /api/v1/organizations/{id}/members
PATCH  /api/v1/organizations/{id}/members/{memberId}
DELETE /api/v1/organizations/{id}/members/{memberId}
```

**Required Additions**:

```gherkin
Scenario: Update member role
  * def orgId = <existing org>
  * def memberId = <existing member>
  
  Given path '/api/v1/organizations', orgId, 'members', memberId
  And request { role: 'ADMIN' }
  And header Authorization = 'Bearer ' + token
  When method PATCH
  Then status 200
  And match response.role == 'ADMIN'

Scenario: Remove member from organization
  Given path '/api/v1/organizations', orgId, 'members', memberId
  And header Authorization = 'Bearer ' + token
  When method DELETE
  Then status 200
  
  # Verify member removed
  Given path '/api/v1/organizations', orgId, 'members'
  When method GET
  Then status 200
  And match response[?(@.id == memberId)] == []
```

**Impact**: MEDIUM - Core organization management functionality

---

### 5. Problem and Hypothesis Endpoints ⚠️ NEW FEATURES

**Current Spec** (lines 230-242): Shows problem/hypothesis creation but these were missing from BFF

**New API Endpoints** (per API fixes):
```
GET    /api/v1/problems
POST   /api/v1/problems
GET    /api/v1/problems/{id}
PUT    /api/v1/problems/{id}
DELETE /api/v1/problems/{id}

GET    /api/v1/hypotheses?problemId={id}
POST   /api/v1/hypotheses
GET    /api/v1/hypotheses/{id}
PUT    /api/v1/hypotheses/{id}
DELETE /api/v1/hypotheses/{id}
```

**Current Status**: The spec example (lines 230-242) is **correct** and aligns with the new implementation!

**Verification Needed**: Ensure the request/response structures match:
- Problem: `{ title, description, affectedUsers, context, organizationId }`
- Hypothesis: `{ problemId, statement, expectedOutcome, successCriteria, organizationId }`

**Impact**: LOW - Spec is already aligned, just needs verification

---

### 6. Metrics Endpoint Paths ⚠️ CRITICAL

**Current Spec** (line 258):
```gherkin
Given path '/api/v1/metrics'
And request { experimentId: '#(experimentId)', name: 'conversion_rate', value: 0.25 }
```

**Issue**: The spec is correct for POST, but GET endpoint path has changed.

**Old Path** (BFF was calling): `/metrics/experiments/{id}/metrics`  
**New Path** (per API fixes): `/metrics?experimentId={id}`

**Required Fix**: Update any GET metrics scenarios:
```gherkin
# Get metrics for experiment
Given path '/api/v1/metrics/experiments', experimentId
When method GET
Then status 200
```

This should still work as the BFF's MetricsController has:
```
GET /api/v1/metrics/experiments/{experimentId}
```

**Impact**: LOW - BFF abstracts the internal service path change

---

### 7. Experiment Cancel Endpoint ⚠️ NEW FEATURE

**Current Spec**: Missing cancel experiment scenario

**New API Endpoint** (per API fixes):
```
POST /api/v1/experiments/{id}/cancel
```

**Required Addition**:
```gherkin
Scenario: Cancel running experiment
  * def experimentId = <running experiment>
  
  Given path '/api/v1/experiments', experimentId, 'cancel'
  And header Authorization = 'Bearer ' + token
  When method POST
  Then status 200
  And match response.status == 'CANCELLED'
  
  # Verify experiment is cancelled
  Given path '/api/v1/experiments', experimentId
  When method GET
  Then status 200
  And match response.status == 'CANCELLED'
```

**Impact**: MEDIUM - Important state transition functionality

---

### 8. Report Endpoints ⚠️ NEW FEATURES

**Current Spec** (lines 275-289): Shows report retrieval after experiment completion

**New API Endpoints** (per API fixes):
```
GET    /api/v1/reports?type={type}&status={status}
POST   /api/v1/reports
GET    /api/v1/reports/{id}
DELETE /api/v1/reports/{id}
GET    /api/v1/reports/{id}/download
```

**Current Spec Path** (line 281):
```gherkin
Given path '/api/v1/experiments', experimentId, 'report'
```

**Issue**: The spec uses experiment-specific report endpoint, but new implementation has generic report endpoints.

**Required Clarification**: Need to determine if:
1. Experiment-specific endpoint still exists: `/api/v1/experiments/{id}/report`
2. Or should use generic: `/api/v1/reports?experimentId={id}`

**Required Additions**:
```gherkin
Feature: Report Management

  Scenario: List reports with filters
    Given path '/api/v1/reports'
    And param type = 'EXPERIMENT'
    And param status = 'COMPLETED'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response[*].type == 'EXPERIMENT'
  
  Scenario: Download report
    * def reportId = <existing report>
    
    Given path '/api/v1/reports', reportId, 'download'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match responseHeaders['Content-Type'][0] == 'application/pdf'
```

**Impact**: HIGH - Core reporting functionality

---

## Summary of Required Changes

### Architecture Testing Spec Updates

1. **Update Authentication Tests** (lines 147-168)
   - Change response assertions to match `LoginResponseDto` structure
   - Use `accessToken` instead of `token`
   - Add assertions for `refreshToken`, `expiresIn`, `tokenType`

2. **Add Organization Listing Test** (new)
   - Create scenario for `GET /api/v1/organizations`

3. **Expand Member Management Tests** (lines 183-204)
   - Add update member role scenario
   - Add remove member scenario
   - Add verification steps

4. **Add Experiment Cancel Test** (new)
   - Create scenario for `POST /api/v1/experiments/{id}/cancel`

5. **Add Report Management Tests** (new feature file)
   - Create `features/reports/report-management.feature`
   - Add list, create, get, delete, download scenarios

6. **Verify Problem/Hypothesis Tests** (lines 230-242)
   - Confirm request/response structures match implementation
   - No changes needed if structures align

7. **Update Metrics Tests** (if needed)
   - Verify GET endpoint paths are correct
   - Confirm organizationId handling

### Task File Updates

Review and update the following task files to reflect API changes:

1. **002-configure-karate-framework.md**
   - Ensure environment configs have correct base URLs
   - Verify authentication token handling

2. **003-implement-wait-helpers.md**
   - Confirm wait helpers work with new response structures
   - Update report waiting logic if needed

3. **Create new task**: `007-implement-authentication-tests.md`
   - Implement updated authentication test scenarios
   - Cover login, register, getCurrentUser, logout

4. **Create new task**: `008-implement-organization-tests.md`
   - Implement organization CRUD tests
   - Implement member management tests

5. **Create new task**: `009-implement-experiment-tests.md`
   - Implement problem/hypothesis tests
   - Implement experiment lifecycle tests
   - Implement cancel experiment tests

6. **Create new task**: `010-implement-report-tests.md`
   - Implement report management tests
   - Implement report download tests

---

## Recommended Action Plan

### Phase 1: Update Specification (Immediate)

1. Update `specs/architecture-testing.md`:
   - Fix authentication response structure (lines 154-168)
   - Add organization listing scenario
   - Expand member management scenarios
   - Add experiment cancel scenario
   - Add report management section

2. Update spec metadata:
   - Change "Last Updated" to 2026-03-31
   - Add reference to API fixes documentation

### Phase 2: Create Missing Test Scenarios (High Priority)

1. Create `features/reports/report-management.feature`
2. Expand `features/organizations/manage-members.feature`
3. Add cancel scenario to `features/experiments/experiment-lifecycle.feature`

### Phase 3: Update Task Files (Medium Priority)

1. Review existing tasks (001-006)
2. Create new tasks (007-010) for test implementation
3. Update task dependencies and estimates

### Phase 4: Verification (Before Implementation)

1. Review all endpoint paths against BFF controllers
2. Verify request/response DTOs match
3. Confirm header propagation (X-User-Id, X-Organization-Id)
4. Test authentication flow manually

---

## Alignment Checklist

- [ ] Authentication tests updated for LoginResponseDto
- [ ] Organization listing test added
- [ ] Member update/remove tests added
- [ ] Experiment cancel test added
- [ ] Report management tests added
- [ ] Problem/Hypothesis tests verified
- [ ] Metrics endpoint paths verified
- [ ] Spec metadata updated
- [ ] Task files reviewed and updated
- [ ] New task files created

---

## Risk Assessment

### High Risk
- **Authentication changes**: All tests depend on correct authentication
- **Report endpoints**: Unclear if experiment-specific endpoint still exists

### Medium Risk
- **Member management**: New endpoints need comprehensive testing
- **Experiment cancel**: State transition logic needs validation

### Low Risk
- **Problem/Hypothesis**: Spec already aligns with implementation
- **Metrics paths**: BFF abstracts internal changes

---

## Next Steps

1. **Immediate**: Update architecture-testing.md specification
2. **Short-term**: Create missing test feature files
3. **Medium-term**: Implement updated test scenarios
4. **Long-term**: Run full test suite against deployed environments

---

## References

- [Architecture Testing Spec](../../specs/architecture-testing.md)
- [API Fixes Implementation Summary](../api/api-fixes-implementation-summary.md)
- [API Discrepancy Report](../api/api-discrepancy-report.md)
- [BFF API Controllers](../../services/bff-api/src/main/java/com/turaf/bff/controllers/)
- [Task Files](../../tasks/architecture-tests/)
