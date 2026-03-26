# Lambda Module - Event-Driven Functions for Turaf Platform
# Cost-optimized configuration - all functions disabled by default for demo

# CloudWatch Log Groups for Lambda Functions
resource "aws_cloudwatch_log_group" "event_processor" {
  count = var.enable_event_processor ? 1 : 0
  
  name              = "/aws/lambda/event-processor-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "event-processor-logs-${var.environment}"
      Environment = var.environment
      Service     = "lambda"
    }
  )
}

resource "aws_cloudwatch_log_group" "notification_processor" {
  count = var.enable_notification_processor ? 1 : 0
  
  name              = "/aws/lambda/notification-processor-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "notification-processor-logs-${var.environment}"
      Environment = var.environment
      Service     = "lambda"
    }
  )
}

resource "aws_cloudwatch_log_group" "report_generator" {
  count = var.enable_report_generator ? 1 : 0
  
  name              = "/aws/lambda/report-generator-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "report-generator-logs-${var.environment}"
      Environment = var.environment
      Service     = "lambda"
    }
  )
}

# Event Processor Lambda Function
resource "aws_lambda_function" "event_processor" {
  count = var.enable_event_processor ? 1 : 0
  
  function_name = "event-processor-${var.environment}"
  role          = var.lambda_execution_role_arn
  
  runtime = var.lambda_runtime
  handler = var.lambda_runtime == "nodejs20.x" ? "index.handler" : (
    var.lambda_runtime == "python3.11" ? "handler.main" : 
    "com.turaf.events.EventHandler::handleRequest"
  )
  
  # Deployment package from S3 (requires manual upload)
  s3_bucket = var.lambda_artifacts_bucket
  s3_key    = "event-processor/${var.event_processor_version}/function.zip"
  
  memory_size = var.event_processor_memory
  timeout     = var.event_processor_timeout
  
  reserved_concurrent_executions = var.reserved_concurrent_executions
  
  environment {
    variables = {
      ENVIRONMENT    = var.environment
      AWS_REGION     = var.region
      EVENT_BUS_NAME = var.event_bus_name
      LOG_LEVEL      = var.environment == "prod" ? "INFO" : "DEBUG"
    }
  }
  
  # Optional VPC configuration
  dynamic "vpc_config" {
    for_each = var.use_vpc_mode && length(var.private_subnet_ids) > 0 ? [1] : []
    content {
      subnet_ids         = var.private_subnet_ids
      security_group_ids = [var.lambda_security_group_id]
    }
  }

  tags = merge(
    var.tags,
    {
      Name        = "event-processor-${var.environment}"
      Environment = var.environment
      Service     = "lambda"
    }
  )

  depends_on = [aws_cloudwatch_log_group.event_processor]
}

# Notification Processor Lambda Function
resource "aws_lambda_function" "notification_processor" {
  count = var.enable_notification_processor ? 1 : 0
  
  function_name = "notification-processor-${var.environment}"
  role          = var.lambda_execution_role_arn
  
  runtime = var.lambda_runtime
  handler = var.lambda_runtime == "nodejs20.x" ? "index.handler" : (
    var.lambda_runtime == "python3.11" ? "handler.main" : 
    "com.turaf.notification.NotificationHandler::handleRequest"
  )
  
  s3_bucket = var.lambda_artifacts_bucket
  s3_key    = "notification-processor/${var.notification_processor_version}/function.zip"
  
  memory_size = var.notification_processor_memory
  timeout     = var.notification_processor_timeout
  
  reserved_concurrent_executions = var.reserved_concurrent_executions
  
  environment {
    variables = {
      ENVIRONMENT    = var.environment
      AWS_REGION     = var.region
      EVENT_BUS_NAME = var.event_bus_name
      FROM_EMAIL     = var.from_email
      LOG_LEVEL      = var.environment == "prod" ? "INFO" : "DEBUG"
    }
  }

  tags = merge(
    var.tags,
    {
      Name        = "notification-processor-${var.environment}"
      Environment = var.environment
      Service     = "lambda"
    }
  )

  depends_on = [aws_cloudwatch_log_group.notification_processor]
}

# Report Generator Lambda Function
resource "aws_lambda_function" "report_generator" {
  count = var.enable_report_generator ? 1 : 0
  
  function_name = "report-generator-${var.environment}"
  role          = var.lambda_execution_role_arn
  
  runtime = var.lambda_runtime
  handler = var.lambda_runtime == "nodejs20.x" ? "index.handler" : (
    var.lambda_runtime == "python3.11" ? "handler.main" : 
    "com.turaf.reporting.ReportingHandler::handleRequest"
  )
  
  s3_bucket = var.lambda_artifacts_bucket
  s3_key    = "report-generator/${var.report_generator_version}/function.zip"
  
  memory_size = var.report_generator_memory
  timeout     = var.report_generator_timeout
  
  reserved_concurrent_executions = var.reserved_concurrent_executions
  
  environment {
    variables = {
      ENVIRONMENT         = var.environment
      AWS_REGION          = var.region
      EVENT_BUS_NAME      = var.event_bus_name
      REPORTS_BUCKET_NAME = var.reports_bucket_name
      LOG_LEVEL           = var.environment == "prod" ? "INFO" : "DEBUG"
    }
  }
  
  # VPC config for database access
  dynamic "vpc_config" {
    for_each = var.use_vpc_mode && length(var.private_subnet_ids) > 0 ? [1] : []
    content {
      subnet_ids         = var.private_subnet_ids
      security_group_ids = [var.lambda_security_group_id]
    }
  }

  tags = merge(
    var.tags,
    {
      Name        = "report-generator-${var.environment}"
      Environment = var.environment
      Service     = "lambda"
    }
  )

  depends_on = [aws_cloudwatch_log_group.report_generator]
}

# EventBridge Rule for Event Processor
resource "aws_cloudwatch_event_rule" "event_processor" {
  count = var.enable_event_processor ? 1 : 0
  
  name           = "event-processor-${var.environment}"
  description    = "Trigger event processor Lambda for domain events"
  event_bus_name = var.event_bus_name

  event_pattern = jsonencode({
    source = [{
      prefix = "turaf."
    }]
  })

  tags = merge(
    var.tags,
    {
      Name        = "event-processor-rule-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_cloudwatch_event_target" "event_processor" {
  count = var.enable_event_processor ? 1 : 0
  
  rule           = aws_cloudwatch_event_rule.event_processor[0].name
  event_bus_name = var.event_bus_name
  target_id      = "event-processor-lambda"
  arn            = aws_lambda_function.event_processor[0].arn
}

resource "aws_lambda_permission" "event_processor_eventbridge" {
  count = var.enable_event_processor ? 1 : 0
  
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.event_processor[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.event_processor[0].arn
}

# SQS Event Source Mapping for Notification Processor
resource "aws_lambda_event_source_mapping" "notification_processor" {
  count = var.enable_notification_processor ? 1 : 0
  
  event_source_arn = var.notifications_queue_arn
  function_name    = aws_lambda_function.notification_processor[0].arn
  batch_size       = 10
  
  scaling_config {
    maximum_concurrency = 10
  }
}

# SQS Event Source Mapping for Report Generator
resource "aws_lambda_event_source_mapping" "report_generator" {
  count = var.enable_report_generator ? 1 : 0
  
  event_source_arn = var.reports_queue_arn
  function_name    = aws_lambda_function.report_generator[0].arn
  batch_size       = 5
  
  scaling_config {
    maximum_concurrency = 5
  }
}

# Lambda Function URLs (optional - for direct HTTP invocation)
resource "aws_lambda_function_url" "report_generator" {
  count = var.enable_report_generator ? 1 : 0
  
  function_name      = aws_lambda_function.report_generator[0].function_name
  authorization_type = "AWS_IAM"
  
  cors {
    allow_credentials = true
    allow_origins     = ["*"]
    allow_methods     = ["POST"]
    allow_headers     = ["*"]
    max_age           = 86400
  }
}
