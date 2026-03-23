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

- [ ] CloudWatch log groups created
- [ ] Metrics and alarms configured
- [ ] SNS topics for alerts created
- [ ] X-Ray tracing enabled
- [ ] Dashboard created
- [ ] terraform plan succeeds

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify alarm thresholds
- Check log retention settings

## References

- Specification: `specs/aws-infrastructure.md` (Monitoring section)
- Related Tasks: 010-configure-dev-environment
