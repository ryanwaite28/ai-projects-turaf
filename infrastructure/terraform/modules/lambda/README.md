# Lambda Module

Terraform module for AWS Lambda functions for event-driven processing in the Turaf platform.

## Features

- **Event Processor**: Process domain events from EventBridge
- **Notification Processor**: Send email/SMS notifications via SES
- **Report Generator**: Generate reports and analytics
- **EventBridge Integration**: Automatic event routing
- **SQS Integration**: Queue-based processing
- **VPC Support**: Optional VPC deployment for database access
- **Function URLs**: Direct HTTP invocation support
- **Cost Optimization**: All functions disabled by default for demo

## Architecture

### Demo Configuration (Cost-Optimized)

**All Lambda functions disabled by default** - Use ECS services instead for demo to save costs.

```
Demo Approach:
├── Event Processing: Use ECS services directly
├── Notifications: Use SES directly from services
└── Reports: Generate on-demand from ECS services

Cost: $0/month (no Lambda functions deployed)
```

### Production Configuration (Optional)

```
EventBridge Event Bus
├── Event Processor Lambda (512 MB, 60s timeout)
│   └── Processes domain events
│
SQS Queues
├── Notifications Queue → Notification Processor Lambda (512 MB, 60s)
│   └── Sends emails via SES
│
└── Reports Queue → Report Generator Lambda (1024 MB, 300s)
    └── Generates reports to S3

Cost: ~$5-15/month (depending on usage)
```

## Usage

### Demo Configuration (All Disabled)

```hcl
module "lambda" {
  source = "../../modules/lambda"

  environment = "dev"
  region      = "us-east-1"
  
  # IAM
  lambda_execution_role_arn = module.security.lambda_execution_role_arn
  
  # EventBridge
  event_bus_name = module.messaging.event_bus_name
  event_bus_arn  = module.messaging.event_bus_arn
  
  # All functions disabled for demo (use ECS instead)
  enable_event_processor        = false
  enable_notification_processor = false
  enable_report_generator       = false
  
  # No VPC mode (save NAT costs)
  use_vpc_mode = false
  
  # Minimal logging
  log_retention_days = 7
  
  tags = {
    Project     = "turaf"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}
```

### Production Configuration (All Enabled)

```hcl
module "lambda" {
  source = "../../modules/lambda"

  environment = "prod"
  region      = "us-east-1"
  
  # IAM
  lambda_execution_role_arn = module.security.lambda_execution_role_arn
  
  # Networking (for database access)
  vpc_id                    = module.networking.vpc_id
  private_subnet_ids        = module.networking.private_subnet_ids
  lambda_security_group_id  = module.security.lambda_security_group_id
  
  # EventBridge
  event_bus_name = module.messaging.event_bus_name
  event_bus_arn  = module.messaging.event_bus_arn
  
  # SQS
  notifications_queue_arn = module.messaging.notifications_queue_arn
  reports_queue_arn       = module.messaging.reports_queue_arn
  
  # Storage
  reports_bucket_name     = module.storage.reports_bucket_name
  lambda_artifacts_bucket = var.lambda_artifacts_bucket
  
  # SES
  from_email = "noreply@turaf.io"
  
  # Enable all functions
  enable_event_processor        = true
  enable_notification_processor = true
  enable_report_generator       = true
  
  # VPC mode for database access
  use_vpc_mode = true
  
  # Function configuration
  event_processor_memory     = 512
  event_processor_timeout    = 60
  notification_processor_memory = 512
  notification_processor_timeout = 60
  report_generator_memory    = 1024
  report_generator_timeout   = 300
  
  # Runtime
  lambda_runtime = "nodejs20.x"
  
  # Logging
  log_retention_days = 30
  
  # Cost control
  reserved_concurrent_executions = 10
  
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
| lambda_execution_role_arn | Lambda execution role ARN | string | - | yes |
| event_bus_name | EventBridge event bus name | string | - | yes |
| event_bus_arn | EventBridge event bus ARN | string | - | yes |
| enable_event_processor | Enable event processor | bool | false | no |
| enable_notification_processor | Enable notification processor | bool | false | no |
| enable_report_generator | Enable report generator | bool | false | no |
| use_vpc_mode | Run in VPC | bool | false | no |
| lambda_runtime | Lambda runtime | string | nodejs20.x | no |
| log_retention_days | Log retention days | number | 7 | no |

## Outputs

| Name | Description |
|------|-------------|
| event_processor_function_arn | Event processor ARN (null if disabled) |
| notification_processor_function_arn | Notification processor ARN (null if disabled) |
| report_generator_function_arn | Report generator ARN (null if disabled) |
| report_generator_function_url | Report generator function URL (null if disabled) |
| lambda_summary | Summary of deployed functions |

## Cost Estimation

### Demo Configuration (All Disabled)
- **Lambda Functions**: $0/month (none deployed)
- **CloudWatch Logs**: $0/month (no logs)
- **Total**: **$0/month**

### Production Configuration (All Enabled)

**Lambda Compute**:
- Event Processor (512 MB, 1M invocations/month, 1s avg): ~$2/month
- Notification Processor (512 MB, 500K invocations/month, 1s avg): ~$1/month
- Report Generator (1024 MB, 10K invocations/month, 30s avg): ~$2/month

**CloudWatch Logs**:
- 3 functions × 30-day retention: ~$1/month

**VPC Costs** (if enabled):
- NAT Gateway data processing: ~$5-10/month
- ENI charges: Included

**Total**: ~$10-15/month (without VPC), ~$20-30/month (with VPC)

**Free Tier Benefits**:
- 1M requests/month free
- 400,000 GB-seconds compute free
- Demo usage likely stays within Free Tier

## Lambda Functions

### Event Processor

**Purpose**: Process domain events from EventBridge

**Triggers**: EventBridge rules matching `turaf.*` events

**Configuration**:
- Memory: 512 MB (default)
- Timeout: 60 seconds (default)
- Runtime: Node.js 20.x (default)

**Use Cases**:
- Event validation and enrichment
- Cross-service event propagation
- Event logging and auditing

### Notification Processor

**Purpose**: Send email and SMS notifications

**Triggers**: SQS notifications queue

**Configuration**:
- Memory: 512 MB (default)
- Timeout: 60 seconds (default)
- Runtime: Node.js 20.x (default)

**Use Cases**:
- User notifications
- System alerts
- Email campaigns

**Integration**: Amazon SES for email delivery

### Report Generator

**Purpose**: Generate reports and analytics

**Triggers**: 
- SQS reports queue
- Direct HTTP invocation via Function URL

**Configuration**:
- Memory: 1024 MB (default)
- Timeout: 300 seconds (default)
- Runtime: Node.js 20.x (default)

**Use Cases**:
- Scheduled reports
- On-demand analytics
- Data exports

**Output**: Reports saved to S3 bucket

## Deployment

### Prerequisites

1. **Lambda Deployment Packages**: Upload function code to S3
   ```bash
   aws s3 cp event-processor.zip s3://lambda-artifacts/event-processor/latest/function.zip
   aws s3 cp notification-processor.zip s3://lambda-artifacts/notification-processor/latest/function.zip
   aws s3 cp report-generator.zip s3://lambda-artifacts/report-generator/latest/function.zip
   ```

2. **IAM Role**: Lambda execution role with required permissions

3. **VPC Configuration** (if using VPC mode):
   - Private subnets with NAT Gateway or VPC endpoints
   - Security group allowing outbound traffic

### Deployment Steps

1. Upload Lambda packages to S3
2. Set `enable_*` variables to `true`
3. Run `terraform apply`
4. Test functions with sample events

## VPC Mode

### When to Use VPC Mode

**Enable VPC mode if**:
- Lambda needs to access RDS databases
- Lambda needs to access ElastiCache
- Lambda needs to access internal services

**Disable VPC mode if**:
- Lambda only uses AWS services (S3, SES, EventBridge)
- Cost optimization is priority
- Cold start latency is a concern

### VPC Mode Costs

- **NAT Gateway**: ~$32/month + data transfer
- **ENI**: No additional charge
- **Cold Starts**: Slower (10-30s vs 1-3s)

### Recommendation

For demo: **Disable VPC mode** - Use ECS services for database access instead

For production: **Enable VPC mode** only for functions needing database access

## Runtime Options

### Node.js 20.x (Recommended)
- Fast cold starts (~1-2s)
- Small package sizes
- Good for event processing

### Python 3.11
- Excellent for data processing
- Rich library ecosystem
- Good for report generation

### Java 17
- Best for complex business logic
- Slower cold starts (~10-15s)
- Larger package sizes

## Monitoring

### CloudWatch Metrics
- Invocations
- Duration
- Errors
- Throttles
- Concurrent executions

### CloudWatch Logs
- Function logs with configurable retention
- Automatic log group creation
- Structured logging support

### Alarms (Optional)
- Error rate > 5%
- Duration > timeout threshold
- Throttles > 0

## Cost Optimization

### Demo Environment
1. **Disable all functions** - Use ECS services instead
2. **No VPC mode** - Avoid NAT Gateway costs
3. **Short log retention** - 7 days
4. **No reserved concurrency** - Use on-demand

**Savings**: ~$15-30/month

### Production Environment
1. **Enable only needed functions**
2. **Set reserved concurrency** - Prevent runaway costs
3. **Use appropriate memory** - Right-size for workload
4. **Monitor and optimize** - Review CloudWatch metrics

## Troubleshooting

### Function Not Triggering

**Problem**: Lambda not invoked by EventBridge/SQS

**Solutions**:
1. Check EventBridge rule pattern
2. Verify Lambda permissions
3. Check SQS event source mapping
4. Review CloudWatch Logs

### Timeout Errors

**Problem**: Function exceeds timeout

**Solutions**:
1. Increase timeout value
2. Optimize function code
3. Use async processing
4. Split into smaller functions

### VPC Connectivity Issues

**Problem**: Cannot access resources in VPC

**Solutions**:
1. Verify security group rules
2. Check subnet routing
3. Ensure NAT Gateway/VPC endpoints
4. Test network connectivity

### High Costs

**Problem**: Lambda costs higher than expected

**Solutions**:
1. Check invocation count
2. Review function duration
3. Optimize memory allocation
4. Set reserved concurrency limits
5. Consider switching to ECS for long-running tasks

## Security Best Practices

- ✅ Use IAM roles with least privilege
- ✅ Store secrets in Secrets Manager
- ✅ Enable encryption at rest
- ✅ Use VPC for database access
- ✅ Set reserved concurrency limits
- ✅ Enable CloudWatch Logs encryption
- ✅ Use function URLs with IAM auth

## Migration from ECS to Lambda

### When to Migrate

**Use Lambda if**:
- Event-driven workloads
- Sporadic traffic patterns
- Short execution times (<15 minutes)
- Minimal state requirements

**Use ECS if**:
- Long-running processes
- Consistent traffic
- Complex dependencies
- Stateful applications

### Migration Steps

1. Package application as Lambda function
2. Upload to S3
3. Enable Lambda function in Terraform
4. Test with sample events
5. Update EventBridge rules
6. Monitor performance
7. Disable ECS service if successful

## References

- [AWS Lambda Pricing](https://aws.amazon.com/lambda/pricing/)
- [Lambda Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html)
- [Lambda in VPC](https://docs.aws.amazon.com/lambda/latest/dg/configuration-vpc.html)
- [Lambda Function URLs](https://docs.aws.amazon.com/lambda/latest/dg/lambda-urls.html)
