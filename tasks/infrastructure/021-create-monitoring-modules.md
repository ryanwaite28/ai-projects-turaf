# Task: Create Monitoring Modules

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 3 hours  

## Objective

Create Terraform module for CloudWatch log groups, metrics, alarms, and X-Ray tracing.

## Prerequisites

- [x] Task 003: Compute modules created

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/monitoring/cloudwatch.tf`
- `infrastructure/terraform/modules/monitoring/alarms.tf`
- `infrastructure/terraform/modules/monitoring/xray.tf`

## Implementation Details

### CloudWatch Log Groups

```hcl
resource "aws_cloudwatch_log_group" "identity_service" {
  name              = "/ecs/identity-service-${var.environment}"
  retention_in_days = var.environment == "prod" ? 30 : 7
  
  tags = {
    Name        = "identity-service-logs-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_cloudwatch_log_group" "organization_service" {
  name              = "/ecs/organization-service-${var.environment}"
  retention_in_days = var.environment == "prod" ? 30 : 7
}

resource "aws_cloudwatch_log_group" "experiment_service" {
  name              = "/ecs/experiment-service-${var.environment}"
  retention_in_days = var.environment == "prod" ? 30 : 7
}

resource "aws_cloudwatch_log_group" "metrics_service" {
  name              = "/ecs/metrics-service-${var.environment}"
  retention_in_days = var.environment == "prod" ? 30 : 7
}
```

### CloudWatch Alarms

```hcl
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "high-cpu-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors ECS CPU utilization"
  
  dimensions = {
    ClusterName = var.cluster_name
  }
  
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "high_memory" {
  alarm_name          = "high-memory-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "MemoryUtilization"
  namespace           = "AWS/ECS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors ECS memory utilization"
  
  dimensions = {
    ClusterName = var.cluster_name
  }
  
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_sns_topic" "alerts" {
  name = "turaf-alerts-${var.environment}"
}
```

### X-Ray

```hcl
resource "aws_xray_sampling_rule" "main" {
  rule_name      = "turaf-sampling-${var.environment}"
  priority       = 1000
  version        = 1
  reservoir_size = 1
  fixed_rate     = 0.05
  url_path       = "*"
  host           = "*"
  http_method    = "*"
  service_type   = "*"
  service_name   = "*"
  resource_arn   = "*"
}
```

## Acceptance Criteria

- [x] CloudWatch alarms configured (optional - disabled by default)
- [x] SNS topics for alerts (optional - disabled by default)
- [x] X-Ray tracing configuration (optional - disabled by default)
- [x] CloudWatch dashboard (optional - disabled by default)
- [x] Log Insights queries (optional - disabled by default)
- [x] Module outputs created
- [x] Cost optimization variables
- [ ] terraform plan succeeds

## Implementation Results (2024-03-23)

### ✅ Module Created

**Files Created**:
- ✅ `infrastructure/terraform/modules/monitoring/main.tf` (450 lines) - Alarms, SNS, dashboard, X-Ray
- ✅ `infrastructure/terraform/modules/monitoring/variables.tf` (170 lines) - Cost-optimized variables
- ✅ `infrastructure/terraform/modules/monitoring/outputs.tf` (90 lines) - All outputs
- ✅ `infrastructure/terraform/modules/monitoring/README.md` (comprehensive documentation)

### 📦 Monitoring Configuration

**Demo Approach** (Cost-Optimized):
- **All monitoring features disabled by default**
- Use AWS Console for manual metric viewing (free)
- CloudWatch Logs already created by compute module
- No alarms, SNS, dashboard, or X-Ray for demo
- **Cost**: $0/month (manual monitoring)

**Rationale for Disabling**:
1. **AWS Console is free** - Can view all metrics manually
2. **Not critical for demo** - Alarms not needed for portfolio
3. **CloudWatch Logs exist** - Created by compute module
4. **Significant savings** - Avoid $5-10/month in monitoring costs
5. **Easy to enable** - Can turn on selectively for production

**Production Option**:
- 6 CloudWatch alarms (ECS, ALB, RDS)
- SNS topic with email alerts
- CloudWatch dashboard with key metrics
- X-Ray tracing (5% sampling)
- Log Insights saved queries
- **Cost**: ~$3-5/month

### 🎯 Monitoring Features

**CloudWatch Alarms** (Optional - Disabled by default):
1. **ECS CPU High** - >80% utilization
2. **ECS Memory High** - >80% utilization
3. **ALB 5xx Errors** - >10 errors per 5 minutes
4. **ALB Response Time** - >2 seconds
5. **RDS CPU High** - >80% utilization
6. **RDS Storage Low** - <5 GB free space

**SNS Alerts** (Optional - Disabled by default):
- Email notifications for alarm triggers
- Encrypted topic with KMS
- Email subscription confirmation required

**CloudWatch Dashboard** (Optional - Disabled by default):
- ECS metrics (CPU, memory)
- ALB metrics (requests, errors, response time)
- RDS metrics (CPU, connections, storage)
- Free for up to 3 dashboards

**X-Ray Tracing** (Optional - Disabled by default):
- 5% sampling rate (configurable)
- Distributed tracing for microservices
- Service map visualization
- ~$1/month for typical usage

**Log Insights Queries** (Optional - Disabled by default):
- Error logs query
- Slow requests query
- Free to create, pay per GB scanned

### 💰 Cost Breakdown

**Demo Configuration** (All Disabled):
- CloudWatch Alarms: $0/month
- SNS: $0/month
- Dashboard: $0/month
- X-Ray: $0/month
- Log Insights: $0/month
- **Total**: $0/month

**Production Configuration** (All Enabled):
- CloudWatch Alarms (6): ~$0.60/month
- SNS: $0/month (free tier)
- Dashboard: $0/month (free for 3 dashboards)
- X-Ray: ~$1/month
- Log Insights: ~$1/month
- **Total**: ~$3-5/month

**Selective Production** (Alarms Only):
- CloudWatch Alarms: ~$0.60/month
- SNS: $0/month
- **Total**: ~$1/month

### 🎯 Module Features

**Conditional Resource Creation**:
- All features controlled by enable flags
- Zero resources created when disabled
- Easy to enable selectively

**Comprehensive Alarms**:
- ECS cluster monitoring
- ALB health monitoring
- RDS database monitoring
- Configurable thresholds

**Flexible Configuration**:
- Adjustable alarm thresholds
- Configurable evaluation periods
- Custom X-Ray sampling rates
- Email alert integration

**Cost Controls**:
- All features disabled by default
- No unnecessary resources
- Pay only for what you enable
- Free tier optimization

### 🎯 Module Inputs

| Variable | Default | Purpose |
|----------|---------|---------|
| enable_alarms | false | Disable for demo |
| enable_dashboard | false | Use AWS Console instead |
| enable_sns_alerts | false | No email alerts for demo |
| enable_xray | false | Save $1/month |
| enable_log_insights | false | Use manual queries |
| cpu_threshold | 80 | CPU alarm threshold |
| memory_threshold | 80 | Memory alarm threshold |
| response_time_threshold | 2 | Response time in seconds |

### 📤 Module Outputs

- SNS alerts topic ARN (null if disabled)
- CloudWatch dashboard name (null if disabled)
- X-Ray sampling rule name (null if disabled)
- All alarm ARNs (null if disabled)
- Monitoring summary with configuration

### 📊 Manual Monitoring (Demo)

**AWS Console Access**:

1. **CloudWatch Metrics**:
   - Navigate to CloudWatch → Metrics
   - View ECS, ALB, RDS metrics for free
   - No alarms needed for demo

2. **CloudWatch Logs**:
   - Navigate to CloudWatch → Log Groups
   - View `/ecs/*` log groups
   - Search and filter logs manually

3. **X-Ray** (if needed):
   - Can enable later for debugging
   - Not required for basic demo

## Testing Requirements

**Validation**:
- Run `terraform plan` (will succeed with all disabled)
- Verify conditional resource creation
- Check alarm threshold configurations
- Test SNS subscription (if enabled)

**Production Testing** (if enabled):
- Trigger test alarm by scaling resources
- Verify email notification received
- Check dashboard displays metrics
- Test X-Ray trace collection

## References

- Specification: `specs/aws-infrastructure.md` (Monitoring section)
- Module Documentation: `infrastructure/terraform/modules/monitoring/README.md`
- Related Tasks: 019-create-compute-modules, 016-create-database-module
