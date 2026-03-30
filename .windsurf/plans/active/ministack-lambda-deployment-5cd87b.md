# Ministack Lambda Deployment for Reporting and Notification Services

Create an init script for ministack that deploys reporting-service and notification-service as AWS Lambda functions triggered by SQS queues.

## Plan Metadata

**Created**: 2026-03-29  
**Status**: Active  
**Related Files**:
- `infrastructure/docker/ministack/init-aws.sh` (reference)
- `services/reporting-service/` (source code)
- `services/notification-service/` (source code)

**Related Docs**:
- `specs/reporting-service.md`
- `specs/aws-infrastructure.md`

---

## Architecture Overview

The script will implement the **EventBridge → SQS → Lambda** pattern:

1. **EventBridge** publishes events to event bus (already configured)
2. **EventBridge Rules** route events to **SQS queues** (already configured)
3. **Lambda functions** are triggered by **SQS event source mappings** (NEW)

### Services to Deploy

#### Reporting Service
- **Queue**: `turaf-report-events`
- **Lambda**: `turaf-reporting-service-local`
- **Handler**: `src.lambda_handler.lambda_handler`
- **Memory**: 1024 MB
- **Timeout**: 60 seconds
- **Trigger**: SQS queue `turaf-report-events`

#### Notification Service
- **Queue**: `turaf-notification-events`
- **Lambda**: `turaf-notification-service-local`
- **Handler**: `notification_handler.lambda_handler`
- **Memory**: 512 MB
- **Timeout**: 30 seconds
- **Trigger**: SQS queue `turaf-notification-events`

---

## Implementation Steps

### 1. Create Lambda Deployment Script Structure

**File**: `infrastructure/docker/ministack/init-lambda-services.sh`

**Sections**:
- Environment setup (AWS credentials, endpoint)
- Lambda packaging functions
- IAM role creation
- Lambda function deployment
- SQS event source mapping configuration
- Verification and summary

### 2. Package Lambda Functions

**Approach**: Create ZIP files with dependencies

**For Reporting Service**:
```bash
# Create temporary build directory
# Install dependencies from requirements.txt into build dir
# Copy source code into build dir
# Create ZIP archive
# Upload to ministack Lambda
```

**For Notification Service**:
```bash
# Same process as reporting service
# Different handler path and dependencies
```

**Challenges**:
- WeasyPrint has binary dependencies (may need to handle separately)
- Python packages need to be in root of ZIP or in python/ directory
- Ministack may have limitations on package size

### 3. Create IAM Roles and Policies

**Reporting Service Role**:
- `lambda:InvokeFunction`
- `s3:PutObject`, `s3:GetObject` on `turaf-reports-local`
- `events:PutEvents` on event bus
- `dynamodb:GetItem`, `dynamodb:PutItem` on `processed_events`
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
- `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes`

**Notification Service Role**:
- `lambda:InvokeFunction`
- `ses:SendEmail`, `ses:SendRawEmail`
- `dynamodb:GetItem`, `dynamodb:PutItem` on `processed_notification_events`
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
- `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes`

### 4. Deploy Lambda Functions

**AWS CLI Commands**:
```bash
aws --endpoint-url=$ENDPOINT lambda create-function \
  --function-name turaf-reporting-service-local \
  --runtime python3.11 \
  --role arn:aws:iam::000000000000:role/lambda-reporting-role \
  --handler src.lambda_handler.lambda_handler \
  --zip-file fileb://reporting-service.zip \
  --timeout 60 \
  --memory-size 1024 \
  --environment Variables={...}
```

**Environment Variables**:
- Reporting: `ENVIRONMENT=local`, `S3_BUCKET_NAME=turaf-reports-local`, `EVENT_BUS_NAME=turaf-events`, etc.
- Notification: `ENVIRONMENT=local`, `SES_FROM_EMAIL=notifications@turaf.com`, etc.

### 5. Configure SQS Event Source Mappings

**Create Event Source Mapping**:
```bash
aws --endpoint-url=$ENDPOINT lambda create-event-source-mapping \
  --function-name turaf-reporting-service-local \
  --event-source-arn arn:aws:sqs:us-east-1:000000000000:turaf-report-events \
  --batch-size 1 \
  --enabled
```

**Configuration**:
- Batch size: 1 (process one event at a time)
- Maximum batching window: 0 seconds
- Maximum retry attempts: 3
- On failure: Send to DLQ

### 6. Add Verification Steps

**Verify Deployment**:
- Check Lambda functions exist
- Check IAM roles created
- Check event source mappings active
- Test with sample event (optional)

---

## Script Design

### Helper Functions

```bash
# Package Python Lambda function
package_lambda() {
  service_name=$1
  service_path=$2
  handler_path=$3
  output_zip=$4
  
  # Create build directory
  # Install dependencies
  # Copy source code
  # Create ZIP
}

# Create IAM role for Lambda
create_lambda_role() {
  role_name=$1
  policy_document=$2
  
  # Create role
  # Attach policies
}

# Deploy Lambda function
deploy_lambda() {
  function_name=$1
  zip_file=$2
  handler=$3
  role_arn=$4
  memory=$5
  timeout=$6
  env_vars=$7
  
  # Create or update function
}

# Create event source mapping
create_event_source_mapping() {
  function_name=$1
  queue_arn=$2
  
  # Create mapping
}
```

### Error Handling

- Check if Lambda already exists (update vs create)
- Handle missing dependencies gracefully
- Provide clear error messages
- Clean up temporary files on exit

---

## Considerations

### Ministack Limitations

**Known Issues**:
- Lambda may have limited runtime support
- Event source mappings may not fully work
- IAM role simulation may be basic
- Large ZIP files may cause issues

**Fallback Strategy**:
- Use inline code for basic testing if ZIP deployment fails
- Simplify dependencies if package size is too large
- Document known limitations in script comments

### Dependencies

**Reporting Service**:
- `boto3` - Usually available in Lambda runtime
- `jinja2` - Need to package
- `weasyprint` - **COMPLEX** (has binary dependencies)
- `requests` - Need to package
- `python-json-logger` - Need to package
- `tenacity` - Need to package

**Notification Service**:
- `boto3` - Usually available in Lambda runtime
- `jinja2` - Need to package
- `requests` - Need to package
- `tenacity` - Need to package
- `python-json-logger` - Need to package

**Approach for WeasyPrint**:
- May need to use Lambda layers
- Or use simpler PDF library for local testing
- Or skip PDF generation in ministack (HTML only)

### Integration with Existing Init Script

**Options**:
1. **Separate script**: `init-lambda-services.sh` (run after `init-aws.sh`)
2. **Integrated**: Add Lambda section to existing `init-aws.sh`
3. **Modular**: Source from main init script

**Recommendation**: Create separate script for modularity and clarity.

---

## Testing Plan

### Manual Testing

1. Run `init-lambda-services.sh`
2. Verify Lambda functions created
3. Publish test event to EventBridge
4. Check SQS queue receives event
5. Verify Lambda triggered by SQS
6. Check CloudWatch logs for execution
7. Verify S3 report created (reporting service)
8. Verify email sent (notification service)

### Validation Commands

```bash
# List Lambda functions
aws --endpoint-url=http://localhost:4566 lambda list-functions

# Get function details
aws --endpoint-url=http://localhost:4566 lambda get-function \
  --function-name turaf-reporting-service-local

# List event source mappings
aws --endpoint-url=http://localhost:4566 lambda list-event-source-mappings \
  --function-name turaf-reporting-service-local

# Invoke function manually
aws --endpoint-url=http://localhost:4566 lambda invoke \
  --function-name turaf-reporting-service-local \
  --payload '{"Records":[...]}' \
  response.json
```

---

## Deliverables

1. **Script**: `infrastructure/docker/ministack/init-lambda-services.sh`
   - Executable bash script
   - Well-commented and documented
   - Error handling and validation
   - Summary output

2. **Documentation**: Update script with inline comments explaining:
   - Architecture decisions
   - Known limitations
   - Usage instructions
   - Troubleshooting tips

3. **Integration**: Instructions for running with docker-compose or manually

---

## Success Criteria

- ✅ Script successfully creates both Lambda functions
- ✅ IAM roles and policies configured correctly
- ✅ SQS event source mappings active
- ✅ Lambda functions can be invoked manually
- ✅ Environment variables set correctly
- ✅ Script is idempotent (can run multiple times)
- ✅ Clear error messages and logging
- ✅ Summary output shows deployment status

---

## Future Enhancements

- Add Lambda layers for shared dependencies
- Support for Lambda function updates
- Automated testing with sample events
- CloudWatch log group creation
- Dead letter queue configuration
- Lambda concurrency limits
- Cost estimation output
