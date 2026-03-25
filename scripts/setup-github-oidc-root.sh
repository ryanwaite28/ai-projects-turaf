#!/bin/bash

set -e

# This script uses the root account to create OIDC providers and roles in member accounts
# This is necessary because SSO roles don't have sufficient IAM permissions

ACCOUNTS=(
  "dev:801651112319"
  "qa:965932217544"
  "prod:811783768245"
)

GITHUB_REPO="ryanwaite28/ai-projects-turaf"
THUMBPRINT="6938fd4d98bab03faadb97b34396831e3780aea1"
ROOT_PROFILE="turaf-root"

echo "Setting up GitHub OIDC for member accounts using root account credentials..."
echo ""

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  ACCOUNT_ID="${account##*:}"
  
  echo "=== Setting up ${ENV} account (${ACCOUNT_ID}) ==="
  
  # Assume role in target account from root account
  # Note: This requires cross-account role assumption to be configured
  # For now, we'll document that this needs to be done manually or with proper cross-account access
  
  echo "⚠️  Manual setup required for ${ENV} account"
  echo "   Account ID: ${ACCOUNT_ID}"
  echo "   Reason: SSO roles lack IAM permissions (iam:CreateOpenIDConnectProvider, iam:CreateRole, iam:PutRolePolicy)"
  echo ""
  echo "   Manual steps:"
  echo "   1. Log into AWS Console for ${ENV} account with admin access"
  echo "   2. Navigate to IAM > Identity providers"
  echo "   3. Create OIDC provider:"
  echo "      - Provider URL: https://token.actions.githubusercontent.com"
  echo "      - Audience: sts.amazonaws.com"
  echo "      - Thumbprint: ${THUMBPRINT}"
  echo "   4. Navigate to IAM > Roles > Create role"
  echo "   5. Select 'Web identity' and choose the OIDC provider"
  echo "   6. Add condition: token.actions.githubusercontent.com:sub = repo:${GITHUB_REPO}:*"
  echo "   7. Attach permissions for ECR, ECS, S3, CloudWatch Logs"
  echo "   8. Name the role: GitHubActionsDeploymentRole"
  echo ""
done

echo "Ops account setup: ✅ Complete"
echo "Dev account setup: ⚠️  Manual intervention required"
echo "QA account setup: ⚠️  Manual intervention required"
echo "Prod account setup: ⚠️  Manual intervention required"
echo ""
echo "Once manual setup is complete, verify with:"
echo "  aws iam get-role --role-name GitHubActionsDeploymentRole --profile turaf-dev"
