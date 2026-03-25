#!/bin/bash

set -e

# Flyway Network Access Verification Script (Cross-Account)
# Purpose: Verify security group configuration for CodeBuild to access RDS

ACCOUNTS=(
  "dev:801651112319"
  "qa:965932217544"
  "prod:811783768245"
)

ROOT_PROFILE="turaf-root"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"

echo "=========================================="
echo "Flyway Network Access Verification (Cross-Account)"
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
    --role-session-name "flyway-network-verify-${ENV}" \
    --query 'Credentials' \
    --output json)
  
  export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
  export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
  export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
  
  # ============================================
  # 1. Check VPC exists
  # ============================================
  
  echo ""
  echo "1. Checking VPC..."
  echo "----------------------------------------"
  
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  VPC_ID=$(aws ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
    --query 'Vpcs[0].VpcId' \
    --output text 2>/dev/null)
  
  if [ "$VPC_ID" != "None" ] && [ -n "$VPC_ID" ]; then
    echo "  ✅ VPC exists: ${VPC_ID}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
  else
    echo "  ❌ VPC not found"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
    unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN
    continue
  fi
  
  # ============================================
  # 2. Check CodeBuild Security Group
  # ============================================
  
  echo ""
  echo "2. Checking CodeBuild security group..."
  echo "----------------------------------------"
  
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  SG_ID=$(aws ec2 describe-security-groups \
    --filters "Name=tag:Name,Values=turaf-codebuild-flyway-${ENV}" \
    --query 'SecurityGroups[0].GroupId' \
    --output text 2>/dev/null)
  
  if [ "$SG_ID" != "None" ] && [ -n "$SG_ID" ]; then
    echo "  ✅ Security group exists: ${SG_ID}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    
    # Check egress rules
    EGRESS_RULES=$(aws ec2 describe-security-groups \
      --group-ids $SG_ID \
      --query 'SecurityGroups[0].IpPermissionsEgress' \
      --output json)
    
    # Check PostgreSQL egress
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    if echo "$EGRESS_RULES" | grep -q "5432"; then
      echo "  ✅ PostgreSQL egress rule configured"
      PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
      echo "  ❌ PostgreSQL egress rule missing"
      FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    
    # Check HTTPS egress
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    if echo "$EGRESS_RULES" | grep -q "443"; then
      echo "  ✅ HTTPS egress rule configured"
      PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
      echo "  ❌ HTTPS egress rule missing"
      FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    
    # Check HTTP egress
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    if echo "$EGRESS_RULES" | grep -q "\"80\""; then
      echo "  ✅ HTTP egress rule configured"
      PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
      echo "  ❌ HTTP egress rule missing"
      FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    
  else
    echo "  ❌ Security group not found"
    FAILED_CHECKS=$((FAILED_CHECKS + 4))
    TOTAL_CHECKS=$((TOTAL_CHECKS + 3))
    unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN
    continue
  fi
  
  # ============================================
  # 3. Check RDS Security Group
  # ============================================
  
  echo ""
  echo "3. Checking RDS security group..."
  echo "----------------------------------------"
  
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  RDS_SG_ID=$(aws ec2 describe-security-groups \
    --filters "Name=tag:Name,Values=turaf-rds-${ENV}" \
    --query 'SecurityGroups[0].GroupId' \
    --output text 2>/dev/null)
  
  if [ "$RDS_SG_ID" != "None" ] && [ -n "$RDS_SG_ID" ]; then
    echo "  ✅ RDS security group exists: ${RDS_SG_ID}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    
    # Check ingress from CodeBuild
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    INGRESS_RULES=$(aws ec2 describe-security-groups \
      --group-ids $RDS_SG_ID \
      --query 'SecurityGroups[0].IpPermissions' \
      --output json)
    
    if echo "$INGRESS_RULES" | grep -q "$SG_ID"; then
      echo "  ✅ CodeBuild ingress rule configured"
      PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
      echo "  ❌ CodeBuild ingress rule missing"
      FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    
  else
    echo "  ❌ RDS security group not found"
    FAILED_CHECKS=$((FAILED_CHECKS + 2))
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  fi
  
  # ============================================
  # 4. Check Private Subnets
  # ============================================
  
  echo ""
  echo "4. Checking private subnets..."
  echo "----------------------------------------"
  
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  SUBNET_COUNT=$(aws ec2 describe-subnets \
    --filters "Name=tag:Name,Values=turaf-private-*-${ENV}" \
    --query 'length(Subnets)' \
    --output text 2>/dev/null)
  
  if [ "$SUBNET_COUNT" -ge 2 ]; then
    echo "  ✅ Private subnets found: ${SUBNET_COUNT}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
  else
    echo "  ❌ Insufficient private subnets (found: ${SUBNET_COUNT}, need: 2+)"
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
  echo "Network Configuration Summary:"
  echo "----------------------------------------"
  
  for account in "${ACCOUNTS[@]}"; do
    ENV="${account%%:*}"
    ACCOUNT_ID="${account##*:}"
    
    # Assume role again to get details
    CREDENTIALS=$(aws sts assume-role \
      --profile $ROOT_PROFILE \
      --role-arn "arn:aws:iam::${ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
      --role-session-name "flyway-network-summary-${ENV}" \
      --query 'Credentials' \
      --output json)
    
    export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
    export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
    export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
    
    VPC_ID=$(aws ec2 describe-vpcs \
      --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
      --query 'Vpcs[0].VpcId' \
      --output text 2>/dev/null)
    
    SG_ID=$(aws ec2 describe-security-groups \
      --filters "Name=tag:Name,Values=turaf-codebuild-flyway-${ENV}" \
      --query 'SecurityGroups[0].GroupId' \
      --output text 2>/dev/null)
    
    RDS_SG_ID=$(aws ec2 describe-security-groups \
      --filters "Name=tag:Name,Values=turaf-rds-${ENV}" \
      --query 'SecurityGroups[0].GroupId' \
      --output text 2>/dev/null)
    
    SUBNET_IDS=$(aws ec2 describe-subnets \
      --filters "Name=tag:Name,Values=turaf-private-*-${ENV}" \
      --query 'Subnets[*].SubnetId' \
      --output text 2>/dev/null | tr '\t' ',')
    
    ENV_UPPER=$(echo "$ENV" | tr '[:lower:]' '[:upper:]')
    echo ""
    echo "${ENV_UPPER} environment:"
    echo "  VPC ID: ${VPC_ID}"
    echo "  CodeBuild SG: ${SG_ID}"
    echo "  RDS SG: ${RDS_SG_ID}"
    echo "  Private Subnets: ${SUBNET_IDS}"
    
    unset AWS_ACCESS_KEY_ID
    unset AWS_SECRET_ACCESS_KEY
    unset AWS_SESSION_TOKEN
  done
  
  echo ""
  echo "=========================================="
  echo "Next Steps:"
  echo "=========================================="
  echo "1. Create CodeBuild migration projects (Task 028)"
  echo "2. Use security group IDs in CodeBuild VPC configuration"
  echo "3. Use private subnet IDs for CodeBuild placement"
  echo "=========================================="
  
  exit 0
else
  echo "❌ Some checks failed!"
  echo ""
  echo "Please review the errors above and run setup-flyway-network-cross-account.sh to fix issues."
  echo "=========================================="
  
  exit 1
fi
