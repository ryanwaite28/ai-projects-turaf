# ECS Task Definitions for Turaf Microservices

# Identity Service Task Definition
resource "aws_ecs_task_definition" "identity_service" {
  family                   = "identity-service-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.identity_service_cpu
  memory                   = var.identity_service_memory
  execution_role_arn       = var.ecs_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = "identity-service"
    image = "${var.identity_service_image}:${var.image_tag}"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      {
        name  = "ENVIRONMENT"
        value = var.environment
      },
      {
        name  = "AWS_REGION"
        value = var.region
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.environment
      }
    ]
    
    secrets = var.db_secrets_arn != "" ? [
      {
        name      = "DB_HOST"
        valueFrom = "${var.db_secrets_arn}:host::"
      },
      {
        name      = "DB_USERNAME"
        valueFrom = "${var.db_secrets_arn}:username::"
      },
      {
        name      = "DB_PASSWORD"
        valueFrom = "${var.db_secrets_arn}:password::"
      }
    ] : []
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.identity_service.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
    
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = merge(
    var.tags,
    {
      Name        = "identity-service-${var.environment}"
      Environment = var.environment
      Service     = "identity"
    }
  )
}

# Organization Service Task Definition
resource "aws_ecs_task_definition" "organization_service" {
  family                   = "organization-service-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.organization_service_cpu
  memory                   = var.organization_service_memory
  execution_role_arn       = var.ecs_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = "organization-service"
    image = "${var.organization_service_image}:${var.image_tag}"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      {
        name  = "ENVIRONMENT"
        value = var.environment
      },
      {
        name  = "AWS_REGION"
        value = var.region
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.environment
      }
    ]
    
    secrets = var.db_secrets_arn != "" ? [
      {
        name      = "DB_HOST"
        valueFrom = "${var.db_secrets_arn}:host::"
      },
      {
        name      = "DB_USERNAME"
        valueFrom = "${var.db_secrets_arn}:username::"
      },
      {
        name      = "DB_PASSWORD"
        valueFrom = "${var.db_secrets_arn}:password::"
      }
    ] : []
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.organization_service.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
    
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = merge(
    var.tags,
    {
      Name        = "organization-service-${var.environment}"
      Environment = var.environment
      Service     = "organization"
    }
  )
}

# Experiment Service Task Definition
resource "aws_ecs_task_definition" "experiment_service" {
  family                   = "experiment-service-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.experiment_service_cpu
  memory                   = var.experiment_service_memory
  execution_role_arn       = var.ecs_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = "experiment-service"
    image = "${var.experiment_service_image}:${var.image_tag}"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      {
        name  = "ENVIRONMENT"
        value = var.environment
      },
      {
        name  = "AWS_REGION"
        value = var.region
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.environment
      }
    ]
    
    secrets = var.db_secrets_arn != "" ? [
      {
        name      = "DB_HOST"
        valueFrom = "${var.db_secrets_arn}:host::"
      },
      {
        name      = "DB_USERNAME"
        valueFrom = "${var.db_secrets_arn}:username::"
      },
      {
        name      = "DB_PASSWORD"
        valueFrom = "${var.db_secrets_arn}:password::"
      }
    ] : []
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.experiment_service.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
    
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = merge(
    var.tags,
    {
      Name        = "experiment-service-${var.environment}"
      Environment = var.environment
      Service     = "experiment"
    }
  )
}

# Optional: Metrics Service Task Definition
resource "aws_ecs_task_definition" "metrics_service" {
  count = var.enable_metrics_service ? 1 : 0
  
  family                   = "metrics-service-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.metrics_service_cpu
  memory                   = var.metrics_service_memory
  execution_role_arn       = var.ecs_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = "metrics-service"
    image = "${var.metrics_service_image}:${var.image_tag}"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      {
        name  = "ENVIRONMENT"
        value = var.environment
      },
      {
        name  = "AWS_REGION"
        value = var.region
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.environment
      }
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.metrics_service[0].name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])

  tags = merge(
    var.tags,
    {
      Name        = "metrics-service-${var.environment}"
      Environment = var.environment
      Service     = "metrics"
    }
  )
}

# Optional: Reporting Service Task Definition
resource "aws_ecs_task_definition" "reporting_service" {
  count = var.enable_reporting_service ? 1 : 0
  
  family                   = "reporting-service-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.reporting_service_cpu
  memory                   = var.reporting_service_memory
  execution_role_arn       = var.ecs_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = "reporting-service"
    image = "${var.reporting_service_image}:${var.image_tag}"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      {
        name  = "ENVIRONMENT"
        value = var.environment
      },
      {
        name  = "AWS_REGION"
        value = var.region
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.environment
      }
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.reporting_service[0].name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])

  tags = merge(
    var.tags,
    {
      Name        = "reporting-service-${var.environment}"
      Environment = var.environment
      Service     = "reporting"
    }
  )
}

# Optional: Notification Service Task Definition
resource "aws_ecs_task_definition" "notification_service" {
  count = var.enable_notification_service ? 1 : 0
  
  family                   = "notification-service-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.notification_service_cpu
  memory                   = var.notification_service_memory
  execution_role_arn       = var.ecs_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([{
    name  = "notification-service"
    image = "${var.notification_service_image}:${var.image_tag}"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      {
        name  = "ENVIRONMENT"
        value = var.environment
      },
      {
        name  = "AWS_REGION"
        value = var.region
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.environment
      }
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.notification_service[0].name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])

  tags = merge(
    var.tags,
    {
      Name        = "notification-service-${var.environment}"
      Environment = var.environment
      Service     = "notification"
    }
  )
}
