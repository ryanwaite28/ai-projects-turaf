# AWS Infrastructure Specification

**Source**: PROJECT.md (Sections 10, 47-56)

This specification defines the complete AWS infrastructure for the Turaf platform.

---

## Infrastructure Overview

**Cloud Provider**: Amazon Web Services (AWS)  
**Infrastructure as Code**: Terraform  
**Deployment Regions**: us-east-1 (primary)  
**Environments**: DEV, QA, PROD  

---

## Compute Services

### Amazon ECS Fargate

**Purpose**: Run containerized microservices

**Cluster Configuration**:
- Cluster Name: `turaf-cluster-{env}`
- Launch Type: Fargate (serverless)
- Capacity Providers: FARGATE, FARGATE_SPOT (dev only)

**Services**:
1. **identity-service**
   - Task Count: 2 (prod), 1 (dev/qa)
   - CPU: 512 (0.5 vCPU)
   - Memory: 1024 MB
   - Auto-scaling: 2-10 tasks (prod)

2. **organization-service**
   - Task Count: 2 (prod), 1 (dev/qa)
   - CPU: 512
   - Memory: 1024 MB
   - Auto-scaling: 2-10 tasks (prod)

3. **experiment-service**
   - Task Count: 2 (prod), 1 (dev/qa)
   - CPU: 1024 (1 vCPU)
   - Memory: 2048 MB
   - Auto-scaling: 2-10 tasks (prod)

4. **metrics-service**
   - Task Count: 2 (prod), 1 (dev/qa)
   - CPU: 1024
   - Memory: 2048 MB
   - Auto-scaling: 2-10 tasks (prod)

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
      EXPERIMENT_SERVICE_URL = "https://api.${var.environment}.turaf.com"
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
- Resources: ALB, NAT Gateway

**Private Subnets** (2 AZs):
- `10.0.11.0/24` (us-east-1a)
- `10.0.12.0/24` (us-east-1b)
- Resources: ECS tasks, RDS, Lambda

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

**Configuration**:
- Name: `turaf-alb-{env}`
- Scheme: Internet-facing
- Subnets: Public subnets (2 AZs)
- Security Group: ALB security group
- SSL Certificate: ACM certificate for `*.turaf.com`

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

**Listener Rules**:
```hcl
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = aws_acm_certificate.main.arn
  
  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
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

**Hosted Zone**: turaf.com

**Records**:
- `api.{env}.turaf.com` → ALB
- `app.{env}.turaf.com` → CloudFront
- `*.{env}.turaf.com` → Wildcard certificate

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
