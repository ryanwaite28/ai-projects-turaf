# Monitoring Module - CloudWatch Dashboards, Alarms, and X-Ray
# Cost-optimized configuration - all features disabled by default for demo

# SNS Topic for Alerts (Optional)
resource "aws_sns_topic" "alerts" {
  count = var.enable_sns_alerts ? 1 : 0
  
  name              = "turaf-alerts-${var.environment}"
  display_name      = "Turaf Platform Alerts - ${var.environment}"
  kms_master_key_id = "alias/aws/sns"

  tags = merge(
    var.tags,
    {
      Name        = "turaf-alerts-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_sns_topic_subscription" "alerts_email" {
  count = var.enable_sns_alerts && var.alarm_email != "" ? 1 : 0
  
  topic_arn = aws_sns_topic.alerts[0].arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

# ECS Cluster Alarms
resource "aws_cloudwatch_metric_alarm" "ecs_cpu_high" {
  count = var.enable_alarms && var.cluster_name != "" ? 1 : 0
  
  alarm_name          = "ecs-cpu-high-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = var.alarm_evaluation_periods
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = var.alarm_period
  statistic           = "Average"
  threshold           = var.cpu_threshold
  alarm_description   = "ECS cluster CPU utilization is too high"
  treat_missing_data  = "notBreaching"

  dimensions = {
    ClusterName = var.cluster_name
  }

  alarm_actions = var.enable_sns_alerts ? [aws_sns_topic.alerts[0].arn] : []

  tags = merge(
    var.tags,
    {
      Name        = "ecs-cpu-high-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "ecs_memory_high" {
  count = var.enable_alarms && var.cluster_name != "" ? 1 : 0
  
  alarm_name          = "ecs-memory-high-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = var.alarm_evaluation_periods
  metric_name         = "MemoryUtilization"
  namespace           = "AWS/ECS"
  period              = var.alarm_period
  statistic           = "Average"
  threshold           = var.memory_threshold
  alarm_description   = "ECS cluster memory utilization is too high"
  treat_missing_data  = "notBreaching"

  dimensions = {
    ClusterName = var.cluster_name
  }

  alarm_actions = var.enable_sns_alerts ? [aws_sns_topic.alerts[0].arn] : []

  tags = merge(
    var.tags,
    {
      Name        = "ecs-memory-high-${var.environment}"
      Environment = var.environment
    }
  )
}

# ALB Alarms
resource "aws_cloudwatch_metric_alarm" "alb_5xx_errors" {
  count = var.enable_alarms && var.alb_arn_suffix != "" ? 1 : 0
  
  alarm_name          = "alb-5xx-errors-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = var.alarm_evaluation_periods
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = var.alarm_period
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "ALB is returning too many 5xx errors"
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
  }

  alarm_actions = var.enable_sns_alerts ? [aws_sns_topic.alerts[0].arn] : []

  tags = merge(
    var.tags,
    {
      Name        = "alb-5xx-errors-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "alb_response_time" {
  count = var.enable_alarms && var.alb_arn_suffix != "" ? 1 : 0
  
  alarm_name          = "alb-response-time-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = var.alarm_evaluation_periods
  metric_name         = "TargetResponseTime"
  namespace           = "AWS/ApplicationELB"
  period              = var.alarm_period
  statistic           = "Average"
  threshold           = var.response_time_threshold
  alarm_description   = "ALB response time is too high"
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
  }

  alarm_actions = var.enable_sns_alerts ? [aws_sns_topic.alerts[0].arn] : []

  tags = merge(
    var.tags,
    {
      Name        = "alb-response-time-${var.environment}"
      Environment = var.environment
    }
  )
}

# RDS Alarms
resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  count = var.enable_alarms && var.rds_instance_id != "" ? 1 : 0
  
  alarm_name          = "rds-cpu-high-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = var.alarm_evaluation_periods
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = var.alarm_period
  statistic           = "Average"
  threshold           = var.cpu_threshold
  alarm_description   = "RDS CPU utilization is too high"
  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = var.rds_instance_id
  }

  alarm_actions = var.enable_sns_alerts ? [aws_sns_topic.alerts[0].arn] : []

  tags = merge(
    var.tags,
    {
      Name        = "rds-cpu-high-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "rds_storage_low" {
  count = var.enable_alarms && var.rds_instance_id != "" ? 1 : 0
  
  alarm_name          = "rds-storage-low-${var.environment}"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = var.alarm_evaluation_periods
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = var.alarm_period
  statistic           = "Average"
  threshold           = 5000000000 # 5 GB in bytes
  alarm_description   = "RDS free storage space is low"
  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = var.rds_instance_id
  }

  alarm_actions = var.enable_sns_alerts ? [aws_sns_topic.alerts[0].arn] : []

  tags = merge(
    var.tags,
    {
      Name        = "rds-storage-low-${var.environment}"
      Environment = var.environment
    }
  )
}

# X-Ray Sampling Rule
resource "aws_xray_sampling_rule" "main" {
  count = var.enable_xray ? 1 : 0
  
  rule_name      = "turaf-sampling-${var.environment}"
  priority       = 1000
  version        = 1
  reservoir_size = var.xray_reservoir_size
  fixed_rate     = var.xray_sampling_rate
  url_path       = "*"
  host           = "*"
  http_method    = "*"
  service_type   = "*"
  service_name   = "*"
  resource_arn   = "*"

  tags = merge(
    var.tags,
    {
      Name        = "turaf-sampling-${var.environment}"
      Environment = var.environment
    }
  )
}

# CloudWatch Dashboard
resource "aws_cloudwatch_dashboard" "main" {
  count = var.enable_dashboard ? 1 : 0
  
  dashboard_name = "turaf-${var.environment}"

  dashboard_body = jsonencode({
    widgets = flatten([
      # ECS Widgets
      var.cluster_name != "" ? [
        {
          type = "metric"
          properties = {
            metrics = [
              ["AWS/ECS", "CPUUtilization", { stat = "Average", label = "CPU" }],
              [".", "MemoryUtilization", { stat = "Average", label = "Memory" }]
            ]
            period = var.dashboard_period
            stat   = "Average"
            region = var.region
            title  = "ECS Cluster Utilization"
            yAxis = {
              left = {
                min = 0
                max = 100
              }
            }
          }
        },
        {
          type = "metric"
          properties = {
            metrics = [
              ["AWS/ECS", "CPUUtilization", { stat = "Average", label = "CPU" }]
            ]
            period = var.dashboard_period
            stat   = "Average"
            region = var.region
            title  = "ECS CPU Utilization"
          }
        }
      ] : [],
      # ALB Widgets
      var.alb_arn_suffix != "" ? [
        {
          type = "metric"
          properties = {
            metrics = [
              ["AWS/ApplicationELB", "RequestCount", { stat = "Sum", label = "Requests" }],
              [".", "HTTPCode_Target_2XX_Count", { stat = "Sum", label = "2xx" }],
              [".", "HTTPCode_Target_4XX_Count", { stat = "Sum", label = "4xx" }],
              [".", "HTTPCode_Target_5XX_Count", { stat = "Sum", label = "5xx" }]
            ]
            period = var.dashboard_period
            stat   = "Sum"
            region = var.region
            title  = "ALB Request Metrics"
          }
        },
        {
          type = "metric"
          properties = {
            metrics = [
              ["AWS/ApplicationELB", "TargetResponseTime", { stat = "Average", label = "Response Time" }]
            ]
            period = var.dashboard_period
            stat   = "Average"
            region = var.region
            title  = "ALB Response Time"
          }
        }
      ] : [],
      # RDS Widgets
      var.rds_instance_id != "" ? [
        {
          type = "metric"
          properties = {
            metrics = [
              ["AWS/RDS", "CPUUtilization", { stat = "Average", label = "CPU" }],
              [".", "DatabaseConnections", { stat = "Average", label = "Connections" }]
            ]
            period = var.dashboard_period
            stat   = "Average"
            region = var.region
            title  = "RDS Metrics"
          }
        }
      ] : []
    ])
  })
}

# CloudWatch Logs Insights Queries (Saved Queries)
resource "aws_cloudwatch_query_definition" "error_logs" {
  count = var.enable_log_insights ? 1 : 0
  
  name = "turaf-error-logs-${var.environment}"

  log_group_names = [
    "/ecs/identity-service-${var.environment}",
    "/ecs/organization-service-${var.environment}",
    "/ecs/experiment-service-${var.environment}"
  ]

  query_string = <<-QUERY
    fields @timestamp, @message
    | filter @message like /ERROR/
    | sort @timestamp desc
    | limit 100
  QUERY
}

resource "aws_cloudwatch_query_definition" "slow_requests" {
  count = var.enable_log_insights ? 1 : 0
  
  name = "turaf-slow-requests-${var.environment}"

  log_group_names = [
    "/ecs/identity-service-${var.environment}",
    "/ecs/organization-service-${var.environment}",
    "/ecs/experiment-service-${var.environment}"
  ]

  query_string = <<-QUERY
    fields @timestamp, @message, duration
    | filter duration > 1000
    | sort duration desc
    | limit 50
  QUERY
}
