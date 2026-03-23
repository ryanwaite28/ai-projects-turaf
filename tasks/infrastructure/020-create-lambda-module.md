# Task: Create Lambda Module

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 3 hours  

## Objective

Create Terraform module for Lambda functions (reporting and notification services).

## Prerequisites

- [x] Task 006: Messaging modules created

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/lambda/main.tf`
- `infrastructure/terraform/modules/lambda/variables.tf`
- `infrastructure/terraform/modules/lambda/outputs.tf`

## Implementation Details

### Lambda Functions

```hcl
resource "aws_lambda_function" "reporting_service" {
  function_name = "reporting-service-${var.environment}"
  role          = aws_iam_role.lambda_execution.arn
  
  runtime = "java17"
  handler = "com.turaf.reporting.ReportingHandler::handleRequest"
  
  s3_bucket = var.lambda_artifacts_bucket
  s3_key    = "reporting-service/${var.reporting_service_version}/reporting-service.jar"
  
  memory_size = 1024
  timeout     = 300
  
  environment {
    variables = {
      ENVIRONMENT         = var.environment
      EVENT_BUS_NAME      = var.event_bus_name
      REPORTS_BUCKET_NAME = var.reports_bucket_name
      IDEMPOTENCY_TABLE   = aws_dynamodb_table.idempotency.name
    }
  }
  
  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [aws_security_group.lambda.id]
  }
  
  tags = {
    Name        = "reporting-service-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_lambda_function" "notification_service" {
  function_name = "notification-service-${var.environment}"
  role          = aws_iam_role.lambda_execution.arn
  
  runtime = "java17"
  handler = "com.turaf.notification.NotificationHandler::handleRequest"
  
  s3_bucket = var.lambda_artifacts_bucket
  s3_key    = "notification-service/${var.notification_service_version}/notification-service.jar"
  
  memory_size = 512
  timeout     = 60
  
  environment {
    variables = {
      ENVIRONMENT        = var.environment
      EVENT_BUS_NAME     = var.event_bus_name
      FROM_EMAIL         = var.from_email
      IDEMPOTENCY_TABLE  = aws_dynamodb_table.idempotency.name
    }
  }
  
  tags = {
    Name        = "notification-service-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_dynamodb_table" "idempotency" {
  name         = "turaf-idempotency-${var.environment}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"
  
  attribute {
    name = "eventId"
    type = "S"
  }
  
  ttl {
    attribute_name = "ttl"
    enabled        = true
  }
  
  tags = {
    Name        = "turaf-idempotency-${var.environment}"
    Environment = var.environment
  }
}
```

## Acceptance Criteria

- [ ] Lambda functions created
- [ ] IAM roles configured
- [ ] VPC configuration set
- [ ] Environment variables configured
- [ ] DynamoDB idempotency table created
- [ ] terraform plan succeeds

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify Lambda configurations
- Check IAM permissions

## References

- Specification: `specs/aws-infrastructure.md` (Lambda section)
- Related Tasks: 008-create-security-modules
