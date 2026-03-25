#!/bin/bash

set -e

# Create CodeBuild Flyway Migration Project for Dev Environment
# Purpose: Create AWS CodeBuild project for executing Flyway database migrations

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"
REGION="us-east-1"

echo "=========================================="
echo "Create CodeBuild Flyway Migration Project"
echo "Environment: ${ENV}"
echo "=========================================="
echo ""

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "❌ jq is not installed"
    echo "   Install: brew install jq"
    exit 1
fi

echo "✅ jq installed"
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
  --role-session-name "create-codebuild-flyway" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
export AWS_DEFAULT_REGION="$REGION"

echo "✅ Assumed role in dev account"
echo ""

# Get infrastructure details from deployed standalone infrastructure
echo "=========================================="
echo "Getting Infrastructure Details"
echo "=========================================="
echo ""

VPC_ID="vpc-0eb73410956d368a8"
SUBNET_1="subnet-0fbca1c0741c511bc"
SUBNET_2="subnet-0a7e1733037f31e69"
CODEBUILD_SG_ID="sg-01b1f0d32cf32bd22"
RDS_ENDPOINT="turaf-postgres-dev.cm7cimwey834.us-east-1.rds.amazonaws.com"
DB_SECRET_ARN="arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/rds/admin-20260324134423738900000001-Wtw0q2"

echo "VPC ID: $VPC_ID"
echo "Private Subnet 1: $SUBNET_1"
echo "Private Subnet 2: $SUBNET_2"
echo "CodeBuild Security Group: $CODEBUILD_SG_ID"
echo "RDS Endpoint: $RDS_ENDPOINT"
echo "DB Secret ARN: $DB_SECRET_ARN"
echo ""

# Check if project already exists
echo "Checking if CodeBuild project already exists..."
PROJECT_EXISTS=$(aws codebuild batch-get-projects \
  --names "turaf-flyway-migrations-${ENV}" \
  --query 'projects[0].name' \
  --output text 2>/dev/null || echo "None")

if [ "$PROJECT_EXISTS" != "None" ]; then
    echo "⚠️  CodeBuild project 'turaf-flyway-migrations-${ENV}' already exists"
    echo ""
    read -p "Do you want to delete and recreate it? (yes/no): " RECREATE
    
    if [ "$RECREATE" = "yes" ]; then
        echo "Deleting existing project..."
        aws codebuild delete-project --name "turaf-flyway-migrations-${ENV}"
        echo "✅ Deleted existing project"
        echo ""
    else
        echo "❌ Cancelled. Exiting without changes."
        exit 0
    fi
fi

# Create CodeBuild project
echo "=========================================="
echo "Creating CodeBuild Project"
echo "=========================================="
echo ""

# Create project configuration
cat > /tmp/codebuild-flyway-${ENV}.json <<EOF
{
  "name": "turaf-flyway-migrations-${ENV}",
  "description": "Flyway database migrations for ${ENV} environment",
  "source": {
    "type": "GITHUB",
    "location": "https://github.com/ryanwaite28/ai-projects-turaf.git",
    "buildspec": "services/flyway-service/buildspec.yml",
    "gitCloneDepth": 1,
    "reportBuildStatus": false
  },
  "artifacts": {
    "type": "NO_ARTIFACTS"
  },
  "environment": {
    "type": "LINUX_CONTAINER",
    "image": "aws/codebuild/standard:7.0",
    "computeType": "BUILD_GENERAL1_SMALL",
    "environmentVariables": [
      {
        "name": "DB_HOST",
        "value": "${RDS_ENDPOINT}",
        "type": "PLAINTEXT"
      },
      {
        "name": "DB_PORT",
        "value": "5432",
        "type": "PLAINTEXT"
      },
      {
        "name": "DB_NAME",
        "value": "turaf",
        "type": "PLAINTEXT"
      },
      {
        "name": "DB_USER",
        "value": "turaf_admin",
        "type": "PLAINTEXT"
      },
      {
        "name": "DB_PASSWORD",
        "value": "${DB_SECRET_ARN}:password",
        "type": "SECRETS_MANAGER"
      },
      {
        "name": "ENVIRONMENT",
        "value": "${ENV}",
        "type": "PLAINTEXT"
      }
    ]
  },
  "serviceRole": "arn:aws:iam::${DEV_ACCOUNT_ID}:role/CodeBuildFlywayRole",
  "timeoutInMinutes": 15,
  "queuedTimeoutInMinutes": 30,
  "vpcConfig": {
    "vpcId": "${VPC_ID}",
    "subnets": ["${SUBNET_1}", "${SUBNET_2}"],
    "securityGroupIds": ["${CODEBUILD_SG_ID}"]
  },
  "logsConfig": {
    "cloudWatchLogs": {
      "status": "ENABLED",
      "groupName": "/aws/codebuild/turaf-flyway-migrations-${ENV}"
    }
  },
  "tags": [
    {
      "key": "Environment",
      "value": "${ENV}"
    },
    {
      "key": "Service",
      "value": "flyway-service"
    },
    {
      "key": "ManagedBy",
      "value": "script"
    },
    {
      "key": "Purpose",
      "value": "database-migrations"
    }
  ]
}
EOF

echo "Creating CodeBuild project: turaf-flyway-migrations-${ENV}"
echo ""

aws codebuild create-project --cli-input-json file:///tmp/codebuild-flyway-${ENV}.json

echo ""
echo "✅ CodeBuild project created successfully!"
echo ""

# Get project details
echo "=========================================="
echo "Project Details"
echo "=========================================="
echo ""

PROJECT_ARN=$(aws codebuild batch-get-projects \
  --names "turaf-flyway-migrations-${ENV}" \
  --query 'projects[0].arn' \
  --output text)

echo "Project Name: turaf-flyway-migrations-${ENV}"
echo "Project ARN: $PROJECT_ARN"
echo "Service Role: arn:aws:iam::${DEV_ACCOUNT_ID}:role/CodeBuildFlywayRole"
echo "VPC: $VPC_ID"
echo "Subnets: $SUBNET_1, $SUBNET_2"
echo "Security Group: $CODEBUILD_SG_ID"
echo "Log Group: /aws/codebuild/turaf-flyway-migrations-${ENV}"
echo ""

# Cleanup
rm -f /tmp/codebuild-flyway-${ENV}.json

echo "=========================================="
echo "Next Steps"
echo "=========================================="
echo ""
echo "1. Test the migration:"
echo "   aws codebuild start-build --project-name turaf-flyway-migrations-${ENV}"
echo ""
echo "2. Monitor build status:"
echo "   aws codebuild batch-get-builds --ids <build-id>"
echo ""
echo "3. View logs:"
echo "   aws logs tail /aws/codebuild/turaf-flyway-migrations-${ENV} --follow"
echo ""
echo "4. Trigger from GitHub Actions:"
echo "   Use GitHubActionsFlywayRole to start builds"
echo ""

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN

echo "✅ Script complete!"
