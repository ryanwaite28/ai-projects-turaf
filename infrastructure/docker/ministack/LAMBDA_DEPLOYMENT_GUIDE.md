# Lambda Deployment Quick Start Guide

This guide provides quick instructions for deploying Lambda services to MiniStack.

## Prerequisites

- MiniStack running on `localhost:4566`
- AWS CLI installed
- Python 3.11+ and pip installed
- Core AWS resources initialized (run `init-aws.sh` first)

## Quick Start

### 1. Test Packaging (Optional but Recommended)

Verify Lambda packaging works without deploying:

```bash
cd infrastructure/docker/ministack
./test-lambda-packaging.sh
```

Expected output:
```
✓ Reporting Service: PASSED
✓ Notification Service: PASSED
✓ All tests passed!
```

### 2. Deploy Lambda Functions

```bash
cd infrastructure/docker/ministack
./init-lambda-services.sh
```

This will:
- Package both services with dependencies
- Create IAM roles and policies
- Deploy Lambda functions to MiniStack
- Configure SQS event source mappings

Expected deployment time: 2-5 minutes

## What Gets Deployed

### Reporting Service Lambda
- **Function Name**: `turaf-reporting-service-local`
- **Runtime**: Python 3.11
- **Memory**: 1024 MB
- **Timeout**: 60 seconds
- **Trigger**: `turaf-report-events` SQS queue
- **Handler**: `src.lambda_handler.lambda_handler`

### Notification Service Lambda
- **Function Name**: `turaf-notification-service-local`
- **Runtime**: Python 3.11
- **Memory**: 512 MB
- **Timeout**: 30 seconds
- **Trigger**: `turaf-notification-events` SQS queue
- **Handler**: `notification_handler.lambda_handler`

## Verification

### List Deployed Functions

```bash
aws --endpoint-url=http://localhost:4566 lambda list-functions
```

### Get Function Details

```bash
# Reporting service
aws --endpoint-url=http://localhost:4566 lambda get-function \
  --function-name turaf-reporting-service-local

# Notification service
aws --endpoint-url=http://localhost:4566 lambda get-function \
  --function-name turaf-notification-service-local
```

### Check Event Source Mappings

```bash
aws --endpoint-url=http://localhost:4566 lambda list-event-source-mappings
```

## Manual Testing

### Invoke Reporting Service

```bash
aws --endpoint-url=http://localhost:4566 lambda invoke \
  --function-name turaf-reporting-service-local \
  --payload '{"Records":[{"body":"{}"}]}' \
  response.json

cat response.json
```

### Invoke Notification Service

```bash
aws --endpoint-url=http://localhost:4566 lambda invoke \
  --function-name turaf-notification-service-local \
  --payload '{"Records":[{"body":"{}"}]}' \
  response.json

cat response.json
```

## Troubleshooting

### MiniStack Not Running

**Error**: `Error: MiniStack is not running at http://localhost:4566`

**Solution**:
```bash
# Start MiniStack
docker-compose up -d ministack

# Wait for it to be ready
sleep 5

# Try again
./init-lambda-services.sh
```

### Packaging Fails

**Error**: `Failed to package reporting-service`

**Solution**:
- Check Python version: `python --version` (need 3.11+)
- Check pip is installed: `pip --version`
- Try test script first: `./test-lambda-packaging.sh`

### Lambda Deployment Fails

**Error**: `Failed to deploy reporting-service Lambda`

**Solution**:
- Verify MiniStack health: `curl http://localhost:4566/_localstack/health`
- Check AWS CLI: `aws --version`
- Review error messages in script output

### Event Source Mapping Not Working

**Issue**: Lambda not triggered by SQS messages

**Note**: This is a known MiniStack limitation. Event source mappings may not be fully functional.

**Workaround**: Use manual invocation for testing:
```bash
aws --endpoint-url=http://localhost:4566 lambda invoke \
  --function-name turaf-reporting-service-local \
  --payload file://test-event.json \
  response.json
```

## Complete Setup Flow

For a fresh MiniStack setup:

```bash
# 1. Start MiniStack
docker-compose up -d ministack

# 2. Wait for ready
sleep 5

# 3. Initialize AWS resources
./infrastructure/docker/ministack/init-aws.sh

# 4. Deploy Lambda services
./infrastructure/docker/ministack/init-lambda-services.sh

# 5. Verify deployment
aws --endpoint-url=http://localhost:4566 lambda list-functions
```

## Cleanup

To remove Lambda functions:

```bash
# Delete functions
aws --endpoint-url=http://localhost:4566 lambda delete-function \
  --function-name turaf-reporting-service-local

aws --endpoint-url=http://localhost:4566 lambda delete-function \
  --function-name turaf-notification-service-local

# Delete IAM roles
aws --endpoint-url=http://localhost:4566 iam delete-role-policy \
  --role-name lambda-reporting-role \
  --policy-name lambda-reporting-role-basic-policy

aws --endpoint-url=http://localhost:4566 iam delete-role \
  --role-name lambda-reporting-role

aws --endpoint-url=http://localhost:4566 iam delete-role-policy \
  --role-name lambda-notification-role \
  --policy-name lambda-notification-role-basic-policy

aws --endpoint-url=http://localhost:4566 iam delete-role \
  --role-name lambda-notification-role
```

## Files Created

- `init-lambda-services.sh` - Main deployment script
- `test-lambda-packaging.sh` - Packaging validation script
- `README.md` - Detailed documentation
- `LAMBDA_DEPLOYMENT_GUIDE.md` - This quick start guide

## Next Steps

After deployment:
1. Test Lambda functions with manual invocation
2. Publish test events to EventBridge
3. Verify reports are generated in S3
4. Check notification emails are sent via SES

## Support

For detailed documentation, see:
- `README.md` - Complete MiniStack setup guide
- `../../../specs/reporting-service.md` - Reporting service specification
- `../../../specs/notification-service.md` - Notification service specification
