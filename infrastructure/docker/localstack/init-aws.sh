#!/bin/bash

echo "=========================================="
echo "Initializing LocalStack AWS Resources"
echo "=========================================="

# Wait for LocalStack to be fully ready
echo "Waiting for LocalStack to be ready..."
sleep 5

# Set AWS CLI to use LocalStack endpoint
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# ==========================================
# S3 Buckets
# ==========================================
echo "Creating S3 buckets..."
awslocal s3 mb s3://turaf-reports-local 2>/dev/null || echo "Bucket turaf-reports-local already exists"
awslocal s3 mb s3://turaf-artifacts-local 2>/dev/null || echo "Bucket turaf-artifacts-local already exists"
awslocal s3 mb s3://turaf-frontend-local 2>/dev/null || echo "Bucket turaf-frontend-local already exists"

# Enable versioning on reports bucket
awslocal s3api put-bucket-versioning \
    --bucket turaf-reports-local \
    --versioning-configuration Status=Enabled 2>/dev/null || true

echo "✓ S3 buckets created"

# ==========================================
# EventBridge Event Bus
# ==========================================
echo "Creating EventBridge event bus..."
awslocal events create-event-bus --name turaf-events 2>/dev/null || echo "Event bus turaf-events already exists"
echo "✓ EventBridge event bus created"

# ==========================================
# SQS Queues
# ==========================================
echo "Creating SQS queues..."
awslocal sqs create-queue --queue-name turaf-experiment-events 2>/dev/null || echo "Queue turaf-experiment-events already exists"
awslocal sqs create-queue --queue-name turaf-metric-events 2>/dev/null || echo "Queue turaf-metric-events already exists"
awslocal sqs create-queue --queue-name turaf-notification-events 2>/dev/null || echo "Queue turaf-notification-events already exists"
awslocal sqs create-queue --queue-name turaf-report-events 2>/dev/null || echo "Queue turaf-report-events already exists"

# Create DLQ (Dead Letter Queue)
awslocal sqs create-queue --queue-name turaf-dlq 2>/dev/null || echo "Queue turaf-dlq already exists"

# Create FIFO queues for Communications Service
echo "Creating Communications FIFO queues..."
awslocal sqs create-queue \
    --queue-name communications-direct-messages.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=false 2>/dev/null || \
    echo "Queue communications-direct-messages.fifo already exists"

awslocal sqs create-queue \
    --queue-name communications-group-messages.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=false 2>/dev/null || \
    echo "Queue communications-group-messages.fifo already exists"

echo "✓ SQS queues created"

# ==========================================
# SNS Topics
# ==========================================
echo "Creating SNS topics..."
awslocal sns create-topic --name turaf-notifications 2>/dev/null || echo "Topic turaf-notifications already exists"
awslocal sns create-topic --name turaf-alerts 2>/dev/null || echo "Topic turaf-alerts already exists"
echo "✓ SNS topics created"

# ==========================================
# Secrets Manager
# ==========================================
echo "Creating Secrets Manager secrets..."

# Database user passwords
awslocal secretsmanager create-secret \
    --name turaf/db/identity-user-password \
    --secret-string "${IDENTITY_USER_PASSWORD}" 2>/dev/null || \
    awslocal secretsmanager update-secret \
    --secret-id turaf/db/identity-user-password \
    --secret-string "${IDENTITY_USER_PASSWORD}" 2>/dev/null

awslocal secretsmanager create-secret \
    --name turaf/db/organization-user-password \
    --secret-string "${ORGANIZATION_USER_PASSWORD}" 2>/dev/null || \
    awslocal secretsmanager update-secret \
    --secret-id turaf/db/organization-user-password \
    --secret-string "${ORGANIZATION_USER_PASSWORD}" 2>/dev/null

awslocal secretsmanager create-secret \
    --name turaf/db/experiment-user-password \
    --secret-string "${EXPERIMENT_USER_PASSWORD}" 2>/dev/null || \
    awslocal secretsmanager update-secret \
    --secret-id turaf/db/experiment-user-password \
    --secret-string "${EXPERIMENT_USER_PASSWORD}" 2>/dev/null

awslocal secretsmanager create-secret \
    --name turaf/db/metrics-user-password \
    --secret-string "${METRICS_USER_PASSWORD}" 2>/dev/null || \
    awslocal secretsmanager update-secret \
    --secret-id turaf/db/metrics-user-password \
    --secret-string "${METRICS_USER_PASSWORD}" 2>/dev/null

awslocal secretsmanager create-secret \
    --name turaf/db/communications-user-password \
    --secret-string "${COMMUNICATIONS_USER_PASSWORD}" 2>/dev/null || \
    awslocal secretsmanager update-secret \
    --secret-id turaf/db/communications-user-password \
    --secret-string "${COMMUNICATIONS_USER_PASSWORD}" 2>/dev/null

echo "✓ Secrets Manager secrets created"

# ==========================================
# EventBridge Rules (optional)
# ==========================================
echo "Creating EventBridge rules..."

# Rule to route experiment events to SQS
awslocal events put-rule \
    --name turaf-experiment-events-rule \
    --event-bus-name turaf-events \
    --event-pattern '{"source":["turaf.experiment-service"]}' 2>/dev/null || true

# Add target to the rule
QUEUE_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/turaf-experiment-events \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text 2>/dev/null)

if [ -n "$QUEUE_ARN" ]; then
    awslocal events put-targets \
        --rule turaf-experiment-events-rule \
        --event-bus-name turaf-events \
        --targets "Id=1,Arn=$QUEUE_ARN" 2>/dev/null || true
fi

echo "✓ EventBridge rules created"

# ==========================================
# Summary
# ==========================================
echo "=========================================="
echo "LocalStack initialization complete!"
echo ""
echo "Available resources:"
echo "  - S3 Buckets: turaf-reports-local, turaf-artifacts-local, turaf-frontend-local"
echo "  - EventBridge: turaf-events"
echo "  - SQS Queues: turaf-experiment-events, turaf-metric-events, turaf-notification-events, turaf-dlq"
echo "  - SQS FIFO Queues: communications-direct-messages.fifo, communications-group-messages.fifo"
echo "  - SNS Topics: turaf-notifications, turaf-alerts"
echo "  - Secrets: Database user passwords"
echo ""
echo "Access LocalStack at: http://localhost:4566"
echo "=========================================="
