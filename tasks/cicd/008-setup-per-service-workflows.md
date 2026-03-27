# Task: Setup Per-Service Deployment Workflows

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 8 hours  

## Objective

Create dedicated GitHub Actions workflows for each microservice to enable independent deployments across all environments (DEV, QA, PROD). Each service gets its own set of workflows following the service-specific deployment pattern.

**Architecture Note**: This task implements the per-service workflow pattern described in Tasks 002-004. Each service manages its own infrastructure (ECS service, task definition, target group, listener rule) via Terraform executed in the CI/CD pipeline.

## Prerequisites

- [x] Task 001: CI pipeline setup
- [x] Task 005: Infrastructure pipeline setup
- [x] Task 007: AWS OIDC configured
- [ ] Shared infrastructure deployed (ECS cluster, ALB, VPC)
- [ ] Service Terraform directories created (`services/<service>/terraform/`)

## Scope

**Services Requiring Workflows** (9 services × 3 environments = 27 workflow files):

1. `identity-service`
2. `organization-service`
3. `experiment-service`
4. `metrics-service`
5. `reporting-service`
6. `notification-service`
7. `communications-service`
8. `bff-api`
9. `ws-gateway`

**Note**: `flyway-service` already has its dedicated workflow (`database-migrations.yml`)

**Files to Create**:
- `.github/workflows/service-<name>-dev.yml` (9 files)
- `.github/workflows/service-<name>-qa.yml` (9 files)
- `.github/workflows/service-<name>-prod.yml` (9 files)

## Implementation Details

### Workflow Naming Convention

```
service-<service-name>-<environment>.yml
```

Examples:
- `service-identity-dev.yml`
- `service-organization-qa.yml`
- `service-experiment-prod.yml`

### Workflow Template Structure

Each workflow follows this pattern (customized per service and environment):

#### DEV Environment Workflow Template

```yaml
name: Deploy <ServiceName> to DEV

on:
  push:
    branches: [develop]
    paths:
      - 'services/<service-name>/**'
      - '.github/workflows/service-<service-name>-dev.yml'
  workflow_dispatch:

permissions:
  id-token: write
  contents: read

env:
  AWS_REGION: us-east-1
  SERVICE_NAME: <service-name>
  ENVIRONMENT: dev
  AWS_ACCOUNT_ID: 801651112319
  ECR_REPOSITORY: turaf-<service-name>

jobs:
  deploy:
    name: Build and Deploy to DEV
    runs-on: ubuntu-latest
    
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
      
      - name: Get service status
        run: |
          aws ecs describe-services \
            --cluster turaf-cluster-${{ env.ENVIRONMENT }} \
            --services ${{ env.SERVICE_NAME }}-${{ env.ENVIRONMENT }} \
            --region ${{ env.AWS_REGION }} \
            --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount,Deployments:deployments[0].status}'
      
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
          echo "## Deployment Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "- **Service**: ${{ env.SERVICE_NAME }}" >> $GITHUB_STEP_SUMMARY
          echo "- **Environment**: ${{ env.ENVIRONMENT }}" >> $GITHUB_STEP_SUMMARY
          echo "- **Image**: ${{ steps.build-image.outputs.image }}" >> $GITHUB_STEP_SUMMARY
          echo "- **Status**: ${{ job.status }}" >> $GITHUB_STEP_SUMMARY
```

#### QA Environment Workflow Template

```yaml
name: Deploy <ServiceName> to QA

on:
  push:
    branches: [release/*]
    paths:
      - 'services/<service-name>/**'
      - '.github/workflows/service-<service-name>-qa.yml'
  workflow_dispatch:

permissions:
  id-token: write
  contents: read

env:
  AWS_REGION: us-east-1
  SERVICE_NAME: <service-name>
  ENVIRONMENT: qa
  AWS_ACCOUNT_ID: 965932217544
  ECR_REPOSITORY: turaf-<service-name>

jobs:
  deploy:
    name: Build and Deploy to QA
    runs-on: ubuntu-latest
    environment: qa-environment  # Requires manual approval
    
    steps:
      # Same steps as DEV, with QA-specific values
      # ... (identical structure to DEV template)
```

#### PROD Environment Workflow Template

```yaml
name: Deploy <ServiceName> to PROD

on:
  push:
    branches: [main]
    paths:
      - 'services/<service-name>/**'
      - '.github/workflows/service-<service-name>-prod.yml'
  workflow_dispatch:

permissions:
  id-token: write
  contents: read

env:
  AWS_REGION: us-east-1
  SERVICE_NAME: <service-name>
  ENVIRONMENT: prod
  AWS_ACCOUNT_ID: 811783768245
  ECR_REPOSITORY: turaf-<service-name>

jobs:
  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './services/${{ env.SERVICE_NAME }}'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'  # Fail on vulnerabilities

  deploy:
    name: Build and Deploy to PROD
    runs-on: ubuntu-latest
    needs: security-scan
    environment: prod-environment  # Requires manual approval
    
    steps:
      # Same steps as DEV/QA, with PROD-specific values
      # ... (identical structure to DEV template)
```

### Service-Specific Customizations

#### 1. Identity Service
- **Container Port**: 8080
- **Health Endpoint**: `/api/identity-service/health`
- **CPU**: 512
- **Memory**: 1024

#### 2. Organization Service
- **Container Port**: 8081
- **Health Endpoint**: `/api/organization-service/health`
- **CPU**: 512
- **Memory**: 1024

#### 3. Experiment Service
- **Container Port**: 8082
- **Health Endpoint**: `/api/experiment-service/health`
- **CPU**: 256
- **Memory**: 512

#### 4. Metrics Service
- **Container Port**: 8083
- **Health Endpoint**: `/api/metrics-service/health`
- **CPU**: 512
- **Memory**: 1024

#### 5. Reporting Service
- **Container Port**: 8084
- **Health Endpoint**: `/api/reporting-service/health`
- **CPU**: 512
- **Memory**: 1024

#### 6. Notification Service
- **Container Port**: 8085
- **Health Endpoint**: `/api/notification-service/health`
- **CPU**: 256
- **Memory**: 512

#### 7. Communications Service
- **Container Port**: 8086
- **Health Endpoint**: `/api/communications-service/health`
- **CPU**: 256
- **Memory**: 512

#### 8. BFF API
- **Container Port**: 8090
- **Health Endpoint**: `/api/bff/health`
- **CPU**: 512
- **Memory**: 1024

#### 9. WebSocket Gateway
- **Container Port**: 8091
- **Health Endpoint**: `/api/ws/health`
- **CPU**: 256
- **Memory**: 512

## Implementation Steps

### Step 1: Create DEV Workflows (9 files)

For each service, create `.github/workflows/service-<name>-dev.yml`:

```bash
# Example for identity-service
cat > .github/workflows/service-identity-dev.yml << 'EOF'
# ... (use DEV template with identity-service values)
EOF
```

Repeat for all 9 services.

### Step 2: Create QA Workflows (9 files)

For each service, create `.github/workflows/service-<name>-qa.yml`:

```bash
# Example for identity-service
cat > .github/workflows/service-identity-qa.yml << 'EOF'
# ... (use QA template with identity-service values)
EOF
```

Repeat for all 9 services.

### Step 3: Create PROD Workflows (9 files)

For each service, create `.github/workflows/service-<name>-prod.yml`:

```bash
# Example for identity-service
cat > .github/workflows/service-identity-prod.yml << 'EOF'
# ... (use PROD template with identity-service values)
EOF
```

Repeat for all 9 services.

### Step 4: Create Service Terraform Directories

Each service needs a `terraform/` directory with:

```bash
services/<service-name>/terraform/
├── backend.tf      # S3 backend configuration
├── data.tf         # Reference shared infrastructure
├── main.tf         # Service resources (task def, service, target group, listener rule)
├── variables.tf    # Service variables
└── outputs.tf      # Service outputs
```

**Example backend.tf**:
```hcl
terraform {
  backend "s3" {
    # Configured via init command
    # bucket = "turaf-terraform-state-<env>"
    # key    = "<service-name>/terraform.tfstate"
    # region = "us-east-1"
  }
}
```

**Example data.tf**:
```hcl
data "terraform_remote_state" "infra" {
  backend = "s3"
  config = {
    bucket = "turaf-terraform-state-${var.environment}"
    key    = "terraform.tfstate"
    region = var.aws_region
  }
}
```

See `specs/ci-cd-pipelines.md` for complete Terraform examples.

## Acceptance Criteria

### Workflow Files
- [ ] 9 DEV workflow files created
- [ ] 9 QA workflow files created
- [ ] 9 PROD workflow files created
- [ ] All workflows use correct AWS account IDs
- [ ] All workflows use correct IAM role ARNs
- [ ] Path filters correctly target service directories

### Service Terraform
- [ ] All 9 services have `terraform/` directories
- [ ] Each has backend.tf, data.tf, main.tf, variables.tf, outputs.tf
- [ ] Terraform references shared infrastructure correctly
- [ ] Service-specific resources defined (task def, service, target group, listener rule)

### Workflow Functionality
- [ ] DEV workflows trigger on push to `develop` branch
- [ ] QA workflows trigger on push to `release/*` branches
- [ ] PROD workflows trigger on push to `main` branch
- [ ] Manual workflow_dispatch works for all workflows
- [ ] Path filters prevent unnecessary runs
- [ ] OIDC authentication works for all environments
- [ ] Docker images build and push to ECR
- [ ] Terraform deploys service infrastructure
- [ ] ECS services start and stabilize
- [ ] Health checks pass

### Environment Protection
- [ ] QA environment requires manual approval
- [ ] PROD environment requires manual approval
- [ ] PROD workflows include security scanning
- [ ] Security scans block vulnerable images

## Testing Requirements

**For Each Service**:

1. **DEV Deployment**:
   ```bash
   # Make a change to service code
   git checkout develop
   echo "// test change" >> services/identity-service/src/main/java/com/turaf/identity/IdentityApplication.java
   git add .
   git commit -m "test: trigger identity-service DEV deployment"
   git push origin develop
   
   # Verify workflow runs
   gh run list --workflow=service-identity-dev.yml
   ```

2. **QA Deployment**:
   ```bash
   git checkout -b release/v1.0.0
   git push origin release/v1.0.0
   
   # Approve deployment in GitHub UI
   # Verify workflow runs
   gh run list --workflow=service-identity-qa.yml
   ```

3. **PROD Deployment**:
   ```bash
   git checkout main
   git merge release/v1.0.0
   git push origin main
   
   # Approve deployment in GitHub UI
   # Verify workflow runs
   gh run list --workflow=service-identity-prod.yml
   ```

4. **Verify Service Health**:
   ```bash
   # Get ALB DNS
   ALB_DNS=$(aws elbv2 describe-load-balancers \
     --names turaf-alb-dev \
     --query 'LoadBalancers[0].DNSName' \
     --output text)
   
   # Test health endpoint
   curl http://$ALB_DNS/api/identity-service/health
   ```

## Workflow Checklist

### Identity Service
- [x] `service-identity-dev.yml` created
- [x] `service-identity-qa.yml` created
- [x] `service-identity-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

### Organization Service
- [x] `service-organization-dev.yml` created
- [x] `service-organization-qa.yml` created
- [x] `service-organization-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

### Experiment Service
- [x] `service-experiment-dev.yml` created
- [x] `service-experiment-qa.yml` created
- [x] `service-experiment-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

### Metrics Service
- [x] `service-metrics-dev.yml` created
- [x] `service-metrics-qa.yml` created
- [x] `service-metrics-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

### Reporting Service
- [x] `service-reporting-dev.yml` created
- [x] `service-reporting-qa.yml` created
- [x] `service-reporting-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

### Notification Service
- [x] `service-notification-dev.yml` created
- [x] `service-notification-qa.yml` created
- [x] `service-notification-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

### Communications Service
- [x] `service-communications-dev.yml` created
- [x] `service-communications-qa.yml` created
- [x] `service-communications-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

### BFF API
- [x] `service-bff-api-dev.yml` created
- [x] `service-bff-api-qa.yml` created
- [x] `service-bff-api-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

### WebSocket Gateway
- [x] `service-ws-gateway-dev.yml` created
- [x] `service-ws-gateway-qa.yml` created
- [x] `service-ws-gateway-prod.yml` created
- [ ] Terraform directory created
- [ ] Workflows tested

## References

- **Specification**: `specs/ci-cd-pipelines.md` (Service Deployment Workflow Pattern)
- **Pattern Documentation**: Tasks 002 (DEV), 003 (QA), 004 (PROD)
- **Related Tasks**: 
  - 001: CI pipeline setup
  - 005: Infrastructure pipeline setup
  - 007: AWS OIDC configuration
- **IAM Roles**: `docs/IAM_ROLES.md`
- **Infrastructure Guide**: `infrastructure/docs/cicd/CICD_INFRASTRUCTURE_GUIDE.md`

## Notes

- Each service deploys independently - changes to one service don't trigger deployments of others
- Path filters ensure workflows only run when service code changes
- Terraform state is isolated per service for better security and blast radius control
- All services share the same ECS cluster and ALB (deployed via shared infrastructure)
- Service-specific resources (task definition, ECS service, target group, listener rule) are managed per service
- Rollback strategy: redeploy previous image tag via Terraform or use ECS deployment circuit breaker
