# Task: Create Compute Modules

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 4 hours  

## Objective

Create Terraform modules for ECS Fargate clusters, task definitions, and services for all microservices.

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
  cpu                      = "512"
  memory                   = "1024"
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

- [ ] ECS cluster created
- [ ] Task definitions for all services
- [ ] ECS services configured
- [ ] ALB created with target groups
- [ ] Security groups configured
- [ ] terraform plan succeeds

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify task definition JSON
- Check ALB configuration

## References

- Specification: `specs/aws-infrastructure.md` (Compute section)
- Related Tasks: 004-create-database-module
