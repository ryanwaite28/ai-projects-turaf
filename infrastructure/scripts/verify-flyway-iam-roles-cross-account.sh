#!/bin/bash

set -e

# Flyway IAM Roles Verification Script (Cross-Account)
# Purpose: Verify that IAM roles are correctly configured for database migrations

ACCOUNTS=(
  "dev:801651112319"
  "qa:965932217544"
  "prod:811783768245"
)

GITHUB_REPO="ryanwaite28/ai-projects-turaf"
ROOT_PROFILE="turaf-root"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"

echo "=========================================="
echo "Flyway IAM Roles Verification (Cross-Account)"
echo "=========================================="
echo ""

TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  ACCOUNT_ID="${account##*:}"
  
  echo "=========================================="
  echo "Verifying ${ENV} account (${ACCOUNT_ID})"
  echo "=========================================="
  
  # Assume role in target account
  CREDENTIALS=$(aws sts assume-role \
    --profile $ROOT_PROFILE \
    --role-arn "arn:aws:iam::${ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "flyway-iam-verify-${ENV}" \
    --query 'Credentials' \
    --output json)
  
  export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
  export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
  export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
  
  # ============================================
  # 1. Check GitHubActionsFlywayRole exists
  # ============================================
  
  echo ""
  echo "1. Checking GitHubActionsFlywayRole..."
  echo "----------------------------------------"
  
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  if aws iam get-role --role-name GitHubActionsFlywayRole &>/dev/null; then
    echo "  ✅ Role exists"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    
    # Get role ARN
    GH_ROLE_ARN=$(aws iam get-role \
      --role-name GitHubActionsFlywayRole \
      --query 'Role.Arn' \
      --output text)
    echo "  ARN: ${GH_ROLE_ARN}"
    
    # Check trust policy
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    TRUST_POLICY=$(aws iam get-role \
      --role-name GitHubActionsFlywayRole \
      --query 'Role.AssumeRolePolicyDocument' \
      --output json)
    
    if echo "$TRUST_POLICY" | grep -q "token.actions.githubusercontent.com"; then
      echo "  ✅ Trust policy configured for GitHub OIDC"
      PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
      echo "  ❌ Trust policy missing GitHub OIDC configuration"
      FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    
    # Check permissions policy
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    if aws iam get-role-policy \
      --role-name GitHubActionsFlywayRole \
      --policy-name GitHubActionsFlywayPolicy &>/dev/null; then
      echo "  ✅ Permissions policy attached"
      PASSED_CHECKS=$((PASSED_CHECKS + 1))
      
      # Verify CodeBuild permissions
      TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
      POLICY=$(aws iam get-role-policy \
        --role-name GitHubActionsFlywayRole \
        --policy-name GitHubActionsFlywayPolicy \
        --query 'PolicyDocument' \
        --output json)
      
      if echo "$POLICY" | grep -q "codebuild:StartBuild"; then
        echo "  ✅ CodeBuild permissions configured"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
      else
        echo "  ❌ Missing CodeBuild permissions"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
      fi
    else
      echo "  ❌ Permissions policy not attached"
      FAILED_CHECKS=$((FAILED_CHECKS + 2))
      TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    fi
    
  else
    echo "  ❌ Role does not exist"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
  fi
  
  # ============================================
  # 2. Check CodeBuildFlywayRole exists
  # ============================================
  
  echo ""
  echo "2. Checking CodeBuildFlywayRole..."
  echo "----------------------------------------"
  
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  if aws iam get-role --role-name CodeBuildFlywayRole &>/dev/null; then
    echo "  ✅ Role exists"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    
    # Get role ARN
    CB_ROLE_ARN=$(aws iam get-role \
      --role-name CodeBuildFlywayRole \
      --query 'Role.Arn' \
      --output text)
    echo "  ARN: ${CB_ROLE_ARN}"
    
    # Check trust policy
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    TRUST_POLICY=$(aws iam get-role \
      --role-name CodeBuildFlywayRole \
      --query 'Role.AssumeRolePolicyDocument' \
      --output json)
    
    if echo "$TRUST_POLICY" | grep -q "codebuild.amazonaws.com"; then
      echo "  ✅ Trust policy configured for CodeBuild"
      PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
      echo "  ❌ Trust policy missing CodeBuild service principal"
      FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    
    # Check permissions policy
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    if aws iam get-role-policy \
      --role-name CodeBuildFlywayRole \
      --policy-name CodeBuildFlywayPolicy &>/dev/null; then
      echo "  ✅ Permissions policy attached"
      PASSED_CHECKS=$((PASSED_CHECKS + 1))
      
      POLICY=$(aws iam get-role-policy \
        --role-name CodeBuildFlywayRole \
        --policy-name CodeBuildFlywayPolicy \
        --query 'PolicyDocument' \
        --output json)
      
      # Verify Secrets Manager permissions
      TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
      if echo "$POLICY" | grep -q "secretsmanager:GetSecretValue"; then
        echo "  ✅ Secrets Manager permissions configured"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
      else
        echo "  ❌ Missing Secrets Manager permissions"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
      fi
      
      # Verify VPC permissions
      TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
      if echo "$POLICY" | grep -q "ec2:CreateNetworkInterface"; then
        echo "  ✅ VPC permissions configured"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
      else
        echo "  ❌ Missing VPC permissions"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
      fi
      
      # Verify CloudWatch Logs permissions
      TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
      if echo "$POLICY" | grep -q "logs:CreateLogGroup"; then
        echo "  ✅ CloudWatch Logs permissions configured"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
      else
        echo "  ❌ Missing CloudWatch Logs permissions"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
      fi
      
    else
      echo "  ❌ Permissions policy not attached"
      FAILED_CHECKS=$((FAILED_CHECKS + 4))
      TOTAL_CHECKS=$((TOTAL_CHECKS + 3))
    fi
    
  else
    echo "  ❌ Role does not exist"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
  fi
  
  echo ""
  echo "✅ ${ENV} verification complete"
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
echo "Verification Summary"
echo "=========================================="
echo ""
echo "Total checks: ${TOTAL_CHECKS}"
echo "Passed: ${PASSED_CHECKS}"
echo "Failed: ${FAILED_CHECKS}"
echo ""

if [ $FAILED_CHECKS -eq 0 ]; then
  echo "✅ All checks passed!"
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
      --role-session-name "flyway-iam-final-${ENV}" \
      --query 'Credentials' \
      --output json)
    
    export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
    export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
    export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
    
    GH_ROLE_ARN=$(aws iam get-role \
      --role-name GitHubActionsFlywayRole \
      --query 'Role.Arn' \
      --output text 2>/dev/null || echo "NOT FOUND")
    
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
  echo "1. Add role ARNs to GitHub environment secrets"
  echo "2. Configure database migration network access (Task 027)"
  echo "3. Create CodeBuild migration projects (Task 028)"
  echo "=========================================="
  
  exit 0
else
  echo "❌ Some checks failed!"
  echo ""
  echo "Please review the errors above and run setup-flyway-iam-roles-cross-account.sh to fix issues."
  echo "=========================================="
  
  exit 1
fi
