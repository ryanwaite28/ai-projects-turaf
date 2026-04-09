# /implement-task — Implement a Task from a Task File

Arguments: `$ARGUMENTS` (path to task file, e.g. `tasks/bff-api/005-add-token-blacklist.md`)

Implement the task defined in: **$ARGUMENTS**

## Pre-Implementation Steps

1. Read `PROJECT.md` §§ relevant to this service — architecture, service boundaries, testing strategy (§23a).
2. Read the task file at `$ARGUMENTS` — understand scope, acceptance criteria, and any linked specs.
3. Read the spec file linked in the task (e.g. `specs/<service>.md`) for full context.
4. Check for existing patterns in the same service: read 2-3 existing files that are similar to what you're implementing.
5. Read any active plan that covers this task (check `.windsurf/plans/active/`).

## Implementation Rules (from PROJECT.md)

- **SOLID principles**: single responsibility, dependency inversion via interfaces
- **DDD boundaries**: domain objects in `domain/`, application services in `application/`, REST in `interfaces/rest/`, infrastructure in `infrastructure/`
- **Spring Boot best practices**: constructor injection, `@Value` for config, no field injection
- **No service boundary violations**: a service must never directly call another service's DB or internals
- **Error handling**: throw domain exceptions from domain layer, catch and map in GlobalExceptionHandler

## Testing Requirements (PROJECT.md §23a)

- Unit tests for domain logic and application services (target: 80%+ coverage)
  - Command: `mvn test -Dtest="!*IntegrationTest"`
- Integration tests for repositories and AWS integrations
  - Use Testcontainers + MiniStack (SQS, S3, DynamoDB, EventBridge, SNS, Secrets Manager)
  - Mock unsupported services (CloudWatch) with `@MockBean`
  - Command: `mvn test -Dtest="*IntegrationTest"` (requires Docker)

## Implementation

Implement only what is described in the task file. Do not:
- Add features beyond the task scope
- Refactor surrounding code unless directly blocking the task
- Add comments or docstrings to code you didn't change

## Post-Implementation

After implementation:
1. Mark the task's checklist items as completed (`- [x]`)
2. Add `**Completion Date**: <today's date>` to the task file
3. If an active plan covers this task, update its phase checklist

Output: a brief summary of what was implemented and what tests were written.
