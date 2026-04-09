---
name: Auth Architecture Fix Plan
description: Active plan to fix auth architecture (downstream service SecurityConfig) and 33 architecture test failures
type: project
---

Active plan at `.windsurf/plans/active/fix-auth-architecture-and-tests.md` covers 5 root causes.

Root cause #5 (29 failures): downstream services (org, experiment, metrics) have `spring-boot-starter-security` but no SecurityConfig → Spring Boot defaults to HTTP Basic, rejecting Bearer tokens. Fix is adding `ServiceJwtAuthenticationFilter` + `ServiceSecurityConfig` to the common library.

**Why:** Services use `@AuthenticationPrincipal UserPrincipal` which requires Spring Security to set the principal, but no filter does this.

**How to apply:** When working on any downstream service (org, experiment, metrics, communications), check if `ServiceSecurityConfig` from common is wired in. If not, this is the blocker.
