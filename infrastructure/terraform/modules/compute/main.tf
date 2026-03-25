# Compute Module - ECS Fargate for Turaf Platform
# Cost-optimized configuration for demo/portfolio purposes

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

# CloudWatch Log Groups for ECS Services
resource "aws_cloudwatch_log_group" "identity_service" {
  name              = "/ecs/identity-service-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "identity-service-logs-${var.environment}"
      Environment = var.environment
      Service     = "identity"
    }
  )
}

resource "aws_cloudwatch_log_group" "organization_service" {
  name              = "/ecs/organization-service-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "organization-service-logs-${var.environment}"
      Environment = var.environment
      Service     = "organization"
    }
  )
}

resource "aws_cloudwatch_log_group" "experiment_service" {
  name              = "/ecs/experiment-service-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "experiment-service-logs-${var.environment}"
      Environment = var.environment
      Service     = "experiment"
    }
  )
}

# Optional service log groups (disabled by default for demo)
resource "aws_cloudwatch_log_group" "metrics_service" {
  count = var.enable_metrics_service ? 1 : 0
  
  name              = "/ecs/metrics-service-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "metrics-service-logs-${var.environment}"
      Environment = var.environment
      Service     = "metrics"
    }
  )
}

resource "aws_cloudwatch_log_group" "reporting_service" {
  count = var.enable_reporting_service ? 1 : 0
  
  name              = "/ecs/reporting-service-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "reporting-service-logs-${var.environment}"
      Environment = var.environment
      Service     = "reporting"
    }
  )
}

resource "aws_cloudwatch_log_group" "notification_service" {
  count = var.enable_notification_service ? 1 : 0
  
  name              = "/ecs/notification-service-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "notification-service-logs-${var.environment}"
      Environment = var.environment
      Service     = "notification"
    }
  )
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
    }
  )
}

# ALB Target Groups
resource "aws_lb_target_group" "identity_service" {
  name        = "identity-svc-${var.environment}"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = merge(
    var.tags,
    {
      Name        = "identity-service-tg-${var.environment}"
      Environment = var.environment
      Service     = "identity"
    }
  )
}

resource "aws_lb_target_group" "organization_service" {
  name        = "org-svc-${var.environment}"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = merge(
    var.tags,
    {
      Name        = "organization-service-tg-${var.environment}"
      Environment = var.environment
      Service     = "organization"
    }
  )
}

resource "aws_lb_target_group" "experiment_service" {
  name        = "exp-svc-${var.environment}"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = merge(
    var.tags,
    {
      Name        = "experiment-service-tg-${var.environment}"
      Environment = var.environment
      Service     = "experiment"
    }
  )
}

# ALB Listeners
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "Service not found"
      status_code  = "404"
    }
  }
}

# ALB Listener Rules
resource "aws_lb_listener_rule" "identity_service" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.identity_service.arn
  }

  condition {
    path_pattern {
      values = ["/api/identity/*", "/api/auth/*"]
    }
  }
}

resource "aws_lb_listener_rule" "organization_service" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 200

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.organization_service.arn
  }

  condition {
    path_pattern {
      values = ["/api/organizations/*", "/api/teams/*"]
    }
  }
}

resource "aws_lb_listener_rule" "experiment_service" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 300

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.experiment_service.arn
  }

  condition {
    path_pattern {
      values = ["/api/experiments/*", "/api/variants/*", "/api/metrics/*"]
    }
  }
}
