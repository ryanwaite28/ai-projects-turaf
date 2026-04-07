# Lambda Services Deployment Guide

This document describes the deployment process for Lambda-based services in the Turaf platform.

## Overview

Two services in the Turaf platform are deployed as AWS Lambda functions (Python 3.11) rather than containerized ECS services:

1. **Notification Service** (`notification-processor`)
2. **Reporting Service** (`report-generator`)

These services are event-driven processors that respond to domain events via EventBridge and SQS.

---

## Architecture

### Notification Service
- **Function Name**: `notification-processor-{environment}`
- **Runtime**: Python 3.11
- **Trigger**: SQS queue (notifications queue)
- **Purpose**: Send email notifications via SES and webhooks
- **Memory**: 512 MB
- **Timeout**: 60 seconds

### Reporting Service
- **Function Name**: `report-generator-{environment}`
- **Runtime**: Python 3.11
- **Trigger**: SQS queue (reports queue)
- **Purpose**: Generate PDF/HTML reports for completed experiments
- **Memory**: 1024 MB
- **Timeout**: 300 seconds (5 minutes)

---

## Infrastructure

### Lambda Functions
Lambda functions are defined in the shared infrastructure Terraform module:
- **Module**: `infrastructure/terraform/modules/lambda/`
- **Configuration**: `infrastructure/terraform/environments/{env}/main.tf`

### Feature Flags
Lambda functions are **disabled by default** for cost optimization. To enable:

```hcl
# In terraform.tfvars
enable_notification_processor = true
enable_report_generator       = true
```

### S3 Artifacts Bucket
Lambda deployment packages are stored in S3:
- **Bucket Name**: `turaf-lambda-artifacts-{environment}`
- **Key Pattern**: `{service-name}/{version}/function.zip`
- **Latest**: `{service-name}/latest/function.zip`

---

## Deployment Process

### GitHub Actions Workflows

**Notification Service**: `.github/workflows/deploy-notification-lambda.yml`
**Reporting Service**: `.github/workflows/deploy-reporting-lambda.yml`

### Deployment Steps

1. **Security Scan** (main branch only)
   - Trivy vulnerability scanning
   - Upload results to GitHub Security

2. **Package Lambda Function**
   - Install Python dependencies
   - Copy source code to package directory
   - Create ZIP deployment package

3. **Upload to S3**
   - Create artifacts bucket if needed
   - Upload versioned package
   - Upload as 'latest' for easy reference

4. **Update Lambda Function**
   - Check if function exists
   - Update function code from S3
   - Publish new version
   - Wait for update completion

### Deployment Triggers

**Automatic Deployment**:
- Push to `develop` → Deploy to DEV
- Push to `main` → Deploy to DEV → QA → PROD

**Manual Deployment**:
- Workflow dispatch with environment selection

---

## Initial Setup

### Prerequisites

1. **Infrastructure Deployed**
   ```bash
   # Deploy shared infrastructure first
   cd infrastructure/terraform/environments/dev
   terraform apply
   ```

2. **Enable Lambda Functions**
   ```hcl
   # In terraform.tfvars
   enable_notification_processor = true
   enable_report_generator       = true
   lambda_artifacts_bucket       = "turaf-lambda-artifacts-dev"
   ```

3. **Re-apply Infrastructure**
   ```bash
   terraform apply
   ```

### First Deployment

Once infrastructure is deployed with Lambda functions enabled:

```bash
# Push code to trigger deployment
git push origin develop
```

The workflow will:
1. Package the Lambda function
2. Upload to S3
3. Update the Lambda function code

---

## Local Development

### Running Locally

**Notification Service**:
```bash
cd services/notification-service
python -m pip install -r requirements.txt
python notification_handler.py
```

**Reporting Service**:
```bash
cd services/reporting-service
python -m pip install -r requirements.txt
python -m src.main
```

### Testing

**Unit Tests**:
```bash
cd services/notification-service
pytest tests/
```

**Integration Tests** (with LocalStack):
```bash
# Start LocalStack
docker-compose up localstack

# Run integration tests
pytest tests/integration/
```

---

## Monitoring

### CloudWatch Logs

**Log Groups**:
- `/aws/lambda/notification-processor-{environment}`
- `/aws/lambda/report-generator-{environment}`

**View Logs**:
```bash
aws logs tail /aws/lambda/notification-processor-dev --follow
```

### CloudWatch Metrics

**Custom Metrics**:
- `NotificationsSent`
- `NotificationFailures`
- `ReportsGenerated`
- `ReportGenerationErrors`

**View Metrics**:
```bash
aws cloudwatch get-metric-statistics \
  --namespace Turaf/Lambda \
  --metric-name NotificationsSent \
  --dimensions Name=Environment,Value=dev \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-02T00:00:00Z \
  --period 3600 \
  --statistics Sum
```

---

## Troubleshooting

### Function Not Found

**Problem**: `aws lambda get-function` returns "ResourceNotFoundException"

**Solution**: Lambda function hasn't been created by infrastructure yet.
1. Enable in `terraform.tfvars`: `enable_notification_processor = true`
2. Apply infrastructure: `terraform apply`
3. Re-run deployment workflow

### Package Too Large

**Problem**: Deployment package exceeds Lambda limits (50 MB zipped, 250 MB unzipped)

**Solution**: 
1. Use Lambda Layers for large dependencies
2. Optimize dependencies (remove unnecessary packages)
3. Use S3 for large assets (templates, fonts)

### Timeout Errors

**Problem**: Lambda function times out during execution

**Solution**:
1. Increase timeout in Terraform configuration
2. Optimize code for faster execution
3. Use async processing for long-running tasks

### Permission Errors

**Problem**: Lambda function can't access AWS services (SES, S3, EventBridge)

**Solution**: Check IAM role permissions in `infrastructure/terraform/modules/security/iam.tf`

Required permissions:
- `ses:SendEmail`
- `s3:PutObject`, `s3:GetObject`
- `events:PutEvents`
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`

---

## Cost Optimization

### Current Configuration (Demo Mode)

Lambda functions are **disabled by default** to minimize costs during development:

```hcl
enable_notification_processor = false
enable_report_generator       = false
```

### Production Configuration

Enable Lambda functions for production workloads:

```hcl
enable_notification_processor = true
enable_report_generator       = true
reserved_concurrent_executions = 10  # Limit concurrent executions
```

### Cost Breakdown

**Notification Service** (512 MB, 60s timeout):
- Free Tier: 1M requests/month, 400,000 GB-seconds
- After Free Tier: $0.20 per 1M requests + $0.0000166667 per GB-second

**Reporting Service** (1024 MB, 300s timeout):
- Free Tier: Same as above
- After Free Tier: Same pricing

**Estimated Monthly Cost** (1000 notifications, 100 reports):
- Notification: ~$0.01
- Reporting: ~$0.05
- **Total**: ~$0.06/month (within free tier)

---

## Differences from ECS Services

### Lambda Services (notification, reporting)
- ✅ Event-driven, serverless
- ✅ Auto-scaling, pay-per-use
- ✅ No container management
- ✅ Lower cost for sporadic workloads
- ❌ 15-minute max execution time
- ❌ Cold start latency
- ❌ Limited runtime customization

### ECS Services (identity, organization, experiment, metrics, bff-api, communications, ws-gateway)
- ✅ Long-running HTTP services
- ✅ Full container control
- ✅ No execution time limits
- ✅ Consistent performance
- ❌ Higher cost (always running)
- ❌ Manual scaling configuration
- ❌ Container orchestration overhead

---

## References

- [Notification Service Spec](../specs/notification-service.md)
- [Reporting Service Spec](../specs/reporting-service.md)
- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [Lambda Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html)
- [PROJECT.md](../PROJECT.md) - Section 40 (Lambda Services)
