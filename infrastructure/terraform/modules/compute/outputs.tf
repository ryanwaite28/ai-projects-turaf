# Compute Module Outputs

# ECS Cluster Outputs
output "cluster_id" {
  description = "ID of the ECS cluster"
  value       = aws_ecs_cluster.main.id
}

output "cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.main.name
}

output "cluster_arn" {
  description = "ARN of the ECS cluster"
  value       = aws_ecs_cluster.main.arn
}

# ALB Outputs
output "alb_arn" {
  description = "ARN of the Application Load Balancer"
  value       = aws_lb.main.arn
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = aws_lb.main.zone_id
}

output "alb_listener_http_arn" {
  description = "ARN of the HTTP listener"
  value       = aws_lb_listener.http.arn
}

output "alb_listener_https_arn" {
  description = "ARN of the HTTPS listener"
  value       = aws_lb_listener.https.arn
}

# Target Group Outputs
output "identity_service_target_group_arn" {
  description = "ARN of the identity service target group"
  value       = aws_lb_target_group.identity_service.arn
}

output "organization_service_target_group_arn" {
  description = "ARN of the organization service target group"
  value       = aws_lb_target_group.organization_service.arn
}

output "experiment_service_target_group_arn" {
  description = "ARN of the experiment service target group"
  value       = aws_lb_target_group.experiment_service.arn
}

# ECS Service Outputs
output "identity_service_name" {
  description = "Name of the identity ECS service"
  value       = aws_ecs_service.identity_service.name
}

output "organization_service_name" {
  description = "Name of the organization ECS service"
  value       = aws_ecs_service.organization_service.name
}

output "experiment_service_name" {
  description = "Name of the experiment ECS service"
  value       = aws_ecs_service.experiment_service.name
}

output "metrics_service_name" {
  description = "Name of the metrics ECS service (null if disabled)"
  value       = var.enable_metrics_service ? aws_ecs_service.metrics_service[0].name : null
}

output "reporting_service_name" {
  description = "Name of the reporting ECS service (null if disabled)"
  value       = var.enable_reporting_service ? aws_ecs_service.reporting_service[0].name : null
}

output "notification_service_name" {
  description = "Name of the notification ECS service (null if disabled)"
  value       = var.enable_notification_service ? aws_ecs_service.notification_service[0].name : null
}

# Task Definition Outputs
output "identity_service_task_definition_arn" {
  description = "ARN of the identity service task definition"
  value       = aws_ecs_task_definition.identity_service.arn
}

output "organization_service_task_definition_arn" {
  description = "ARN of the organization service task definition"
  value       = aws_ecs_task_definition.organization_service.arn
}

output "experiment_service_task_definition_arn" {
  description = "ARN of the experiment service task definition"
  value       = aws_ecs_task_definition.experiment_service.arn
}

# CloudWatch Log Group Outputs
output "identity_service_log_group" {
  description = "CloudWatch log group for identity service"
  value       = aws_cloudwatch_log_group.identity_service.name
}

output "organization_service_log_group" {
  description = "CloudWatch log group for organization service"
  value       = aws_cloudwatch_log_group.organization_service.name
}

output "experiment_service_log_group" {
  description = "CloudWatch log group for experiment service"
  value       = aws_cloudwatch_log_group.experiment_service.name
}

# Service Configuration Summary
output "service_summary" {
  description = "Summary of deployed services"
  value = {
    cluster_name = aws_ecs_cluster.main.name
    alb_dns_name = aws_lb.main.dns_name
    services = {
      identity = {
        name          = aws_ecs_service.identity_service.name
        desired_count = var.identity_service_desired_count
        cpu           = var.identity_service_cpu
        memory        = var.identity_service_memory
      }
      organization = {
        name          = aws_ecs_service.organization_service.name
        desired_count = var.organization_service_desired_count
        cpu           = var.organization_service_cpu
        memory        = var.organization_service_memory
      }
      experiment = {
        name          = aws_ecs_service.experiment_service.name
        desired_count = var.experiment_service_desired_count
        cpu           = var.experiment_service_cpu
        memory        = var.experiment_service_memory
      }
    }
    fargate_spot_enabled     = var.use_fargate_spot
    container_insights       = var.enable_container_insights
    autoscaling_enabled      = var.enable_autoscaling
  }
}
