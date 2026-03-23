# Task: Create Security Modules

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 3 hours  

## Objective

Create Terraform module for IAM roles, security groups, Secrets Manager, and KMS keys.

## Prerequisites

- [x] Task 002: Networking module created

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/security/iam.tf`
- `infrastructure/terraform/modules/security/security-groups.tf`
- `infrastructure/terraform/modules/security/secrets.tf`
- `infrastructure/terraform/modules/security/kms.tf`

## Implementation Details

### IAM Roles

```hcl
resource "aws_iam_role" "ecs_execution_role" {
  name = "ecs-execution-role-${var.environment}"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution_role_policy" {
  role       = aws_iam_role.ecs_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_task_role" {
  name = "ecs-task-role-${var.environment}"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}
```

### Security Groups

```hcl
resource "aws_security_group" "alb" {
  name        = "turaf-alb-sg-${var.environment}"
  description = "Security group for ALB"
  vpc_id      = var.vpc_id
  
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs_tasks" {
  name        = "turaf-ecs-tasks-sg-${var.environment}"
  description = "Security group for ECS tasks"
  vpc_id      = var.vpc_id
  
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "database" {
  name        = "turaf-database-sg-${var.environment}"
  description = "Security group for RDS databases"
  vpc_id      = var.vpc_id
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}
```

### KMS Keys

```hcl
resource "aws_kms_key" "main" {
  description             = "KMS key for Turaf ${var.environment}"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  
  tags = {
    Name        = "turaf-kms-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_kms_alias" "main" {
  name          = "alias/turaf-${var.environment}"
  target_key_id = aws_kms_key.main.key_id
}
```

## Acceptance Criteria

- [ ] IAM roles created
- [ ] Security groups configured
- [ ] KMS keys created
- [ ] Secrets Manager configured
- [ ] Least privilege access enforced
- [ ] terraform plan succeeds

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify IAM policies
- Check security group rules

## References

- Specification: `specs/aws-infrastructure.md` (Security section)
- Related Tasks: 009-create-monitoring-modules
