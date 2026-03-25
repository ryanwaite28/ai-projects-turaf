# ECS Services for Turaf Microservices

# Identity Service
resource "aws_ecs_service" "identity_service" {
  name            = "identity-service-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.identity_service.arn
  desired_count   = var.identity_service_desired_count
  launch_type     = var.use_fargate_spot ? null : "FARGATE"

  dynamic "capacity_provider_strategy" {
    for_each = var.use_fargate_spot ? [1] : []
    content {
      capacity_provider = "FARGATE_SPOT"
      weight            = 100
      base              = 0
    }
  }

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.identity_service.arn
    container_name   = "identity-service"
    container_port   = 8080
  }

  enable_execute_command = var.enable_execute_command

  deployment_configuration {
    maximum_percent         = 200
    minimum_healthy_percent = 100
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = merge(
    var.tags,
    {
      Name        = "identity-service-${var.environment}"
      Environment = var.environment
      Service     = "identity"
    }
  )

  depends_on = [aws_lb_listener.https]
}

# Organization Service
resource "aws_ecs_service" "organization_service" {
  name            = "organization-service-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.organization_service.arn
  desired_count   = var.organization_service_desired_count
  launch_type     = var.use_fargate_spot ? null : "FARGATE"

  dynamic "capacity_provider_strategy" {
    for_each = var.use_fargate_spot ? [1] : []
    content {
      capacity_provider = "FARGATE_SPOT"
      weight            = 100
      base              = 0
    }
  }

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.organization_service.arn
    container_name   = "organization-service"
    container_port   = 8080
  }

  enable_execute_command = var.enable_execute_command

  deployment_configuration {
    maximum_percent         = 200
    minimum_healthy_percent = 100
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = merge(
    var.tags,
    {
      Name        = "organization-service-${var.environment}"
      Environment = var.environment
      Service     = "organization"
    }
  )

  depends_on = [aws_lb_listener.https]
}

# Experiment Service
resource "aws_ecs_service" "experiment_service" {
  name            = "experiment-service-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.experiment_service.arn
  desired_count   = var.experiment_service_desired_count
  launch_type     = var.use_fargate_spot ? null : "FARGATE"

  dynamic "capacity_provider_strategy" {
    for_each = var.use_fargate_spot ? [1] : []
    content {
      capacity_provider = "FARGATE_SPOT"
      weight            = 100
      base              = 0
    }
  }

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.experiment_service.arn
    container_name   = "experiment-service"
    container_port   = 8080
  }

  enable_execute_command = var.enable_execute_command

  deployment_configuration {
    maximum_percent         = 200
    minimum_healthy_percent = 100
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = merge(
    var.tags,
    {
      Name        = "experiment-service-${var.environment}"
      Environment = var.environment
      Service     = "experiment"
    }
  )

  depends_on = [aws_lb_listener.https]
}

# Optional: Metrics Service
resource "aws_ecs_service" "metrics_service" {
  count = var.enable_metrics_service ? 1 : 0
  
  name            = "metrics-service-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.metrics_service[0].arn
  desired_count   = var.metrics_service_desired_count
  launch_type     = var.use_fargate_spot ? null : "FARGATE"

  dynamic "capacity_provider_strategy" {
    for_each = var.use_fargate_spot ? [1] : []
    content {
      capacity_provider = "FARGATE_SPOT"
      weight            = 100
      base              = 0
    }
  }

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  enable_execute_command = var.enable_execute_command

  tags = merge(
    var.tags,
    {
      Name        = "metrics-service-${var.environment}"
      Environment = var.environment
      Service     = "metrics"
    }
  )
}

# Optional: Reporting Service
resource "aws_ecs_service" "reporting_service" {
  count = var.enable_reporting_service ? 1 : 0
  
  name            = "reporting-service-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.reporting_service[0].arn
  desired_count   = var.reporting_service_desired_count
  launch_type     = var.use_fargate_spot ? null : "FARGATE"

  dynamic "capacity_provider_strategy" {
    for_each = var.use_fargate_spot ? [1] : []
    content {
      capacity_provider = "FARGATE_SPOT"
      weight            = 100
      base              = 0
    }
  }

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  enable_execute_command = var.enable_execute_command

  tags = merge(
    var.tags,
    {
      Name        = "reporting-service-${var.environment}"
      Environment = var.environment
      Service     = "reporting"
    }
  )
}

# Optional: Notification Service
resource "aws_ecs_service" "notification_service" {
  count = var.enable_notification_service ? 1 : 0
  
  name            = "notification-service-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.notification_service[0].arn
  desired_count   = var.notification_service_desired_count
  launch_type     = var.use_fargate_spot ? null : "FARGATE"

  dynamic "capacity_provider_strategy" {
    for_each = var.use_fargate_spot ? [1] : []
    content {
      capacity_provider = "FARGATE_SPOT"
      weight            = 100
      base              = 0
    }
  }

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  enable_execute_command = var.enable_execute_command

  tags = merge(
    var.tags,
    {
      Name        = "notification-service-${var.environment}"
      Environment = var.environment
      Service     = "notification"
    }
  )
}

# Auto-scaling for Identity Service (optional - disabled for demo)
resource "aws_appautoscaling_target" "identity_service" {
  count = var.enable_autoscaling ? 1 : 0
  
  max_capacity       = var.identity_service_max_capacity
  min_capacity       = var.identity_service_min_capacity
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.identity_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "identity_service_cpu" {
  count = var.enable_autoscaling ? 1 : 0
  
  name               = "identity-service-cpu-${var.environment}"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.identity_service[0].resource_id
  scalable_dimension = aws_appautoscaling_target.identity_service[0].scalable_dimension
  service_namespace  = aws_appautoscaling_target.identity_service[0].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}

# Auto-scaling for Organization Service (optional - disabled for demo)
resource "aws_appautoscaling_target" "organization_service" {
  count = var.enable_autoscaling ? 1 : 0
  
  max_capacity       = var.organization_service_max_capacity
  min_capacity       = var.organization_service_min_capacity
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.organization_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "organization_service_cpu" {
  count = var.enable_autoscaling ? 1 : 0
  
  name               = "organization-service-cpu-${var.environment}"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.organization_service[0].resource_id
  scalable_dimension = aws_appautoscaling_target.organization_service[0].scalable_dimension
  service_namespace  = aws_appautoscaling_target.organization_service[0].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}

# Auto-scaling for Experiment Service (optional - disabled for demo)
resource "aws_appautoscaling_target" "experiment_service" {
  count = var.enable_autoscaling ? 1 : 0
  
  max_capacity       = var.experiment_service_max_capacity
  min_capacity       = var.experiment_service_min_capacity
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.experiment_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "experiment_service_cpu" {
  count = var.enable_autoscaling ? 1 : 0
  
  name               = "experiment-service-cpu-${var.environment}"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.experiment_service[0].resource_id
  scalable_dimension = aws_appautoscaling_target.experiment_service[0].scalable_dimension
  service_namespace  = aws_appautoscaling_target.experiment_service[0].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}
