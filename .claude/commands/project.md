# /project — Load Project Context

Read the following files to load authoritative project context before starting any work:

1. Read `PROJECT.md` — the single source of truth for architecture, service boundaries, AWS account structure, CI/CD, testing strategy, and all design decisions.
2. Read `.windsurf/rules/rules.md` — mandatory rules for consistency, AWS standards, architecture standards, security, and code quality.
3. Read `DOCUMENTATION_INDEX.md` (if it exists) — navigation index for all docs.
4. Scan `.windsurf/plans/active/` — list any active plans and summarize their status.

After reading, produce a structured summary covering:

**Architecture**
- Services and their responsibilities (from PROJECT.md)
- Event-driven flows between services
- BFF API pattern: what the BFF does vs. what downstream services do

**Tech Stack**
- Language/framework per service layer
- AWS services in use (SQS, EventBridge, S3, DynamoDB, Lambda, Secrets Manager)
- Testing approach (Karate architecture tests, Testcontainers integration tests, JUnit unit tests)

**Active Work**
- List active plans from `.windsurf/plans/active/` with their status
- Note any blocked or in-progress items

**Key Rules to Remember**
- Top 5 rules from `.windsurf/rules/rules.md` most relevant to current active plans

Keep the summary concise. Flag any conflicts or gaps you notice between the active plans and PROJECT.md.
