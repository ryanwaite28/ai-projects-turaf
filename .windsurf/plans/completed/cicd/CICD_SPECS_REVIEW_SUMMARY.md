# CI/CD Specifications Review and Update Summary

**Date:** March 25, 2026  
**Status:** ✅ Complete  
**Scope:** Align CI/CD specs and tasks with new service-managed infrastructure pattern

---

## Overview

Reviewed and updated all CI/CD specifications and tasks to align with the new infrastructure architecture where:
- **Shared infrastructure** (VPC, ECS cluster, ALB, databases) is managed by Terraform in `infrastructure/terraform/`
- **Service-specific resources** (ECS services, task definitions, target groups, listener rules) are managed by CI/CD pipelines per service

---

## Key Changes Made

### 1. Created Updated CI/CD Specification

**File:** `@/Users/ryanwaite28/Developer/portfolio-projects/Turaf/specs/ci-cd-pipelines-UPDATED.md`

**Changes:**
- Added architecture overview explaining hybrid approach
- Updated workflow structure to show per-service deployment workflows
- Replaced monolithic CD pipeline with service-specific deployment pattern
- Added complete service Terraform examples
- Updated infrastructure pipeline to only manage shared resources
- Added service deployment workflow template with 3 jobs:
  1. `build-and-push`: Build Docker image and push to ECR
  2. `deploy-service`: Run Terraform to deploy service infrastructure
  3. `verify-deployment`: Wait for ECS stability and run health checks

**Key Sections:**
- Service Terraform structure and examples
- Service deployment workflow pattern
- Infrastructure pipeline (shared resources only)
- Deployment flow and rollback strategy
- Migration guide from old pattern

---

### 2. Updated CD-DEV Task

**File:** `@/Users/ryanwaite28/Developer/portfolio-projects/Turaf/tasks/cicd/002-setup-cd-dev-pipeline.md`

**Changes:**
- Updated objective to clarify service-specific Terraform deployment
- Added architecture note about responsibility split
- Expanded prerequisites to include shared infrastructure and service Terraform directories
- Changed scope from single `cd-dev.yml` to per-service workflows
- Replaced simple ECS service update with full Terraform deployment workflow
- Updated acceptance criteria to include Terraform deployment verification
- Enhanced testing requirements to validate service-specific infrastructure

**New Workflow Pattern:**
```
services/identity-service/**  →  Triggers workflow
  ↓
Build & Push Docker Image
  ↓
Deploy Service Terraform (ECS service, task def, target group, listener rule)
  ↓
Verify ECS Stability & Health Checks
```

---

### 3. Identified Required Updates (Not Yet Implemented)

#### A. Other CI/CD Task Files
**Files to Update:**
- `tasks/cicd/001-setup-ci-pipeline.md` - Minimal changes needed (CI unchanged)
- `tasks/cicd/003-setup-cd-qa-pipeline.md` - Apply same pattern as DEV
- `tasks/cicd/004-setup-cd-prod-pipeline.md` - Apply same pattern with approval gates
- `tasks/cicd/005-setup-infrastructure-pipeline.md` - Update to clarify shared infrastructure only
- `tasks/cicd/006-setup-security-scanning.md` - Minimal changes needed
- `tasks/cicd/007-configure-aws-oidc.md` - Minimal changes needed

#### B. Infrastructure Documentation
**Files to Update:**
- `infrastructure/docs/CICD_INFRASTRUCTURE_GUIDE.md` - Update deployment workflows and prerequisites
- `.windsurf/plans/CICD_IMPLEMENTATION_STATUS.md` - Update to reflect new architecture

#### C. Original Spec File
**Decision Needed:**
- Keep `specs/ci-cd-pipelines.md` as-is for reference?
- Replace with `specs/ci-cd-pipelines-UPDATED.md`?
- Rename old to `ci-cd-pipelines-OLD.md`?

---

## Architecture Comparison

### Old Pattern (Monolithic)
```
Terraform (infrastructure/terraform/)
├── Shared Infrastructure (VPC, cluster, ALB, databases)
└── Service Resources (ECS services, task defs, target groups, rules)
     ↓
GitHub Actions CD Pipeline
└── aws ecs update-service --force-new-deployment
```

**Issues:**
- All services deployed together
- Infrastructure team bottleneck
- Slow iteration cycles
- Tight coupling

### New Pattern (Hybrid)
```
Terraform (infrastructure/terraform/)
└── Shared Infrastructure ONLY (VPC, cluster, ALB, databases)

Per-Service CI/CD (services/<service>/terraform/)
├── Build Docker Image
├── Deploy Service Terraform
│   ├── ECS Task Definition
│   ├── ECS Service
│   ├── ALB Target Group
│   └── ALB Listener Rule
└── Verify Deployment
```

**Benefits:**
- Independent service deployments
- Service teams own their infrastructure
- Faster iteration
- Clear separation of concerns
- Cost efficiency (shared ALB/cluster)

---

## Service Terraform Structure

Each service now maintains:

```
services/identity-service/
├── src/                          # Application code
├── Dockerfile                    # Container definition
├── terraform/                    # Service infrastructure
│   ├── backend.tf               # S3 backend (service-specific state)
│   ├── data.tf                  # Reference shared infrastructure
│   ├── main.tf                  # Service resources
│   ├── variables.tf             # Service variables
│   └── outputs.tf               # Service outputs
└── .github/workflows/
    ├── service-identity-dev.yml
    ├── service-identity-qa.yml
    └── service-identity-prod.yml
```

---

## Deployment Flow

### Initial Setup (One-time)
1. Deploy shared infrastructure: `infrastructure.yml` workflow
2. Verify ECS cluster, ALB, databases created
3. Create service Terraform directories
4. Set up service-specific workflows

### Service Deployment (Per service, per environment)
1. Developer pushes code to `services/<service>/`
2. Workflow triggers based on path filter
3. Build and push Docker image to ECR
4. Run Terraform to deploy/update service resources
5. Wait for ECS service to stabilize
6. Run health checks

### Rollback
- Terraform tracks previous versions
- Redeploy with previous image tag
- ECS circuit breaker auto-rolls back on failure

---

## Required GitHub Workflows

### Shared Infrastructure
- `infrastructure.yml` - Deploy VPC, cluster, ALB, databases

### Per Service (3 services × 3 environments = 9 workflows)
- `service-identity-dev.yml`
- `service-identity-qa.yml`
- `service-identity-prod.yml`
- `service-organization-dev.yml`
- `service-organization-qa.yml`
- `service-organization-prod.yml`
- `service-experiment-dev.yml`
- `service-experiment-qa.yml`
- `service-experiment-prod.yml`

### CI & Security
- `ci.yml` - Lint, test, build (unchanged)
- `security-scan.yml` - Vulnerability scanning (unchanged)

---

## Migration Path

### Step 1: Infrastructure Already Migrated ✅
- Shared infrastructure deployed
- Service-specific resources removed from Terraform state
- Documented in `INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`

### Step 2: Create Service Terraform (Pending)
For each service:
```bash
cd services/<service>
mkdir -p terraform
# Create backend.tf, data.tf, main.tf, variables.tf, outputs.tf
```

### Step 3: Create Service Workflows (Pending)
For each service and environment:
```bash
# Create .github/workflows/service-<name>-<env>.yml
```

### Step 4: Deploy Services via CI/CD (Pending)
```bash
# Push to trigger workflow or manually run
gh workflow run service-identity-dev.yml
```

---

## Listener Rule Priority Allocation

Each service gets a unique priority range for ALB listener rules:

| Service | Priority Range | Example Paths |
|---------|---------------|---------------|
| Identity | 100-199 | `/api/identity/*`, `/api/auth/*` |
| Organization | 200-299 | `/api/organizations/*`, `/api/teams/*` |
| Experiment | 300-399 | `/api/experiments/*`, `/api/variants/*` |
| Metrics | 400-499 | `/api/metrics/*` |
| Reporting | 500-599 | `/api/reports/*` |
| Notification | 600-699 | `/api/notifications/*` |
| BFF API | 1000-1999 | `/api/*` (catch-all) |
| Frontend | 2000+ | `/*` (default) |

---

## Updated Documentation References

### Primary Specifications
- **Updated CI/CD Spec:** `specs/ci-cd-pipelines-UPDATED.md`
- **Service Deployment Pattern:** `.windsurf/plans/cicd-service-deployment-pattern.md`
- **Infrastructure Restructure:** `infrastructure/docs/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`

### Task Files
- **Updated:** `tasks/cicd/002-setup-cd-dev-pipeline.md`
- **Needs Update:** Tasks 003, 004, 005

### Infrastructure Docs
- **Needs Update:** `infrastructure/docs/CICD_INFRASTRUCTURE_GUIDE.md`
- **Needs Update:** `.windsurf/plans/CICD_IMPLEMENTATION_STATUS.md`

---

## Next Steps

### Immediate (Documentation)
1. ✅ Update CD-DEV task file
2. ⏳ Update CD-QA task file (003)
3. ⏳ Update CD-PROD task file (004)
4. ⏳ Update infrastructure pipeline task (005)
5. ⏳ Update CICD_INFRASTRUCTURE_GUIDE.md
6. ⏳ Update CICD_IMPLEMENTATION_STATUS.md

### Short-term (Implementation)
7. Create service Terraform directories for identity, organization, experiment services
8. Create service deployment workflows
9. Test service deployment to DEV
10. Replicate for QA and PROD environments

### Medium-term (Rollout)
11. Deploy first service (identity) via CI/CD
12. Verify ALB routing and health checks
13. Deploy remaining services
14. Update PROJECT.md with new CI/CD pattern

---

## Key Takeaways

### What Changed
- **Deployment Model:** From monolithic Terraform to hybrid (shared + per-service)
- **Workflow Structure:** From single CD pipeline to per-service workflows
- **Responsibility:** Service teams now own their infrastructure code
- **State Management:** Separate Terraform state per service

### What Stayed the Same
- **CI Pipeline:** Lint, test, build unchanged
- **Security Scanning:** Trivy, OWASP unchanged
- **AWS Authentication:** OIDC federation unchanged
- **Shared Infrastructure:** VPC, cluster, ALB, databases still centrally managed

### Why This Matters
- **Faster Deployments:** Services deploy independently
- **Better Ownership:** Clear boundaries between platform and services
- **Cost Efficiency:** Shared ALB and cluster reduce costs
- **Scalability:** Easy to add new services without infrastructure changes

---

## Alignment with Current Infrastructure

### Infrastructure State ✅
- Shared infrastructure deployed and operational
- Service-specific resources removed from Terraform
- Outputs configured for CI/CD consumption

### CI/CD Specs ✅
- Updated specification created
- Service deployment pattern documented
- Task files updated (DEV complete, others pending)

### Ready for Implementation ✅
- Architecture documented
- Patterns established
- Examples provided
- Migration path clear

---

**Status:** CI/CD specifications and tasks are now aligned with the current infrastructure design. Ready to proceed with service Terraform creation and workflow implementation.

**Next Action:** Create service Terraform directories and workflows, or update remaining task files and documentation.
