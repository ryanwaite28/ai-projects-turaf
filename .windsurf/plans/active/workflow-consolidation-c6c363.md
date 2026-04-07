# GitHub Actions Workflow Consolidation Plan

Consolidate 27 environment-specific service deployment workflows into 9 unified workflows that handle DEV, QA, and PROD deployments sequentially with appropriate branch triggers and manual approval gates.

---

## Current State Analysis

### Existing Workflows (32 total)

**Service Deployment Workflows (27 files)**:
- 9 services × 3 environments = 27 workflow files
- Services: identity, experiment, metrics, reporting, notification, organization, communications, bff-api, ws-gateway
- Environments: dev, qa, prod (separate file for each)

**Shared Workflows (5 files)**:
- `ci.yml` - Repository-wide quality gates (keep as-is)
- `infrastructure.yml` - Shared infrastructure deployment (keep as-is, already consolidated)
- `database-migrations.yml` - Database migrations (keep as-is, already consolidated)
- `security.yml` - Security scanning (keep as-is)
- `architecture-tests.yml` - Architecture validation (keep as-is)

### Problems with Current Structure

1. **Duplication**: 27 nearly identical workflow files with only environment variables different
2. **Maintenance burden**: Changes require updating 3 files per service (9 services × 3 = 27 files)
3. **Inconsistency risk**: Easy for workflows to drift apart
4. **Poor visibility**: Hard to see full deployment pipeline for a service
5. **No environment progression**: DEV/QA/PROD deployments are independent, no built-in promotion flow

---

## Target State

### Consolidated Service Workflows (9 files)

Each service will have ONE workflow file that handles all three environments:

```
.github/workflows/
├── ci.yml                          # Keep as-is
├── infrastructure.yml              # Keep as-is
├── database-migrations.yml         # Keep as-is
├── security.yml                    # Keep as-is
├── architecture-tests.yml          # Keep as-is
├── service-identity.yml            # NEW: Replaces 3 files
├── service-experiment.yml          # NEW: Replaces 3 files
├── service-metrics.yml             # NEW: Replaces 3 files
├── service-reporting.yml           # NEW: Replaces 3 files
├── service-notification.yml        # NEW: Replaces 3 files
├── service-organization.yml        # NEW: Replaces 3 files
├── service-communications.yml      # NEW: Replaces 3 files
├── service-bff-api.yml             # NEW: Replaces 3 files
└── service-ws-gateway.yml          # NEW: Replaces 3 files
```

**Result**: 32 files → 14 files (18 files removed, 55% reduction)

---

## Deployment Flow Design

### Trigger Strategy (Sequential with Auto-trigger)

**Push to `develop` branch**:
```
Code merged to develop
    ↓
Triggers DEV deployment only
    ↓
DEV job runs automatically
    ↓
Workflow completes
```

**Push to `main` branch**:
```
Code merged to main
    ↓
Triggers full deployment pipeline
    ↓
DEV job runs automatically
    ↓
QA job runs automatically (after DEV succeeds)
    ↓
PROD job waits for manual approval
    ↓
Approver reviews and approves
    ↓
PROD job runs
```

**Manual workflow_dispatch**:
```
User triggers workflow manually
    ↓
Selects target environment (dev/qa/prod)
    ↓
Only selected environment deploys
```

### Branch-to-Environment Mapping

| Branch Pattern | Environments Deployed | Auto/Manual |
|----------------|----------------------|-------------|
| `develop` | DEV only | Automatic |
| `main` | DEV → QA → PROD | DEV/QA automatic, PROD requires approval |
| Manual dispatch | User-selected | Manual |

### Job Dependencies

```yaml
jobs:
  deploy-dev:
    if: github.ref == 'refs/heads/develop' || github.ref == 'refs/heads/main' || inputs.environment == 'dev'
    # Runs automatically
  
  deploy-qa:
    needs: deploy-dev
    if: github.ref == 'refs/heads/main' || inputs.environment == 'qa'
    environment: qa  # GitHub environment protection
    # Runs automatically after DEV (on main branch)
  
  deploy-prod:
    needs: deploy-qa
    if: github.ref == 'refs/heads/main' || inputs.environment == 'prod'
    environment: prod  # Requires manual approval
    # Waits for approval before running
```

---

## Consolidated Workflow Template

### High-Level Structure

```yaml
name: Deploy <Service-Name>

on:
  push:
    branches: [develop, main]
    paths:
      - 'services/<service-name>/**'
      - '.github/workflows/service-<service-name>.yml'
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        type: choice
        options: [dev, qa, prod]

permissions:
  id-token: write
  contents: read
  security-events: write

jobs:
  # Job 1: Security scan (PROD only, runs before deploy-dev)
  security-scan:
    if: github.ref == 'refs/heads/main'
    # Trivy scanning for PROD deployments
  
  # Job 2: Deploy to DEV
  deploy-dev:
    needs: [security-scan]
    if: |
      always() && 
      (github.ref == 'refs/heads/develop' || 
       github.ref == 'refs/heads/main' || 
       inputs.environment == 'dev')
    # Build, push, deploy to DEV environment
  
  # Job 3: Deploy to QA
  deploy-qa:
    needs: deploy-dev
    if: github.ref == 'refs/heads/main' || inputs.environment == 'qa'
    environment: qa
    # Build, push, deploy to QA environment
  
  # Job 4: Deploy to PROD
  deploy-prod:
    needs: deploy-qa
    if: github.ref == 'refs/heads/main' || inputs.environment == 'prod'
    environment: prod  # Manual approval required
    # Build, push, deploy to PROD environment
```

### Key Features

1. **Conditional execution**: Jobs only run for appropriate branches/environments
2. **Environment protection**: Leverages GitHub environment protection rules
3. **Manual approval**: PROD deployment requires approval via `environment: prod`
4. **Security scanning**: Only runs for PROD deployments (main branch)
5. **Flexible triggering**: Supports both automatic and manual deployments
6. **Path filtering**: Only triggers when service code changes

---

## Implementation Steps

### Phase 1: Create Consolidated Workflows (9 files)

**For each service**:
1. Create new consolidated workflow file `service-<name>.yml`
2. Implement multi-environment job structure
3. Add conditional logic for branch-based triggering
4. Configure environment protection for QA/PROD
5. Add security scanning for PROD deployments
6. Test workflow with workflow_dispatch

**Services to consolidate**:
- identity-service
- experiment-service
- metrics-service
- reporting-service
- notification-service
- organization-service
- communications-service
- bff-api
- ws-gateway

### Phase 2: Update Documentation

**Files to update**:

1. **`specs/ci-cd-pipelines.md`**
   - Update workflow structure section
   - Update deployment flow diagrams
   - Update service deployment pattern
   - Add new consolidated workflow examples
   - Update file counts and references
   - Correct GitHub environment names (dev, qa, prod)

2. **`docs/DEPLOYMENT_RUNBOOK.md`**
   - Update deployment matrix
   - Update workflow file references
   - Update deployment procedures
   - Add new manual deployment instructions
   - Correct GitHub environment names (dev, qa, prod)

3. **`infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md`**
   - Section 4.1: Update GitHub environment creation commands
   - Section 4.2: Update environment-specific secrets commands
   - Change all `dev-environment` → `dev`
   - Change all `qa-environment` → `qa`
   - Change all `prod-environment` → `prod`

4. **`GITHUB.md`**
   - Section "GitHub Environments": Update environment names
   - Update all references from suffixed names to simple names
   - Ensure consistency with new naming convention

5. **`tasks/cicd/007-configure-aws-oidc.md`**
   - Update GitHub environment references in "Next Steps" section
   - Correct environment names in secret configuration examples

6. **`tasks/cicd/008-setup-per-service-workflows.md`**
   - Update workflow examples with correct environment names
   - Update YAML code blocks showing `environment:` configuration

7. **`.github/workflows/README-architecture-tests.md`** (if needed)
   - Update any references to service workflows

### Phase 3: Create Changelog

**File**: `changelog/2026-04-01-workflow-consolidation.md`

Document:
- What changed (27 files → 9 files)
- Why (reduce duplication, improve maintainability)
- Impact on deployment process
- Migration notes

### Phase 4: Validation & Testing

**For each consolidated workflow**:
1. Test manual deployment to DEV via workflow_dispatch
2. Test automatic deployment to DEV via develop branch push
3. Test full pipeline (DEV→QA→PROD) via main branch push
4. Verify approval gates work for PROD
5. Verify path filtering works correctly

### Phase 5: Cleanup

**After validation**:
1. Archive old workflow files (move to `.github/workflows/archive/`)
2. Update any scripts or documentation referencing old workflows
3. Verify GitHub Actions UI shows correct workflows

---

## Detailed Workflow Specifications

### Common Elements (All Services)

**Environment Variables Pattern**:
```yaml
env:
  AWS_REGION: us-east-1
  SERVICE_NAME: <service-name>
  ECR_REPOSITORY: turaf-<service-name>
```

**AWS Account Mapping**:
```yaml
# DEV: 801651112319
# QA:  965932217544
# PROD: 811783768245
```

**Terraform Version**: 1.6.0

**Common Steps per Environment**:
1. Checkout code
2. Configure AWS credentials (environment-specific)
3. Login to ECR
4. Build and push Docker image
5. Setup Terraform
6. Terraform init/plan/apply
7. Wait for ECS service stability
8. Health check
9. Deployment summary

### Environment-Specific Differences

**DEV**:
- AWS Account: 801651112319
- No security scanning
- No integration tests
- Simple health check
- No approval required

**QA**:
- AWS Account: 965932217544
- No security scanning (already done in PROD)
- Integration tests included
- Health check
- Optional approval via GitHub environment protection

**PROD**:
- AWS Account: 811783768245
- Security scanning (Trivy) before deployment
- Smoke tests after deployment
- Health check
- Metrics monitoring (5-minute wait)
- **Required approval** via GitHub environment protection
- HTTPS health check (vs HTTP for DEV/QA)

---

## Risk Mitigation

### Potential Issues

1. **Workflow too complex**: Single file handling 3 environments
   - **Mitigation**: Clear conditional logic, extensive comments, thorough testing

2. **Approval process unclear**: Manual approval for PROD
   - **Mitigation**: Document in DEPLOYMENT_RUNBOOK.md, use GitHub environment protection UI

3. **Failed DEV blocks QA/PROD**: Sequential dependencies
   - **Mitigation**: Use `workflow_dispatch` to deploy directly to specific environment if needed

4. **Long-running workflows**: DEV→QA→PROD could take 30+ minutes
   - **Mitigation**: Acceptable for main branch; develop branch only deploys DEV (fast)

5. **Breaking existing automation**: Scripts/tools referencing old workflow names
   - **Mitigation**: Search codebase for workflow references before cleanup

### Rollback Plan

If consolidated workflows have issues:
1. Restore archived workflow files from `.github/workflows/archive/`
2. Delete new consolidated workflows
3. Revert documentation changes
4. Investigate and fix issues
5. Re-attempt consolidation

---

## Success Criteria

### Functional Requirements

- [ ] All 9 services have consolidated workflows
- [ ] DEV deploys automatically from develop branch
- [ ] DEV→QA→PROD deploys automatically from main branch (PROD requires approval)
- [ ] Manual deployment works for any environment
- [ ] PROD deployments require manual approval
- [ ] Security scanning runs for PROD deployments
- [ ] Health checks pass for all environments
- [ ] Path filtering prevents unnecessary deployments

### Non-Functional Requirements

- [ ] Workflow files reduced from 27 to 9 (67% reduction)
- [ ] Documentation updated and accurate
- [ ] Changelog created
- [ ] No breaking changes to deployment process
- [ ] Deployment time similar to current process
- [ ] GitHub Actions UI shows clear workflow status

---

## Timeline Estimate

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| Phase 1 | Create 9 consolidated workflows | 2-3 hours |
| Phase 2 | Update documentation (2 files) | 1 hour |
| Phase 3 | Create changelog | 15 minutes |
| Phase 4 | Validation & testing | 1-2 hours |
| Phase 5 | Cleanup & archival | 30 minutes |
| **Total** | | **5-7 hours** |

---

## Files to Create

1. `.github/workflows/service-identity.yml`
2. `.github/workflows/service-experiment.yml`
3. `.github/workflows/service-metrics.yml`
4. `.github/workflows/service-reporting.yml`
5. `.github/workflows/service-notification.yml`
6. `.github/workflows/service-organization.yml`
7. `.github/workflows/service-communications.yml`
8. `.github/workflows/service-bff-api.yml`
9. `.github/workflows/service-ws-gateway.yml`
10. `changelog/2026-04-01-workflow-consolidation.md`

## Files to Update

1. `specs/ci-cd-pipelines.md`
2. `docs/DEPLOYMENT_RUNBOOK.md`
3. `infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md`
4. `GITHUB.md`
5. `tasks/cicd/007-configure-aws-oidc.md`
6. `tasks/cicd/008-setup-per-service-workflows.md`
7. `.github/workflows/README-architecture-tests.md` (if applicable)

## Files to Archive (27 files)

Move to `.github/workflows/archive/`:
- `service-*-dev.yml` (9 files)
- `service-*-qa.yml` (9 files)
- `service-*-prod.yml` (9 files)

---

## GitHub Environment Configuration

**Prerequisites** (must be configured in GitHub repository settings):

### Environment Naming Convention

**IMPORTANT**: GitHub environments use simple, lowercase names without suffixes:

| Environment | GitHub Environment Name | Current Workflow Reference |
|-------------|------------------------|---------------------------|
| Development | `dev` | ✅ Correct |
| Quality Assurance | `qa` | ✅ Correct |
| Production | `prod` | ✅ Correct |

**Incorrect naming** (do NOT use):
- ❌ `dev-environment`
- ❌ `qa-environment`
- ❌ `prod-environment`

**In workflow YAML files**, use:
```yaml
environment: dev   # For DEV
environment: qa    # For QA
environment: prod  # For PROD
```

### DEV Environment (`dev`)
- **Protection rules**: None (auto-deploy)
- **Deployment branches**: `develop` and `main` branches
- **Secrets**: 
  - `AWS_ROLE_DEV` (if not using repository-level secrets)

### QA Environment (`qa`)
- **Protection rules**: Optional reviewers (recommended but not required)
- **Deployment branches**: `main` branch only
- **Secrets**: 
  - `AWS_ROLE_QA` (if not using repository-level secrets)

### PROD Environment (`prod`)
- **Protection rules**: 
  - **Required reviewers**: 2 reviewers from authorized list
  - **Wait timer**: Optional (e.g., 5 minutes)
- **Deployment branches**: `main` branch only
- **Secrets**:
  - `AWS_ROLE_PROD` (if not using repository-level secrets)

---

## Example: Consolidated Identity Service Workflow

```yaml
name: Deploy Identity Service

on:
  push:
    branches: [develop, main]
    paths:
      - 'services/identity-service/**'
      - '.github/workflows/service-identity.yml'
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        type: choice
        options:
          - dev
          - qa
          - prod

permissions:
  id-token: write
  contents: read
  security-events: write

env:
  AWS_REGION: us-east-1
  SERVICE_NAME: identity-service
  ECR_REPOSITORY: turaf-identity-service

jobs:
  security-scan:
    name: Security Scanning
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name != 'workflow_dispatch'
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './services/${{ env.SERVICE_NAME }}'
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
      
      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: trivy-results.sarif

  deploy-dev:
    name: Deploy to DEV
    runs-on: ubuntu-latest
    needs: [security-scan]
    if: |
      always() && 
      !contains(needs.*.result, 'failure') &&
      (github.ref == 'refs/heads/develop' || 
       github.ref == 'refs/heads/main' || 
       github.event.inputs.environment == 'dev')
    
    env:
      ENVIRONMENT: dev
      AWS_ACCOUNT_ID: 801651112319
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      
      - name: Build, tag, and push image to Amazon ECR
        id: build-image
        working-directory: ./services/${{ env.SERVICE_NAME }}
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:latest
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
          echo "image=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG" >> $GITHUB_OUTPUT
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.6.0
      
      - name: Terraform Init
        working-directory: ./services/${{ env.SERVICE_NAME }}/terraform
        run: |
          terraform init \
            -backend-config="bucket=turaf-terraform-state-${{ env.ENVIRONMENT }}" \
            -backend-config="key=${{ env.SERVICE_NAME }}/terraform.tfstate" \
            -backend-config="region=${{ env.AWS_REGION }}"
      
      - name: Terraform Plan
        working-directory: ./services/${{ env.SERVICE_NAME }}/terraform
        env:
          TF_VAR_environment: ${{ env.ENVIRONMENT }}
          TF_VAR_service_name: ${{ env.SERVICE_NAME }}
          TF_VAR_image_tag: ${{ github.sha }}
          TF_VAR_aws_region: ${{ env.AWS_REGION }}
        run: terraform plan -out=tfplan
      
      - name: Terraform Apply
        working-directory: ./services/${{ env.SERVICE_NAME }}/terraform
        env:
          TF_VAR_environment: ${{ env.ENVIRONMENT }}
          TF_VAR_service_name: ${{ env.SERVICE_NAME }}
          TF_VAR_image_tag: ${{ github.sha }}
          TF_VAR_aws_region: ${{ env.AWS_REGION }}
        run: terraform apply -auto-approve tfplan
      
      - name: Wait for ECS service stability
        run: |
          aws ecs wait services-stable \
            --cluster turaf-cluster-${{ env.ENVIRONMENT }} \
            --services ${{ env.SERVICE_NAME }}-${{ env.ENVIRONMENT }} \
            --region ${{ env.AWS_REGION }}
      
      - name: Health check
        run: |
          ALB_DNS=$(aws elbv2 describe-load-balancers \
            --names turaf-alb-${{ env.ENVIRONMENT }} \
            --query 'LoadBalancers[0].DNSName' \
            --output text)
          
          for i in {1..30}; do
            if curl -f http://$ALB_DNS/api/${{ env.SERVICE_NAME }}/health; then
              echo "✅ Service is healthy"
              exit 0
            fi
            echo "Waiting for service to be healthy... ($i/30)"
            sleep 10
          done
          
          echo "❌ Service health check failed"
          exit 1
      
      - name: Deployment summary
        if: always()
        run: |
          echo "## DEV Deployment Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "- **Service**: ${{ env.SERVICE_NAME }}" >> $GITHUB_STEP_SUMMARY
          echo "- **Environment**: ${{ env.ENVIRONMENT }}" >> $GITHUB_STEP_SUMMARY
          echo "- **Image**: ${{ steps.build-image.outputs.image }}" >> $GITHUB_STEP_SUMMARY
          echo "- **Status**: ${{ job.status }}" >> $GITHUB_STEP_SUMMARY

  deploy-qa:
    name: Deploy to QA
    runs-on: ubuntu-latest
    needs: deploy-dev
    if: github.ref == 'refs/heads/main' || github.event.inputs.environment == 'qa'
    environment: qa
    
    env:
      ENVIRONMENT: qa
      AWS_ACCOUNT_ID: 965932217544
    
    steps:
      # Same steps as deploy-dev, with QA-specific values
      # ... (omitted for brevity)

  deploy-prod:
    name: Deploy to PROD
    runs-on: ubuntu-latest
    needs: deploy-qa
    if: github.ref == 'refs/heads/main' || github.event.inputs.environment == 'prod'
    environment: prod  # Requires manual approval
    
    env:
      ENVIRONMENT: prod
      AWS_ACCOUNT_ID: 811783768245
    
    steps:
      # Same steps as deploy-dev, with PROD-specific values
      # Plus additional monitoring and smoke tests
      # ... (omitted for brevity)
```

---

## Notes

- **Infrastructure workflow** already follows consolidated pattern (handles all 3 environments)
- **Database migrations workflow** already follows consolidated pattern
- **CI workflow** remains unchanged (repository-wide quality gates)
- **Security workflow** remains unchanged (repository-wide scanning)
- **Architecture tests workflow** remains unchanged

This consolidation focuses specifically on the 27 service deployment workflows.

---

## Related Documents

- **Current CI/CD Spec**: `specs/ci-cd-pipelines.md`
- **Deployment Runbook**: `docs/DEPLOYMENT_RUNBOOK.md`
- **Infrastructure Guide**: `infrastructure/docs/cicd/CICD_INFRASTRUCTURE_GUIDE.md`
- **Project Workflow**: `.windsurf/workflows/project.md`
- **Windsurf Rules**: `.windsurf/rules/rules.md`

---

**Created**: 2026-04-01  
**Status**: Pending Approval  
**Estimated Effort**: 5-7 hours  
**Impact**: High (reduces workflow files by 67%, improves maintainability)
