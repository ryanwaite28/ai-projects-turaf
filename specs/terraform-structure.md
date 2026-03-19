# Terraform Structure Specification

**Source**: PROJECT.md (Sections 15, 51)

This specification defines the Terraform module organization and structure for infrastructure as code.

---

## Terraform Overview

**IaC Tool**: Terraform  
**Provider**: AWS  
**State Backend**: S3 with DynamoDB locking  
**Architecture**: Multi-Account AWS Organization  
**GitHub Repository**: https://github.com/ryanwaite28/ai-projects-turaf  

### AWS Account Mapping

| Environment | AWS Account ID | State Bucket | State Key Prefix |
|-------------|---------------|--------------|------------------|
| **DEV** | 801651112319 | `turaf-terraform-state-dev` | `dev/` |
| **QA** | 965932217544 | `turaf-terraform-state-qa` | `qa/` |
| **PROD** | 811783768245 | `turaf-terraform-state-prod` | `prod/` |
| **Ops** | 146072879609 | `turaf-terraform-state-ops` | `ops/` |

**Note**: Each AWS account has its own S3 bucket for Terraform state to maintain account isolation.

---

## Repository Structure

```
infrastructure/
├── modules/
│   ├── networking/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── README.md
│   ├── compute/
│   │   ├── ecs-cluster/
│   │   ├── ecs-service/
│   │   └── alb/
│   ├── database/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── storage/
│   │   ├── s3/
│   │   └── ecr/
│   ├── messaging/
│   │   ├── eventbridge/
│   │   └── sqs/
│   ├── lambda/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── monitoring/
│   │   ├── cloudwatch/
│   │   └── xray/
│   └── security/
│       ├── iam/
│       ├── secrets/
│       ├── kms/
│       └── waf/
├── environments/
│   ├── dev/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── terraform.tfvars
│   │   └── backend.tf
│   ├── qa/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── terraform.tfvars
│   │   └── backend.tf
│   └── prod/
│       ├── main.tf
│       ├── variables.tf
│       ├── terraform.tfvars
│       └── backend.tf
├── backend.tf
├── versions.tf
└── README.md
```

---

## Module Specifications

### Networking Module

**Path**: `modules/networking/`

**Purpose**: Create VPC, subnets, route tables, NAT gateways, security groups

**Inputs**:
```hcl
variable "environment" {
  type = string
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "availability_zones" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b"]
}

variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.11.0/24", "10.0.12.0/24"]
}
```

**Outputs**:
```hcl
output "vpc_id" {
  value = aws_vpc.main.id
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  value = aws_subnet.private[*].id
}

output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  value = aws_security_group.ecs.id
}

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}
```

---

### Compute Module - ECS Cluster

**Path**: `modules/compute/ecs-cluster/`

**Purpose**: Create ECS cluster with capacity providers

**Inputs**:
```hcl
variable "cluster_name" {
  type = string
}

variable "enable_container_insights" {
  type    = bool
  default = true
}
```

**Outputs**:
```hcl
output "cluster_id" {
  value = aws_ecs_cluster.main.id
}

output "cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "cluster_arn" {
  value = aws_ecs_cluster.main.arn
}
```

---

### Compute Module - ECS Service

**Path**: `modules/compute/ecs-service/`

**Purpose**: Create ECS service with task definition, auto-scaling, and ALB integration

**Inputs**:
```hcl
variable "service_name" {
  type = string
}

variable "cluster_id" {
  type = string
}

variable "task_cpu" {
  type = number
}

variable "task_memory" {
  type = number
}

variable "desired_count" {
  type = number
}

variable "container_image" {
  type = string
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "target_group_arn" {
  type = string
}

variable "environment_variables" {
  type    = map(string)
  default = {}
}

variable "secrets" {
  type = list(object({
    name      = string
    valueFrom = string
  }))
  default = []
}
```

---

### Compute Module - ALB

**Path**: `modules/compute/alb/`

**Purpose**: Create Application Load Balancer with listeners and target groups

**Inputs**:
```hcl
variable "name" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "certificate_arn" {
  type = string
}

variable "services" {
  type = list(object({
    name          = string
    path_patterns = list(string)
    priority      = number
    health_check_path = string
  }))
}
```

**Outputs**:
```hcl
output "alb_arn" {
  value = aws_lb.main.arn
}

output "alb_dns_name" {
  value = aws_lb.main.dns_name
}

output "target_group_arns" {
  value = { for k, v in aws_lb_target_group.services : k => v.arn }
}
```

---

### Database Module

**Path**: `modules/database/`

**Purpose**: Create RDS PostgreSQL instance with subnet group and parameter group

**Inputs**:
```hcl
variable "identifier" {
  type = string
}

variable "instance_class" {
  type = string
}

variable "allocated_storage" {
  type = number
}

variable "engine_version" {
  type    = string
  default = "15.3"
}

variable "database_name" {
  type = string
}

variable "master_username" {
  type = string
}

variable "master_password" {
  type      = string
  sensitive = true
}

variable "multi_az" {
  type = bool
}

variable "backup_retention_period" {
  type = number
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "kms_key_id" {
  type = string
}
```

**Outputs**:
```hcl
output "endpoint" {
  value = aws_db_instance.main.endpoint
}

output "address" {
  value = aws_db_instance.main.address
}

output "port" {
  value = aws_db_instance.main.port
}

output "database_name" {
  value = aws_db_instance.main.db_name
}
```

---

### Storage Module - S3

**Path**: `modules/storage/s3/`

**Purpose**: Create S3 bucket with versioning, encryption, and lifecycle policies

**Inputs**:
```hcl
variable "bucket_name" {
  type = string
}

variable "versioning_enabled" {
  type    = bool
  default = true
}

variable "lifecycle_rules" {
  type = list(object({
    id      = string
    enabled = bool
    transitions = list(object({
      days          = number
      storage_class = string
    }))
    expiration_days = number
  }))
  default = []
}
```

---

### Storage Module - ECR

**Path**: `modules/storage/ecr/`

**Purpose**: Create ECR repositories for container images

**Inputs**:
```hcl
variable "repository_name" {
  type = string
}

variable "image_scanning_enabled" {
  type    = bool
  default = true
}

variable "image_tag_mutability" {
  type    = string
  default = "MUTABLE"
}
```

---

### Messaging Module - EventBridge

**Path**: `modules/messaging/eventbridge/`

**Purpose**: Create EventBridge event bus, rules, and targets

**Inputs**:
```hcl
variable "event_bus_name" {
  type = string
}

variable "rules" {
  type = list(object({
    name          = string
    description   = string
    event_pattern = string
    targets = list(object({
      arn                = string
      dead_letter_arn    = string
      retry_attempts     = number
      max_event_age      = number
    }))
  }))
}
```

---

### Lambda Module

**Path**: `modules/lambda/`

**Purpose**: Create Lambda function with IAM role, CloudWatch logs, and VPC config

**Inputs**:
```hcl
variable "function_name" {
  type = string
}

variable "runtime" {
  type    = string
  default = "java17"
}

variable "handler" {
  type = string
}

variable "memory_size" {
  type = number
}

variable "timeout" {
  type = number
}

variable "filename" {
  type = string
}

variable "environment_variables" {
  type    = map(string)
  default = {}
}

variable "vpc_config" {
  type = object({
    subnet_ids         = list(string)
    security_group_ids = list(string)
  })
  default = null
}

variable "reserved_concurrent_executions" {
  type    = number
  default = -1
}
```

---

### Security Module - IAM

**Path**: `modules/security/iam/`

**Purpose**: Create IAM roles and policies for services

**Roles Created**:
- ECS Task Execution Role
- ECS Task Role (per service)
- Lambda Execution Role (per function)
- GitHub Actions OIDC Role

---

### Security Module - Secrets Manager

**Path**: `modules/security/secrets/`

**Purpose**: Create and manage secrets

**Inputs**:
```hcl
variable "secrets" {
  type = map(object({
    description     = string
    secret_string   = string
    recovery_window = number
  }))
}
```

---

### Security Module - KMS

**Path**: `modules/security/kms/`

**Purpose**: Create KMS keys for encryption

**Keys Created**:
- RDS encryption key
- S3 encryption key
- Secrets Manager encryption key

---

## Environment Configuration

### DEV Environment

**File**: `environments/dev/terraform.tfvars`

```hcl
environment = "dev"
aws_region  = "us-east-1"

# Networking
vpc_cidr             = "10.0.0.0/16"
availability_zones   = ["us-east-1a", "us-east-1b"]

# Database
db_instance_class        = "db.t3.micro"
db_allocated_storage     = 20
db_multi_az             = false
db_backup_retention_days = 7

# ECS
ecs_services = {
  identity-service = {
    cpu           = 512
    memory        = 1024
    desired_count = 1
  }
  organization-service = {
    cpu           = 512
    memory        = 1024
    desired_count = 1
  }
  experiment-service = {
    cpu           = 1024
    memory        = 2048
    desired_count = 1
  }
  metrics-service = {
    cpu           = 1024
    memory        = 2048
    desired_count = 1
  }
}

# Lambda
lambda_reporting_memory  = 1024
lambda_notification_memory = 512
```

---

### QA Environment

**File**: `environments/qa/terraform.tfvars`

```hcl
environment = "qa"
aws_region  = "us-east-1"

# Database
db_instance_class        = "db.t3.small"
db_allocated_storage     = 50
db_multi_az             = true
db_backup_retention_days = 7

# ECS
ecs_services = {
  identity-service = {
    cpu           = 512
    memory        = 1024
    desired_count = 2
  }
  organization-service = {
    cpu           = 512
    memory        = 1024
    desired_count = 2
  }
  experiment-service = {
    cpu           = 1024
    memory        = 2048
    desired_count = 2
  }
  metrics-service = {
    cpu           = 1024
    memory        = 2048
    desired_count = 2
  }
}
```

---

### PROD Environment

**File**: `environments/prod/terraform.tfvars`

```hcl
environment = "prod"
aws_region  = "us-east-1"

# Database
db_instance_class        = "db.t3.medium"
db_allocated_storage     = 100
db_multi_az             = true
db_backup_retention_days = 30
db_deletion_protection  = true

# ECS with Auto-scaling
ecs_services = {
  identity-service = {
    cpu           = 512
    memory        = 1024
    desired_count = 2
    min_count     = 2
    max_count     = 10
  }
  organization-service = {
    cpu           = 512
    memory        = 1024
    desired_count = 2
    min_count     = 2
    max_count     = 10
  }
  experiment-service = {
    cpu           = 1024
    memory        = 2048
    desired_count = 2
    min_count     = 2
    max_count     = 10
  }
  metrics-service = {
    cpu           = 1024
    memory        = 2048
    desired_count = 2
    min_count     = 2
    max_count     = 10
  }
}
```

---

## State Management

### Backend Configuration

Each environment has its own backend configuration in the respective AWS account.

**DEV Environment** (`environments/dev/backend.tf`):

```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-dev"
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "turaf-terraform-locks-dev"
    kms_key_id     = "arn:aws:kms:us-east-1:801651112319:key/KEY_ID"
  }
}
```

**QA Environment** (`environments/qa/backend.tf`):

```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-qa"
    key            = "qa/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "turaf-terraform-locks-qa"
    kms_key_id     = "arn:aws:kms:us-east-1:965932217544:key/KEY_ID"
  }
}
```

**PROD Environment** (`environments/prod/backend.tf`):

```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-prod"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "turaf-terraform-locks-prod"
    kms_key_id     = "arn:aws:kms:us-east-1:811783768245:key/KEY_ID"
  }
}
```

### State Bucket Setup

**Manual Setup** (one-time per AWS account):

**DEV Account (801651112319)**:
```bash
# Assume role or configure credentials for DEV account
export AWS_PROFILE=turaf-dev

# Create S3 bucket for state
aws s3api create-bucket \
  --bucket turaf-terraform-state-dev \
  --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket turaf-terraform-state-dev \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket turaf-terraform-state-dev \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access
aws s3api put-public-access-block \
  --bucket turaf-terraform-state-dev \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Create DynamoDB table for locking
aws dynamodb create-table \
  --table-name turaf-terraform-locks-dev \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

**Repeat for QA (965932217544) and PROD (811783768245) accounts** with respective bucket names and table names.

---

## Terraform Workflow

### Initialize

```bash
cd infrastructure/environments/dev
terraform init
```

### Plan

```bash
terraform plan -out=tfplan
```

### Apply

```bash
terraform apply tfplan
```

### Destroy

```bash
terraform destroy
```

---

## Module Usage Example

**File**: `environments/dev/main.tf`

```hcl
module "networking" {
  source = "../../modules/networking"
  
  environment          = var.environment
  vpc_cidr            = var.vpc_cidr
  availability_zones  = var.availability_zones
  public_subnet_cidrs = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
}

module "database" {
  source = "../../modules/database"
  
  identifier              = "turaf-db-${var.environment}"
  instance_class          = var.db_instance_class
  allocated_storage       = var.db_allocated_storage
  database_name           = "turaf"
  master_username         = "turaf_admin"
  master_password         = random_password.db_password.result
  multi_az               = var.db_multi_az
  backup_retention_period = var.db_backup_retention_days
  subnet_ids             = module.networking.private_subnet_ids
  security_group_ids     = [module.networking.rds_security_group_id]
  kms_key_id             = module.security.rds_kms_key_id
}

module "ecs_cluster" {
  source = "../../modules/compute/ecs-cluster"
  
  cluster_name              = "turaf-cluster-${var.environment}"
  enable_container_insights = true
}

module "identity_service" {
  source = "../../modules/compute/ecs-service"
  
  service_name        = "identity-service"
  cluster_id          = module.ecs_cluster.cluster_id
  task_cpu            = var.ecs_services["identity-service"].cpu
  task_memory         = var.ecs_services["identity-service"].memory
  desired_count       = var.ecs_services["identity-service"].desired_count
  container_image     = "${module.ecr_identity.repository_url}:latest"
  subnet_ids          = module.networking.private_subnet_ids
  security_group_ids  = [module.networking.ecs_security_group_id]
  target_group_arn    = module.alb.target_group_arns["identity-service"]
  
  environment_variables = {
    ENVIRONMENT    = var.environment
    DATABASE_HOST  = module.database.address
  }
  
  secrets = [
    {
      name      = "DATABASE_PASSWORD"
      valueFrom = module.security.db_password_secret_arn
    }
  ]
}
```

---

## Terraform Best Practices

### Variable Validation

```hcl
variable "environment" {
  type = string
  validation {
    condition     = contains(["dev", "qa", "prod"], var.environment)
    error_message = "Environment must be dev, qa, or prod."
  }
}
```

### Resource Tagging

```hcl
locals {
  common_tags = {
    Project     = "Turaf"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

resource "aws_instance" "example" {
  # ... other configuration ...
  
  tags = merge(
    local.common_tags,
    {
      Name = "turaf-instance-${var.environment}"
    }
  )
}
```

### Outputs

```hcl
output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = module.alb.alb_dns_name
}

output "database_endpoint" {
  description = "RDS database endpoint"
  value       = module.database.endpoint
  sensitive   = true
}
```

---

## References

- PROJECT.md: Infrastructure specifications
- Terraform Best Practices
- AWS Provider Documentation
