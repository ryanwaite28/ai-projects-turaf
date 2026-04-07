terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "service" {
  name              = "/ecs/${var.service_name}-${var.environment}"
  retention_in_days = var.log_retention_days
  
  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "Terraform-CI/CD"
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "service" {
  family                   = "${var.service_name}-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = data.terraform_remote_state.infra.outputs.ecs_execution_role_arn
  task_role_arn            = data.terraform_remote_state.infra.outputs.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = var.service_name
    image = "${var.ecr_repository_url}:${var.image_tag}"
    
    portMappings = [{
      containerPort = var.container_port
      protocol      = "tcp"
    }]
    
    environment = [
      { name = "ENVIRONMENT", value = var.environment },
      { name = "AWS_REGION", value = var.region },
      { name = "SERVICE_NAME", value = var.service_name }
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.service.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
    
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${var.container_port}${var.health_check_path} || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])
  
  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "Terraform-CI/CD"
  }
}

# ALB Target Group
resource "aws_lb_target_group" "service" {
  name        = "${substr(var.service_name, 0, 20)}-${var.environment}"
  port        = var.container_port
  protocol    = "HTTP"
  vpc_id      = data.terraform_remote_state.infra.outputs.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = var.health_check_path
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "Terraform-CI/CD"
  }
}

# ALB Listener Rule
resource "aws_lb_listener_rule" "service" {
  listener_arn = data.terraform_remote_state.infra.outputs.alb_listener_http_arn
  priority     = var.listener_rule_priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.service.arn
  }

  condition {
    path_pattern {
      values = ["/api/v1/${var.service_name}/*"]
    }
  }

  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "Terraform-CI/CD"
  }
}

# ECS Service
resource "aws_ecs_service" "service" {
  name            = "${var.service_name}-${var.environment}"
  cluster         = data.terraform_remote_state.infra.outputs.cluster_arn
  task_definition = aws_ecs_task_definition.service.arn
  desired_count   = var.desired_count
  launch_type     = null

  capacity_provider_strategy {
    capacity_provider = var.use_fargate_spot ? "FARGATE_SPOT" : "FARGATE"
    weight            = 100
    base              = 0
  }

  network_configuration {
    subnets          = data.terraform_remote_state.infra.outputs.private_subnet_ids
    security_groups  = [data.terraform_remote_state.infra.outputs.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.service.arn
    container_name   = var.service_name
    container_port   = var.container_port
  }

  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener_rule.service]

  tags = {
    Service     = var.service_name
    Environment = var.environment
    ManagedBy   = "Terraform-CI/CD"
  }
}
