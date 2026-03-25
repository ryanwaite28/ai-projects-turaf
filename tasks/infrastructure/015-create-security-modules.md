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

- [x] IAM roles created (ECS execution and task roles)
- [x] Security groups configured (ALB, ECS, RDS, ElastiCache, DocumentDB)
- [x] KMS keys created (main, RDS, S3)
- [x] IAM policies configured with least privilege
- [x] Module documentation created
- [ ] terraform plan succeeds (requires environment configuration)

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify IAM policies
- Check security group rules

## Implementation Results (2024-03-23)

### ✅ Module Created

**Files Created**:
- ✅ `infrastructure/terraform/modules/security/main.tf` (450 lines)
- ✅ `infrastructure/terraform/modules/security/variables.tf` (40 lines)
- ✅ `infrastructure/terraform/modules/security/outputs.tf` (75 lines)
- ✅ `infrastructure/terraform/modules/security/README.md` (comprehensive documentation)

### 🔐 Security Components

**IAM Roles**:
- **ECS Execution Role**: Pull images, retrieve secrets, write logs
- **ECS Task Role**: Access S3, SQS, SNS, SES, Secrets Manager

**Security Groups** (5 groups):
- **ALB**: Ports 80/443 from internet
- **ECS Tasks**: Port 8080 from ALB and VPC
- **RDS**: Port 5432 from ECS tasks
- **ElastiCache**: Port 6379 from ECS tasks
- **DocumentDB**: Port 27017 from ECS tasks

**KMS Keys** (3 keys):
- **Main KMS Key**: General encryption, Secrets Manager
- **RDS KMS Key**: Database encryption
- **S3 KMS Key**: Bucket encryption

### 📊 Features

- ✅ **Least Privilege**: Fine-grained IAM permissions
- ✅ **Network Security**: Security groups with minimal access
- ✅ **Encryption**: KMS keys with rotation enabled
- ✅ **Secrets Management**: Integration with Secrets Manager
- ✅ **Audit Trail**: CloudWatch Logs integration
- ✅ **Resource Scoping**: Permissions limited to environment resources

### 🎯 IAM Permissions

**ECS Execution Role**:
- AWS Managed: `AmazonECSTaskExecutionRolePolicy`
- Custom: Secrets Manager, KMS, CloudWatch Logs

**ECS Task Role**:
- S3: Get/Put/Delete objects (scoped to `turaf-{env}-*`)
- SQS: Send/Receive/Delete messages (scoped to `turaf-{env}-*`)
- SNS: Publish (scoped to `turaf-{env}-*`)
- SES: Send emails (condition: from `noreply@turafapp.com`)
- Secrets Manager: Get secrets (scoped to `turaf/{env}/*`)
- KMS: Decrypt (scoped to environment key)

### 🔒 Security Best Practices

- ✅ **Least Privilege**: All permissions scoped to specific resources
- ✅ **Encryption**: KMS key rotation enabled (30-day deletion window)
- ✅ **Network Isolation**: Security groups allow only required ports
- ✅ **Secrets Protection**: No hardcoded credentials
- ✅ **Audit**: CloudWatch Logs for all operations
- ✅ **Conditions**: SES restricted to specific sender address

### 💰 Cost Estimation

**Per Environment**:
- KMS Keys (3): $3/month
- Secrets Manager (~10 secrets): $4/month
- Security Groups & IAM: Free
- **Total**: ~$7/month

### 🎯 Module Inputs

| Variable | Description | Required |
|----------|-------------|----------|
| environment | Environment name (dev/qa/prod) | Yes |
| account_id | AWS account ID | Yes |
| vpc_id | VPC ID for security groups | Yes |
| vpc_cidr | VPC CIDR block | Yes |

### 📤 Module Outputs

- IAM role ARNs (execution and task)
- Security group IDs (ALB, ECS, RDS, ElastiCache, DocumentDB)
- KMS key ARNs (main, RDS, S3)

### 📋 Next Steps

1. Create database module (Task 016)
2. Create storage modules (Task 017)
3. Create messaging modules (Task 018)
4. Create compute modules (Task 019)

## References

- Specification: `specs/aws-infrastructure.md` (Security section)
- Related Tasks: 016-create-database-module, 019-create-compute-modules
- Module Documentation: `infrastructure/terraform/modules/security/README.md`
