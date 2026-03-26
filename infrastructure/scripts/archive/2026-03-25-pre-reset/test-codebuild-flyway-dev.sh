#!/bin/bash

set -e

# Test CodeBuild Flyway Migration Execution for Dev Environment
# Purpose: Start a test build and monitor its execution

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"
REGION="us-east-1"
PROJECT_NAME="turaf-flyway-migrations-${ENV}"

echo "=========================================="
echo "Test CodeBuild Flyway Migration Execution"
echo "Environment: ${ENV}"
echo "=========================================="
echo ""

# Check AWS credentials
echo "Checking AWS credentials..."
if ! aws sts get-caller-identity --profile $ROOT_PROFILE &> /dev/null; then
    echo "❌ AWS credentials not configured for $ROOT_PROFILE profile"
    echo "   Run: aws sso login --profile $ROOT_PROFILE"
    exit 1
fi

echo "✅ AWS credentials configured"
echo ""

# Assume role in dev account
echo "Assuming role in dev account..."
CREDENTIALS=$(aws sts assume-role \
  --profile $ROOT_PROFILE \
  --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
  --role-session-name "test-codebuild-flyway" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
export AWS_DEFAULT_REGION="$REGION"

echo "✅ Assumed role in dev account"
echo ""

# Start build
echo "=========================================="
echo "Starting Build"
echo "=========================================="
echo ""

BUILD_RESPONSE=$(aws codebuild start-build \
  --project-name "$PROJECT_NAME" \
  --output json)

BUILD_ID=$(echo $BUILD_RESPONSE | jq -r '.build.id')
BUILD_NUMBER=$(echo $BUILD_RESPONSE | jq -r '.build.buildNumber')

echo "✅ Build started successfully"
echo "Build ID: $BUILD_ID"
echo "Build Number: $BUILD_NUMBER"
echo ""

# Monitor build progress
echo "=========================================="
echo "Monitoring Build Progress"
echo "=========================================="
echo ""
echo "Waiting for build to complete..."
echo "(This may take a few minutes)"
echo ""

# Wait for build to complete (with timeout)
TIMEOUT=600  # 10 minutes
ELAPSED=0
INTERVAL=10

while [ $ELAPSED -lt $TIMEOUT ]; do
    BUILD_STATUS=$(aws codebuild batch-get-builds \
      --ids "$BUILD_ID" \
      --query 'builds[0].buildStatus' \
      --output text)
    
    if [ "$BUILD_STATUS" = "SUCCEEDED" ] || [ "$BUILD_STATUS" = "FAILED" ] || \
       [ "$BUILD_STATUS" = "STOPPED" ] || [ "$BUILD_STATUS" = "FAULT" ]; then
        break
    fi
    
    echo "Status: $BUILD_STATUS (${ELAPSED}s elapsed)"
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
done

echo ""

# Get final build details
echo "=========================================="
echo "Build Results"
echo "=========================================="
echo ""

BUILD_INFO=$(aws codebuild batch-get-builds \
  --ids "$BUILD_ID" \
  --query 'builds[0]' \
  --output json)

FINAL_STATUS=$(echo $BUILD_INFO | jq -r '.buildStatus')
START_TIME=$(echo $BUILD_INFO | jq -r '.startTime')
END_TIME=$(echo $BUILD_INFO | jq -r '.endTime')
DURATION=$(echo $BUILD_INFO | jq -r '.buildDuration // 0')

echo "Build ID: $BUILD_ID"
echo "Status: $FINAL_STATUS"
echo "Start Time: $START_TIME"
echo "End Time: $END_TIME"
echo "Duration: ${DURATION} minutes"
echo ""

# Show build phases
echo "Build Phases:"
echo $BUILD_INFO | jq -r '.phases[] | "  \(.phaseType): \(.phaseStatus) (\(.durationInSeconds // 0)s)"'
echo ""

# Get CloudWatch logs
echo "=========================================="
echo "CloudWatch Logs (Last 50 lines)"
echo "=========================================="
echo ""

LOG_GROUP="/aws/codebuild/turaf-flyway-migrations-${ENV}"
LOG_STREAM=$(echo $BUILD_INFO | jq -r '.logs.streamName')

if [ "$LOG_STREAM" != "null" ] && [ -n "$LOG_STREAM" ]; then
    aws logs get-log-events \
      --log-group-name "$LOG_GROUP" \
      --log-stream-name "$LOG_STREAM" \
      --limit 50 \
      --query 'events[*].message' \
      --output text || echo "⚠️  Could not retrieve logs"
else
    echo "⚠️  Log stream not available yet"
fi

echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""

if [ "$FINAL_STATUS" = "SUCCEEDED" ]; then
    echo "✅ Migration test PASSED"
    echo ""
    echo "The CodeBuild project is working correctly and can execute migrations."
    EXIT_CODE=0
elif [ "$FINAL_STATUS" = "FAILED" ]; then
    echo "❌ Migration test FAILED"
    echo ""
    echo "Review the logs above for error details."
    echo ""
    echo "Common issues:"
    echo "  - Database connection problems (check security groups)"
    echo "  - Secrets Manager access denied (check IAM role permissions)"
    echo "  - Migration SQL errors (check migration files)"
    EXIT_CODE=1
elif [ "$FINAL_STATUS" = "IN_PROGRESS" ]; then
    echo "⏳ Build still in progress (timeout reached)"
    echo ""
    echo "Check build status manually:"
    echo "  aws codebuild batch-get-builds --ids $BUILD_ID"
    EXIT_CODE=2
else
    echo "⚠️  Build ended with status: $FINAL_STATUS"
    EXIT_CODE=3
fi

echo ""
echo "View full logs in AWS Console:"
echo "https://console.aws.amazon.com/codesuite/codebuild/projects/turaf-flyway-migrations-${ENV}/build/${BUILD_ID}/"
echo ""

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN

exit $EXIT_CODE
