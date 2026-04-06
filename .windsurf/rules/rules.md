# Windsurf Rules for Turaf Platform

**Last Updated**: March 27, 2026  
**Purpose**: Enforce consistency, quality, and alignment with project standards  
**Derived From**: Core documentation (PROJECT.md, BEST_PRACTICES.md, GITHUB.md, AWS_ACCOUNTS.md, DOCUMENTATION_INDEX.md)

---

## 1. Documentation Hierarchy & Single Source of Truth

### 1.1 Authoritative Source

**CRITICAL**: PROJECT.md is the single authoritative source for all project documentation.

All changes MUST align with PROJECT.md, which contains:
- Complete system architecture and design
- AWS account structure and multi-account strategy (§ 2)
- GitHub repository configuration and CI/CD workflows (§ 3)
- Local development setup and troubleshooting (§ 22a)
- Deployment procedures and operational runbooks
- Testing strategy and architecture decision records (§ 17)
- All infrastructure and service specifications

### 1.2 Documentation Layers (Strict Hierarchy)

```
PROJECT.md (Single Source of Truth - AUTHORITATIVE)
    ↓
Specifications in specs/ (Detailed Service Specs)
    ↓
Tasks in tasks/ (Implementation Tracking)
    ↓
Implementation Plans in .windsurf/plans/ (Execution Details)
    ↓
Code Implementation
```

**Rule**: If conflict exists between layers, higher layer wins. Pause and ask user how to proceed if PROJECT.md conflicts with request.

### 1.3 Before Any Implementation

**MANDATORY CHECKS**:
- [ ] Read PROJECT.md for authoritative context
- [ ] Review relevant specs in specs/
- [ ] Check existing tasks in tasks/
- [ ] Look for completed plans in .windsurf/plans/completed/ for patterns
- [ ] Verify no conflicts with PROJECT.md

---

## 2. AWS Account & Infrastructure Standards

### 2.1 AWS Account IDs (NEVER HARDCODE INCORRECTLY)

**Authoritative Source**: PROJECT.md § 2 (AWS Account Architecture)

| Account | ID | Email | Purpose |
|---------|------------|-------|---------|
| root | 072456928432 | aws@turafapp.com | Management Account |
| Ops | 146072879609 | aws-ops@turafapp.com | DevOps/CI/CD |
| dev | 801651112319 | aws-dev@turafapp.com | Development |
| qa | 965932217544 | aws-qa@turafapp.com | QA/Staging |
| prod | 811783768245 | aws-prod@turafapp.com | Production |

**Rule**: Always reference PROJECT.md § 2 when using account IDs. NEVER guess or use placeholder values.

### 2.2 IAM Roles & OIDC

**Authoritative Source**: PROJECT.md § 3 (CI/CD Integration)

- **Role Name Pattern**: `GitHubActionsDeploymentRole` (consistent across all accounts)
- **OIDC Provider**: `token.actions.githubusercontent.com`
- **Repository**: `ryanwaite28/ai-projects-turaf`
- **Authentication**: OIDC federation (NO long-lived credentials)

**Rule**: All GitHub Actions workflows MUST use OIDC authentication. Never create or use IAM access keys.

### 2.3 Domain & DNS Standards

**Primary Domain**: `turafapp.com`

**Naming Convention**: `{service}.{env}.turafapp.com`

**Environments**:
- `dev` - Development
- `qa` - QA/Staging  
- `prod` - Production (may omit `.prod` for brevity)

**Examples**:
- `api.dev.turafapp.com` - DEV BFF API
- `api.qa.turafapp.com` - QA BFF API
- `api.turafapp.com` - PROD BFF API
- `app.dev.turafapp.com` - DEV Frontend
- `internal-alb.dev.turafapp.com` - DEV Internal ALB

**Rule**: Always follow this naming convention. Never create custom domain patterns.

---

## 3. Repository & Git Workflow Standards

### 3.1 Branch Strategy

**Authoritative Source**: PROJECT.md § 3 (GitHub Repository)

- **main** - Production-ready, deploys to PROD (811783768245)
- **develop** - Development branch, deploys to DEV (801651112319)
- **release/*** - Release candidates, deploy to QA (965932217544)
- **feature/*** - Feature development, CI validation only
- **hotfix/*** - Critical production fixes
- **docs/*** - Documentation updates
- **test/*** - Testing branches
- **perf/*** - Performance improvements
- **security/*** - Security fixes
- **refactor/*** - Code refactoring
- **chore/*** - Maintenance tasks

**Rule**: Always use appropriate branch prefix. Never commit directly to main or develop.

### 3.2 Commit Standards

**Format**: Conventional Commits

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation only
- `style` - Code style (formatting, no logic change)
- `refactor` - Code refactoring
- `perf` - Performance improvement
- `test` - Adding/updating tests
- `chore` - Maintenance tasks
- `ci` - CI/CD changes
- `build` - Build system changes

**Rule**: All commits MUST follow conventional commit format for automated changelog generation.

### 3.3 Pull Request Requirements

**All PRs to main must pass**:
- Lint (Java, Angular, Terraform)
- Unit Tests (Backend, Frontend)
- Build (All services)
- SonarQube Quality Gate
- Security Scan (OWASP, npm audit)
- Terraform Validate (if infrastructure changes)
- Minimum 1 approval (solo repo, but enforce for discipline)

**Rule**: Never merge without passing all status checks.

---

## 4. Architecture & Design Standards

### 4.1 Service Architecture

**Pattern**: Domain-Driven Design (DDD) + Event-Driven Architecture

**Service Boundaries** (from PROJECT.md):
- Identity Service - Authentication, user management
- Organization Service - Multi-tenant organization management
- Experiment Service - A/B testing, experimentation
- Metrics Service - Metrics collection, analysis
- Reporting Service - Report generation
- Notification Service - Multi-channel notifications
- Communications Service - Email/SMS delivery
- BFF API - Backend for Frontend
- WebSocket Gateway - Real-time communication

**Rule**: Respect service boundaries. Never create cross-service dependencies that violate DDD principles.

### 4.2 Clean Architecture Layers

**Mandatory Layer Structure**:

```
domain/          # Pure business logic, no dependencies
  ├── model/     # Entities, value objects
  ├── event/     # Domain events
  └── service/   # Domain services

application/     # Use cases, orchestration
  ├── service/   # Application services
  └── dto/       # Data transfer objects

infrastructure/  # External concerns
  ├── persistence/  # Database implementations
  ├── messaging/    # Event bus implementations
  └── config/       # Configuration

api/            # REST controllers, GraphQL resolvers
  ├── rest/
  └── graphql/
```

**Rule**: Always follow this layer structure. Domain layer MUST NOT depend on infrastructure or application layers.

### 4.3 Event-Driven Standards

**Event Naming Convention**: `{Entity}{Action}` (PascalCase)

**Examples**:
- `ProblemCreated`
- `HypothesisCreated`
- `ExperimentStarted`
- `ExperimentCompleted`
- `MetricRecorded`

**Event Envelope** (mandatory fields):
```json
{
  "eventId": "uuid",
  "eventType": "ExperimentStarted",
  "timestamp": "ISO-8601",
  "sourceService": "experiment-service",
  "tenantId": "uuid",
  "payload": { ... }
}
```

**Rule**: All domain events MUST include these fields. Events are immutable.

### 4.4 Database Architecture

**Pattern**: Single Database, Multi-Schema (ADR-006)

**Schema per Service**:
- `identity_schema` - Identity Service
- `organization_schema` - Organization Service
- `experiment_schema` - Experiment Service
- `metrics_schema` - Metrics Service
- `reporting_schema` - Reporting Service
- `notification_schema` - Notification Service
- `communications_schema` - Communications Service

**Rule**: Each service owns its schema. No cross-schema queries. Use events for cross-service communication.

---

## 5. Technology Stack Standards

### 5.1 Backend (Java/Spring Boot)

**Versions**:
- Java 17 (LTS)
- Spring Boot 3.x
- Maven for dependency management

**Required Dependencies**:
- Spring Web
- Spring Data JPA
- Spring Cloud AWS
- Lombok
- MapStruct (for DTO mapping)
- JUnit 5 + Mockito (testing)
- Testcontainers (integration testing)

**Rule**: Use Spring Boot 3.x features. No legacy Spring patterns.

### 5.2 Frontend (Angular)

**Version**: Angular 17+

**Standards**:
- Standalone components (no NgModules)
- Signals for state management
- RxJS for async operations
- TailwindCSS for styling
- Lucide for icons
- shadcn/ui for components (Angular port)

**Rule**: Use modern Angular patterns. No legacy module-based architecture.

### 5.3 Infrastructure (AWS + Terraform)

**IaC Tool**: Terraform (HCL)

**AWS Services** (approved):
- **Compute**: ECS Fargate, Lambda
- **Networking**: VPC, ALB (public + internal), CloudFront
- **Storage**: S3, RDS PostgreSQL, ElastiCache Redis
- **Messaging**: EventBridge, SQS, SNS
- **Observability**: CloudWatch, X-Ray
- **Security**: ACM, Secrets Manager, IAM
- **CI/CD**: CodeBuild, CodeDeploy, CodePipeline

**Rule**: Use only approved AWS services. All infrastructure MUST be defined in Terraform.

---

## 6. Testing Standards

### 6.1 Test Coverage Requirements

**Minimum Coverage**:
- Domain Layer: 90%+
- Application Services: 85%+
- Infrastructure: 70%+
- Overall Project: 80%+

**Rule**: Never reduce test coverage. All PRs must maintain or improve coverage.

### 6.2 Testing Strategy

**Unit Tests**:
- Fast, isolated tests
- Mock external dependencies
- Test domain logic thoroughly
- Use JUnit 5 + Mockito

**Integration Tests**:
- Use Testcontainers for PostgreSQL, Redis
- Use MiniStack for emulated AWS services (S3, SQS, SNS, DynamoDB, SES, EventBridge, Secrets Manager)
- Use @MockBean for unsupported AWS services (CloudWatch)
- Suffix: `*IntegrationTest.java`

**E2E Tests**:
- Playwright for frontend
- Test critical user journeys
- Run in QA environment

**Rule**: Write tests BEFORE implementation (TDD). Integration tests must use Testcontainers/MiniStack.

### 6.3 Test Execution

**Local Development**:
```bash
# Unit tests only
mvn test -Dtest="!*IntegrationTest"

# Integration tests (Docker required)
mvn test -Dtest="*IntegrationTest"

# All tests
mvn test
```

**CI/CD Pipeline**:
1. Lint
2. Unit Tests
3. Build
4. Integration Tests
5. Security Scan

**Rule**: All tests must pass before merge. Docker must be running for integration tests.

---

## 7. CI/CD & Deployment Standards

### 7.1 Deployment Environments

**Environment Mapping**:
- `develop` branch → DEV (801651112319) - Automatic
- `release/*` branches → QA (965932217544) - Automatic
- `main` branch → PROD (811783768245) - Manual approval required

**Rule**: Never deploy to PROD without manual approval from 2+ reviewers.

### 7.2 Deployment Strategy

**DEV**: Rolling deployment, fast iteration
**QA**: Blue-green deployment, integration testing
**PROD**: Blue-green deployment, canary analysis, rollback capability

**Rule**: Always use blue-green deployment for QA and PROD.

### 7.3 Artifact Management

**Docker Images** (Amazon ECR):
- DEV: `dev-latest`, `dev-{commit-sha}`
- QA: `qa-latest`, `qa-{commit-sha}`, `v{version}-rc.{n}`
- PROD: `prod-latest`, `prod-{commit-sha}`, `v{version}`

**Retention**:
- DEV: Last 10 images, 30 days
- QA: Last 20 images, 60 days
- PROD: All tagged releases, indefinite

**Rule**: Always tag images with environment prefix and commit SHA.

### 7.4 Versioning

**Format**: Semantic Versioning `vMAJOR.MINOR.PATCH`

**Increment Rules**:
- MAJOR: Breaking API changes, breaking schema changes
- MINOR: New features, backward-compatible API changes
- PATCH: Bug fixes, security patches, performance improvements

**Rule**: Follow semantic versioning strictly. Update VERSION file and all pom.xml files.

---

## 8. Documentation Standards

### 8.1 Documentation Update Rules

**When to Create New Docs**:
- ✅ New architectural decision → Create ADR in `docs/adr/`
- ✅ New major feature → Create spec in `specs/`
- ✅ New service → Create `specs/{service}-service.md`
- ✅ New operational procedure → Add to `docs/operations/RUNBOOKS.md`

**When to Update Existing Docs**:
- ✅ Refining existing feature
- ✅ Fixing errors or outdated information
- ✅ Adding examples
- ✅ Clarifying procedures
- ✅ Updating status metadata

**Rule**: Minimize duplication. Update existing docs unless scope is vastly different.

### 8.2 Status Metadata (Mandatory)

**All specs and major docs MUST include**:

```markdown
**Last Updated**: YYYY-MM-DD
**Status**: Current | Deprecated | Superseded
**Related Documents**: [links to related docs]
**Authoritative Source**: PROJECT.md (if applicable)
```

**Rule**: Always update "Last Updated" date when modifying documentation.

### 8.3 Cross-Referencing

**Mandatory Cross-References**:
- Specs → Related tasks
- Tasks → Related specs
- Plans → Related specs and tasks
- ADRs → Related specs
- Changelog → PROJECT.md changes

**Rule**: Always add cross-references when creating or updating documentation.

### 8.4 Changelog Requirements

**When to Create Changelog Entry**:
- Modifying PROJECT.md
- Major architectural changes
- Infrastructure restructuring
- Breaking changes

**Format**: `changelog/YYYY-MM-DD-description.md`

**Rule**: Create changelog entry for all PROJECT.md modifications.

---

## 9. Plan Management Standards

### 9.1 Plan Lifecycle

**Creating Plans**:
- Location: `.windsurf/plans/active/`
- Include metadata: Created, Status, Related Plans, Related Docs
- Link to relevant specs and tasks

**During Implementation**:
- Update plan status as work progresses
- Document deviations or issues
- Keep related documentation in sync

**Completing Plans**:
1. Update status to "Completed"
2. Add completion date
3. Move to appropriate completed subdirectory:
   - `completed/infrastructure-restructure/` - Infrastructure
   - `completed/cicd/` - CI/CD
   - `completed/architecture/` - Architecture
   - `completed/assessments/` - Documentation assessments
4. Update related specs and tasks
5. Create ADR if architectural change
6. Create changelog if PROJECT.md changed

**Rule**: ALWAYS move completed plans to appropriate completed/ subdirectory.

### 9.2 Plan Organization

```
.windsurf/plans/
├── active/              # Current work
├── completed/
│   ├── infrastructure-restructure/
│   ├── cicd/
│   ├── architecture/
│   └── assessments/
└── archived/            # Superseded plans
```

**Rule**: Keep active/ directory clean. Move completed plans immediately.

---

## 10. Code Quality Standards

### 10.1 Code Style

**Java**:
- Google Java Style Guide
- Use Lombok for boilerplate reduction
- Use MapStruct for DTO mapping
- Meaningful variable names
- No magic numbers

**TypeScript/Angular**:
- Angular Style Guide
- ESLint + Prettier
- Strict TypeScript mode
- Functional programming patterns where appropriate

**Rule**: All code must pass linting before commit.

### 10.2 Documentation Comments

**Required**:
- All public APIs
- Complex business logic
- Domain entities and value objects
- Event schemas

**Format**:
- Java: Javadoc
- TypeScript: JSDoc

**Rule**: Document all public interfaces and complex logic.

### 10.3 SOLID Principles

**Mandatory**:
- Single Responsibility Principle
- Open/Closed Principle
- Liskov Substitution Principle
- Interface Segregation Principle
- Dependency Inversion Principle

**Rule**: Code reviews must verify SOLID compliance.

---

## 11. Security Standards

### 11.1 Secrets Management

**NEVER**:
- ❌ Commit secrets to Git
- ❌ Hardcode credentials
- ❌ Use long-lived IAM access keys
- ❌ Store secrets in environment variables (use Secrets Manager)

**ALWAYS**:
- ✅ Use AWS Secrets Manager for secrets
- ✅ Use OIDC for GitHub Actions authentication
- ✅ Rotate secrets every 90 days
- ✅ Use least-privilege IAM policies

**Rule**: All secrets MUST be stored in AWS Secrets Manager. Use OIDC for CI/CD.

### 11.2 Security Scanning

**Required Scans**:
- OWASP dependency check
- npm audit (frontend)
- SonarQube security analysis
- Terraform security scan (tfsec)

**Rule**: All security vulnerabilities must be addressed before merge.

### 11.3 Multi-Tenancy Security

**Tenant Isolation**:
- All queries MUST filter by tenantId
- Row-level security in database
- Tenant context in all domain events
- API endpoints validate tenant access

**Rule**: Never allow cross-tenant data access. Always validate tenant ownership.

---

## 12. AI-Assisted Development Rules

### 12.1 Context Gathering (Phase 1)

**MANDATORY WORKFLOW**:
1. Read PROJECT.md for authoritative context (single source of truth)
2. Check relevant specs in specs/
3. Review existing tasks in tasks/
4. Look for patterns in .windsurf/plans/completed/
5. Check ADRs in PROJECT.md § 17 for architectural decisions

**Rule**: ALWAYS gather context before implementation. Use code_search for efficiency.

### 12.2 Consistency Verification (Phase 2)

**Before Implementation, Verify**:
- [ ] Aligns with PROJECT.md
- [ ] Existing specs/tasks checked
- [ ] Related docs identified
- [ ] Completed plans reviewed for patterns
- [ ] No conflicts with authoritative sources

**Rule**: If conflict with PROJECT.md, STOP and ask user how to proceed.

### 12.3 Implementation (Phase 3)

**Follow Patterns**:
- Use completed plans as templates
- Maintain consistency with existing code
- Update related documentation
- Create changelog entry if modifying PROJECT.md

**Rule**: Never deviate from established patterns without explicit approval.

### 12.4 Completion Checklist

**Before Marking Work Complete**:
- [ ] Changes align with PROJECT.md
- [ ] Related specs updated with status metadata
- [ ] Related tasks marked complete
- [ ] Cross-references added
- [ ] Plan moved to completed/
- [ ] ADR added to PROJECT.md § 17 (if architectural change)
- [ ] All tests passing
- [ ] Code coverage maintained/improved

**Rule**: Work is NOT complete until all checklist items verified.

---

## 13. Efficiency Metrics & Goals

### 13.1 Target Improvements

With proper documentation and workflow adherence:

- **Context Gathering**: 60% faster
- **Problem Solving**: 80% faster
- **Error Reduction**: 50% fewer errors
- **Onboarding**: 60% faster

**Rule**: Measure and track these metrics to validate workflow effectiveness.

### 13.2 Key Principles

1. **Documentation First** - Always check docs before asking
2. **Single Source of Truth** - PROJECT.md is authoritative
3. **Follow Patterns** - Use existing patterns from completed plans
4. **Update as You Go** - Keep documentation current
5. **Cross-Reference** - Link related documents
6. **Verify Consistency** - Check alignment across all docs
7. **Create ADRs** - Document architectural decisions
8. **Use Runbooks** - Follow standard procedures
9. **Leverage AI** - Use specific, context-rich prompts
10. **Continuous Improvement** - Regularly assess documentation quality

**Rule**: These principles are non-negotiable. Follow them consistently.

---

## 14. Workflow Integration

### 14.1 Project Workflow Reference

**See**: `.windsurf/workflows/project.md` for detailed workflow guidance

**Key Workflows**:
- Documentation assessment
- Plan lifecycle management
- Task-based implementation
- Testing strategy
- Deployment procedures

**Rule**: Reference project workflow for detailed step-by-step guidance.

### 14.2 Assessment Workflow

**Command**: `@[/project] assess the current workspace documentation`

**Process**:
1. Evaluate documentation quality
2. Create assessment in `docs/assessments/workspace-docs/`
3. Ask if plan should be created
4. Create plan in `.windsurf/plans/assessments/workspace-docs/`
5. Link plan in assessment

**Rule**: No code changes during assessment - documentation evaluation only.

---

## 15. Common Violations to Avoid

### 15.1 Documentation Violations

❌ Creating duplicate documentation instead of updating existing
❌ Not updating status metadata
❌ Missing cross-references
❌ Not creating changelog for PROJECT.md changes
❌ Not moving completed plans to completed/

### 15.2 Architecture Violations

❌ Cross-service database queries
❌ Violating service boundaries
❌ Domain layer depending on infrastructure
❌ Not publishing domain events for state changes
❌ Tight coupling between services

### 15.3 Infrastructure Violations

❌ Hardcoding AWS account IDs
❌ Using wrong IAM role names
❌ Not following DNS naming convention
❌ Creating resources outside Terraform
❌ Using long-lived credentials

### 15.4 Code Quality Violations

❌ Committing without tests
❌ Reducing test coverage
❌ Not following SOLID principles
❌ Missing documentation comments
❌ Hardcoding configuration values

### 15.5 Security Violations

❌ Committing secrets
❌ Not validating tenant access
❌ Skipping security scans
❌ Using overly permissive IAM policies
❌ Not rotating secrets

**Rule**: Code reviews must catch and reject these violations.

---

## 16. Quick Reference

### 16.1 Most Important Documents

| Purpose | Document |
|---------|----------|
| **Everything** | **PROJECT.md** (Single Source of Truth) |
| Service Details | specs/[service]-service.md |
| Implementation Tasks | tasks/ |
| Completed Patterns | .windsurf/plans/completed/ |

### 16.2 Critical Commands

```bash
# Find documentation
grep -r "keyword" docs/ specs/ infrastructure/docs/

# Check documentation index
cat DOCUMENTATION_INDEX.md

# List ADRs
ls -la docs/adr/

# Check completed plans
ls -la .windsurf/plans/completed/

# Run tests
mvn test                              # All tests
mvn test -Dtest="!*IntegrationTest"  # Unit tests only
mvn test -Dtest="*IntegrationTest"   # Integration tests only
```

---

## 17. Enforcement

These rules are **MANDATORY** for all development work on the Turaf platform.

**Violations will result in**:
- PR rejection
- Required rework
- Documentation of the violation
- Update to rules if pattern emerges

**Exceptions**:
- Must be explicitly approved by project owner
- Must be documented in ADR
- Must include rationale

---

**Last Updated**: March 27, 2026  
**Maintained By**: Development Team  
**Review Frequency**: Monthly  
**Related Documents**: 
- [PROJECT.md](../../PROJECT.md) - Single source of truth for all project documentation
- [Project Workflow](../workflows/project.md)
