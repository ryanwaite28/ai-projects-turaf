# Compute Module Outputs - Shared Infrastructure Only
# These outputs are used by CI/CD pipelines to deploy service-specific resources

# ECS Cluster Outputs
output "cluster_id" {
  description = "ID of the ECS cluster (for CI/CD deployments)"
  value       = aws_ecs_cluster.main.id
}

output "cluster_name" {
  description = "Name of the ECS cluster (for CI/CD deployments)"
  value       = aws_ecs_cluster.main.name
}

output "cluster_arn" {
  description = "ARN of the ECS cluster (for CI/CD deployments)"
  value       = aws_ecs_cluster.main.arn
}

# ALB Outputs
output "alb_arn" {
  description = "ARN of the Application Load Balancer (for target group attachments)"
  value       = aws_lb.main.arn
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer (for testing and DNS configuration)"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer (for Route53 alias records)"
  value       = aws_lb.main.zone_id
}

output "alb_arn_suffix" {
  description = "ARN suffix of the Application Load Balancer (for CloudWatch metrics)"
  value       = aws_lb.main.arn_suffix
}

output "alb_security_group_id" {
  description = "Security group ID of the ALB (for reference)"
  value       = var.alb_security_group_id
}

# ALB Listener Outputs
output "alb_listener_http_arn" {
  description = "ARN of the HTTP listener (for creating listener rules in CI/CD)"
  value       = aws_lb_listener.http.arn
}

output "alb_listener_https_arn" {
  description = "ARN of the HTTPS listener (null if not created, for creating listener rules in CI/CD)"
  value       = length(aws_lb_listener.https) > 0 ? aws_lb_listener.https[0].arn : null
}

# Internal ALB Outputs
output "internal_alb_arn" {
  description = "ARN of the Internal Application Load Balancer"
  value       = aws_lb.internal.arn
}

output "internal_alb_dns_name" {
  description = "DNS name of the Internal ALB (for BFF service configuration)"
  value       = aws_lb.internal.dns_name
}

output "internal_alb_zone_id" {
  description = "Zone ID of the Internal ALB"
  value       = aws_lb.internal.zone_id
}

output "internal_alb_listener_http_arn" {
  description = "ARN of the Internal ALB HTTP listener (for creating listener rules)"
  value       = aws_lb_listener.internal_http.arn
}

# Networking Outputs (for CI/CD service deployments)
output "vpc_id" {
  description = "VPC ID (for creating target groups)"
  value       = var.vpc_id
}

output "private_subnet_ids" {
  description = "Private subnet IDs (for ECS task network configuration)"
  value       = var.private_subnet_ids
}

output "ecs_security_group_id" {
  description = "ECS tasks security group ID (for service network configuration)"
  value       = var.ecs_security_group_id
}

# IAM Role Outputs (for CI/CD task definitions)
output "ecs_execution_role_arn" {
  description = "ECS execution role ARN (for task definitions)"
  value       = var.ecs_execution_role_arn
}

output "ecs_task_role_arn" {
  description = "ECS task role ARN (for task definitions)"
  value       = var.ecs_task_role_arn
}

# Infrastructure Summary
output "infrastructure_summary" {
  description = "Summary of shared compute infrastructure for CI/CD reference"
  value = {
    cluster_name             = aws_ecs_cluster.main.name
    cluster_arn              = aws_ecs_cluster.main.arn
    alb_dns_name             = aws_lb.main.dns_name
    alb_arn                  = aws_lb.main.arn
    http_listener_arn        = aws_lb_listener.http.arn
    https_listener_arn       = length(aws_lb_listener.https) > 0 ? aws_lb_listener.https[0].arn : null
    vpc_id                   = var.vpc_id
    private_subnet_ids       = var.private_subnet_ids
    ecs_security_group_id    = var.ecs_security_group_id
    ecs_execution_role_arn   = var.ecs_execution_role_arn
    ecs_task_role_arn        = var.ecs_task_role_arn
    fargate_spot_enabled     = var.use_fargate_spot
    container_insights       = var.enable_container_insights
  }
}
