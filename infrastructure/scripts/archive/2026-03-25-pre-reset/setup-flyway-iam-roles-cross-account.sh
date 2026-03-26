#!/bin/bash

set -e

# Flyway IAM Roles Setup Script (Cross-Account)
# Purpose: Create IAM roles for centralized database migrations using cross-account access
# Uses: Root account to assume OrganizationAccountAccessRole in each target account

ACCOUNTS=(
  "dev:801651112319"
  "qa:965932217544"
  "prod:811783768245"
)

GITHUB_REPO="ryanwaite28/ai-projects-turaf"
ROOT_PROFILE="turaf-root"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"

echo "=========================================="
echo "Flyway IAM Roles Setup (Cross-Account)"
echo "=========================================="
echo "This script will create IAM roles for database migrations in all environments"
echo "Using root account to assume admin role in each target account"
echo ""

# Verify root profile is logged in
echo "Verifying root account access..."
aws sts get-caller-identity --profile $ROOT_PROFILE > /dev/null 2>&1 || {
  echo "❌ Root profile not authenticated. Please run:"
  echo "   aws sso login --profile $ROOT_PROFILE"
  exit 1
}
echo "✅ Root account authenticated"
echo ""

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  ACCOUNT_ID="${account##*:}"
  
  echo "=========================================="
  echo "Setting up IAM roles for ${ENV} account (${ACCOUNT_ID})"
  echo "=========================================="
  
  # Assume role in target account
  echo "Assuming OrganizationAccountAccessRole in account ${ACCOUNT_ID}..."
  
  CREDENTIALS=$(aws sts assume-role \
    --profile $ROOT_PROFILE \
    --role-arn "arn:aws:iam::${ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "flyway-iam-setup-${ENV}" \
    --query 'Credentials' \
    --output json)
  
  export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
  export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
  export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
  
  echo "✅ Assumed role in ${ENV} account"
  
  # ============================================
  # 1. Create GitHub Actions Flyway Role
  # ============================================
  
  echo ""
  echo "1. Creating GitHubActionsFlywayRole..."
  echo "----------------------------------------"
  
  cat > /tmp/github-actions-flyway-trust-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
      },
      "StringLike": {
        "token.actions.githubusercontent.com:sub": "repo:${GITHUB_REPO}:*"
      }
    }
  }]
}
EOF
  
  # Create role (ignore error if already exists)
  aws iam create-role \
    --role-name GitHubActionsFlywayRole \
    --assume-role-policy-document file:///tmp/github-actions-flyway-trust-${ENV}.json \
    --description "Role for GitHub Actions to trigger Flyway database migrations" \
    --tags Key=Environment,Value=${ENV} Key=Service,Value=flyway-service \
    2>/dev/null || echo "  ℹ️  GitHubActionsFlywayRole already exists, updating policies..."
  
  cat > /tmp/github-actions-flyway-permissions-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CodeBuildPermissions",
      "Effect": "Allow",
      "Action": [
        "codebuild:StartBuild",
        "codebuild:BatchGetBuilds"
      ],
      "Resource": "arn:aws:codebuild:us-east-1:${ACCOUNT_ID}:project/turaf-flyway-migrations-${ENV}"
    },
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": [
        "logs:GetLogEvents",
        "logs:FilterLogEvents"
      ],
      "Resource": "arn:aws:logs:us-east-1:${ACCOUNT_ID}:log-group:/aws/codebuild/turaf-flyway-migrations-${ENV}:*"
    }
  ]
}
EOF
  
  # Attach permissions policy
  aws iam put-role-policy \
    --role-name GitHubActionsFlywayRole \
    --policy-name GitHubActionsFlywayPolicy \
    --policy-document file:///tmp/github-actions-flyway-permissions-${ENV}.json
  
  echo "  ✅ GitHubActionsFlywayRole configured"
  
  # ============================================
  # 2. Create CodeBuild Flyway Role
  # ============================================
  
  echo ""
  echo "2. Creating CodeBuildFlywayRole..."
  echo "----------------------------------------"
  
  cat > /tmp/codebuild-flyway-trust-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Service": "codebuild.amazonaws.com"
    },
    "Action": "sts:AssumeRole"
  }]
}
EOF
  
  # Create role (ignore error if already exists)
  aws iam create-role \
    --role-name CodeBuildFlywayRole \
    --assume-role-policy-document file:///tmp/codebuild-flyway-trust-${ENV}.json \
    --description "Role for CodeBuild to execute Flyway database migrations" \
    --tags Key=Environment,Value=${ENV} Key=Service,Value=flyway-service \
    2>/dev/null || echo "  ℹ️  CodeBuildFlywayRole already exists, updating policies..."
  
  cat > /tmp/codebuild-flyway-permissions-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SecretsManagerAccess",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:turaf/db/master-*"
      ]
    },
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": [
        "arn:aws:logs:us-east-1:${ACCOUNT_ID}:log-group:/aws/codebuild/turaf-flyway-migrations-${ENV}",
        "arn:aws:logs:us-east-1:${ACCOUNT_ID}:log-group:/aws/codebuild/turaf-flyway-migrations-${ENV}:*"
      ]
    },
    {
      "Sid": "VPCAccess",
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DeleteNetworkInterface",
        "ec2:DescribeSubnets",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeDhcpOptions",
        "ec2:DescribeVpcs",
        "ec2:CreateNetworkInterfacePermission"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECRAccess",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    }
  ]
}
EOF
  
  # Attach permissions policy
  aws iam put-role-policy \
    --role-name CodeBuildFlywayRole \
    --policy-name CodeBuildFlywayPolicy \
    --policy-document file:///tmp/codebuild-flyway-permissions-${ENV}.json
  
  echo "  ✅ CodeBuildFlywayRole configured"
  
  # ============================================
  # 3. Get Role ARNs
  # ============================================
  
  echo ""
  echo "3. Retrieving role ARNs..."
  echo "----------------------------------------"
  
  GH_ROLE_ARN=$(aws iam get-role \
    --role-name GitHubActionsFlywayRole \
    --query 'Role.Arn' \
    --output text)
  
  CB_ROLE_ARN=$(aws iam get-role \
    --role-name CodeBuildFlywayRole \
    --query 'Role.Arn' \
    --output text)
  
  echo "  GitHubActionsFlywayRole: ${GH_ROLE_ARN}"
  echo "  CodeBuildFlywayRole: ${CB_ROLE_ARN}"
  
  echo ""
  echo "✅ ${ENV} environment complete"
  echo ""
  
  # Clear credentials for next iteration
  unset AWS_ACCESS_KEY_ID
  unset AWS_SECRET_ACCESS_KEY
  unset AWS_SESSION_TOKEN
done

# ============================================
# Summary
# ============================================

echo "=========================================="
echo "✅ All IAM roles configured successfully!"
echo "=========================================="
echo ""
echo "Role ARNs for GitHub Secrets:"
echo "----------------------------------------"

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  ACCOUNT_ID="${account##*:}"
  
  # Assume role again to get ARNs
  CREDENTIALS=$(aws sts assume-role \
    --profile $ROOT_PROFILE \
    --role-arn "arn:aws:iam::${ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "flyway-iam-verify-${ENV}" \
    --query 'Credentials' \
    --output json)
  
  export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
  export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
  export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
  
  GH_ROLE_ARN=$(aws iam get-role \
    --role-name GitHubActionsFlywayRole \
    --query 'Role.Arn' \
    --output text)
  
  ENV_UPPER=$(echo "$ENV" | tr '[:lower:]' '[:upper:]')
  echo ""
  echo "${ENV_UPPER} environment:"
  echo "  AWS_FLYWAY_ROLE_ARN=${GH_ROLE_ARN}"
  
  unset AWS_ACCESS_KEY_ID
  unset AWS_SECRET_ACCESS_KEY
  unset AWS_SESSION_TOKEN
done

echo ""
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Add role ARNs to GitHub environment secrets (Task 012)"
echo "2. Configure database migration network access (Task 027)"
echo "3. Create CodeBuild migration projects (Task 028)"
echo "=========================================="

# Cleanup temp files
rm -f /tmp/github-actions-flyway-trust-*.json
rm -f /tmp/github-actions-flyway-permissions-*.json
rm -f /tmp/codebuild-flyway-trust-*.json
rm -f /tmp/codebuild-flyway-permissions-*.json

echo ""
echo "✅ Setup complete!"
