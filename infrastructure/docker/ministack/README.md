# MiniStack Local Development Setup

This directory contains initialization scripts for setting up AWS services in MiniStack for local development.

## Scripts

### init-aws.sh

Initializes core AWS infrastructure resources:
- S3 buckets
- EventBridge event buses
- SQS queues (standard and FIFO)
- SNS topics
- Secrets Manager secrets
- DynamoDB tables
- SES email identities
- EventBridge rules

**Usage**:
```bash
./init-aws.sh
```

**Prerequisites**:
- MiniStack running on `localhost:4566`
- AWS CLI installed
- Environment variables set (see docker-compose.yml)

### init-lambda-services.sh

Deploys Lambda functions for event-driven services:
- **Reporting Service**: Generates experiment reports
- **Notification Service**: Sends email and webhook notifications

**Usage**:
```bash
# Run after init-aws.sh
./init-lambda-services.sh
```

**Prerequisites**:
- MiniStack running on `localhost:4566`
- AWS CLI installed
- Python 3.11+ and pip installed
- Core AWS resources created (run init-aws.sh first)

**What it does**:
1. Packages Python Lambda functions with dependencies
2. Creates IAM roles and policies
3. Deploys Lambda functions to MiniStack
4. Configures SQS event source mappings

**Deployed Functions**:
- `turaf-reporting-service-local` (1024 MB, 60s timeout)
  - Triggered by: `turaf-report-events` SQS queue
  - Handler: `src.lambda_handler.lambda_handler`
  
- `turaf-notification-service-local` (512 MB, 30s timeout)
  - Triggered by: `turaf-notification-events` SQS queue
  - Handler: `notification_handler.lambda_handler`

## Complete Setup

To initialize all MiniStack resources:

```bash
# 1. Start MiniStack (via docker-compose)
docker-compose up -d ministack

# 2. Wait for MiniStack to be ready
sleep 5

# 3. Initialize core AWS resources
./infrastructure/docker/ministack/init-aws.sh

# 4. Deploy Lambda services
./infrastructure/docker/ministack/init-lambda-services.sh
```

## Testing Lambda Functions

### Manual Invocation

```bash
# Invoke reporting service
aws --endpoint-url=http://localhost:4566 lambda invoke \
  --function-name turaf-reporting-service-local \
  --payload '{"Records":[{"body":"{}"}]}' \
  response.json

# Invoke notification service
aws --endpoint-url=http://localhost:4566 lambda invoke \
  --function-name turaf-notification-service-local \
  --payload '{"Records":[{"body":"{}"}]}' \
  response.json
```

### List Functions

```bash
aws --endpoint-url=http://localhost:4566 lambda list-functions
```

### View Function Details

```bash
aws --endpoint-url=http://localhost:4566 lambda get-function \
  --function-name turaf-reporting-service-local
```

### List Event Source Mappings

```bash
aws --endpoint-url=http://localhost:4566 lambda list-event-source-mappings
```

## Known Limitations

### MiniStack Lambda Support

- **Event Source Mappings**: May not be fully functional in MiniStack
  - SQS → Lambda triggers may not work automatically
  - Manual invocation required for testing
  
- **Binary Dependencies**: Some Python packages with binary dependencies (e.g., WeasyPrint) may not work correctly
  - Consider using simpler alternatives for local testing
  
- **CloudWatch Logs**: May have limited support
  - Check MiniStack documentation for log viewing capabilities

### Workarounds

If event source mappings don't work:
1. Publish events to EventBridge manually
2. Invoke Lambda functions directly with test payloads
3. Use integration tests that call Lambda invoke API

## Architecture

```
EventBridge (turaf-events)
    ↓
EventBridge Rules
    ↓
SQS Queues (turaf-report-events, turaf-notification-events)
    ↓
Lambda Functions (reporting-service, notification-service)
    ↓
Outputs (S3 reports, SES emails, EventBridge events)
```

## Troubleshooting

### Script Fails to Package Lambda

**Issue**: `pip install` fails or package size too large

**Solution**:
- Check Python version (requires 3.11+)
- Install dependencies manually: `pip install -r requirements.txt`
- Check available disk space

### Lambda Function Not Created

**Issue**: `aws lambda create-function` fails

**Solution**:
- Verify MiniStack is running: `curl http://localhost:4566/_localstack/health`
- Check AWS CLI configuration
- Review error messages in script output

### Event Source Mapping Not Working

**Issue**: Lambda not triggered by SQS messages

**Solution**:
- This is a known MiniStack limitation
- Use manual invocation for testing
- Or test with real AWS Lambda in DEV environment

### Import Errors in Lambda

**Issue**: Lambda fails with `ModuleNotFoundError`

**Solution**:
- Verify dependencies are packaged in ZIP
- Check handler path matches function structure
- Review Lambda logs (if available)

## Environment Variables

### Reporting Service

- `ENVIRONMENT`: local
- `S3_BUCKET_NAME`: turaf-reports-local
- `EVENT_BUS_NAME`: turaf-events
- `EXPERIMENT_SERVICE_URL`: http://host.docker.internal:8080
- `METRICS_SERVICE_URL`: http://host.docker.internal:8080
- `IDEMPOTENCY_TABLE_NAME`: processed_events
- `AWS_ENDPOINT_URL`: http://localhost:4566

### Notification Service

- `ENVIRONMENT`: local
- `SES_FROM_EMAIL`: notifications@turaf.com
- `EXPERIMENT_SERVICE_URL`: http://host.docker.internal:8080
- `ORGANIZATION_SERVICE_URL`: http://host.docker.internal:8080
- `FRONTEND_URL`: http://localhost:4200
- `IDEMPOTENCY_TABLE_NAME`: processed_notification_events
- `AWS_ENDPOINT_URL`: http://localhost:4566
- `LOG_LEVEL`: INFO

## Cleanup

To remove Lambda functions:

```bash
# Delete functions
aws --endpoint-url=http://localhost:4566 lambda delete-function \
  --function-name turaf-reporting-service-local

aws --endpoint-url=http://localhost:4566 lambda delete-function \
  --function-name turaf-notification-service-local

# Delete IAM roles
aws --endpoint-url=http://localhost:4566 iam delete-role \
  --role-name lambda-reporting-role

aws --endpoint-url=http://localhost:4566 iam delete-role \
  --role-name lambda-notification-role
```

## References

- [MiniStack Documentation](https://docs.localstack.cloud/user-guide/aws/lambda/)
- [AWS Lambda Python](https://docs.aws.amazon.com/lambda/latest/dg/lambda-python.html)
- [Reporting Service Spec](../../../specs/reporting-service.md)
- [Notification Service Spec](../../../specs/notification-service.md)
