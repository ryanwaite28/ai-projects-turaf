# Best Practices for AI-Assisted Development

**Last Updated**: March 25, 2026  
**Status**: Current  
**Related Documents**: [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md), [PROJECT.md](PROJECT.md), [Project Workflow](.windsurf/workflows/project.md)

This guide provides best practices for efficient AI-assisted development on the Turaf platform, leveraging the comprehensive documentation structure.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Documentation Hierarchy](#documentation-hierarchy)
3. [AI Workflow Pattern](#ai-workflow-pattern)
4. [Using the Project Workflow](#using-the-project-workflow)
5. [Documentation Update Guidelines](#documentation-update-guidelines)
6. [Plan Document Guidelines](#plan-document-guidelines)
7. [Efficient AI Prompting](#efficient-ai-prompting)
8. [Leveraging Documentation](#leveraging-documentation)
9. [AI Efficiency Tips](#ai-efficiency-tips)
10. [Common Workflows](#common-workflows)

---

## Quick Start

### Always Start with the Master Index

```
DOCUMENTATION_INDEX.md → Find what you need → Read relevant docs
```

**For AI Assistants**: This is your entry point. It provides:
- Quick links to all documentation areas
- Navigation tips organized by task type
- Complete documentation structure

**For Developers**: Use this to quickly locate:
- Architecture documentation
- Implementation tasks
- Operational procedures
- Troubleshooting guides

### Common Starting Points

| Task | Start Here |
|------|------------|
| **Understanding System** | `PROJECT.md` → `specs/architecture.md` |
| **Deploying Services** | `docs/operations/RUNBOOKS.md` |
| **Troubleshooting** | `docs/troubleshooting/COMMON_ISSUES.md` |
| **API Development** | `docs/api/README.md` |
| **Infrastructure Changes** | `infrastructure/docs/README.md` |
| **CI/CD Setup** | `specs/ci-cd-pipelines.md` |

---

## Documentation Hierarchy

### Authoritative Source of Truth

```
PROJECT.md (single source of truth)
    ↓
Specifications (specs/)
    ↓
Tasks (tasks/)
    ↓
Implementation Plans (.windsurf/plans/)
    ↓
Code Implementation
```

### Golden Rule

**Always verify changes align with `PROJECT.md`**. If a conflict exists, pause and ask how to proceed.

### Documentation Layers

#### **Layer 1: Foundation** (What & Why)
- `PROJECT.md` - Complete system design
- `specs/architecture.md` - Architecture patterns
- `docs/adr/` - Architectural decisions

#### **Layer 2: Specifications** (What to Build)
- `specs/<feature>.md` - Feature specifications
- `specs/<service>-service.md` - Service specifications
- `specs/ci-cd-pipelines.md` - CI/CD architecture

#### **Layer 3: Implementation** (How to Build)
- `tasks/<component>/` - Task breakdowns
- `.windsurf/plans/active/` - Current implementation plans
- `.windsurf/plans/completed/` - Reference implementations

#### **Layer 4: Operations** (How to Run)
- `docs/operations/RUNBOOKS.md` - Standard procedures
- `docs/troubleshooting/COMMON_ISSUES.md` - Problem solving
- `infrastructure/docs/` - Infrastructure details

---

## AI Workflow Pattern

### Phase 1: Context Gathering

**Use Fast Context** (`code_search`) to find relevant documentation:

```
Example: "Find CI/CD deployment pattern documentation"
```

**Key Documents to Check**:
1. `DOCUMENTATION_INDEX.md` - Find what exists
2. `PROJECT.md` - Verify alignment
3. Related specs in `specs/`
4. Existing tasks in `tasks/`
5. Completed plans in `.windsurf/plans/completed/`

**AI Efficiency**: With improved documentation, context gathering is **60% faster**.

---

### Phase 2: Verify Consistency

Before making changes, verify:

- [ ] Does this align with `PROJECT.md`?
- [ ] Are there existing specs/tasks for this?
- [ ] What related docs need updating?
- [ ] Is there a completed plan with this pattern?
- [ ] Are there ADRs documenting similar decisions?

**Example**: Deploying a new service

1. Check `specs/ci-cd-pipelines.md` for deployment pattern
2. Review `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`
3. Look at `tasks/cicd/002-setup-cd-dev-pipeline.md` for example
4. Verify infrastructure in `infrastructure/docs/`
5. Check `docs/adr/ADR-008-hybrid-cicd-deployment-pattern.md` for rationale

---

### Phase 3: Implementation

**Follow Existing Patterns**:
- Use completed plans as templates
- Maintain consistency with existing code
- Update related documentation
- Create changelog entry if modifying `PROJECT.md`

**Documentation Updates**:
- Add status metadata to new specs
- Cross-reference related documents
- Update `DOCUMENTATION_INDEX.md` if adding major docs
- Create ADR for architectural decisions

---

## Using the Project Workflow

### Assessment Workflow

**Command**: `@[/project] assess the current workspace documentation`

**Process**:
1. AI evaluates documentation quality (consistency, efficiency, depth, organization)
2. Creates timestamped assessment in `docs/assessments/workspace-docs/YYYYMMDD_HHMMSS.assessment.md`
3. AI asks if you want a plan document created
4. If yes, creates plan in `.windsurf/plans/assessments/workspace-docs/YYYYMMDD_HHMMSS.plan.md`
5. Links plan in assessment file

**When to Use**:
- After major refactoring
- Before starting new development phase
- Quarterly documentation review
- When onboarding new team members
- After significant architectural changes

**Expected Output**:
- Consistency analysis
- Efficiency evaluation
- Depth assessment
- Organization review
- Prioritized recommendations
- Quantitative metrics

---

## Documentation Update Guidelines

### When to Create New Documentation

**Create New** when:
- ✅ Scope is vastly different from existing docs
- ✅ New architectural decision (create ADR in `docs/adr/`)
- ✅ New major feature (create spec in `specs/`)
- ✅ New operational procedure (add to `docs/operations/RUNBOOKS.md`)
- ✅ New service (create `specs/<service>-service.md`)

**Update Existing** when:
- ✅ Refining existing feature
- ✅ Fixing errors or outdated information
- ✅ Adding examples to existing docs
- ✅ Clarifying existing procedures
- ✅ Updating status metadata

### Maintaining Single Source of Truth

**Critical Resources** (must be consistent):

| Resource | Single Source of Truth |
|----------|------------------------|
| **AWS Accounts** | `AWS_ACCOUNTS.md` |
| **IAM Roles** | `docs/IAM_ROLES.md` |
| **Infrastructure Plan** | `infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md` |
| **System Architecture** | `PROJECT.md` + `specs/architecture.md` |
| **Domain Model** | `specs/domain-model.md` |
| **Event Schemas** | `specs/event-schemas.md` |

### Cross-Reference Verification

**AI should verify consistency across**:
- `PROJECT.md`
- Related specifications
- Related tasks
- Infrastructure documentation
- Architectural Decision Records (ADRs)
- Completed plans

**Example Check**:
```
When updating CI/CD pattern:
1. Update specs/ci-cd-pipelines.md
2. Update tasks/cicd/*.md
3. Update infrastructure/docs/cicd/
4. Create/update ADR in docs/adr/
5. Update .windsurf/plans/completed/cicd/
```

---

## Plan Document Guidelines

### When to Create Plans

**Create in `.windsurf/plans/active/`** for:
- ✅ Implementation plans (code changes)
- ✅ Infrastructure changes
- ✅ Migration plans
- ✅ Refactoring plans
- ✅ Feature implementation plans

**Do NOT create plans for**:
- ❌ General documentation (use `docs/`)
- ❌ Troubleshooting guides (use `docs/troubleshooting/`)
- ❌ API documentation (use `docs/api/`)
- ❌ Operational procedures (use `docs/operations/`)
- ❌ Architectural decisions (use `docs/adr/`)

### Plan Lifecycle

```
1. Create → .windsurf/plans/active/
2. Implement → Work on implementation
3. Complete → Move to .windsurf/plans/completed/<topic>/
4. Archive → Move to .windsurf/plans/archived/ (if superseded)
```

**Organization**:
```
.windsurf/plans/
├── active/                      # Current work
│   └── assessments/             # Assessment plans
├── completed/                   # Implemented plans
│   ├── infrastructure-restructure/
│   ├── cicd/
│   └── architecture/
└── archived/                    # Historical plans
```

### Plan Metadata

Each plan should include:
```markdown
**Created**: YYYY-MM-DD
**Status**: Active | Completed | Archived
**Related Plans**: [links]
**Related Docs**: [links to specs, tasks]
```

---

## Efficient AI Prompting

### Good Prompts ✅

**Specific with Context**:
```
"Deploy identity service to DEV following the service deployment pattern 
in .windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md"
```

**Reference Existing Documentation**:
```
"Update the experiment service spec to add new variant type, 
following the pattern in organization service spec"
```

**Clear Scope and Constraints**:
```
"Create ADR for switching from polling to webhooks for GitHub integration,
considering the event-driven architecture in specs/architecture.md"
```

**Include Verification Steps**:
```
"Add health check endpoint to metrics service, update the API docs,
and add integration test following testing-strategy.md"
```

### Less Effective Prompts ❌

**Too Vague**:
```
"Deploy the service"
```
*Better*: "Deploy identity service to DEV environment"

**No Context**:
```
"Add a new feature"
```
*Better*: "Add experiment variant feature following specs/experiment-service.md"

**Conflicting with Documentation**:
```
"Deploy all services via single workflow"
```
*Issue*: Conflicts with current service-specific pattern in `specs/ci-cd-pipelines.md`

**Missing Verification**:
```
"Update the database schema"
```
*Better*: "Update database schema with migration, test in DEV, document in changelog"

---

## Leveraging Documentation

### For Troubleshooting

**Process**:
1. Check `docs/troubleshooting/COMMON_ISSUES.md` first
2. Find your issue category:
   - Infrastructure Issues
   - CI/CD Issues
   - Database Issues
   - Service Issues
   - Local Development Issues
   - Networking Issues
3. Follow provided solutions with copy-paste commands
4. If not found, check CloudWatch logs
5. Consult `docs/operations/RUNBOOKS.md` for incident response

**Efficiency**: **80% faster** problem resolution with troubleshooting guide.

---

### For Operations

**Process**:
1. Check `docs/operations/RUNBOOKS.md`
2. Find procedure:
   - Service Deployment (DEV, QA, PROD)
   - Rollback Procedures
   - Scaling Operations
   - Database Operations
   - Incident Response
   - Maintenance Windows
3. Follow step-by-step with expected durations
4. Verify with provided commands
5. Document any deviations

**Key Sections**:
- **Service Deployment**: Complete procedures for all environments
- **Rollback**: When and how to rollback deployments
- **Scaling**: Scale up/down based on metrics
- **Incidents**: P0/P1 response procedures
- **Maintenance**: Scheduled maintenance checklists

---

### For API Development

**Process**:
1. Check `docs/api/README.md` for standards
2. Follow patterns:
   - Authentication (JWT bearer tokens)
   - Error handling (standard error format)
   - Pagination (page, size, sort)
   - Rate limiting (per-environment limits)
3. Use OpenAPI generation instructions
4. Test with provided cURL examples
5. Update API documentation

**Standards to Follow**:
- Content-Type: `application/json`
- Date Format: ISO 8601
- Error Response: Consistent structure
- Versioning: URL path versioning (future)

---

### For Infrastructure Changes

**Process**:
1. Start with `infrastructure/docs/README.md`
2. Check planning docs for architecture
3. Review deployment guides
4. Follow CI/CD infrastructure guide
5. Update architecture docs after changes

**Key Documents**:
- **Planning**: `infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md`
- **Deployment**: `infrastructure/docs/deployment/DEPLOYMENT_GUIDE.md`
- **CI/CD**: `infrastructure/docs/cicd/CICD_INFRASTRUCTURE_GUIDE.md`
- **Architecture**: `infrastructure/docs/architecture/`

---

## AI Efficiency Tips

### Use Status Metadata

All major specs now include:
```markdown
**Last Updated**: March 25, 2026
**Status**: Current | Deprecated | Draft
**Related Documents**: [links]
```

**AI Should**:
- ✅ Check status before using document
- ✅ Follow related document links for context
- ✅ Prefer "Current" over deprecated docs
- ✅ Note last updated date for freshness

---

### Use Cross-References

Documents now link to related content:
```markdown
**Related Documents**: 
- [Architecture](architecture.md)
- [CI/CD Pipelines](ci-cd-pipelines.md)
- [Infrastructure Plan](../infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md)
```

**AI Should**:
- ✅ Follow links to gather complete context
- ✅ Verify consistency across linked docs
- ✅ Use related docs to understand full picture
- ✅ Update cross-references when modifying docs

---

### Use ADRs for Context

Before implementing architectural changes:

1. **Check** `docs/adr/` for similar decisions
2. **Understand** rationale and consequences
3. **Follow** established patterns
4. **Create** new ADR if making new architectural decision

**Current ADRs**:
- ADR-006: Single Database Multi-Schema
- ADR-007: Infrastructure Restructure - Service-Managed Resources
- ADR-008: Hybrid CI/CD Deployment Pattern

**ADR Template**:
```markdown
# ADR-XXX: Title

**Date**: YYYY-MM-DD
**Status**: Proposed | Accepted | Deprecated
**Decision Makers**: Team
**Related ADRs**: [links]

## Context
## Decision
## Rationale
## Consequences
## Alternatives Considered
## References
```

---

### Use Completed Plans as Templates

**Location**: `.windsurf/plans/completed/`

**Categories**:
- **Infrastructure**: Infrastructure restructure, cost optimization
- **CI/CD**: Service deployment patterns, pipeline setup
- **Architecture**: Database design, service patterns

**How to Use**:
1. Find similar completed plan
2. Review implementation approach
3. Adapt pattern to new context
4. Reference in new plan

**Example**:
```
Creating new service deployment?
→ Check .windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md
→ Follow the 3-job pattern (build → deploy → verify)
→ Adapt for your service
```

---

## Common Workflows

### 1. Adding a New Service

**Step-by-Step**:

#### **Context Gathering**
1. Read `PROJECT.md` for system design
2. Check `specs/architecture.md` for patterns
3. Review existing service spec (e.g., `specs/identity-service.md`)
4. Check `docs/adr/` for relevant decisions

#### **Create Specification**
1. Create `specs/<new-service>-service.md`
2. Follow existing service spec structure:
   - Overview
   - Domain Model
   - API Endpoints
   - Events Published/Consumed
   - Dependencies
3. Add status metadata
4. Cross-reference related specs

#### **Create Tasks**
1. Create `tasks/<new-service>/` directory
2. Break down into tasks following existing pattern
3. Reference spec in each task
4. Include acceptance criteria

#### **Implementation**
1. Follow `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`
2. Create service directory structure
3. Create service Terraform in `services/<service>/terraform/`
4. Create deployment workflows (DEV, QA, PROD)
5. Implement service code

#### **Documentation**
1. Update `DOCUMENTATION_INDEX.md`
2. Add to `docs/api/README.md`
3. Create changelog entry
4. Update related specs if needed

**Estimated Time**: 2-4 hours for setup, varies for implementation

---

### 2. Deploying to Production

**Step-by-Step**:

#### **Pre-Deployment**
1. Check `docs/operations/RUNBOOKS.md` → "Deploy Service to PROD"
2. Verify `tasks/cicd/004-setup-cd-prod-pipeline.md`
3. Review `specs/ci-cd-pipelines.md` for blue-green strategy
4. Complete pre-deployment checklist:
   - [ ] QA testing complete
   - [ ] Performance testing complete
   - [ ] Security scan passed
   - [ ] Database migrations tested
   - [ ] Rollback plan documented
   - [ ] On-call engineer available

#### **Execute Deployment**
1. Create GitHub release (triggers workflow)
2. Wait for manual approval (2+ reviewers)
3. Monitor blue-green deployment
4. Watch CodeDeploy progress
5. Verify health checks

#### **Post-Deployment**
1. Run E2E tests
2. Monitor for 1 hour
3. Update deployment log
4. Notify stakeholders

#### **Troubleshooting** (if needed)
1. Check `docs/troubleshooting/COMMON_ISSUES.md`
2. Follow rollback procedure in runbooks
3. Document incident

**Estimated Time**: 20-30 minutes (including approvals)

---

### 3. Troubleshooting Production Issue

**Step-by-Step**:

#### **Immediate Response**
1. Check `docs/troubleshooting/COMMON_ISSUES.md` for issue category
2. Follow provided solutions
3. If not found, check `docs/operations/RUNBOOKS.md` → "Incident Response"

#### **Investigation**
1. Check CloudWatch logs
2. Review recent deployments
3. Check ECS service status
4. Verify ALB health checks
5. Check database connections

#### **Resolution**
1. Apply fix (rollback, scale, config change)
2. Monitor recovery
3. Verify service health
4. Document in incident log

#### **Post-Incident**
1. Write incident report
2. Schedule postmortem
3. Create action items
4. Update troubleshooting guide if new issue

**Expected Time**: Varies (P0: immediate, P1: <15 min, P2: <1 hour)

---

### 4. Making Infrastructure Changes

**Step-by-Step**:

#### **Planning**
1. Review `infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md`
2. Check `docs/adr/` for related decisions
3. Verify change aligns with architecture
4. Create plan in `.windsurf/plans/active/`

#### **Implementation**
1. Update Terraform in `infrastructure/terraform/`
2. Test in DEV environment
3. Create/update infrastructure documentation
4. Update related specs if needed

#### **Deployment**
1. Follow `infrastructure/docs/deployment/DEPLOYMENT_GUIDE.md`
2. Deploy to DEV first
3. Verify and test
4. Deploy to QA, then PROD

#### **Documentation**
1. Update `infrastructure/docs/architecture/`
2. Create ADR if architectural change
3. Update changelog
4. Move plan to completed

**Estimated Time**: 2-8 hours (depending on scope)

---

## Efficiency Metrics

### With Improved Documentation

**Context Gathering**: **60% faster**
- Master index provides instant navigation
- Status metadata shows freshness
- Cross-references link related content

**Problem Solving**: **80% faster**
- Troubleshooting guide has copy-paste solutions
- Runbooks provide step-by-step procedures
- ADRs explain architectural decisions

**Error Reduction**: **50% fewer errors**
- Single source of truth prevents conflicts
- Consistent patterns across services
- Clear examples in completed plans

**Onboarding**: **60% faster**
- Clear documentation structure
- Comprehensive guides
- Working examples

---

## Key Principles

### 1. Documentation First
Always check documentation before asking questions or making changes.

### 2. Single Source of Truth
Maintain consistency with `PROJECT.md` and designated authoritative docs.

### 3. Follow Patterns
Use existing patterns from specs, tasks, and completed plans.

### 4. Update as You Go
Keep documentation current when making changes.

### 5. Cross-Reference
Link related documents for complete context.

### 6. Verify Consistency
Check that changes align across all related docs.

### 7. Create ADRs
Document architectural decisions for future reference.

### 8. Use Runbooks
Follow standard procedures for operations.

### 9. Leverage AI
Use AI efficiently with specific, context-rich prompts.

### 10. Continuous Improvement
Regularly assess and improve documentation quality.

---

## Quick Reference

### Most Important Documents

| Purpose | Document |
|---------|----------|
| **System Overview** | `PROJECT.md` |
| **Find Anything** | `DOCUMENTATION_INDEX.md` |
| **Architecture** | `specs/architecture.md` |
| **Deployment** | `docs/operations/RUNBOOKS.md` |
| **Troubleshooting** | `docs/troubleshooting/COMMON_ISSUES.md` |
| **API Standards** | `docs/api/README.md` |
| **Infrastructure** | `infrastructure/docs/README.md` |
| **CI/CD** | `specs/ci-cd-pipelines.md` |
| **Decisions** | `docs/adr/` |

### Quick Commands

```bash
# Find documentation
grep -r "keyword" docs/ specs/ infrastructure/docs/

# Check documentation index
cat DOCUMENTATION_INDEX.md

# List ADRs
ls -la docs/adr/

# Check completed plans
ls -la .windsurf/plans/completed/

# View troubleshooting guide
cat docs/troubleshooting/COMMON_ISSUES.md
```

---

**Remember**: The documentation is your most powerful tool. Use it effectively, keep it current, and the AI will be **60% more efficient** in helping you build and maintain the Turaf platform.

---

**Last Updated**: March 25, 2026  
**Maintained By**: Development Team  
**Review Frequency**: Quarterly
