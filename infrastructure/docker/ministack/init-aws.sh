#!/bin/bash

echo "=========================================="
echo "Initializing MiniStack AWS Resources"
echo "=========================================="

# Wait for MiniStack to be fully ready
echo "Waiting for MiniStack to be ready..."
sleep 5

# Set AWS CLI to use MiniStack endpoint
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
ENDPOINT="http://localhost:4566"

# ==========================================
# S3 Buckets
# ==========================================
echo "Creating S3 buckets..."
aws --endpoint-url=$ENDPOINT s3 mb s3://turaf-reports-local 2>/dev/null || echo "Bucket turaf-reports-local already exists"
aws --endpoint-url=$ENDPOINT s3 mb s3://turaf-artifacts-local 2>/dev/null || echo "Bucket turaf-artifacts-local already exists"
aws --endpoint-url=$ENDPOINT s3 mb s3://turaf-frontend-local 2>/dev/null || echo "Bucket turaf-frontend-local already exists"

# Enable versioning on reports bucket
aws --endpoint-url=$ENDPOINT s3api put-bucket-versioning \
    --bucket turaf-reports-local \
    --versioning-configuration Status=Enabled 2>/dev/null || true

echo "✓ S3 buckets created"

# ==========================================
# EventBridge Event Bus
# ==========================================
echo "Creating EventBridge event bus..."
aws --endpoint-url=$ENDPOINT events create-event-bus --name turaf-events 2>/dev/null || echo "Event bus turaf-events already exists"
aws --endpoint-url=$ENDPOINT events create-event-bus --name turaf-event-bus 2>/dev/null || echo "Event bus turaf-event-bus already exists"
echo "✓ EventBridge event buses created"

# ==========================================
# SQS Queues
# ==========================================
echo "Creating SQS queues..."
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name turaf-experiment-events 2>/dev/null || echo "Queue turaf-experiment-events already exists"
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name turaf-metric-events 2>/dev/null || echo "Queue turaf-metric-events already exists"
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name turaf-notification-events 2>/dev/null || echo "Queue turaf-notification-events already exists"
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name turaf-report-events 2>/dev/null || echo "Queue turaf-report-events already exists"

# Create DLQ (Dead Letter Queue)
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name turaf-dlq 2>/dev/null || echo "Queue turaf-dlq already exists"

# Create FIFO queues for Communications Service
echo "Creating Communications FIFO queues..."
aws --endpoint-url=$ENDPOINT sqs create-queue \
    --queue-name communications-direct-messages.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=false 2>/dev/null || \
    echo "Queue communications-direct-messages.fifo already exists"

aws --endpoint-url=$ENDPOINT sqs create-queue \
    --queue-name communications-group-messages.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=false 2>/dev/null || \
    echo "Queue communications-group-messages.fifo already exists"

echo "✓ SQS queues created"

# ==========================================
# SNS Topics
# ==========================================
echo "Creating SNS topics..."
aws --endpoint-url=$ENDPOINT sns create-topic --name turaf-notifications 2>/dev/null || echo "Topic turaf-notifications already exists"
aws --endpoint-url=$ENDPOINT sns create-topic --name turaf-alerts 2>/dev/null || echo "Topic turaf-alerts already exists"
echo "✓ SNS topics created"

# ==========================================
# Secrets Manager
# ==========================================
echo "Creating Secrets Manager secrets..."

# Database user passwords
aws --endpoint-url=$ENDPOINT secretsmanager create-secret \
    --name turaf/db/identity-user-password \
    --secret-string "${IDENTITY_USER_PASSWORD}" 2>/dev/null || \
    aws --endpoint-url=$ENDPOINT secretsmanager update-secret \
    --secret-id turaf/db/identity-user-password \
    --secret-string "${IDENTITY_USER_PASSWORD}" 2>/dev/null

aws --endpoint-url=$ENDPOINT secretsmanager create-secret \
    --name turaf/db/organization-user-password \
    --secret-string "${ORGANIZATION_USER_PASSWORD}" 2>/dev/null || \
    aws --endpoint-url=$ENDPOINT secretsmanager update-secret \
    --secret-id turaf/db/organization-user-password \
    --secret-string "${ORGANIZATION_USER_PASSWORD}" 2>/dev/null

aws --endpoint-url=$ENDPOINT secretsmanager create-secret \
    --name turaf/db/experiment-user-password \
    --secret-string "${EXPERIMENT_USER_PASSWORD}" 2>/dev/null || \
    aws --endpoint-url=$ENDPOINT secretsmanager update-secret \
    --secret-id turaf/db/experiment-user-password \
    --secret-string "${EXPERIMENT_USER_PASSWORD}" 2>/dev/null

aws --endpoint-url=$ENDPOINT secretsmanager create-secret \
    --name turaf/db/metrics-user-password \
    --secret-string "${METRICS_USER_PASSWORD}" 2>/dev/null || \
    aws --endpoint-url=$ENDPOINT secretsmanager update-secret \
    --secret-id turaf/db/metrics-user-password \
    --secret-string "${METRICS_USER_PASSWORD}" 2>/dev/null

aws --endpoint-url=$ENDPOINT secretsmanager create-secret \
    --name turaf/db/communications-user-password \
    --secret-string "${COMMUNICATIONS_USER_PASSWORD}" 2>/dev/null || \
    aws --endpoint-url=$ENDPOINT secretsmanager update-secret \
    --secret-id turaf/db/communications-user-password \
    --secret-string "${COMMUNICATIONS_USER_PASSWORD}" 2>/dev/null

echo "✓ Secrets Manager secrets created"

# ==========================================
# DynamoDB Tables
# ==========================================
echo "Creating DynamoDB tables..."

# Idempotency table for notification-service
aws --endpoint-url=$ENDPOINT dynamodb create-table \
    --table-name processed_notification_events \
    --attribute-definitions \
        AttributeName=eventId,AttributeType=S \
    --key-schema \
        AttributeName=eventId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST 2>/dev/null || \
    echo "Table processed_notification_events already exists"

# Enable TTL on notification events table
aws --endpoint-url=$ENDPOINT dynamodb update-time-to-live \
    --table-name processed_notification_events \
    --time-to-live-specification "Enabled=true,AttributeName=ttl" 2>/dev/null || true

# Idempotency table for reporting-service
aws --endpoint-url=$ENDPOINT dynamodb create-table \
    --table-name processed_events \
    --attribute-definitions \
        AttributeName=eventId,AttributeType=S \
    --key-schema \
        AttributeName=eventId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST 2>/dev/null || \
    echo "Table processed_events already exists"

# Enable TTL on reporting events table
aws --endpoint-url=$ENDPOINT dynamodb update-time-to-live \
    --table-name processed_events \
    --time-to-live-specification "Enabled=true,AttributeName=ttl" 2>/dev/null || true

echo "✓ DynamoDB tables created"

# ==========================================
# SES Email Identities
# ==========================================
echo "Verifying SES email identities..."

# Verify email identity for notification-service
aws --endpoint-url=$ENDPOINT ses verify-email-identity \
    --email-address notifications@turaf.com 2>/dev/null || \
    echo "Email identity notifications@turaf.com already verified"

echo "✓ SES email identities verified"

# ==========================================
# EventBridge Rules
# ==========================================
echo "Creating EventBridge rules..."

# Rule to route experiment events to SQS
aws --endpoint-url=$ENDPOINT events put-rule \
    --name turaf-experiment-events-rule \
    --event-bus-name turaf-events \
    --event-pattern '{"source":["turaf.experiment-service"]}' 2>/dev/null || true

# Add target to the rule
QUEUE_ARN=$(aws --endpoint-url=$ENDPOINT sqs get-queue-attributes \
    --queue-url $ENDPOINT/000000000000/turaf-experiment-events \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text 2>/dev/null)

if [ -n "$QUEUE_ARN" ]; then
    aws --endpoint-url=$ENDPOINT events put-targets \
        --rule turaf-experiment-events-rule \
        --event-bus-name turaf-events \
        --targets "Id=1,Arn=$QUEUE_ARN" 2>/dev/null || true
fi

echo "✓ EventBridge rules created"

# ==========================================
# Summary
# ==========================================
echo "=========================================="
echo "MiniStack initialization complete!"
echo ""
echo "Available resources:"
echo "  - S3 Buckets: turaf-reports-local, turaf-artifacts-local, turaf-frontend-local"
echo "  - EventBridge: turaf-events, turaf-event-bus"
echo "  - SQS Queues: turaf-experiment-events, turaf-metric-events, turaf-notification-events, turaf-dlq"
echo "  - SQS FIFO Queues: communications-direct-messages.fifo, communications-group-messages.fifo"
echo "  - SNS Topics: turaf-notifications, turaf-alerts"
echo "  - DynamoDB Tables: processed_notification_events, processed_events (with TTL enabled)"
echo "  - SES Identities: notifications@turaf.com"
echo "  - Secrets: Database user passwords"
echo ""
echo "Access MiniStack at: http://localhost:4566"
echo "=========================================="
