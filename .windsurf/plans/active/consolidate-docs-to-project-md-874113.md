# Consolidate All Documentation into PROJECT.md

Merge all project documentation into a single authoritative `PROJECT.md`, archive stale/historical files, update the workflow and rules to enforce the single-doc standard, and reconcile specs/tasks with the consolidated content.

---

## Inventory Summary

**Current PROJECT.md**: ~2713 lines (37 sections covering architecture, domain, AWS, testing, etc.)

### Files to Consolidate INTO PROJECT.md

| File | Size | Action |
|------|------|--------|
| `GITHUB.md` | 637 lines | Merge (CI/CD, workflows, branch rules, OIDC roles) |
| `README-DEPLOYMENT.md` | 559 lines | Merge (deployment guide, local dev, monitoring) |
| `AWS_ACCOUNTS.md` | 19 lines | Merge (already partially in PROJECT.md; merge ARNs) |
| `BEST_PRACTICES.md` | 779 lines | Merge (AI workflow patterns, documentation guidelines) |
| `DOCUMENTATION_INDEX.md` | 292 lines | Delete after consolidation (no longer needed) |
| `docs/AWS_MULTI_ACCOUNT_STRATEGY.md` | ~14KB | Merge (multi-account rationale, SCPs, cross-account) |
| `docs/DATABASE_SETUP.md` | ~12KB | Merge (operational DB guide, migration, local setup) |
| `docs/DEPLOYMENT_RUNBOOK.md` | ~15KB | Merge (runbook procedures) |
| `docs/IAM_ROLES.md` | ~11KB | Merge (all IAM role names, ARNs, policies) |
| `docs/LOCAL_DEVELOPMENT.md` | ~22KB | Merge (local dev setup, Docker Compose, troubleshooting) |
| `docs/common-module-design.md` | ~19KB | Merge (shared module patterns) |
| `docs/operations/RUNBOOKS.md` | ~15KB | Merge (operational runbooks) |
| `docs/troubleshooting/COMMON_ISSUES.md` | ~14KB | Merge (troubleshooting guide) |
| `docs/adr/ADR-006-single-database-multi-schema.md` | ~8KB | Inline into ADR section |
| `docs/adr/ADR-007-infrastructure-restructure-service-managed-resources.md` | ~8KB | Inline into ADR section |
| `docs/adr/ADR-008-hybrid-cicd-deployment-pattern.md` | ~10KB | Inline into ADR section |
| `infrastructure/dns-config.md` | small | Merge DNS details |
| `infrastructure/acm-certificates.md` | small | Merge certificate details |
| `infrastructure/ecr-repositories.md` | small | Merge ECR config |
| `infrastructure/ses-configuration.md` | small | Merge SES config |
| `infrastructure/email-forwarding-config.md` | small | Merge email config |
| `infrastructure/github-configuration.md` | small | Merge GitHub infra config |
| `infrastructure/github-oidc-roles.md` | small | Merge OIDC role details |
| `infrastructure/terraform-state-backends.md` | small | Merge state backend config |
| `infrastructure/apply-iam-policy-updates.md` | small | Merge IAM procedures |
| `infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md` | med | Merge infrastructure plan |
| `infrastructure/docs/planning/INFRASTRUCTURE_COSTS.md` | med | Merge (dedupe with existing §57) |
| `infrastructure/docs/deployment/DEPLOYMENT_GUIDE.md` | med | Merge deployment guide |
| `infrastructure/docs/deployment/DEV_INFRASTRUCTURE_STATUS.md` | med | Merge current state |
| `infrastructure/docs/deployment/DEPLOYMENT_SUMMARY.md` | med | Merge deployment summary |
| `infrastructure/docs/deployment/DEPLOYED_INFRASTRUCTURE.md` | med | Merge resource inventory |
| `infrastructure/docs/cicd/CICD_INFRASTRUCTURE_GUIDE.md` | med | Merge CI/CD prereqs |
| `infrastructure/docs/cicd/CICD_PREREQUISITES_STATUS.md` | med | Merge CI/CD status |
| `infrastructure/docs/architecture/COMPUTE_INFRASTRUCTURE_ANALYSIS.md` | med | Merge compute analysis |
| `infrastructure/docs/architecture/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md` | med | Merge restructure summary |
| `infrastructure/docs/README.md` | small | Delete (index file, no longer needed) |
| `infrastructure/docker/ministack/IMPLEMENTATION_SUMMARY.md` | med | Merge MiniStack details |
| `infrastructure/docker/ministack/LAMBDA_DEPLOYMENT_GUIDE.md` | med | Merge Lambda local guide |
| `infrastructure/docker/ministack/README.md` | small | Keep (co-located tool README) |

### Files to Archive (→ `docs/archive/`)

| File | Reason |
|------|--------|
| `docs/IMPLEMENTATION_SUMMARY.md` | Historical implementation record |
| `docs/INFRASTRUCTURE_ALIGNMENT_SUMMARY.md` | Transient alignment work record |
| `docs/api/api-discrepancy-report.md` | Past audit report |
| `docs/api/api-fixes-implementation-summary.md` | Past fix record |
| `docs/api/README.md` | API docs index (no longer needed) |
| `docs/assessments/*` | Past assessment reports |
| `docs/audits/*` | Past audit reports |
| `docs/archive/*` (existing) | Already archived |
| `infrastructure/TASK_027_NETWORK_ACCESS.md` | Completed task artifact |
| `infrastructure/TASK_028_CODEBUILD_PROJECTS.md` | Completed task artifact |
| `infrastructure/TASK_028_TEST_RESULTS.md` | Completed task artifact |
| `infrastructure/scripts/archive/` | Already archived |
| `changelog/*` | Historical changelog entries |
| `reports/specs-tasks-evaluation-2024-03-23.md` | Past evaluation report |

### Files to Keep As-Is

| File | Reason |
|------|--------|
| `README.md` | GitHub landing page (rewrite as stub → PROJECT.md) |
| `frontend/README.md` | Co-located frontend README |
| `infrastructure/terraform/modules/*/README.md` | Co-located module READMEs (8 files) |
| `infrastructure/docker/ministack/README.md` | Co-located tool README |
| `infrastructure/scps/README.md` | Co-located SCP README |
| `infrastructure/scripts/README.md` | Co-located scripts README |
| `infrastructure/scripts/FLYWAY_IAM_README.md` | Co-located Flyway README |
| `specs/*` | Remain as detailed specs (reconcile after) |
| `tasks/*` | Remain as task tracking (reconcile after) |

---

## Implementation Phases

### Phase 1: Read and Catalog All Content (~30 files)

Read every file being consolidated. Build a deduplication map identifying overlapping content between files and existing PROJECT.md sections. Key dedup areas:
- AWS account info (in PROJECT.md §2, AWS_ACCOUNTS.md, rules.md, GITHUB.md)
- Infrastructure costs (PROJECT.md §57 vs infrastructure/docs/planning/INFRASTRUCTURE_COSTS.md)
- Database architecture (PROJECT.md §27 vs docs/DATABASE_SETUP.md)
- CI/CD info (PROJECT.md §14/§31 vs GITHUB.md vs specs/ci-cd-pipelines.md)
- Deployment info (README-DEPLOYMENT.md vs docs/DEPLOYMENT_RUNBOOK.md vs infrastructure/docs/deployment/)

### Phase 2: Restructure PROJECT.md Table of Contents

Design a clean, navigable section structure. Proposed outline:

```
# Turaf Platform — Authoritative Project Document

## Part I: Project Overview
  1. Overview & Objectives
  2. Product Concept
  3. Skills Demonstrated
  4. Success Criteria

## Part II: Architecture & Design
  5. System Architecture
  6. Domain Model (DDD)
  7. Service Boundaries
  8. Clean Architecture Structure
  9. Event-Driven Architecture
  10. Event Schemas & Standards
  11. Multi-Tenant Architecture
  12. Network Architecture (Dual ALB)
  13. Data Architecture (Single DB, Multi-Schema)

## Part III: AWS Infrastructure
  14. AWS Account Architecture (Multi-Account)
  15. Domain & DNS Architecture
  16. VPC & Networking
  17. Compute (ECS Fargate)
  18. Database (RDS PostgreSQL)
  19. Messaging (EventBridge, SQS)
  20. Storage (S3, ECR)
  21. Serverless (Lambda)
  22. Security (IAM Roles, KMS, Security Groups)
  23. SSL/TLS & Certificates
  24. Email (SES)
  25. Monitoring & Observability
  26. Infrastructure Costs
  27. Terraform Structure

## Part IV: CI/CD & DevOps
  28. GitHub Repository & Branch Strategy
  29. GitHub Actions Workflows
  30. OIDC Authentication
  31. Service Deployment Pattern
  32. Database Migration Pipeline (Flyway)
  33. Frontend Deployment
  34. Environment Promotion

## Part V: Local Development
  35. Prerequisites & Setup
  36. Docker Compose Configuration
  37. MiniStack (Local AWS Emulation)
  38. Running Services Locally
  39. Troubleshooting & Common Issues

## Part VI: Testing Strategy
  40. Testing Pyramid
  41. Unit Testing
  42. Integration Testing (Testcontainers + MiniStack)
  43. Architecture Testing (Karate)
  44. Test Coverage Requirements
  45. CI/CD Test Execution

## Part VII: Operational Procedures
  46. Deployment Runbook
  47. Database Operations
  48. Rollback Procedures
  49. Incident Response

## Part VIII: Engineering Standards
  50. SOLID Principles
  51. Domain-Driven Design
  52. Coding Standards
  53. Common Module Design Patterns
  54. AI Agent Development Instructions

## Part IX: Architecture Decision Records
  55. ADR-006: Single Database, Multi-Schema
  56. ADR-007: Infrastructure Restructure
  57. ADR-008: Hybrid CI/CD Deployment Pattern

## Part X: Development Phases & Roadmap
  58. Development Phases
  59. Long-Term Extensions
```

### Phase 3: Build Consolidated PROJECT.md

For each section in the new outline:
1. Start with existing PROJECT.md content for that topic
2. Merge in additional detail from the source files (deduplicated)
3. Resolve any conflicting information (prefer most recent / most accurate)
4. Remove cross-file references (e.g., "see AWS_ACCOUNTS.md") — replace with internal section links

**Deduplication rules**:
- If PROJECT.md already covers a topic adequately, only add net-new details from other files
- If a separate doc is more detailed/accurate, replace the PROJECT.md section with that content
- Never include the same information twice

**Estimated final size**: ~4,000-6,000 lines (substantial but navigable with ToC)

### Phase 4: Archive Stale Files

Move all stale/historical files to `docs/archive/` with subdirectories:
- `docs/archive/audits/` — audit reports
- `docs/archive/assessments/` — assessment reports
- `docs/archive/api-reports/` — API discrepancy/fix reports
- `docs/archive/changelog/` — old changelog entries
- `docs/archive/implementation-summaries/` — old summaries
- `docs/archive/infrastructure-tasks/` — completed infra task artifacts

### Phase 5: Delete Consolidated Source Files

After content is merged, delete the now-redundant source files:
- Root: `AWS_ACCOUNTS.md`, `GITHUB.md`, `README-DEPLOYMENT.md`, `BEST_PRACTICES.md`, `DOCUMENTATION_INDEX.md`
- `docs/`: All non-archive files that were merged
- `infrastructure/`: All standalone .md docs that were merged (not module READMEs)

### Phase 6: Update README.md as Stub

Rewrite `README.md` to be a brief GitHub landing page:
```markdown
# Turaf — Event-Driven SaaS Platform
### Try Until Results Are Found

See **[PROJECT.md](PROJECT.md)** for complete project documentation.
```

### Phase 7: Update Windsurf Workflow & Rules

**`.windsurf/workflows/project.md`**:
- Remove references to BEST_PRACTICES.md, DOCUMENTATION_INDEX.md, AWS_ACCOUNTS.md, GITHUB.md
- Update all "Required Reading" to just PROJECT.md + rules.md
- Update documentation guidelines: "Do not create new documentation files. All documentation lives in PROJECT.md."
- Remove changelog workflow (no longer needed — PROJECT.md is versioned by git)
- Simplify consistency verification to just PROJECT.md alignment

**`.windsurf/rules/rules.md`**:
- Update §1.1 authoritative sources to just PROJECT.md (single source of truth)
- Remove references to deleted files
- Add rule: "Never create new .md documentation files. Update PROJECT.md instead."
- Update §1.3 mandatory checks to reference PROJECT.md sections

### Phase 8: Reconcile Specs with PROJECT.md

For each spec in `specs/`:
- Verify it aligns with the consolidated PROJECT.md content
- Fix any conflicting information (PROJECT.md wins)
- Update cross-references to point to PROJECT.md sections instead of deleted docs
- Key areas to check:
  - `specs/aws-infrastructure.md` — must match consolidated AWS sections
  - `specs/ci-cd-pipelines.md` — must match consolidated CI/CD sections
  - `specs/identity-service.md` — must match user model (username/firstName/lastName)
  - `specs/bff-api.md` — must match consolidated API/deployment info
  - `specs/testing-strategy.md` — must match consolidated testing sections

### Phase 9: Reconcile Tasks with PROJECT.md

- Update `tasks/TASK_SUMMARY.md` to reference PROJECT.md instead of deleted docs
- Verify task descriptions align with consolidated content
- Remove any references to deleted documentation files

---

## Success Criteria

- [ ] Single `PROJECT.md` contains all project documentation
- [ ] No duplicate information across remaining files
- [ ] `README.md` is a stub pointing to `PROJECT.md`
- [ ] Stale/historical files archived in `docs/archive/`
- [ ] All consolidated source files deleted
- [ ] `.windsurf/workflows/project.md` updated for single-doc standard
- [ ] `.windsurf/rules/rules.md` updated for single-doc standard
- [ ] All specs aligned with PROJECT.md (no conflicts)
- [ ] All tasks aligned with PROJECT.md (no conflicts)
- [ ] No broken cross-references in remaining files
- [ ] Co-located READMEs (frontend, terraform modules, scripts) untouched

## Risks

- **PROJECT.md size**: ~5000+ lines. Mitigated by clear ToC with numbered sections.
- **Git diff noise**: One large commit. Mitigated by doing archive moves and deletes in separate commits if desired.
- **Spec drift post-consolidation**: Mitigated by Phase 8 reconciliation and updated rules preventing new doc files.
