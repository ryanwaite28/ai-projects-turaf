# Architecture Tests - Task Breakdown

This directory contains the task breakdown for implementing architecture tests using the Karate framework.

## Overview

Architecture tests validate the complete system integration from entry points (BFF API, WebSocket Gateway) through all downstream services, event-driven processes, and data persistence.

## Task List

### Foundation Tasks

1. **[001-setup-project-structure.md](001-setup-project-structure.md)** - Create Maven project structure with Karate dependencies
2. **[002-configure-karate-framework.md](002-configure-karate-framework.md)** - Configure Karate with environment-specific settings
3. **[003-implement-wait-helpers.md](003-implement-wait-helpers.md)** - Create Java helpers for async waiting

### Infrastructure Tasks

4. **[004-update-iam-permissions.md](004-update-iam-permissions.md)** - Update GitHubActionsDeploymentRole with S3 and CloudFront permissions
5. **[005-create-terraform-infrastructure.md](005-create-terraform-infrastructure.md)** - Create S3 bucket and CloudFront distribution for test reports
6. **[006-create-github-actions-workflow.md](006-create-github-actions-workflow.md)** - Create CI/CD workflow for running tests

### Test Implementation Tasks

7. **[007-implement-authentication-tests.md](007-implement-authentication-tests.md)** - Implement authentication flow tests (login, register, token refresh, logout)
8. **[008-implement-organization-tests.md](008-implement-organization-tests.md)** - Implement organization CRUD and member management tests
9. **[009-implement-experiment-tests.md](009-implement-experiment-tests.md)** - Implement problem, hypothesis, experiment lifecycle, and metrics tests
10. **[010-implement-report-tests.md](010-implement-report-tests.md)** - Implement report management and async generation tests
11. **[011-websocket-tests.md](011-websocket-tests.md)** - Implement WebSocket real-time messaging tests
12. **[012-orchestration-tests.md](012-orchestration-tests.md)** - Implement cross-service orchestration tests
13. **[013-docker-compose-setup.md](013-docker-compose-setup.md)** - Create Docker Compose for local testing

## Implementation Order

Tasks should be completed in numerical order as they build upon each other:

**Phase 1: Foundation (Tasks 1-3)**
- Set up project structure
- Configure Karate framework
- Implement helper utilities

**Phase 2: Infrastructure (Tasks 4-6)**
- Update IAM permissions
- Deploy test report infrastructure
- Create CI/CD workflow

**Phase 3: Test Implementation (Tasks 7-10)**
- Implement authentication tests (login, register, token handling)
- Implement organization and member management tests
- Implement experiment lifecycle tests (problems, hypotheses, experiments, metrics)
- Implement report management and async generation tests

**Phase 4: Advanced Tests (Tasks 11-12)**
- Implement WebSocket real-time messaging tests
- Implement cross-service orchestration tests

**Phase 5: Local Development (Task 13)**
- Set up Docker Compose for local testing
- Validate tests run locally

## Related Documentation

- [Architecture Testing Specification](../../specs/architecture-testing.md)
- [Testing Strategy](../../specs/testing-strategy.md)
- [PROJECT.md Section 23b](../../PROJECT.md#23b-architecture-testing-strategy)
- [API Alignment Review](../../docs/assessments/architecture-tests-api-alignment-2026-03-31.md)
- [API Fixes Implementation Summary](../../docs/api/api-fixes-implementation-summary.md)

## Progress Tracking

Track task completion status in each individual task file.

### Current Status

- ✅ Task 001: Setup Project Structure - **COMPLETED**
- ⏳ Task 002: Configure Karate Framework - Pending
- ⏳ Task 003: Implement Wait Helpers - Pending
- ⏳ Task 004: Update IAM Permissions - Pending
- ⏳ Task 005: Create Terraform Infrastructure - Pending
- ⏳ Task 006: Create GitHub Actions Workflow - Pending
- ⏳ Task 007: Implement Authentication Tests - Pending
- ⏳ Task 008: Implement Organization Tests - Pending
- ⏳ Task 009: Implement Experiment Tests - Pending
- ⏳ Task 010: Implement Report Tests - Pending
- ⏳ Task 011: WebSocket Tests - Pending
- ⏳ Task 012: Orchestration Tests - Pending
- ⏳ Task 013: Docker Compose Setup - Pending

## API Alignment Notes

**Important**: Tasks 007-010 have been updated to align with recent API integration fixes (2026-03-31):

- **Authentication**: Tests now expect `LoginResponseDto` with `accessToken`, `refreshToken`, `user`, `expiresIn`, `tokenType`
- **Organizations**: Added tests for `GET /api/v1/organizations` and member management endpoints
- **Experiments**: Added tests for problems, hypotheses, and experiment cancel endpoint
- **Reports**: New task for report management including async generation and download

See [API Alignment Review](../../docs/assessments/architecture-tests-api-alignment-2026-03-31.md) for detailed changes.
