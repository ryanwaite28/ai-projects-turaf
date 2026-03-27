# AWS Infrastructure Specification

**Source**: PROJECT.md (Sections 14-17, 24-25)  
**Last Updated**: March 25, 2026  
**Status**: Current  
**Related Documents**: [Terraform Structure](terraform-structure.md), [CI/CD Pipelines](ci-cd-pipelines.md), [Infrastructure Plan](../infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md)

This specification defines the complete AWS infrastructure for the Turaf platform across all environments (DEV, QA, PROD).

---

## Infrastructure Overview

**Cloud Provider**: Amazon Web Services (AWS)  
**Infrastructure as Code**: Terraform  
**Deployment Regions**: us-east-1 (primary)  
**Architecture**: Multi-Account AWS Organization  
**GitHub Repository**: https://github.com/ryanwaite28/ai-projects-turaf  

---

## AWS Organization Structure

**Organization ID**: `o-l3zk5a91yj`  
**Root Account**: `072456928432`  
**Management Account ARN**: `arn:aws:organizations::072456928432:root/o-l3zk5a91yj/r-gs6r`

### Member Accounts

| Account Name | Account ID | Email | ARN | Purpose |
|--------------|------------|-------|-----|---------|
| **root** | 072456928432 | aws@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/072456928432` | Management account |
| **Ops** | 146072879609 | aws-ops@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/146072879609` | DevOps tooling |
| **dev** | 801651112319 | aws-dev@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/801651112319` | Development environment |
| **qa** | 965932217544 | aws-qa@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/965932217544` | QA/Staging environment |
| **prod** | 811783768245 | aws-prod@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/811783768245` | Production environment |

### Account-Level Security Controls

**Service Control Policies (SCPs)**:
- Prevent deletion of CloudTrail logs
- Enforce encryption at rest for all storage
- Restrict resource creation to specific regions (us-east-1)
- Require MFA for sensitive operations
- Block public S3 bucket access by default

**Cross-Account IAM Roles**:
- GitHub Actions OIDC roles per account
- Cross-account logging role (all accounts → Ops account)
- Cross-account audit role (all accounts → root account)

---

## Shared Infrastructure Resources

### ACM SSL/TLS Certificates

**Multi-Account Strategy**: Each AWS account has its own ACM certificate for ALB HTTPS listeners. Certificates cannot be shared across accounts.

**Certificate Inventory**:

| Account | ARN | Domain | Usage | Status |
|---------|-----|--------|-------|--------|
| **Root** (072456928432) | `arn:aws:acm:us-east-1:072456928432:certificate/c660ca8d-5584-4d6f-b75f-e5f10fc5a8ab` | `*.turafapp.com` | CloudFront distributions | ISSUED |
| **DEV** (801651112319) | `arn:aws:acm:us-east-1:801651112319:certificate/8b83b688-7458-4627-9fd4-ff3b2801bf70` | `*.turafapp.com` | DEV ALB HTTPS listeners | ISSUED |
| **QA** (965932217544) | `arn:aws:acm:us-east-1:965932217544:certificate/906b4a44-11e3-4ee7-b10d-9f715ffc0ee6` | `*.turafapp.com` | QA ALB HTTPS listeners | ISSUED |
| **PROD** (811783768245) | `arn:aws:acm:us-east-1:811783768245:certificate/779b5c14-8fc0-44fe-80b4-090bdee1ef62` | `*.turafapp.com` | PROD ALB HTTPS listeners | ISSUED |

**Certificate Properties**:
- **Domain**: `*.turafapp.com` (wildcard)
- **SAN**: `turafapp.com`
- **Region**: us-east-1 (required for CloudFront, works for ALB in any region)
- **Validation**: DNS (CNAME records in Route 53)
- **Auto-Renewal**: Enabled (60 days before expiration)
- **Key Algorithm**: RSA-2048

**Terraform Reference**:
```hcl
# Reference environment-specific certificate
data "aws_acm_certificate" "main" {
  domain   = "*.turafapp.com"
  statuses = ["ISSUED"]
}

# Use in ALB HTTPS listener
resource "aws_lb_listener" "https" {
  certificate_arn = data.aws_acm_certificate.main.arn
  # ...
}
```

**Documentation**: See `infrastructure/acm-certificates.md` for complete certificate details and validation records.

---

## Environment Deployment Strategy

Each environment is deployed to a dedicated AWS account:

| Environment | AWS Account | Infrastructure Scope | Monthly Cost |
|-------------|-------------|---------------------|--------------|
| **DEV** | 801651112319 | Single-AZ, minimal config, Fargate Spot, Free Tier | ~$55/month |
| **QA** | 965932217544 | Multi-AZ, production-like (optional for demo) | ~$200/month |
| **PROD** | 811783768245 | Multi-AZ, auto-scaling, enhanced monitoring (optional) | ~$378/month |
| **Ops** | 146072879609 | Centralized tooling, logging aggregation (optional) | ~$50/month |

**Demo Configuration**: Deploy DEV environment only to minimize costs (~$55/month with Free Tier)

---

## Compute Services

### Amazon ECS Fargate

**Purpose**: Run containerized microservices

**Cluster Configuration**:
- Cluster Name: `turaf-cluster-{env}`
- Launch Type: Fargate (serverless)
- Capacity Providers: FARGATE_SPOT (dev - 70% cost savings)

**Demo Services** (Minimal Configuration):
1. **identity-service**
   - Task Count: 1
   - CPU: 256 (0.25 vCPU)
   - Memory: 512 MB
   - Cost: ~$5/month

2. **organization-service**
   - Task Count: 1
   - CPU: 256 (0.25 vCPU)
   - Memory: 512 MB
   - Cost: ~$5/month

3. **experiment-service**
   - Task Count: 1
   - CPU: 256 (0.25 vCPU)
   - Memory: 512 MB
   - Cost: ~$5/month

**Services Deferred for Demo**:
- ❌ metrics-service (can be added later)
- ❌ reporting-service (can be added later)
- ❌ notification-service (use SES directly)

**Task Definition**:
```hcl
resource "aws_ecs_task_definition" "service" {
  family                   = "turaf-${var.service_name}-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn
  
  container_definitions = jsonencode([{
    name  = var.service_name
    image = "${var.ecr_repository_url}:${var.image_tag}"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      { name = "ENVIRONMENT", value = var.environment },
      { name = "DATABASE_HOST", value = aws_db_instance.postgres.endpoint }
    ]
    
    secrets = [
      {
        name      = "DATABASE_PASSWORD"
        valueFrom = aws_secretsmanager_secret.db_password.arn
      }
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/turaf-${var.service_name}"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}
```

---

### AWS Lambda

**Purpose**: Event-driven processors

**Functions**:

1. **reporting-service**
   - Runtime: Python 3.11
   - Memory: 1024 MB
   - Timeout: 60 seconds
   - Reserved Concurrency: 10
   - Trigger: EventBridge (ExperimentCompleted)

2. **notification-service**
   - Runtime: Python 3.11
   - Memory: 512 MB
   - Timeout: 30 seconds
   - Reserved Concurrency: 20
   - Triggers: EventBridge (ExperimentCompleted, ReportGenerated, MemberAdded)

**Lambda Configuration**:
```hcl
resource "aws_lambda_function" "reporting_service" {
  function_name    = "turaf-reporting-service-${var.environment}"
  role             = aws_iam_role.lambda_execution_role.arn
  handler          = "reporting_handler.lambda_handler"
  runtime          = "python3.11"
  memory_size      = 1024
  timeout          = 60
  reserved_concurrent_executions = 10
  
  filename         = "reporting-service.zip"
  source_code_hash = filebase64sha256("reporting-service.zip")
  
  environment {
    variables = {
      ENVIRONMENT           = var.environment
      S3_BUCKET_NAME       = aws_s3_bucket.reports.id
      EVENT_BUS_NAME       = aws_cloudwatch_event_bus.turaf.name
      EXPERIMENT_SERVICE_URL = "https://api.${var.environment}.turafapp.com"
    }
  }
  
  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda.id]
  }
}
```

---

## Networking

### VPC Configuration

**CIDR Block**: 10.0.0.0/16

**Subnets**:

**Public Subnets** (2 AZs):
- `10.0.1.0/24` (us-east-1a)
- `10.0.2.0/24` (us-east-1b)
- Resources: ALB, (NAT Gateway disabled for demo)

**Private Subnets** (2 AZs):
- `10.0.11.0/24` (us-east-1a)
- `10.0.12.0/24` (us-east-1b)
- Resources: ECS tasks, RDS
- Internet Access: VPC Endpoints only (no NAT Gateway)

**Database Subnets** (2 AZs):
- `10.0.21.0/24` (us-east-1a)
- `10.0.22.0/24` (us-east-1b)
- Resources: RDS PostgreSQL

**Cost Optimization**:
- ❌ **NAT Gateways**: Disabled for demo (-$65/month)
- ✅ **VPC Endpoints**: ECR API, ECR DKR, S3 (gateway) only
- Alternative: ECS tasks use VPC endpoints for AWS service access

**VPC Terraform**:
```hcl
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = {
    Name        = "turaf-vpc-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
}

resource "aws_nat_gateway" "main" {
  count         = 2
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id
}
```

---

### Security Groups

**ALB Security Group**:
- Inbound: 443 (HTTPS) from 0.0.0.0/0
- Inbound: 80 (HTTP) from 0.0.0.0/0 (redirect to HTTPS)
- Outbound: All traffic

**ECS Security Group**:
- Inbound: 8080 from ALB security group
- Outbound: All traffic

**RDS Security Group**:
- Inbound: 5432 from ECS security group
- Inbound: 5432 from Lambda security group
- Outbound: None

**Lambda Security Group**:
- Outbound: All traffic (for API calls, S3, SES)

---

### Application Load Balancer

**Public ALB Configuration**:
- **Name**: `turaf-alb-{env}`
- **Scheme**: Internet-facing
- **Subnets**: Public subnets (2 AZs)
- **Security Group**: ALB security group
- **DNS**: `api.{env}.turafapp.com` (Route 53 Alias record)

**SSL/TLS Configuration**:
- **Certificate**: Environment-specific ACM certificate
  - DEV: `arn:aws:acm:us-east-1:801651112319:certificate/8b83b688-7458-4627-9fd4-ff3b2801bf70`
  - QA: `arn:aws:acm:us-east-1:965932217544:certificate/906b4a44-11e3-4ee7-b10d-9f715ffc0ee6`
  - PROD: `arn:aws:acm:us-east-1:811783768245:certificate/779b5c14-8fc0-44fe-80b4-090bdee1ef62`
- **SSL Policy**: `ELBSecurityPolicy-TLS-1-2-2017-01`
- **Protocols**: TLS 1.2, TLS 1.3

**Target Groups** (one per service):
- identity-service: `/api/v1/auth/*`
- organization-service: `/api/v1/organizations/*`
- experiment-service: `/api/v1/problems/*`, `/api/v1/hypotheses/*`, `/api/v1/experiments/*`
- metrics-service: `/api/v1/metrics/*`

**Health Checks**:
- Path: `/actuator/health`
- Interval: 30 seconds
- Timeout: 5 seconds
- Healthy threshold: 2
- Unhealthy threshold: 3

**HTTPS Listener (Port 443)**:
```hcl
# Data source for environment-specific ACM certificate
data "aws_acm_certificate" "main" {
  domain   = "*.turafapp.com"
  statuses = ["ISSUED"]
  most_recent = true
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = data.aws_acm_certificate.main.arn
  
  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }
}

# HTTP to HTTPS redirect (Port 80)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"
  
  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener_rule" "identity_service" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100
  
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.identity_service.arn
  }
  
  condition {
    path_pattern {
      values = ["/api/v1/auth/*"]
    }
  }
}
```

---

## Data Storage

### Amazon RDS PostgreSQL

**Architecture**: Single database instance with multi-schema isolation per service

**Demo Configuration** (Cost-Optimized):
- **Instance Class**: db.t3.micro (Free Tier eligible)
- **Storage**: 20 GB gp3 (Free Tier eligible)
- **Deployment**: Single-AZ (Multi-AZ disabled for demo)
- **Backup Retention**: 1 day (minimum)
- **Performance Insights**: Disabled
- **Cost**: $0/month (Free Tier) or ~$12/month after

**Database Design**:
- **Instance**: Single RDS PostgreSQL instance per environment
- **Database Name**: `turaf`
- **Schemas**: One schema per microservice
  - `identity_schema` - User authentication data
  - `organization_schema` - Organization and membership data
  - `experiment_schema` - Problems, hypotheses, experiments
  - `metrics_schema` - Metrics and aggregations
- **Users**: One database user per service with schema-scoped permissions
  - `identity_user` → `identity_schema`
  - `organization_user` → `organization_schema`
  - `experiment_user` → `experiment_schema`
  - `metrics_user` → `metrics_schema`

**Services Disabled for Demo**:
- ❌ **ElastiCache Redis**: Use in-memory cache or local Redis (-$12/month)
- ❌ **DocumentDB**: Use PostgreSQL JSON columns (-$54/month)

**Configuration**:

**DEV**:
- Instance Class: db.t3.micro
- Storage: 20 GB SSD
- Multi-AZ: No
- Backups: 7 days retention

**QA**:
- Instance Class: db.t3.small
- Storage: 50 GB SSD
- Multi-AZ: Yes
- Backups: 7 days retention

**PROD**:
- Instance Class: db.t3.medium
- Storage: 100 GB SSD (autoscaling to 500 GB)
- Multi-AZ: Yes
- Read Replicas: 1
- Backups: 30 days retention
- Encryption: Enabled (KMS)

**Terraform Configuration**:
```hcl
resource "aws_db_instance" "postgres" {
  identifier     = "turaf-db-${var.environment}"
  engine         = "postgres"
  engine_version = "15.3"
  
  instance_class    = var.db_instance_class
  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true
  kms_key_id        = aws_kms_key.rds.arn
  
  db_name  = "turaf"
  username = "turaf_admin"
  password = random_password.db_password.result
  
  multi_az               = var.environment == "prod"
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  
  backup_retention_period = var.backup_retention_days
  backup_window          = "03:00-04:00"
  maintenance_window     = "Mon:04:00-Mon:05:00"
  
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  
  deletion_protection = var.environment == "prod"
  skip_final_snapshot = var.environment != "prod"
  
  tags = {
    Name        = "turaf-db-${var.environment}"
    Environment = var.environment
  }
}
```

**Schema Initialization**:
```hcl
# Create database users and schemas
resource "null_resource" "database_schemas" {
  depends_on = [aws_db_instance.postgres]
  
  provisioner "local-exec" {
    command = <<-EOT
      psql -h ${aws_db_instance.postgres.endpoint} -U turaf_admin -d turaf <<SQL
        -- Create schemas
        CREATE SCHEMA IF NOT EXISTS identity_schema;
        CREATE SCHEMA IF NOT EXISTS organization_schema;
        CREATE SCHEMA IF NOT EXISTS experiment_schema;
        CREATE SCHEMA IF NOT EXISTS metrics_schema;
        
        -- Create users
        CREATE USER identity_user WITH PASSWORD '${random_password.identity_user.result}';
        CREATE USER organization_user WITH PASSWORD '${random_password.organization_user.result}';
        CREATE USER experiment_user WITH PASSWORD '${random_password.experiment_user.result}';
        CREATE USER metrics_user WITH PASSWORD '${random_password.metrics_user.result}';
        
        -- Grant schema permissions
        GRANT ALL PRIVILEGES ON SCHEMA identity_schema TO identity_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA identity_schema TO identity_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON TABLES TO identity_user;
        
        GRANT ALL PRIVILEGES ON SCHEMA organization_schema TO organization_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA organization_schema TO organization_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON TABLES TO organization_user;
        
        GRANT ALL PRIVILEGES ON SCHEMA experiment_schema TO experiment_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA experiment_schema TO experiment_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON TABLES TO experiment_user;
        
        GRANT ALL PRIVILEGES ON SCHEMA metrics_schema TO metrics_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA metrics_schema TO metrics_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON TABLES TO metrics_user;
      SQL
    EOT
  }
}

# Store service user passwords in Secrets Manager
resource "aws_secretsmanager_secret" "identity_user_password" {
  name = "turaf/db/identity-user-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "identity_user_password" {
  secret_id     = aws_secretsmanager_secret.identity_user_password.id
  secret_string = random_password.identity_user.result
}

resource "aws_secretsmanager_secret" "organization_user_password" {
  name = "turaf/db/organization-user-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "organization_user_password" {
  secret_id     = aws_secretsmanager_secret.organization_user_password.id
  secret_string = random_password.organization_user.result
}

resource "aws_secretsmanager_secret" "experiment_user_password" {
  name = "turaf/db/experiment-user-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "experiment_user_password" {
  secret_id     = aws_secretsmanager_secret.experiment_user_password.id
  secret_string = random_password.experiment_user.result
}

resource "aws_secretsmanager_secret" "metrics_user_password" {
  name = "turaf/db/metrics-user-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "metrics_user_password" {
  secret_id     = aws_secretsmanager_secret.metrics_user_password.id
  secret_string = random_password.metrics_user.result
}
```

---

### Amazon S3

**Buckets**:

1. **turaf-reports-{env}**
   - Purpose: Store experiment reports
   - Versioning: Enabled
   - Encryption: AES-256 (SSE-S3)
   - Lifecycle: Transition to Glacier after 90 days
   - Public Access: Blocked

2. **turaf-artifacts-{env}**
   - Purpose: Store build artifacts
   - Versioning: Enabled
   - Encryption: AES-256
   - Lifecycle: Delete after 30 days

3. **turaf-frontend-{env}**
   - Purpose: Host Angular SPA
   - Versioning: Enabled
   - Public Access: Via CloudFront only
   - Website Hosting: Enabled

**S3 Bucket Configuration**:
```hcl
resource "aws_s3_bucket" "reports" {
  bucket = "turaf-reports-${var.environment}"
  
  tags = {
    Name        = "turaf-reports-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_s3_bucket_versioning" "reports" {
  bucket = aws_s3_bucket.reports.id
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "reports" {
  bucket = aws_s3_bucket.reports.id
  
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "reports" {
  bucket = aws_s3_bucket.reports.id
  
  rule {
    id     = "transition-to-glacier"
    status = "Enabled"
    
    transition {
      days          = 90
      storage_class = "GLACIER"
    }
  }
}
```

---

## Messaging and Events

### Amazon EventBridge

**Event Bus**:
- Name: `turaf-event-bus-{env}`
- Purpose: Domain event distribution

**Event Rules**:

1. **ExperimentCompleted → Reporting Service**
```hcl
resource "aws_cloudwatch_event_rule" "experiment_completed_to_reporting" {
  name           = "turaf-experiment-completed-to-reporting-${var.environment}"
  event_bus_name = aws_cloudwatch_event_bus.turaf.name
  
  event_pattern = jsonencode({
    source      = ["turaf.experiment-service"]
    detail-type = ["ExperimentCompleted"]
  })
}

resource "aws_cloudwatch_event_target" "reporting_lambda" {
  rule           = aws_cloudwatch_event_rule.experiment_completed_to_reporting.name
  event_bus_name = aws_cloudwatch_event_bus.turaf.name
  arn            = aws_lambda_function.reporting_service.arn
  
  dead_letter_config {
    arn = aws_sqs_queue.reporting_dlq.arn
  }
  
  retry_policy {
    maximum_event_age       = 3600
    maximum_retry_attempts  = 5
  }
}
```

2. **ExperimentCompleted → Notification Service**
3. **ReportGenerated → Notification Service**
4. **MemberAdded → Notification Service**

**Event Archive**:
```hcl
resource "aws_cloudwatch_event_archive" "turaf_events" {
  name             = "turaf-events-archive-${var.environment}"
  event_source_arn = aws_cloudwatch_event_bus.turaf.arn
  retention_days   = 365
}
```

---

### Amazon SQS (Dead Letter Queues)

**DLQs**:
- `turaf-reporting-dlq-{env}`: Failed reporting events
- `turaf-notification-dlq-{env}`: Failed notification events

**Configuration**:
```hcl
resource "aws_sqs_queue" "reporting_dlq" {
  name                      = "turaf-reporting-dlq-${var.environment}"
  message_retention_seconds = 1209600  # 14 days
  
  tags = {
    Name        = "turaf-reporting-dlq-${var.environment}"
    Environment = var.environment
  }
}
```

---

## Security

### IAM Roles

**ECS Task Execution Role**:
- Pull images from ECR
- Write logs to CloudWatch
- Read secrets from Secrets Manager

**ECS Task Role** (per service):
- Publish events to EventBridge
- Read/write to S3 (service-specific)
- Query RDS (via security group)

**Lambda Execution Role**:
- Write logs to CloudWatch
- Access VPC resources
- Read secrets from Secrets Manager
- Publish events to EventBridge
- Send emails via SES
- Read/write S3

**GitHub Actions Role** (OIDC):
- Push images to ECR
- Update ECS services
- Deploy Lambda functions
- Apply Terraform changes

---

### AWS Secrets Manager

**Secrets**:
- `turaf/db/password-{env}`: Database password
- `turaf/jwt/signing-key-{env}`: JWT signing key
- `turaf/github/api-token-{env}`: GitHub API token (for CI/CD)

**Secret Rotation**:
- Database password: Automatic rotation every 90 days
- JWT signing key: Manual rotation

---

### AWS KMS

**Encryption Keys**:
- `turaf-rds-key-{env}`: RDS encryption
- `turaf-s3-key-{env}`: S3 encryption
- `turaf-secrets-key-{env}`: Secrets Manager encryption

---

### AWS WAF

**Web ACL**: Attached to ALB

**Rules**:
- Rate limiting: 2000 requests per 5 minutes per IP
- Block common attack patterns (SQL injection, XSS)
- Geo-blocking (optional)
- IP reputation list

---

## Monitoring and Observability

### Amazon CloudWatch

**Log Groups**:
- `/ecs/turaf-identity-service`
- `/ecs/turaf-organization-service`
- `/ecs/turaf-experiment-service`
- `/ecs/turaf-metrics-service`
- `/aws/lambda/turaf-reporting-service-{env}`
- `/aws/lambda/turaf-notification-service-{env}`

**Log Retention**: 30 days (dev/qa), 90 days (prod)

**Custom Metrics**:
- `Turaf/Experiments/Started`
- `Turaf/Experiments/Completed`
- `Turaf/Reports/Generated`
- `Turaf/Metrics/Recorded`

**Dashboards**:
- API Performance Dashboard
- Infrastructure Dashboard
- Business Metrics Dashboard

---

### AWS X-Ray

**Tracing**:
- Enabled for all ECS services
- Enabled for all Lambda functions
- Sampling rate: 5% (dev/qa), 1% (prod)

---

### CloudWatch Alarms

**Infrastructure Alarms**:
- ECS CPU > 80%
- ECS Memory > 80%
- RDS CPU > 80%
- RDS Storage < 20% free
- ALB 5xx errors > 5%
- Lambda errors > 5%
- DLQ depth > 10 messages

**Notification**: SNS topic → Email/Slack

---

## DNS and CDN

### Amazon Route 53

**Hosted Zone**: turafapp.com

**Records**:
- `api.{env}.turafapp.com` → ALB
- `app.{env}.turafapp.com` → CloudFront
- `*.{env}.turafapp.com` → Wildcard certificate

---

### Amazon CloudFront

**Distribution**: Frontend SPA delivery

**Origin**: S3 bucket (turaf-frontend-{env})

**Behaviors**:
- Default: Serve from S3
- Cache policy: CachingOptimized
- Compress: Yes
- HTTPS only

**Custom Error Pages**:
- 403, 404 → /index.html (SPA routing)

---

## Container Registry

### Amazon ECR

**Repositories**:
- `turaf/identity-service`
- `turaf/organization-service`
- `turaf/experiment-service`
- `turaf/metrics-service`

**Configuration**:
- Image scanning: Enabled
- Lifecycle policy: Keep last 10 images
- Immutable tags: Enabled (prod)

---

## Cost Optimization

**DEV Environment**:
- Use Fargate Spot for ECS tasks
- Single-AZ RDS
- Smaller instance sizes
- Shorter log retention

**PROD Environment**:
- Reserved capacity for predictable workloads
- Auto-scaling for variable workloads
- S3 lifecycle policies
- CloudWatch log filtering

---

## Disaster Recovery

**RDS Backups**:
- Automated daily backups
- Point-in-time recovery
- Cross-region backup replication (prod)

**S3 Versioning**:
- All buckets versioned
- MFA delete enabled (prod)

**Infrastructure Recovery**:
- Terraform state in S3 with versioning
- Infrastructure can be recreated from code

---

## References

- PROJECT.md: Infrastructure specifications
- AWS Well-Architected Framework
- Terraform AWS Provider Documentation
