#!/bin/bash

set -e

# Flyway IAM Roles Setup Script
# Purpose: Create IAM roles for centralized database migrations
# Creates: GitHubActionsFlywayRole and CodeBuildFlywayRole in each environment

ACCOUNTS=(
  "dev:turaf-dev:801651112319"
  "qa:turaf-qa:965932217544"
  "prod:turaf-prod:811783768245"
)

GITHUB_REPO="ryanwaite28/ai-projects-turaf"

echo "=========================================="
echo "Flyway IAM Roles Setup"
echo "=========================================="
echo "This script will create IAM roles for database migrations in all environments"
echo ""

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  PROFILE=$(echo "$account" | cut -d: -f2)
  ACCOUNT_ID=$(echo "$account" | cut -d: -f3)
  
  echo "=========================================="
  echo "Setting up IAM roles for ${ENV} account (${ACCOUNT_ID})"
  echo "=========================================="
  
  export AWS_PROFILE=$PROFILE
  
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
  PROFILE=$(echo "$account" | cut -d: -f2)
  ACCOUNT_ID=$(echo "$account" | cut -d: -f3)
  
  export AWS_PROFILE=$PROFILE
  
  GH_ROLE_ARN=$(aws iam get-role \
    --role-name GitHubActionsFlywayRole \
    --query 'Role.Arn' \
    --output text)
  
  ENV_UPPER=$(echo "$ENV" | tr '[:lower:]' '[:upper:]')
  echo ""
  echo "${ENV_UPPER} environment:"
  echo "  AWS_FLYWAY_ROLE_ARN=${GH_ROLE_ARN}"
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
