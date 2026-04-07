# Resolve Deployment Gaps — All Environments

Close all infrastructure, CI/CD, and service packaging gaps so that DEV, QA, and PROD environments can receive deployments via GitHub Actions.

**Created**: 2026-04-06  
**Status**: Proposed  
**Related Docs**: PROJECT.md, specs/ci-cd-pipelines.md, specs/frontend-deployment.md, specs/infrastructure-prerequisites.md, specs/terraform-structure.md  
**Estimated Files**: ~60 new/modified

---

## Current State Summary

| Layer | DEV | QA | PROD |
|-------|-----|-----|------|
| Shared Infra Terraform | ⚠️ Monitoring commented out, outputs commented out | ❌ Old monolithic compute pattern | ❌ Old monolithic compute pattern |
| Terraform tfvars | ❌ 76+ lint errors | ❌ Likely similar | ❌ Likely similar |
| Service Dockerfiles | ❌ Only 2 of 9 services | Same | Same |
| Per-service Terraform | ❌ None exist | Same | Same |
| GH Actions workflows | ⚠️ Infra workflow broken, WS Gateway + Frontend missing | Same | Same |
| Frontend hosting | ❌ No Terraform, no workflow, no Dockerfile | Same | Same |
| V016 migration | ❌ Pending | Same | Same |

### Critical Finding

QA and PROD `main.tf` use the **old monolithic compute module** (inline service images, per-service CPU/memory, autoscaling). DEV was already updated to the **new hybrid pattern** (ADR-008) where the compute module only creates shared infra (ECS cluster, ALBs) and services manage their own resources via CI/CD. **QA/PROD must be aligned to DEV's pattern.**

---

## Phase 1: Align QA/PROD Environments to Hybrid Pattern

**Priority**: 🔴 Critical — blocks everything downstream  
**Files**: 4 modified

| # | Action | File |
|---|--------|------|
| 1a | Rewrite QA `main.tf` to match DEV hybrid pattern (remove inline service config from compute module) | `infrastructure/terraform/environments/qa/main.tf` |
| 1b | Rewrite PROD `main.tf` to match DEV hybrid pattern | `infrastructure/terraform/environments/prod/main.tf` |
| 1c | Update QA `variables.tf` — remove old service-specific vars, ensure parity with DEV | `infrastructure/terraform/environments/qa/variables.tf` |
| 1d | Update PROD `variables.tf` — same cleanup | `infrastructure/terraform/environments/prod/variables.tf` |

**Approach**: Use DEV `main.tf` as the template. Adapt for env-specific defaults (e.g., PROD multi-AZ, NAT gateway enabled).

---

## Phase 2: Fix Terraform tfvars Lint Errors

**Priority**: 🔴 Critical — blocks `terraform plan`  
**Files**: 3 modified (tfvars files are gitignored, so also update `.tfvars.example` files)

The lint errors report variables in `.tfvars` that don't exist in `variables.tf`:
- `ecs_task_cpu`, `ecs_task_memory` — old monolithic vars, remove from tfvars
- `enable_lambda_reporting`, `enable_lambda_notification`, `lambda_*_memory` — rename to match actual variable names
- `enable_eventbridge`, `enable_sqs` — not actual variables, remove
- `enable_dashboards` → should be `enable_dashboard`
- `alarm_evaluation_periods`, `alarm_period` — these are monitoring module vars, not env-level vars (monitoring is disabled in DEV)
- `s3_lifecycle_enabled` — doesn't exist, remove
- `domain_name`, `project_name`, `aws_account_id` — not declared, either add or remove

| # | Action | File |
|---|--------|------|
| 2a | Audit DEV tfvars.example, remove/rename mismatched vars | `environments/dev/terraform.tfvars.example` |
| 2b | Audit QA tfvars.example | `environments/qa/terraform.tfvars.example` |
| 2c | Audit PROD tfvars.example | `environments/prod/terraform.tfvars.example` |

**Note**: Actual `.tfvars` are gitignored. We fix the `.example` files and document what the user needs to update locally.

---

## Phase 3: Uncomment DEV Outputs and Monitoring

**Priority**: 🟡 High — service Terraform needs these outputs  
**Files**: 2 modified

| # | Action | File |
|---|--------|------|
| 3a | Uncomment compute, messaging, lambda, monitoring outputs in DEV | `environments/dev/outputs.tf` |
| 3b | Uncomment monitoring module in DEV main.tf (all features default to disabled, so it's safe) | `environments/dev/main.tf` |

---

## Phase 4: Create Dockerfiles for All Services

**Priority**: 🔴 Critical — no images = no deployment  
**Files**: 7 new

**Already have Dockerfiles**: communications-service, ws-gateway  
**Need Dockerfiles**:

| # | Service | Runtime | Port | File |
|---|---------|---------|------|------|
| 4a | identity-service | Java 21 + Spring Boot | 8080 | `services/identity-service/Dockerfile` |
| 4b | organization-service | Java 21 + Spring Boot | 8081 | `services/organization-service/Dockerfile` |
| 4c | experiment-service | Java 21 + Spring Boot | 8082 | `services/experiment-service/Dockerfile` |
| 4d | metrics-service | Java 21 + Spring Boot | 8083 | `services/metrics-service/Dockerfile` |
| 4e | bff-api | Java 21 + Spring Boot | 8090 | `services/bff-api/Dockerfile` |
| 4f | flyway-service | Java 21 + Flyway CLI | N/A | `services/flyway-service/Dockerfile` |
| 4g | reporting-service | Python 3.12 (Lambda) | N/A | `services/reporting-service/Dockerfile` (optional — Lambda uses zip) |

**Template** (Java services): Multi-stage — Maven build → Eclipse Temurin JRE 21 runtime. Include health check, non-root user, proper signal handling.

---

## Phase 5: Create Per-Service Terraform

**Priority**: 🔴 Critical — CI/CD workflows depend on `services/<svc>/terraform/`  
**Files**: ~35 new (5 files × 7 services)

Each service gets:
```
services/<service>/terraform/
├── backend.tf       # S3 remote state
├── data.tf          # Reference shared infra outputs
├── main.tf          # ECS task def, service, target group, listener rule, log group
├── variables.tf     # environment, service_name, image_tag, cpu, memory, container_port, etc.
└── outputs.tf       # service_url, task_definition_arn
```

**Services** (ECS/Fargate):

| # | Service | Container Port | ALB Priority | Health Path |
|---|---------|---------------|--------------|-------------|
| 5a | identity-service | 8080 | 100 | `/actuator/health` |
| 5b | organization-service | 8081 | 200 | `/actuator/health` |
| 5c | experiment-service | 8082 | 300 | `/actuator/health` |
| 5d | metrics-service | 8083 | 400 | `/actuator/health` |
| 5e | communications-service | 8086 | 500 | `/actuator/health` |
| 5f | bff-api | 8090 | 50 | `/actuator/health` |
| 5g | ws-gateway | 8091 | 600 | `/health` |

**Lambda services** (reporting, notification) use the Lambda module in shared infra — no per-service Terraform needed unless we want separate deployment control. Skip for now.

**Template source**: `specs/ci-cd-pipelines.md` lines 290-441 has the complete Terraform example.

---

## Phase 6: Fix Infrastructure Workflow

**Priority**: 🟡 High  
**Files**: 1 modified

| # | Action | File |
|---|--------|------|
| 6a | Restructure `infrastructure.yml` — remove artifact dependency, use inline plan+apply for each env. Enable `workflow_dispatch` for manual runs. | `.github/workflows/infrastructure.yml` |

**Changes**:
- Uncomment `workflow_dispatch` input
- DEV: plan + apply inline (no artifact download)
- QA: same pattern, gated on `main` branch
- PROD: same pattern, gated on `main` branch + environment approval

---

## Phase 7: Create WS Gateway Workflow

**Priority**: 🟡 Medium  
**Files**: 1 new

| # | Action | File |
|---|--------|------|
| 7a | Create consolidated WS Gateway deployment workflow matching the pattern of `service-identity.yml` | `.github/workflows/service-ws-gateway.yml` |

---

## Phase 8: Create Frontend Deployment

**Priority**: 🟡 Medium  
**Files**: ~8 new

| # | Action | File |
|---|--------|------|
| 8a | Create frontend Terraform (S3 bucket, CloudFront, OAI, Route53 record) | `frontend/terraform/main.tf` |
| 8b | Frontend Terraform backend | `frontend/terraform/backend.tf` |
| 8c | Frontend Terraform variables | `frontend/terraform/variables.tf` |
| 8d | Frontend Terraform outputs | `frontend/terraform/outputs.tf` |
| 8e | Frontend deployment workflow (build Angular → S3 sync → CloudFront invalidation) | `.github/workflows/frontend.yml` |
| 8f | Environment config files (dev, qa, prod) | `frontend/src/environments/environment.{dev,qa,prod}.ts` |

**Source**: `specs/frontend-deployment.md` has complete Terraform and workflow templates.

---

## Phase 9: Create V016 Migration

**Priority**: 🟡 Medium  
**Files**: 1 new

| # | Action | File |
|---|--------|------|
| 9a | Create migration to fix communications org_id type | `services/flyway-service/migrations/V016__communications_fix_organization_id_type.sql` |

**Content**: `ALTER TABLE communications_schema.conversations ALTER COLUMN organization_id TYPE VARCHAR(36);`

---

## Phase 10: Document AWS Prerequisites

**Priority**: 🟡 Medium  
**Files**: 1 new (section in PROJECT.md or standalone bootstrap script)

Document/script the manual AWS resources needed before first deployment:

| Resource | Purpose | Per-Account |
|----------|---------|-------------|
| S3 state buckets | `turaf-terraform-state-{env}` | dev, qa, prod |
| ECR repositories | `turaf-{service-name}` per service | dev, qa, prod |
| OIDC identity provider | `token.actions.githubusercontent.com` | dev, qa, prod |
| `GitHubActionsDeploymentRole` | IAM role for GitHub Actions | dev, qa, prod |
| GitHub secrets | `AWS_ROLE_DEV`, `AWS_ROLE_QA`, `AWS_ROLE_PROD` | repo-level |
| GitHub environments | `dev`, `qa`, `prod` with protection rules | repo-level |
| ACM certificate | `*.turafapp.com` in us-east-1 | root account |
| SES domain verification | `turafapp.com` for notification emails | dev, qa, prod |

**Deliverable**: Bootstrap Terraform in `infrastructure/terraform/bootstrap/` that creates state buckets, ECR repos, and OIDC provider. Plus a checklist in PROJECT.md.

---

## Phase 11: Validation

**Priority**: 🔴 Critical — final gate  
**Files**: 0 new

| # | Check | Command |
|---|-------|---------|
| 11a | `terraform validate` passes for DEV | `cd infrastructure/terraform/environments/dev && terraform validate` |
| 11b | `terraform validate` passes for QA | same for qa |
| 11c | `terraform validate` passes for PROD | same for prod |
| 11d | All GitHub Actions workflows are valid YAML | `yamllint .github/workflows/*.yml` |
| 11e | Docker builds succeed locally | `docker build services/identity-service` |
| 11f | Service Terraform validates | `cd services/identity-service/terraform && terraform validate` |

---

## Execution Order

```
Phase 1 (align QA/PROD) ─┐
Phase 2 (fix tfvars)     ─┤── Phase 3 (DEV outputs) ── Phase 6 (fix infra workflow)
                           │                                    │
Phase 4 (Dockerfiles)     ─┤── Phase 5 (svc terraform) ────── Phase 7 (ws-gateway workflow)
                           │                                    │
Phase 9 (V016 migration)  │   Phase 8 (frontend) ─────────────┤
                           │                                    │
Phase 10 (prerequisites)  ─┘                          Phase 11 (validation) ──→ ✅ Done
```

**Phases 1-5** are the critical path. Everything else can proceed in parallel.

---

## Estimated Effort

| Phase | Files | Est. Time |
|-------|-------|-----------|
| 1. Align QA/PROD | 4 | 30 min |
| 2. Fix tfvars | 3 | 20 min |
| 3. DEV outputs/monitoring | 2 | 10 min |
| 4. Dockerfiles | 7 | 30 min |
| 5. Per-service Terraform | 35 | 90 min |
| 6. Fix infra workflow | 1 | 15 min |
| 7. WS Gateway workflow | 1 | 10 min |
| 8. Frontend deployment | 8 | 45 min |
| 9. V016 migration | 1 | 5 min |
| 10. Prerequisites doc | 1-5 | 30 min |
| 11. Validation | 0 | 15 min |
| **Total** | **~63** | **~5 hrs** |
