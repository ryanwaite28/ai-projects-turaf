# Claude Code — Project Configuration

## Project

This is **Turaf** — a production-grade event-driven SaaS platform for problem tracking and solution validation.

**Authoritative docs** (read before any work):
- `PROJECT.md` — single source of truth for all architecture, services, and design decisions
- `.windsurf/rules/rules.md` — mandatory rules for consistency and quality
- `.windsurf/workflows/project.md` — workflow guide (AI-adapted into the commands below)

## Available Commands

| Command | Purpose |
|---------|---------|
| `/project` | Load and summarize project context from PROJECT.md |
| `/new-plan <name>` | Create a new implementation plan in `.windsurf/plans/active/` |
| `/complete-plan <category/filename>` | Archive a completed plan and update related docs |
| `/implement-task <task-file>` | Implement a task following PROJECT.md principles |
| `/next-task` | Find the next uncompleted task based on build order |
| `/run-tests <service>` | Run tests for a service and diagnose failures |
| `/verify-consistency` | Run the full consistency verification checklist |
| `/assess-docs` | Assess documentation quality across the workspace |
| `/generate-spec <service>` | Generate a spec document for a service or feature |

## Key Conventions

- **Tech stack**: Java 21, Spring Boot 3, Karate (architecture tests), Testcontainers, AWS (SQS, EventBridge, S3, DynamoDB, Lambda)
- **Architecture pattern**: DDD — `domain/` → `application/` → `interfaces/rest/` → `infrastructure/`
- **Auth pattern**: BFF validates JWT; downstream services use `common` `ServiceJwtAuthenticationFilter`
- **Multi-tenancy**: All requests carry `organizationId` from JWT; enforced via `TenantContextHolder`
- **Test strategy**: Unit (`!*IntegrationTest`), Integration (Testcontainers+MiniStack), Architecture (Karate against running stack)

## Service Ports (local/docker)

| Service | Port |
|---------|------|
| BFF API | 8080 |
| Identity | 8081 |
| Organization | 8082 |
| Experiment | 8083 |
| Metrics | 8084 |
| Communications | 8085 |
| WS Gateway | 3000 |
| MiniStack | 4566 |
| PostgreSQL | 5432 |
