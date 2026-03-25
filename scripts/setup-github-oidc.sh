#!/bin/bash

set -e

ACCOUNTS=(
  "ops:turaf-ops:146072879609"
  "dev:turaf-dev:801651112319"
  "qa:turaf-qa:965932217544"
  "prod:turaf-prod:811783768245"
)

GITHUB_REPO="ryanwaite28/ai-projects-turaf"
THUMBPRINT="6938fd4d98bab03faadb97b34396831e3780aea1"

echo "Setting up GitHub OIDC for all accounts..."
echo ""

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  PROFILE=$(echo "$account" | cut -d: -f2)
  ACCOUNT_ID=$(echo "$account" | cut -d: -f3)
  
  echo "=== Setting up ${ENV} account (${ACCOUNT_ID}) ==="
  
  # Create OIDC provider
  echo "Creating OIDC provider..."
  aws iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list ${THUMBPRINT} \
    --profile ${PROFILE} 2>/dev/null || echo "  OIDC provider already exists"
  
  # Create trust policy
  cat > /tmp/trust-policy-${ENV}.json <<EOF
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
  
  # Create permissions policy
  cat > /tmp/permissions-policy-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ECRPermissions",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECSPermissions",
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices",
        "ecs:DescribeTaskDefinition",
        "ecs:RegisterTaskDefinition",
        "ecs:ListTasks",
        "ecs:DescribeTasks"
      ],
      "Resource": "*"
    },
    {
      "Sid": "IAMPassRole",
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": [
        "arn:aws:iam::${ACCOUNT_ID}:role/ecsTaskExecutionRole",
        "arn:aws:iam::${ACCOUNT_ID}:role/ecsTaskRole"
      ]
    },
    {
      "Sid": "S3Permissions",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::turaf-artifacts-*",
        "arn:aws:s3:::turaf-artifacts-*/*"
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
      "Resource": "arn:aws:logs:*:*:log-group:/aws/ecs/*"
    }
  ]
}
EOF
  
  # Create IAM role
  echo "Creating IAM role..."
  aws iam create-role \
    --role-name GitHubActionsDeploymentRole \
    --assume-role-policy-document file:///tmp/trust-policy-${ENV}.json \
    --description "Role for GitHub Actions to deploy to ${ENV} account" \
    --profile ${PROFILE} 2>/dev/null || echo "  Role already exists"
  
  # Attach inline policy
  echo "Attaching permissions policy..."
  aws iam put-role-policy \
    --role-name GitHubActionsDeploymentRole \
    --policy-name GitHubActionsDeploymentPolicy \
    --policy-document file:///tmp/permissions-policy-${ENV}.json \
    --profile ${PROFILE}
  
  # Get role ARN
  ROLE_ARN=$(aws iam get-role \
    --role-name GitHubActionsDeploymentRole \
    --profile ${PROFILE} \
    --query 'Role.Arn' \
    --output text)
  
  echo "✅ ${ENV}: ${ROLE_ARN}"
  echo ""
done

echo "All GitHub OIDC configurations complete!"
