#!/bin/bash

set -e

# Verify CodeBuild Flyway Migration Project for Dev Environment
# Purpose: Verify CodeBuild project configuration and test execution

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"
REGION="us-east-1"
PROJECT_NAME="turaf-flyway-migrations-${ENV}"

echo "=========================================="
echo "Verify CodeBuild Flyway Migration Project"
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
  --role-session-name "verify-codebuild-flyway" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
export AWS_DEFAULT_REGION="$REGION"

echo "✅ Assumed role in dev account"
echo ""

# Verify project exists
echo "=========================================="
echo "1. Verifying Project Exists"
echo "=========================================="
echo ""

PROJECT_EXISTS=$(aws codebuild batch-get-projects \
  --names "$PROJECT_NAME" \
  --query 'projects[0].name' \
  --output text 2>/dev/null || echo "None")

if [ "$PROJECT_EXISTS" = "None" ]; then
    echo "❌ CodeBuild project '$PROJECT_NAME' does not exist"
    exit 1
fi

echo "✅ Project exists: $PROJECT_NAME"
echo ""

# Get project details
echo "=========================================="
echo "2. Project Configuration"
echo "=========================================="
echo ""

PROJECT_INFO=$(aws codebuild batch-get-projects \
  --names "$PROJECT_NAME" \
  --query 'projects[0]' \
  --output json)

PROJECT_ARN=$(echo $PROJECT_INFO | jq -r '.arn')
SERVICE_ROLE=$(echo $PROJECT_INFO | jq -r '.serviceRole')
VPC_ID=$(echo $PROJECT_INFO | jq -r '.vpcConfig.vpcId')
SUBNETS=$(echo $PROJECT_INFO | jq -r '.vpcConfig.subnets | join(", ")')
SECURITY_GROUPS=$(echo $PROJECT_INFO | jq -r '.vpcConfig.securityGroupIds | join(", ")')

echo "Project ARN: $PROJECT_ARN"
echo "Service Role: $SERVICE_ROLE"
echo "VPC ID: $VPC_ID"
echo "Subnets: $SUBNETS"
echo "Security Groups: $SECURITY_GROUPS"
echo ""

# Verify environment variables
echo "=========================================="
echo "3. Environment Variables"
echo "=========================================="
echo ""

ENV_VARS=$(echo $PROJECT_INFO | jq -r '.environment.environmentVariables[] | "\(.name)=\(.value) (\(.type))"')
echo "$ENV_VARS"
echo ""

# Verify VPC configuration
echo "=========================================="
echo "4. VPC Configuration Checks"
echo "=========================================="
echo ""

CHECKS_PASSED=0
CHECKS_FAILED=0

# Check VPC
EXPECTED_VPC="vpc-0eb73410956d368a8"
if [ "$VPC_ID" = "$EXPECTED_VPC" ]; then
    echo "✅ VPC configured correctly: $VPC_ID"
    ((CHECKS_PASSED++))
else
    echo "❌ VPC mismatch. Expected: $EXPECTED_VPC, Got: $VPC_ID"
    ((CHECKS_FAILED++))
fi

# Check security group
EXPECTED_SG="sg-01b1f0d32cf32bd22"
if [[ "$SECURITY_GROUPS" == *"$EXPECTED_SG"* ]]; then
    echo "✅ Security group configured correctly: $EXPECTED_SG"
    ((CHECKS_PASSED++))
else
    echo "❌ Security group mismatch. Expected: $EXPECTED_SG, Got: $SECURITY_GROUPS"
    ((CHECKS_FAILED++))
fi

# Check service role
EXPECTED_ROLE="arn:aws:iam::${DEV_ACCOUNT_ID}:role/CodeBuildFlywayRole"
if [ "$SERVICE_ROLE" = "$EXPECTED_ROLE" ]; then
    echo "✅ Service role configured correctly"
    ((CHECKS_PASSED++))
else
    echo "❌ Service role mismatch. Expected: $EXPECTED_ROLE, Got: $SERVICE_ROLE"
    ((CHECKS_FAILED++))
fi

# Check CloudWatch Logs
LOG_GROUP=$(echo $PROJECT_INFO | jq -r '.logsConfig.cloudWatchLogs.groupName')
EXPECTED_LOG_GROUP="/aws/codebuild/turaf-flyway-migrations-${ENV}"
if [ "$LOG_GROUP" = "$EXPECTED_LOG_GROUP" ]; then
    echo "✅ CloudWatch Logs configured correctly: $LOG_GROUP"
    ((CHECKS_PASSED++))
else
    echo "❌ Log group mismatch. Expected: $EXPECTED_LOG_GROUP, Got: $LOG_GROUP"
    ((CHECKS_FAILED++))
fi

echo ""

# Check recent builds
echo "=========================================="
echo "5. Recent Builds"
echo "=========================================="
echo ""

BUILD_IDS=$(aws codebuild list-builds-for-project \
  --project-name "$PROJECT_NAME" \
  --query 'ids[0:5]' \
  --output json 2>/dev/null || echo "[]")

BUILD_COUNT=$(echo $BUILD_IDS | jq -r 'length')

if [ "$BUILD_COUNT" -eq 0 ]; then
    echo "ℹ️  No builds have been executed yet"
else
    echo "Found $BUILD_COUNT recent build(s):"
    echo ""
    
    BUILDS=$(aws codebuild batch-get-builds \
      --ids $(echo $BUILD_IDS | jq -r '.[]') \
      --query 'builds[*].{ID:id,Status:buildStatus,StartTime:startTime,Duration:buildDuration}' \
      --output table)
    
    echo "$BUILDS"
fi

echo ""

# Summary
echo "=========================================="
echo "Verification Summary"
echo "=========================================="
echo ""
echo "Checks Passed: $CHECKS_PASSED"
echo "Checks Failed: $CHECKS_FAILED"
echo "Recent Builds: $BUILD_COUNT"
echo ""

if [ $CHECKS_FAILED -eq 0 ]; then
    echo "✅ All configuration checks passed!"
    echo ""
    echo "Project is ready for migration execution."
else
    echo "❌ Some checks failed. Please review the configuration above."
    exit 1
fi

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN

echo ""
echo "=========================================="
echo "Next Steps"
echo "=========================================="
echo ""
echo "To test migration execution, run:"
echo "  ./infrastructure/scripts/test-codebuild-flyway-dev.sh"
echo ""
