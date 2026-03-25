# Compute Module

Terraform module for ECS Fargate compute resources including cluster, task definitions, services, and Application Load Balancer for the Turaf platform.

## Features

- **ECS Fargate Cluster**: Serverless container orchestration
- **Fargate Spot**: 70% cost savings for demo/dev environments
- **Application Load Balancer**: HTTPS traffic routing with path-based rules
- **Auto-scaling**: Optional auto-scaling based on CPU utilization
- **Container Insights**: Optional monitoring (disabled for cost savings)
- **ECS Exec**: Optional debugging capability
- **Health Checks**: Automated health monitoring for all services
- **Circuit Breaker**: Automatic rollback on failed deployments

## Architecture

### Demo Configuration (Cost-Optimized)

```
Application Load Balancer (HTTPS)
├── /api/identity/*     → Identity Service (1 task, 0.25 vCPU, 512 MB, Spot)
├── /api/organizations/* → Organization Service (1 task, 0.25 vCPU, 512 MB, Spot)
└── /api/experiments/*   → Experiment Service (1 task, 0.25 vCPU, 512 MB, Spot)

ECS Cluster: turaf-cluster-dev
├── Capacity Provider: FARGATE_SPOT (default)
├── Container Insights: Disabled
└── Auto-scaling: Disabled
```

**Cost**: ~$15/month (with Fargate Spot)

### Production Configuration (Optional)

```
Application Load Balancer (HTTPS)
├── /api/identity/*      → Identity Service (2-10 tasks, 0.5 vCPU, 1 GB)
├── /api/organizations/* → Organization Service (2-10 tasks, 0.5 vCPU, 1 GB)
├── /api/experiments/*   → Experiment Service (2-10 tasks, 1 vCPU, 2 GB)
├── /api/metrics/*       → Metrics Service (2-10 tasks, 1 vCPU, 2 GB)
├── /api/reports/*       → Reporting Service (1-5 tasks, 0.5 vCPU, 1 GB)
└── /api/notifications/* → Notification Service (1-3 tasks, 0.25 vCPU, 512 MB)

ECS Cluster: turaf-cluster-prod
├── Capacity Provider: FARGATE (standard)
├── Container Insights: Enabled
└── Auto-scaling: Enabled (CPU-based)
```

**Cost**: ~$150-300/month (depending on load)

## Usage

### Minimal Demo Configuration (Recommended)

```hcl
module "compute" {
  source = "../../modules/compute"

  environment = "dev"
  region      = "us-east-1"
  
  # Networking
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  public_subnet_ids   = module.networking.public_subnet_ids
  
  # Security
  ecs_security_group_id = module.security.ecs_security_group_id
  alb_security_group_id = module.security.alb_security_group_id
  ecs_execution_role_arn = module.security.ecs_execution_role_arn
  ecs_task_role_arn     = module.security.ecs_task_role_arn
  acm_certificate_arn   = var.acm_certificate_arn
  
  # Database
  db_secrets_arn = module.database.rds_master_secret_arn
  
  # ECR Images
  identity_service_image     = "${var.ecr_base_url}/identity-service"
  organization_service_image = "${var.ecr_base_url}/organization-service"
  experiment_service_image   = "${var.ecr_base_url}/experiment-service"
  image_tag                  = var.image_tag
  
  # Cost Optimization - Demo Settings
  use_fargate_spot          = true   # 70% savings
  enable_container_insights = false  # Save $2/month
  enable_autoscaling        = false  # Disable for demo
  
  # Disable optional services
  enable_metrics_service      = false
  enable_reporting_service    = false
  enable_notification_service = false
  
  # Minimal resources
  identity_service_cpu           = 256
  identity_service_memory        = 512
  identity_service_desired_count = 1
  
  organization_service_cpu           = 256
  organization_service_memory        = 512
  organization_service_desired_count = 1
  
  experiment_service_cpu           = 256
  experiment_service_memory        = 512
  experiment_service_desired_count = 1
  
  # Logging
  log_retention_days    = 7
  enable_execute_command = false
  
  tags = {
    Project     = "turaf"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}
```

### Production Configuration

```hcl
module "compute" {
  source = "../../modules/compute"

  environment = "prod"
  region      = "us-east-1"
  
  # ... (same networking, security, database config)
  
  # Production Settings
  use_fargate_spot          = false  # Use standard Fargate
  enable_container_insights = true   # Enable monitoring
  enable_autoscaling        = true   # Enable auto-scaling
  
  # Enable all services
  enable_metrics_service      = true
  enable_reporting_service    = true
  enable_notification_service = true
  
  # Production resources
  identity_service_cpu           = 512
  identity_service_memory        = 1024
  identity_service_desired_count = 2
  identity_service_min_capacity  = 2
  identity_service_max_capacity  = 10
  
  # ... (similar config for other services)
  
  log_retention_days    = 30
  enable_execute_command = true
  
  tags = {
    Project     = "turaf"
    Environment = "prod"
    ManagedBy   = "terraform"
  }
}
```

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| environment | Environment name | string | - | yes |
| region | AWS region | string | - | yes |
| vpc_id | VPC ID | string | - | yes |
| private_subnet_ids | Private subnet IDs for ECS tasks | list(string) | - | yes |
| public_subnet_ids | Public subnet IDs for ALB | list(string) | - | yes |
| ecs_security_group_id | ECS security group ID | string | - | yes |
| alb_security_group_id | ALB security group ID | string | - | yes |
| ecs_execution_role_arn | ECS execution role ARN | string | - | yes |
| ecs_task_role_arn | ECS task role ARN | string | - | yes |
| acm_certificate_arn | ACM certificate ARN | string | - | yes |
| identity_service_image | Identity service ECR image | string | - | yes |
| organization_service_image | Organization service ECR image | string | - | yes |
| experiment_service_image | Experiment service ECR image | string | - | yes |
| use_fargate_spot | Use Fargate Spot | bool | true | no |
| enable_container_insights | Enable Container Insights | bool | false | no |
| enable_autoscaling | Enable auto-scaling | bool | false | no |
| enable_metrics_service | Enable metrics service | bool | false | no |
| enable_reporting_service | Enable reporting service | bool | false | no |
| enable_notification_service | Enable notification service | bool | false | no |
| log_retention_days | CloudWatch log retention | number | 7 | no |

## Outputs

| Name | Description |
|------|-------------|
| cluster_id | ECS cluster ID |
| cluster_name | ECS cluster name |
| alb_dns_name | ALB DNS name |
| alb_zone_id | ALB zone ID |
| identity_service_name | Identity service name |
| organization_service_name | Organization service name |
| experiment_service_name | Experiment service name |
| service_summary | Summary of all deployed services |

## Cost Estimation

### Demo Configuration
- **Fargate Spot (3 services)**: ~$15/month
  - Identity: 0.25 vCPU, 512 MB, 1 task = ~$5/month
  - Organization: 0.25 vCPU, 512 MB, 1 task = ~$5/month
  - Experiment: 0.25 vCPU, 512 MB, 1 task = ~$5/month
- **ALB**: ~$16/month (base) + $0.008/LCU-hour
- **CloudWatch Logs**: ~$0.50/month (7-day retention, minimal logs)
- **Total**: ~$32/month

### Production Configuration (All Services)
- **Fargate Standard (6 services)**: ~$150/month
- **ALB**: ~$20/month
- **Container Insights**: ~$2/month
- **CloudWatch Logs**: ~$5/month (30-day retention)
- **Auto-scaling overhead**: Variable
- **Total**: ~$180-300/month

## Service Configuration

### Core Services (Always Enabled)

#### Identity Service
- **Purpose**: User authentication and authorization
- **Endpoints**: `/api/identity/*`, `/api/auth/*`
- **Demo**: 0.25 vCPU, 512 MB, 1 task
- **Prod**: 0.5 vCPU, 1 GB, 2-10 tasks

#### Organization Service
- **Purpose**: Organization and team management
- **Endpoints**: `/api/organizations/*`, `/api/teams/*`
- **Demo**: 0.25 vCPU, 512 MB, 1 task
- **Prod**: 0.5 vCPU, 1 GB, 2-10 tasks

#### Experiment Service
- **Purpose**: A/B testing and experimentation
- **Endpoints**: `/api/experiments/*`, `/api/variants/*`, `/api/metrics/*`
- **Demo**: 0.25 vCPU, 512 MB, 1 task
- **Prod**: 1 vCPU, 2 GB, 2-10 tasks

### Optional Services (Disabled by Default)

#### Metrics Service
- **Purpose**: Real-time metrics aggregation
- **Prod**: 1 vCPU, 2 GB, 2-10 tasks
- **Cost**: ~$30/month

#### Reporting Service
- **Purpose**: Report generation and analytics
- **Prod**: 0.5 vCPU, 1 GB, 1-5 tasks
- **Cost**: ~$15/month

#### Notification Service
- **Purpose**: Email/SMS notifications
- **Prod**: 0.25 vCPU, 512 MB, 1-3 tasks
- **Cost**: ~$8/month

## Fargate Spot vs Standard

### Fargate Spot (Demo/Dev)
- **Cost**: 70% cheaper than standard
- **Availability**: May be interrupted with 2-minute warning
- **Use Case**: Non-critical workloads, dev/test environments
- **Recommended**: Demo and development environments

### Fargate Standard (Production)
- **Cost**: Full price
- **Availability**: Guaranteed
- **Use Case**: Production workloads
- **Recommended**: Production environments

## Auto-scaling

Auto-scaling is disabled by default for demo to save costs. When enabled:

- **Metric**: CPU utilization
- **Target**: 70% CPU
- **Scale Out**: Add tasks when CPU > 70%
- **Scale In**: Remove tasks when CPU < 70%
- **Cooldown**: 300 seconds

## Health Checks

All services include:
- **Container Health Check**: curl to `/actuator/health` every 30s
- **ALB Health Check**: HTTP GET to `/actuator/health` every 30s
- **Healthy Threshold**: 2 consecutive successes
- **Unhealthy Threshold**: 3 consecutive failures

## Deployment

### Circuit Breaker
- **Enabled**: Automatic rollback on deployment failure
- **Rollback**: Reverts to previous task definition

### Deployment Configuration
- **Maximum**: 200% of desired count
- **Minimum**: 100% of desired count
- **Strategy**: Rolling update with zero downtime

## Monitoring

### CloudWatch Logs
- **Log Groups**: One per service
- **Retention**: 7 days (demo), 30 days (prod)
- **Stream Prefix**: `ecs`

### Container Insights (Optional)
- **Metrics**: CPU, memory, network, disk
- **Dashboards**: Automatic CloudWatch dashboards
- **Cost**: ~$2/month additional

### ECS Exec (Optional)
- **Purpose**: Debug running containers
- **Cost**: Additional CloudWatch Logs charges
- **Security**: Requires IAM permissions

## Troubleshooting

### Service Won't Start

**Problem**: Tasks fail to start

**Solutions**:
1. Check CloudWatch Logs for errors
2. Verify ECR image exists and is accessible
3. Check IAM role permissions
4. Verify security group allows traffic
5. Check database connectivity

### High Costs

**Problem**: Fargate costs higher than expected

**Solutions**:
1. Enable Fargate Spot (70% savings)
2. Reduce CPU/memory allocation
3. Reduce task count
4. Disable Container Insights
5. Reduce log retention period

### ALB 503 Errors

**Problem**: Load balancer returns 503

**Solutions**:
1. Check ECS service has healthy tasks
2. Verify target group health checks
3. Check security group rules
4. Verify container port mapping
5. Check application health endpoint

### Deployment Failures

**Problem**: New deployments fail

**Solutions**:
1. Check circuit breaker events
2. Verify new task definition is valid
3. Check health check configuration
4. Review CloudWatch Logs
5. Verify sufficient capacity in subnets

## Security Best Practices

- ✅ Tasks run in private subnets
- ✅ ALB in public subnets with security groups
- ✅ HTTPS only (HTTP redirects to HTTPS)
- ✅ Secrets stored in Secrets Manager
- ✅ IAM roles follow least privilege
- ✅ Container images scanned for vulnerabilities
- ✅ ECS Exec disabled by default

## Migration Guide

### From EC2 to Fargate

1. Create task definitions with Fargate compatibility
2. Update service to use Fargate launch type
3. Remove EC2 instances from cluster
4. Update security groups for awsvpc networking

### Enabling Optional Services

1. Set `enable_*_service = true`
2. Provide ECR image URL
3. Run `terraform apply`
4. Verify service starts successfully

### Scaling Up for Production

1. Increase CPU/memory per task
2. Increase desired task count
3. Enable auto-scaling
4. Switch to standard Fargate
5. Enable Container Insights
6. Increase log retention

## References

- [ECS Fargate Pricing](https://aws.amazon.com/fargate/pricing/)
- [Fargate Spot](https://aws.amazon.com/blogs/aws/aws-fargate-spot-now-generally-available/)
- [ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [Container Insights](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights.html)
