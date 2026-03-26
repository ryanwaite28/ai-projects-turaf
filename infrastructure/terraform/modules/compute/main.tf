# Compute Module - Shared ECS Infrastructure for Turaf Platform
# Provides base infrastructure for ECS services managed by CI/CD pipelines
# Service-specific resources (ECS services, task definitions, target groups, listener rules)
# are managed per-service via CI/CD pipelines

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "turaf-cluster-${var.environment}"

  setting {
    name  = "containerInsights"
    value = var.enable_container_insights ? "enabled" : "disabled"
  }

  tags = merge(
    var.tags,
    {
      Name        = "turaf-cluster-${var.environment}"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Purpose     = "Shared ECS cluster for all microservices"
    }
  )
}

# ECS Cluster Capacity Providers (Fargate Spot for cost savings)
resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name = aws_ecs_cluster.main.name

  capacity_providers = var.use_fargate_spot ? ["FARGATE_SPOT", "FARGATE"] : ["FARGATE"]

  default_capacity_provider_strategy {
    capacity_provider = var.use_fargate_spot ? "FARGATE_SPOT" : "FARGATE"
    weight            = 100
    base              = 0
  }
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "turaf-alb-${var.environment}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = var.environment == "prod" ? true : false
  enable_http2              = true
  idle_timeout              = 60

  tags = merge(
    var.tags,
    {
      Name        = "turaf-alb-${var.environment}"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Purpose     = "Shared ALB for all microservices"
    }
  )
}

# ALB HTTP Listener (Port 80)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = var.acm_certificate_arn != "" ? "redirect" : "fixed-response"

    dynamic "redirect" {
      for_each = var.acm_certificate_arn != "" ? [1] : []
      content {
        port        = "443"
        protocol    = "HTTPS"
        status_code = "HTTP_301"
      }
    }

    dynamic "fixed_response" {
      for_each = var.acm_certificate_arn == "" ? [1] : []
      content {
        content_type = "text/plain"
        message_body = "No service configured"
        status_code  = "404"
      }
    }
  }

  tags = merge(
    var.tags,
    {
      Name        = "turaf-alb-http-listener-${var.environment}"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )
}

# ALB HTTPS Listener (Port 443) - Optional
resource "aws_lb_listener" "https" {
  count = var.acm_certificate_arn != "" ? 1 : 0
  
  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "No service configured"
      status_code  = "404"
    }
  }

  tags = merge(
    var.tags,
    {
      Name        = "turaf-alb-https-listener-${var.environment}"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )
}
