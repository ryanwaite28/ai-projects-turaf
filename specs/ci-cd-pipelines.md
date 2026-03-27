# CI/CD Pipelines Specification

**Source**: PROJECT.md (Sections 14, 49-50)  
**Last Updated**: March 25, 2026  
**Status**: Current  
**Architecture**: Hybrid - Shared infrastructure via Terraform, service-specific resources via CI/CD  
**Related Documents**: [AWS Infrastructure](aws-infrastructure.md), [Terraform Structure](terraform-structure.md), [Infrastructure Guide](../infrastructure/docs/cicd/CICD_INFRASTRUCTURE_GUIDE.md)  
**Previous Version**: [v1 Monolithic Pattern](archive/ci-cd-pipelines-v1-monolithic.md)

This specification defines the complete CI/CD pipeline architecture using GitHub Actions, aligned with the new infrastructure pattern where services manage their own ECS resources.

---

## Architecture Overview

### Infrastructure Responsibility Split

**Shared Infrastructure (Terraform - `infrastructure/terraform/`):**
- VPC, subnets, NAT gateways
- ECS Cluster
- Application Load Balancer (ALB)
- ALB base HTTP/HTTPS listeners
- RDS, Redis, S3, EventBridge, SQS
- Security groups, IAM roles, KMS keys

**Service-Specific Resources (CI/CD per service - `services/<service>/terraform/`):**
- ECS Task Definition
- ECS Service
- ALB Target Group
- ALB Listener Rule
- CloudWatch Log Group
- Service-specific environment configuration

---

## Pipeline Overview

**CI/CD Platform**: GitHub Actions  
**GitHub Repository**: https://github.com/ryanwaite28/ai-projects-turaf  
**Workflow Location**: `.github/workflows/`  
**Environments**: DEV, QA, PROD  
**AWS Authentication**: OIDC Federation  

---

## Workflow Structure

### 1. Continuous Integration (ci.yml)
**Triggers**: All branches, PRs  
**Purpose**: Lint, test, build verification  
**Scope**: All code (services, frontend, infrastructure)

### 2. Shared Infrastructure (infrastructure.yml)
**Triggers**: Changes to `infrastructure/terraform/`, manual  
**Purpose**: Deploy/update shared infrastructure  
**Scope**: VPC, ECS cluster, ALB, databases, messaging

### 3. Service Deployment (service-<name>-<env>.yml)
**Triggers**: Changes to service code, manual  
**Purpose**: Build image, deploy service-specific infrastructure  
**Scope**: Single service to single environment

---

## Service Deployment Workflow Pattern

Each service follows this pattern for deployment:

```yaml
name: Deploy Identity Service to DEV

on:
  push:
    branches: [develop]
    paths:
      - 'services/identity-service/**'
  workflow_dispatch:

env:
  AWS_REGION: us-east-1
  SERVICE_NAME: identity-service
  ENVIRONMENT: dev
  AWS_ACCOUNT_ID: 801651112319

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
    outputs:
      image-tag: ${{ steps.meta.outputs.tags }}
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      
      - name: Docker meta
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ steps.login-ecr.outputs.registry }}/turaf/${{ env.SERVICE_NAME }}
          tags: |
            type=sha,prefix=${{ env.ENVIRONMENT }}-
            type=raw,value=${{ env.ENVIRONMENT }}-latest
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: services/${{ env.SERVICE_NAME }}
          push: true
          tags: ${{ steps.meta.outputs.tags }}
      
      - name: Scan image with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ steps.login-ecr.outputs.registry }}/turaf/${{ env.SERVICE_NAME }}:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
      
      - name: Upload Trivy results
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-results.sarif'

  deploy-service:
    runs-on: ubuntu-latest
    needs: build-and-push
    permissions:
      id-token: write
      contents: read
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.5.7
      
      - name: Terraform Init
        working-directory: services/${{ env.SERVICE_NAME }}/terraform
        run: terraform init
      
      - name: Terraform Plan
        working-directory: services/${{ env.SERVICE_NAME }}/terraform
        run: |
          terraform plan -out=tfplan \
            -var="environment=${{ env.ENVIRONMENT }}" \
            -var="image_tag=${{ github.sha }}" \
            -var="desired_count=1"
      
      - name: Terraform Apply
        working-directory: services/${{ env.SERVICE_NAME }}/terraform
        run: terraform apply -auto-approve tfplan
      
      - name: Get service outputs
        working-directory: services/${{ env.SERVICE_NAME }}/terraform
        run: terraform output -json > service-outputs.json
      
      - name: Upload outputs
        uses: actions/upload-artifact@v4
        with:
          name: service-outputs
          path: services/${{ env.SERVICE_NAME }}/terraform/service-outputs.json

  verify-deployment:
    runs-on: ubuntu-latest
    needs: deploy-service
    permissions:
      id-token: write
      contents: read
    
    steps:
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Wait for service stability
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
          # Get ALB DNS from shared infrastructure
          ALB_DNS=$(aws elbv2 describe-load-balancers \
            --names turaf-alb-${{ env.ENVIRONMENT }} \
            --query 'LoadBalancers[0].DNSName' \
            --output text)
          
          # Wait for service to be healthy
          for i in {1..30}; do
            if curl -f http://$ALB_DNS/api/${{ env.SERVICE_NAME }}/health; then
              echo "Service is healthy"
              exit 0
            fi
            echo "Waiting for service to be healthy... ($i/30)"
            sleep 10
          done
          
          echo "Service health check failed"
          exit 1
```

---

## Service Terraform Structure

Each service maintains its own Terraform configuration:

```
services/identity-service/
├── src/                          # Application code
├── Dockerfile                    # Container definition
├── terraform/                    # Service infrastructure
│   ├── backend.tf               # S3 backend for service state
│   ├── data.tf                  # Reference shared infrastructure
│   ├── main.tf                  # Service resources
│   ├── variables.tf             # Service variables
│   └── outputs.tf               # Service outputs
└── .github/
    └── workflows/
        ├── service-identity-dev.yml
        ├── service-identity-qa.yml
        └── service-identity-prod.yml
```

### Example: Service Terraform (main.tf)

```hcl
# Reference shared infrastructure
data "terraform_remote_state" "infra" {
  backend = "s3"
  config = {
    bucket = "turaf-terraform-state-${var.environment}"
    key    = "terraform.tfstate"
    region = "us-east-1"
  }
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "service" {
  name              = "/ecs/${var.service_name}-${var.environment}"
  retention_in_days = 7
  
  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "CI/CD"
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "service" {
  family                   = "${var.service_name}-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = data.terraform_remote_state.infra.outputs.ecs_execution_role_arn
  task_role_arn            = data.terraform_remote_state.infra.outputs.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = var.service_name
    image = "${data.terraform_remote_state.infra.outputs.ecr_repository_url}:${var.image_tag}"
    
    portMappings = [{
      containerPort = var.container_port
      protocol      = "tcp"
    }]
    
    environment = [
      { name = "ENVIRONMENT", value = var.environment },
      { name = "AWS_REGION", value = var.region }
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.service.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}

# ALB Target Group
resource "aws_lb_target_group" "service" {
  name        = "${substr(var.service_name, 0, 20)}-${var.environment}"
  port        = var.container_port
  protocol    = "HTTP"
  vpc_id      = data.terraform_remote_state.infra.outputs.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "CI/CD"
  }
}

# ALB Listener Rule
resource "aws_lb_listener_rule" "service" {
  listener_arn = data.terraform_remote_state.infra.outputs.alb_listener_http_arn
  priority     = var.listener_rule_priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.service.arn
  }

  condition {
    path_pattern {
      values = ["/api/${var.service_name}/*"]
    }
  }

  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "CI/CD"
  }
}

# ECS Service
resource "aws_ecs_service" "service" {
  name            = "${var.service_name}-${var.environment}"
  cluster         = data.terraform_remote_state.infra.outputs.cluster_arn
  task_definition = aws_ecs_task_definition.service.arn
  desired_count   = var.desired_count
  launch_type     = null

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 100
    base              = 0
  }

  network_configuration {
    subnets          = data.terraform_remote_state.infra.outputs.private_subnet_ids
    security_groups  = [data.terraform_remote_state.infra.outputs.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.service.arn
    container_name   = var.service_name
    container_port   = var.container_port
  }

  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener_rule.service]

  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "CI/CD"
  }
}
```

---

## CI Pipeline (ci.yml)

### Purpose and Scope

The CI pipeline serves as a **repository-wide quality gate** that runs on all code changes before deployment. It differs fundamentally from service deployment workflows:

**CI Pipeline Role**:
- **Type**: Centralized quality checks
- **When**: On every PR and code push (all branches)
- **What**: Lint, test, build, security scan
- **Scope**: All services in the monorepo (`./services` directory)
- **Purpose**: Prevent bad code from merging (quality gatekeeper)
- **Deployment**: None - verification only

**Service Deployment Workflows** (Task 008):
- **Type**: Individual service deployments
- **When**: After code is merged to target branch
- **What**: Build Docker image → Deploy to ECS → Health check
- **Scope**: One service at a time
- **Purpose**: Deploy services independently to AWS
- **Deployment**: Yes - updates running services

### Workflow Execution Flow

```
Developer pushes code
    ↓
CI Pipeline runs (quality gates)
    ├─ Lint all services
    ├─ Test all services  
    ├─ Build all services
    └─ Security scan
    ↓
✅ CI passes → Code can be merged
    ↓
Code merged to develop/release/main
    ↓
Per-Service Deployment Workflows trigger
    ├─ Only for services with changes
    ├─ Build Docker image
    ├─ Deploy to appropriate environment
    └─ Health check
```

### CI Jobs

**1. Lint**
- Runs Checkstyle on all Java code
- Enforces code style standards
- Blocks merge on violations

**2. Test**
- Executes all unit and integration tests
- Generates JaCoCo coverage reports
- Uploads coverage to Codecov
- Requires 80%+ coverage (PROJECT.md Section 23a)

**3. Build**
- Compiles all services with Maven
- Verifies no compilation errors
- Uploads build artifacts (7-day retention)

**4. Security Scan**
- Runs Snyk vulnerability scanning
- Checks dependencies for known vulnerabilities
- Uploads SARIF results to GitHub Code Scanning
- Threshold: HIGH and CRITICAL vulnerabilities

### Key Distinction

**CI Pipeline = "Can this code be merged?"** (Quality verification)  
**Deployment Workflows = "Deploy this service to AWS"** (Infrastructure automation)

The CI pipeline is a **gatekeeper** that runs first. Service deployment workflows only run **after** code passes CI and is merged to the appropriate branch.

---

## Infrastructure Pipeline (infrastructure.yml)

**Updated** - Only manages shared infrastructure, not services.

```yaml
name: Deploy Shared Infrastructure

on:
  push:
    branches: [main]
    paths:
      - 'infrastructure/terraform/**'
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to deploy'
        required: true
        type: choice
        options:
          - dev
          - qa
          - prod

jobs:
  terraform-plan:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
    strategy:
      matrix:
        environment: [dev, qa, prod]
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets[format('AWS_ROLE_{0}', matrix.environment)] }}
          aws-region: us-east-1
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
      
      - name: Terraform Init
        working-directory: infrastructure/terraform/environments/${{ matrix.environment }}
        run: terraform init
      
      - name: Terraform Plan
        working-directory: infrastructure/terraform/environments/${{ matrix.environment }}
        run: terraform plan -out=tfplan
      
      - name: Upload plan
        uses: actions/upload-artifact@v4
        with:
          name: tfplan-${{ matrix.environment }}
          path: infrastructure/terraform/environments/${{ matrix.environment }}/tfplan

  terraform-apply-dev:
    runs-on: ubuntu-latest
    needs: terraform-plan
    if: github.ref == 'refs/heads/main'
    permissions:
      id-token: write
      contents: read
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_DEV }}
          aws-region: us-east-1
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
      
      - name: Download plan
        uses: actions/download-artifact@v4
        with:
          name: tfplan-dev
          path: infrastructure/terraform/environments/dev
      
      - name: Terraform Apply
        working-directory: infrastructure/terraform/environments/dev
        run: terraform apply -auto-approve tfplan

  terraform-apply-qa:
    runs-on: ubuntu-latest
    needs: terraform-apply-dev
    environment: qa-infrastructure
    permissions:
      id-token: write
      contents: read
    
    steps:
      # Similar to dev, with manual approval via environment protection

  terraform-apply-prod:
    runs-on: ubuntu-latest
    needs: terraform-apply-qa
    environment: prod-infrastructure
    permissions:
      id-token: write
      contents: read
    
    steps:
      # Similar to qa, with stricter approval requirements
```

---

## Deployment Flow

### Initial Setup (One-time)
1. Deploy shared infrastructure via `infrastructure.yml`
2. Verify ECS cluster, ALB, databases are created
3. Create service Terraform directories
4. Set up service-specific workflows

### Service Deployment (Per service, per environment)
1. Developer pushes code to service directory
2. Workflow triggers based on path filter
3. Build and push Docker image to ECR
4. Run Terraform to deploy/update service resources
5. Wait for ECS service to stabilize
6. Run health checks

### Rollback Strategy
- Terraform state tracks previous versions
- Use `terraform apply` with previous image tag
- ECS deployment circuit breaker auto-rolls back on failure

---

## Benefits of This Architecture

1. **Independent Service Deployments**: Services deploy without affecting others
2. **Faster Iteration**: No infrastructure team bottleneck
3. **Clear Ownership**: Service teams own their infrastructure
4. **Cost Efficiency**: Shared ALB and cluster reduce costs
5. **Better Security**: Service-specific IAM policies and isolated state

---

## Migration from Old Pattern

### Step 1: Remove Services from Shared Terraform
```bash
cd infrastructure/terraform/environments/dev
terraform state rm 'module.compute.aws_ecs_service.identity_service'
terraform state rm 'module.compute.aws_ecs_task_definition.identity_service'
# ... repeat for all services
terraform apply  # Removes service resources
```

### Step 2: Create Service Terraform
```bash
cd services/identity-service
mkdir -p terraform
# Create backend.tf, data.tf, main.tf, variables.tf, outputs.tf
```

### Step 3: Deploy Service via CI/CD
```bash
# Push to trigger workflow or manually run
gh workflow run service-identity-dev.yml
```

---

## Required GitHub Secrets

**Repository Secrets**:
- `SONAR_TOKEN`
- `SONAR_HOST_URL`
- `CODECOV_TOKEN`
- `SLACK_WEBHOOK_URL`

**Environment Secrets** (per environment):
- `AWS_ROLE_DEV`: `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
- `AWS_ROLE_QA`: `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
- `AWS_ROLE_PROD`: `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`

---

## References

- **Infrastructure Restructure Summary**: `infrastructure/docs/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- **CI/CD Deployment Pattern**: `.windsurf/plans/cicd-service-deployment-pattern.md`
- **PROJECT.md**: Sections 14, 49-50
