# Plan: Fix Auth Architecture & Architecture Test Failures

**Status**: Phases 1-4 Complete — Awaiting Verification (Phase 5)  
**Created**: 2026-04-09  
**Related Docs**: `specs/bff-api.md`, `tasks/architecture-tests/010-implement-report-tests.md`  
**Related Services**: `common`, `bff-api`, `organization-service`, `experiment-service`, `metrics-service`, `architecture-tests`

---

## Problem Summary

Architecture tests: **50 run, 33 failures** from 5 root causes identified via `local-integration-test-runs/`.

---

## Root Causes

| # | Cause | Scope | Tests Affected |
|---|-------|-------|----------------|
| 1 | Karate `match X > 0` is invalid syntax — use `assert` | test feature files | 2 failures |
| 2 | `testuser_#(timestamp)` not interpolated → DB constraint on repeat runs | test feature file | 2 failures |
| 3 | `/api/v1/auth/refresh` not in BFF `permitAll()` → 401 | bff-api SecurityConfig | 1 failure |
| 4 | Logout only revokes refresh token; access token stays valid (no blacklist) | bff-api + identity-service | 1 failure |
| 5 | Downstream services have `spring-boot-starter-security` but no `SecurityConfig` → Spring Boot defaults to HTTP Basic, rejecting Bearer tokens; `@AuthenticationPrincipal UserPrincipal` never populated | common + org/experiment/metrics services | 29 failures |

---

## Implementation Plan

### Phase 1 — Common Library: Shared Security Infrastructure ✅
> Fixes root cause #5 centrally so all downstream services benefit

**1.1** Add `ServiceJwtAuthenticationFilter` to `services/common/src/main/java/com/turaf/common/security/`
- Reads `Authorization: Bearer <JWT>` header
- Validates JWT signature using `${jwt.secret}`
- Extracts `sub` (userId) and `organizationId` claim
- Creates `UsernamePasswordAuthenticationToken` with `UserPrincipal` as principal
- Sets it in `SecurityContextHolder`
- Falls back to `X-User-Id` / `X-Organization-Id` headers if no JWT present (internal service-to-service calls)
- `shouldNotFilter`: skip `/actuator/**`, `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/refresh`

**1.2** Add `ServiceSecurityConfig` to `services/common/src/main/java/com/turaf/common/security/`
- `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`
- Disables HTTP Basic, CSRF, stateless sessions
- Permits `/actuator/**`
- Requires authentication for all other requests
- Registers `ServiceJwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- Annotated with `@ConditionalOnProperty(name = "turaf.security.service-mode", havingValue = "true", matchIfMissing = true)` so it can be disabled per-service if needed

**1.3** Update `UserPrincipal` to implement `java.security.Principal` (required for Spring Security principal resolution)

### Phase 2 — Downstream Services: Wire Common Security Config ✅
> Each service gets the common config wired in; no duplicate code

**2.1** `organization-service`
- Add `jwt.secret: ${JWT_SECRET}` to `application.yml` (already in docker yml; add to base yml with safe default)
- Add `turaf.security.service-mode: true` to `application.yml`
- Remove `TenantFilterConfig` manual registration (now handled by `ServiceSecurityConfig` + `ServiceJwtAuthenticationFilter`)

**2.2** `experiment-service`
- Same as 2.1

**2.3** `metrics-service`
- Same as 2.1

**2.4** Verify `communications-service` — determine if it uses same pattern; apply if needed

### Phase 3 — BFF API Fixes ✅
> Fixes root causes #3 and #4

**3.1** `SecurityConfig.java` — add `/api/v1/auth/refresh` to `permitAll()`:
```java
.requestMatchers("/api/v1/auth/refresh").permitAll()
```

**3.2** Implement access token blacklist — add `TokenBlacklistService` in `services/bff-api/src/main/java/com/turaf/bff/security/`:
- `ConcurrentHashMap<String, Instant>` keyed by token hash → expiry time
- `void invalidate(String token)` — adds to map
- `boolean isBlacklisted(String token)` — checks map; prunes expired entries
- `@Scheduled(fixedDelay = 60_000)` cleanup task

**3.3** Update `JwtAuthenticationFilter.java` — check `TokenBlacklistService.isBlacklisted(token)` before accepting; return 401 if blacklisted

**3.4** Update `AuthController.logout` — extract the raw token from the request header and call `tokenBlacklistService.invalidate(token)` before delegating to identity service

### Phase 4 — Architecture Test Fixes ✅
> Fixes root causes #1 and #2

**4.1** `login.feature:18` — replace:
```
And match response.expiresIn > 0
```
with:
```
And assert response.expiresIn > 0
```

**4.2** `token-refresh.feature:20` — same fix as 4.1

**4.3** `registration.feature:7` — replace:
```
* def newUser = { ..., username: 'testuser_#(timestamp)', ... }
```
with:
```
* def username = 'testuser_' + timestamp
* def newUser = { ..., username: '#(username)', ... }
```

### Phase 5 — Verification
**5.1** Re-run architecture tests locally: `cd services/architecture-tests && mvn test`
**5.2** Verify 0 failures
**5.3** Update task files and move plan to `completed/architecture/`

---

## Implementation Order

```
Phase 1 (common library) → Phase 2 (downstream services) → Phase 3 (BFF) → Phase 4 (tests) → Phase 5 (verify)
```

Phases 3 and 4 are independent of each other and can run in parallel after Phase 2.

---

## Files to Create / Modify

### New Files
- `services/common/src/main/java/com/turaf/common/security/ServiceJwtAuthenticationFilter.java`
- `services/common/src/main/java/com/turaf/common/security/ServiceSecurityConfig.java`
- `services/bff-api/src/main/java/com/turaf/bff/security/TokenBlacklistService.java`

### Modified Files
- `services/common/src/main/java/com/turaf/common/security/UserPrincipal.java` — implement `Principal`
- `services/bff-api/src/main/java/com/turaf/bff/security/SecurityConfig.java` — add refresh to permitAll
- `services/bff-api/src/main/java/com/turaf/bff/security/JwtAuthenticationFilter.java` — blacklist check
- `services/bff-api/src/main/java/com/turaf/bff/controllers/AuthController.java` — invalidate on logout
- `services/organization-service/src/main/resources/application.yml` — add jwt.secret default
- `services/experiment-service/src/main/resources/application.yml` — same
- `services/metrics-service/src/main/resources/application.yml` — same
- `services/architecture-tests/src/test/resources/features/authentication/login.feature`
- `services/architecture-tests/src/test/resources/features/authentication/token-refresh.feature`
- `services/architecture-tests/src/test/resources/features/authentication/registration.feature`

---

## Consistency Checklist
- [x] Changes align with PROJECT.md architecture (BFF validates auth, services trust BFF)
- [x] Common library changes apply to all downstream services uniformly
- [x] No circular dependencies introduced
- [ ] Tests cover: ServiceJwtAuthenticationFilter (unit), blacklist (unit), logout invalidation (arch test)
- [ ] Plan moved to `completed/architecture/` when done (after Phase 5 verification)
