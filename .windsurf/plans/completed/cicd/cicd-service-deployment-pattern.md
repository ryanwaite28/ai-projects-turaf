---
description: CI/CD Pattern for Service-Specific ECS Deployments
---

# CI/CD Service Deployment Pattern

## Overview

The Turaf infrastructure uses a **split responsibility model**:

- **Terraform (Infrastructure as Code):** Manages shared, long-lived infrastructure
- **CI/CD Pipelines (per service):** Manages service-specific, frequently-changing resources

This pattern allows each microservice to independently manage its deployment lifecycle without requiring infrastructure team intervention for every service update.

---

## Shared Infrastructure (Managed by Terraform)

The following resources are provisioned once and shared across all services:

### ECS Cluster
- **Resource:** `aws_ecs_cluster.main`
- **Name:** `turaf-cluster-dev`
- **Capacity Providers:** FARGATE, FARGATE_SPOT
- **Container Insights:** Configurable

### Application Load Balancer (ALB)
- **Resource:** `aws_lb.main`
- **Name:** `turaf-alb-dev`
- **Type:** Internet-facing
- **Listeners:**
  - HTTP (port 80) - Always created
  - HTTPS (port 443) - Optional, based on ACM certificate

### Networking
- VPC and subnets
- Security groups (ALB, ECS tasks)
- VPC endpoints

### IAM Roles
- ECS execution role (for pulling images, accessing Secrets Manager)
- ECS task role (for application runtime permissions)

### Database & Cache
- RDS PostgreSQL
- ElastiCache Redis
- Secrets Manager for credentials

---

## Service-Specific Resources (Managed by CI/CD)

Each microservice's CI/CD pipeline manages its own:

### 1. ECS Task Definition
```hcl
resource "aws_ecs_task_definition" "service" {
  family                   = "my-service-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = data.terraform_remote_state.infra.outputs.ecs_execution_role_arn
  task_role_arn            = data.terraform_remote_state.infra.outputs.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = "my-service"
    image = "${var.ecr_repository_url}:${var.image_tag}"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      { name = "ENVIRONMENT", value = var.environment },
      { name = "AWS_REGION", value = var.region }
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/my-service-${var.environment}"
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}
```

### 2. CloudWatch Log Group
```hcl
resource "aws_cloudwatch_log_group" "service" {
  name              = "/ecs/my-service-${var.environment}"
  retention_in_days = 7

  tags = {
    Service     = "my-service"
    Environment = var.environment
    ManagedBy   = "CI/CD"
  }
}
```

### 3. ALB Target Group
```hcl
resource "aws_lb_target_group" "service" {
  name        = "my-svc-${var.environment}"
  port        = 8080
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
    Service     = "my-service"
    Environment = var.environment
    ManagedBy   = "CI/CD"
  }
}
```

### 4. ALB Listener Rule
```hcl
resource "aws_lb_listener_rule" "service" {
  listener_arn = data.terraform_remote_state.infra.outputs.alb_listener_http_arn
  priority     = var.listener_rule_priority  # Each service gets unique priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.service.arn
  }

  condition {
    path_pattern {
      values = ["/api/my-service/*"]
    }
  }

  tags = {
    Service     = "my-service"
    Environment = var.environment
    ManagedBy   = "CI/CD"
  }
}
```

### 5. ECS Service
```hcl
resource "aws_ecs_service" "service" {
  name            = "my-service-${var.environment}"
  cluster         = data.terraform_remote_state.infra.outputs.cluster_arn
  task_definition = aws_ecs_task_definition.service.arn
  desired_count   = var.desired_count
  launch_type     = null  # Use capacity provider strategy

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
    container_name   = "my-service"
    container_port   = 8080
  }

  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = {
    Service     = "my-service"
    Environment = var.environment
    ManagedBy   = "CI/CD"
  }
}
```

---

## CI/CD Pipeline Structure

### Directory Structure (per service)
```
services/identity-service/
├── src/                          # Application code
├── Dockerfile                    # Container definition
├── .github/
│   └── workflows/
│       ├── build.yml            # Build & test
│       └── deploy.yml           # Deploy to ECS
└── terraform/                   # Service-specific infrastructure
    ├── backend.tf               # Remote state for service
    ├── data.tf                  # Reference shared infrastructure
    ├── main.tf                  # Service resources
    ├── variables.tf
    └── outputs.tf
```

### GitHub Actions Workflow Example

#### Build Workflow (`.github/workflows/build.yml`)
```yaml
name: Build and Push

on:
  push:
    branches: [main, develop]
    paths:
      - 'services/identity-service/**'

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: us-east-1
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      
      - name: Build and push Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_REPOSITORY: turaf/identity-service
          IMAGE_TAG: ${{ github.sha }}
        run: |
          cd services/identity-service
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:latest
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
```

#### Deploy Workflow (`.github/workflows/deploy.yml`)
```yaml
name: Deploy to ECS

on:
  workflow_run:
    workflows: ["Build and Push"]
    types: [completed]
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    permissions:
      id-token: write
      contents: read
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: us-east-1
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
      
      - name: Terraform Init
        working-directory: services/identity-service/terraform
        run: terraform init
      
      - name: Terraform Apply
        working-directory: services/identity-service/terraform
        env:
          TF_VAR_image_tag: ${{ github.sha }}
          TF_VAR_environment: dev
        run: terraform apply -auto-approve
```

---

## Service Terraform Configuration

### `services/identity-service/terraform/backend.tf`
```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-dev"
    key            = "services/identity-service/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "turaf-terraform-locks-dev"
  }
}
```

### `services/identity-service/terraform/data.tf`
```hcl
# Reference shared infrastructure
data "terraform_remote_state" "infra" {
  backend = "s3"
  
  config = {
    bucket = "turaf-terraform-state-dev"
    key    = "terraform.tfstate"
    region = "us-east-1"
  }
}

# Convenience locals
locals {
  cluster_arn            = data.terraform_remote_state.infra.outputs.cluster_arn
  cluster_name           = data.terraform_remote_state.infra.outputs.cluster_name
  vpc_id                 = data.terraform_remote_state.infra.outputs.vpc_id
  private_subnet_ids     = data.terraform_remote_state.infra.outputs.private_subnet_ids
  ecs_security_group_id  = data.terraform_remote_state.infra.outputs.ecs_security_group_id
  ecs_execution_role_arn = data.terraform_remote_state.infra.outputs.ecs_execution_role_arn
  ecs_task_role_arn      = data.terraform_remote_state.infra.outputs.ecs_task_role_arn
  alb_listener_arn       = data.terraform_remote_state.infra.outputs.alb_listener_http_arn
  ecr_repository_url     = data.terraform_remote_state.infra.outputs.identity_service_ecr_url
}
```

### `services/identity-service/terraform/variables.tf`
```hcl
variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

variable "desired_count" {
  description = "Number of tasks to run"
  type        = number
  default     = 1
}

variable "cpu" {
  description = "Task CPU units"
  type        = number
  default     = 256
}

variable "memory" {
  description = "Task memory in MB"
  type        = number
  default     = 512
}

variable "listener_rule_priority" {
  description = "ALB listener rule priority (must be unique)"
  type        = number
  default     = 100
}
```

---

## Deployment Flow

### Initial Infrastructure Setup (One-time)
```bash
cd infrastructure/terraform/environments/dev
terraform init
terraform apply  # Deploys shared infrastructure
```

### Service Deployment (Per service, via CI/CD)
```bash
# Triggered automatically on push to main branch
# Or manually:
cd services/identity-service/terraform
terraform init
terraform apply -var="image_tag=v1.2.3"
```

---

## Benefits of This Pattern

1. **Independent Service Deployments**
   - Each service can deploy without affecting others
   - No need to redeploy entire infrastructure for service changes

2. **Faster Iteration**
   - Service teams own their deployment configuration
   - No bottleneck on infrastructure team

3. **Clear Separation of Concerns**
   - Infrastructure team: Shared resources (VPC, cluster, ALB)
   - Service teams: Service-specific resources (tasks, target groups)

4. **Cost Efficiency**
   - Shared ALB and cluster reduce costs
   - Services can scale independently

5. **Better Security**
   - Service-specific IAM policies
   - Isolated Terraform state per service
   - Principle of least privilege

---

## Listener Rule Priority Management

Each service must use a unique priority for its ALB listener rule:

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

## Required Terraform Outputs from Shared Infrastructure

The shared infrastructure must export these outputs for CI/CD pipelines:

```hcl
# ECS
- cluster_name
- cluster_arn
- cluster_id

# ALB
- alb_arn
- alb_dns_name
- alb_listener_http_arn
- alb_listener_https_arn

# Networking
- vpc_id
- private_subnet_ids
- ecs_security_group_id

# IAM
- ecs_execution_role_arn
- ecs_task_role_arn

# ECR
- <service>_ecr_repository_url (per service)
```

---

## Migration from Current State

### Step 1: Destroy Service-Specific Resources
```bash
cd infrastructure/terraform/environments/dev

# Remove services from state (they'll be recreated by CI/CD)
terraform state rm 'module.compute.aws_ecs_service.identity_service'
terraform state rm 'module.compute.aws_ecs_service.organization_service'
terraform state rm 'module.compute.aws_ecs_service.experiment_service'
terraform state rm 'module.compute.aws_ecs_task_definition.identity_service'
terraform state rm 'module.compute.aws_ecs_task_definition.organization_service'
terraform state rm 'module.compute.aws_ecs_task_definition.experiment_service'
terraform state rm 'module.compute.aws_lb_target_group.identity_service'
terraform state rm 'module.compute.aws_lb_target_group.organization_service'
terraform state rm 'module.compute.aws_lb_target_group.experiment_service'
terraform state rm 'module.compute.aws_lb_listener_rule.identity_service'
terraform state rm 'module.compute.aws_lb_listener_rule.organization_service'
terraform state rm 'module.compute.aws_lb_listener_rule.experiment_service'
terraform state rm 'module.compute.aws_cloudwatch_log_group.identity_service'
terraform state rm 'module.compute.aws_cloudwatch_log_group.organization_service'
terraform state rm 'module.compute.aws_cloudwatch_log_group.experiment_service'
```

### Step 2: Apply Updated Infrastructure
```bash
# This will keep shared resources, remove service-specific ones
terraform apply
```

### Step 3: Set Up Service Terraform
```bash
# For each service
cd services/identity-service
mkdir -p terraform
# Create backend.tf, data.tf, main.tf, variables.tf
```

### Step 4: Deploy Services via CI/CD
```bash
# Push to trigger CI/CD or manually:
cd services/identity-service/terraform
terraform init
terraform apply
```

---

## Best Practices

### 1. State Management
- Use separate S3 state files per service
- Enable state locking with DynamoDB
- Use remote state data sources to reference shared infrastructure

### 2. Naming Conventions
- Services: `<service-name>-${environment}`
- Target groups: `<short-name>-${environment}` (max 32 chars)
- Log groups: `/ecs/<service-name>-${environment}`

### 3. Tagging Strategy
```hcl
tags = {
  Service     = "identity-service"
  Environment = var.environment
  ManagedBy   = "CI/CD"
  Repository  = "turaf"
  Component   = "microservice"
}
```

### 4. Security
- Never hardcode secrets in task definitions
- Use Secrets Manager or Parameter Store
- Reference secrets by ARN in task definition
- Use least-privilege IAM policies per service

### 5. Rollback Strategy
- Keep previous task definition revisions
- Use deployment circuit breaker
- Monitor CloudWatch alarms during deployment
- Implement blue/green deployments for critical services

---

## Troubleshooting

### Issue: Listener rule priority conflict
**Solution:** Ensure each service uses a unique priority in the assigned range

### Issue: Target group not found
**Solution:** Verify target group is created before ECS service

### Issue: Task fails to start
**Solution:** 
- Check CloudWatch logs
- Verify ECR image exists
- Check task role permissions
- Verify secrets are accessible

### Issue: Health check failing
**Solution:**
- Verify health check path is correct
- Ensure service is listening on correct port
- Check security group allows ALB → ECS traffic

---

## Example: Complete Service Deployment

See `services/identity-service/terraform/` for a complete working example of this pattern.

---

## Summary

This pattern provides:
- ✅ Clear separation between infrastructure and application deployments
- ✅ Independent service lifecycle management
- ✅ Faster iteration and deployment cycles
- ✅ Better security and isolation
- ✅ Scalable architecture for growing teams
