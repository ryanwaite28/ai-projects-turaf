# Compute Module Variables - Shared Infrastructure Only
# Service-specific resources (ECS services, task definitions, target groups, listener rules)
# are managed per-service via CI/CD pipelines

variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for ECS tasks"
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "List of public subnet IDs for ALB"
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "Security group ID for ECS tasks"
  type        = string
}

variable "alb_security_group_id" {
  description = "Security group ID for ALB"
  type        = string
}

variable "ecs_execution_role_arn" {
  description = "IAM role ARN for ECS task execution"
  type        = string
}

variable "ecs_task_role_arn" {
  description = "IAM role ARN for ECS tasks"
  type        = string
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS (optional - if empty, HTTPS listener will not be created)"
  type        = string
  default     = ""
}

# Cost Optimization Flags
variable "use_fargate_spot" {
  description = "Use Fargate Spot for cost savings (70% cheaper, suitable for demo)"
  type        = bool
  default     = true
}

variable "enable_container_insights" {
  description = "Enable Container Insights (additional cost - disable for demo)"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Additional tags for all resources"
  type        = map(string)
  default     = {}
}
