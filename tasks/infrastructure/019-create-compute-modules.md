# Task 019: Create Compute Modules

**Service**: Infrastructure  
**Phase**: 10  
**Status**: Pending  
**Assigned To**: AI Assistant  
**Estimated Time**: 4 hours  
**Priority**: High

## Objective

Create Terraform modules for ECS Fargate compute resources including cluster, task definitions, and services.

**Cost Optimization**: Use minimal Fargate configuration with Spot instances for demo (~$15/month for 3 services). for all microservices.

## Prerequisites

- [x] Task 002: Networking module created

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/compute/ecs-cluster.tf`
- `infrastructure/terraform/modules/compute/ecs-services.tf`
- `infrastructure/terraform/modules/compute/task-definitions.tf`
- `infrastructure/terraform/modules/compute/alb.tf`

## Implementation Details

### ECS Cluster

```hcl
resource "aws_ecs_cluster" "main" {
  name = "turaf-cluster-${var.environment}"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}
```

### Task Definitions

```hcl
resource "aws_ecs_task_definition" "identity_service" {
  family                   = "identity-service-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn
  
  container_definitions = jsonencode([{
    name  = "identity-service"
    image = "${var.ecr_repository_url}/identity-service:${var.image_tag}"
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    environment = [
      {
        name  = "ENVIRONMENT"
        value = var.environment
      }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/identity-service-${var.environment}"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}
```

### ECS Services (Cost-Optimized for Demo)

**Demo Configuration**: Deploy only 3 core services with minimal resources

1. **identity-service**
   - Task count: 1
   - CPU: 256 (0.25 vCPU) - Minimal
   - Memory: 512 MB - Minimal
   - Capacity Provider: FARGATE_SPOT (70% savings)
   - Auto-scaling: Disabled for demo
   - **Cost**: ~$5/month

2. **organization-service**
   - Task count: 1
   - CPU: 256 (0.25 vCPU)
   - Memory: 512 MB
   - Capacity Provider: FARGATE_SPOT
   - Auto-scaling: Disabled for demo
   - **Cost**: ~$5/month

3. **experiment-service**
   - Task count: 1
   - CPU: 256 (0.25 vCPU)
   - Memory: 512 MB
   - Capacity Provider: FARGATE_SPOT
   - Auto-scaling: Disabled for demo
   - **Cost**: ~$5/month

**Services Deferred for Demo** (Can be added later):
- ❌ **metrics-service**: Use CloudWatch directly
- ❌ **reporting-service**: Generate reports on-demand
- ❌ **notification-service**: Use SES directly from services

**Total ECS Cost**: ~$15/month (vs ~$90/month for all services)

**Production Configuration** (Optional):

1. **identity-service**
   - Task count: 2
   - CPU: 512 (0.5 vCPU)
   - Memory: 1024 MB
   - Auto-scaling: 2-10 tasks

2. **organization-service**
   - Task count: 2
   - CPU: 512
   - Memory: 1024 MB
   - Auto-scaling: 2-10 tasks

3. **experiment-service**
   - Task count: 2
   - CPU: 1024 (1 vCPU)
   - Memory: 2048 MB
   - Auto-scaling: 2-10 tasks

4. **metrics-service**
   - Task count: 2
   - CPU: 1024
   - Memory: 2048 MB
   - Auto-scaling: 2-10 tasks

5. **reporting-service**
   - Task count: 1
   - CPU: 512
   - Memory: 1024 MB
   - Auto-scaling: 1-5 tasks

6. **notification-service**
   - Task count: 1
   - CPU: 256
   - Memory: 512 MB
   - Auto-scaling: 1-3 tasks

### Application Load Balancer

```hcl
resource "aws_lb" "main" {
  name               = "turaf-alb-${var.environment}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.public_subnet_ids
  
  enable_deletion_protection = var.environment == "prod" ? true : false
}

resource "aws_lb_target_group" "identity_service" {
  name        = "identity-service-${var.environment}"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  
  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 10
  }
}
```

## Acceptance Criteria

- [x] ECS cluster created
- [x] Task definitions for all services (core + optional)
- [x] ECS services configured with Fargate Spot
- [x] ALB created with target groups
- [x] ALB listeners and routing rules
- [x] CloudWatch log groups
- [x] Auto-scaling configuration (optional)
- [x] Module outputs created
- [x] Cost optimization variables
- [ ] terraform plan succeeds (requires environment configuration)

## Implementation Results (2024-03-23)

### ✅ Module Created

**Files Created**:
- ✅ `infrastructure/terraform/modules/compute/main.tf` (350 lines) - ECS cluster, ALB, target groups, listeners
- ✅ `infrastructure/terraform/modules/compute/task-definitions.tf` (450 lines) - All service task definitions
- ✅ `infrastructure/terraform/modules/compute/ecs-services.tf` (400 lines) - ECS services with auto-scaling
- ✅ `infrastructure/terraform/modules/compute/variables.tf` (305 lines) - Cost-optimized variables
- ✅ `infrastructure/terraform/modules/compute/outputs.tf` (170 lines) - All outputs
- ✅ `infrastructure/terraform/modules/compute/README.md` (comprehensive documentation)

### 📦 Compute Configuration

**Demo Approach** (Cost-Optimized):
- ECS Fargate cluster with Fargate Spot (70% savings)
- 3 core services: Identity, Organization, Experiment
- 0.25 vCPU, 512 MB per service
- 1 task per service (no auto-scaling)
- Container Insights disabled
- 7-day log retention
- **Cost**: ~$15/month (Fargate) + ~$16/month (ALB) = **~$32/month**

**Service Architecture**:
```
Application Load Balancer (HTTPS)
├── /api/identity/*      → Identity Service
├── /api/organizations/* → Organization Service
└── /api/experiments/*   → Experiment Service

ECS Cluster: turaf-cluster-dev
├── Capacity Provider: FARGATE_SPOT
├── Container Insights: Disabled
└── Auto-scaling: Disabled
```

**Services Deferred for Demo**:
- ❌ Metrics Service (use CloudWatch directly)
- ❌ Reporting Service (generate on-demand)
- ❌ Notification Service (use SES directly)

**Production Option**:
- 6 services with standard Fargate
- 0.5-1 vCPU, 1-2 GB per service
- 2-10 tasks per service with auto-scaling
- Container Insights enabled
- 30-day log retention
- **Cost**: ~$180-300/month

### 🎯 Key Features

**ECS Cluster**:
- Fargate Spot capacity provider (default)
- Optional standard Fargate
- Optional Container Insights
- ECS Exec support (disabled by default)

**Application Load Balancer**:
- HTTPS listener with ACM certificate
- HTTP → HTTPS redirect
- Path-based routing rules
- Health checks on `/actuator/health`
- 30-second deregistration delay

**Task Definitions**:
- Spring Boot microservices (port 8080)
- Database credentials from Secrets Manager
- CloudWatch Logs integration
- Container health checks
- Environment-specific configuration

**ECS Services**:
- Fargate Spot for cost savings
- Circuit breaker with automatic rollback
- Rolling deployments (200% max, 100% min)
- Optional CPU-based auto-scaling
- Private subnet deployment

### 💰 Cost Breakdown

**Demo Configuration**:
- Fargate Spot (3 services × 0.25 vCPU × 512 MB): ~$15/month
- ALB (base + minimal LCUs): ~$16/month
- CloudWatch Logs (7-day retention): ~$0.50/month
- **Total**: ~$32/month

**Production Configuration** (all services):
- Fargate Standard (6 services, larger sizes): ~$150/month
- ALB: ~$20/month
- Container Insights: ~$2/month
- CloudWatch Logs (30-day retention): ~$5/month
- **Total**: ~$180-300/month

### 🎯 Module Inputs

| Variable | Default | Purpose |
|----------|---------|---------|
| use_fargate_spot | true | Use Spot for 70% savings |
| enable_container_insights | false | Disable to save $2/month |
| enable_autoscaling | false | Disable for demo |
| enable_metrics_service | false | Defer for demo |
| enable_reporting_service | false | Defer for demo |
| enable_notification_service | false | Defer for demo |
| *_service_cpu | 256 | Minimal CPU (0.25 vCPU) |
| *_service_memory | 512 | Minimal memory |
| *_service_desired_count | 1 | Single task |
| log_retention_days | 7 | Short retention for demo |

### 📤 Module Outputs

- ECS cluster ID, name, ARN
- ALB DNS name and zone ID
- All service names and ARNs
- Target group ARNs
- CloudWatch log group names
- Service summary with configuration

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify task definition JSON structure
- Check ALB listener rules
- Verify health check paths
- Test HTTPS redirect
- Check security group rules

## References

- Specification: `specs/aws-infrastructure.md` (Compute section)
- Module Documentation: `infrastructure/terraform/modules/compute/README.md`
- Related Tasks: 015-create-security-modules, 016-create-database-module, 014-create-networking-module
