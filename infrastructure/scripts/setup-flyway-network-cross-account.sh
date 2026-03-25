#!/bin/bash

set -e

# Flyway Network Access Setup Script (Cross-Account)
# Purpose: Configure security groups for CodeBuild to access RDS
# Uses: Root account to assume OrganizationAccountAccessRole in each target account

ACCOUNTS=(
  "dev:801651112319"
  "qa:965932217544"
  "prod:811783768245"
)

ROOT_PROFILE="turaf-root"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"

echo "=========================================="
echo "Flyway Network Access Setup (Cross-Account)"
echo "=========================================="
echo "This script will configure security groups for CodeBuild to access RDS"
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
  echo "Configuring network for ${ENV} account (${ACCOUNT_ID})"
  echo "=========================================="
  
  # Assume role in target account
  echo "Assuming OrganizationAccountAccessRole in account ${ACCOUNT_ID}..."
  
  CREDENTIALS=$(aws sts assume-role \
    --profile $ROOT_PROFILE \
    --role-arn "arn:aws:iam::${ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "flyway-network-setup-${ENV}" \
    --query 'Credentials' \
    --output json)
  
  export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
  export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
  export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
  
  echo "✅ Assumed role in ${ENV} account"
  
  # ============================================
  # 1. Get VPC ID
  # ============================================
  
  echo ""
  echo "1. Getting VPC information..."
  echo "----------------------------------------"
  
  VPC_ID=$(aws ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
    --query 'Vpcs[0].VpcId' \
    --output text 2>/dev/null)
  
  if [ "$VPC_ID" == "None" ] || [ -z "$VPC_ID" ]; then
    echo "❌ VPC not found for ${ENV} environment"
    echo "   Expected tag: Name=turaf-vpc-${ENV}"
    echo "   Skipping ${ENV}..."
    unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN
    continue
  fi
  
  echo "  VPC ID: ${VPC_ID}"
  
  # ============================================
  # 2. Create CodeBuild Security Group
  # ============================================
  
  echo ""
  echo "2. Creating CodeBuild security group..."
  echo "----------------------------------------"
  
  # Check if security group already exists
  EXISTING_SG=$(aws ec2 describe-security-groups \
    --filters "Name=tag:Name,Values=turaf-codebuild-flyway-${ENV}" \
    --query 'SecurityGroups[0].GroupId' \
    --output text 2>/dev/null)
  
  if [ "$EXISTING_SG" != "None" ] && [ -n "$EXISTING_SG" ]; then
    echo "  ℹ️  Security group already exists: ${EXISTING_SG}"
    SG_ID=$EXISTING_SG
  else
    SG_ID=$(aws ec2 create-security-group \
      --group-name turaf-codebuild-flyway-${ENV} \
      --description "Security group for CodeBuild Flyway migrations in ${ENV}" \
      --vpc-id $VPC_ID \
      --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=turaf-codebuild-flyway-${ENV}},{Key=Environment,Value=${ENV}},{Key=Service,Value=flyway-service}]" \
      --query 'GroupId' \
      --output text)
    
    echo "  ✅ Created security group: ${SG_ID}"
  fi
  
  # ============================================
  # 3. Get RDS Security Group
  # ============================================
  
  echo ""
  echo "3. Getting RDS security group..."
  echo "----------------------------------------"
  
  RDS_SG_ID=$(aws ec2 describe-security-groups \
    --filters "Name=tag:Name,Values=turaf-rds-${ENV}" \
    --query 'SecurityGroups[0].GroupId' \
    --output text 2>/dev/null)
  
  if [ "$RDS_SG_ID" == "None" ] || [ -z "$RDS_SG_ID" ]; then
    echo "❌ RDS security group not found for ${ENV} environment"
    echo "   Expected tag: Name=turaf-rds-${ENV}"
    echo "   Skipping ${ENV}..."
    unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN
    continue
  fi
  
  echo "  RDS Security Group ID: ${RDS_SG_ID}"
  
  # ============================================
  # 4. Configure CodeBuild Security Group Rules
  # ============================================
  
  echo ""
  echo "4. Configuring CodeBuild security group rules..."
  echo "----------------------------------------"
  
  # Remove default egress rule (allow all) - ignore error if already removed
  aws ec2 revoke-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=-1,IpRanges='[{CidrIp=0.0.0.0/0}]' \
    2>/dev/null && echo "  ✅ Removed default egress rule" || echo "  ℹ️  Default egress rule already removed"
  
  # Add egress rule for PostgreSQL to RDS
  aws ec2 authorize-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs="[{GroupId=${RDS_SG_ID},Description='Allow PostgreSQL to RDS'}]" \
    2>/dev/null && echo "  ✅ Added PostgreSQL egress to RDS" || echo "  ℹ️  PostgreSQL egress rule already exists"
  
  # Add egress rule for HTTPS (package downloads, GitHub)
  aws ec2 authorize-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges="[{CidrIp=0.0.0.0/0,Description='Allow HTTPS for package downloads'}]" \
    2>/dev/null && echo "  ✅ Added HTTPS egress" || echo "  ℹ️  HTTPS egress rule already exists"
  
  # Add egress rule for HTTP (package downloads)
  aws ec2 authorize-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges="[{CidrIp=0.0.0.0/0,Description='Allow HTTP for package downloads'}]" \
    2>/dev/null && echo "  ✅ Added HTTP egress" || echo "  ℹ️  HTTP egress rule already exists"
  
  # ============================================
  # 5. Update RDS Security Group
  # ============================================
  
  echo ""
  echo "5. Updating RDS security group..."
  echo "----------------------------------------"
  
  # Add ingress rule to RDS security group
  aws ec2 authorize-security-group-ingress \
    --group-id $RDS_SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs="[{GroupId=${SG_ID},Description='Allow PostgreSQL from CodeBuild Flyway'}]" \
    2>/dev/null && echo "  ✅ Added CodeBuild ingress to RDS" || echo "  ℹ️  CodeBuild ingress rule already exists"
  
  echo ""
  echo "✅ ${ENV} environment complete"
  echo "   CodeBuild SG: ${SG_ID}"
  echo "   RDS SG: ${RDS_SG_ID}"
  echo "   VPC: ${VPC_ID}"
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
echo "✅ All environments configured successfully!"
echo "=========================================="
echo ""
echo "Security Group IDs for CodeBuild Projects:"
echo "----------------------------------------"

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  ACCOUNT_ID="${account##*:}"
  
  # Assume role again to get IDs
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
  
  # Get private subnet IDs
  SUBNET_IDS=$(aws ec2 describe-subnets \
    --filters "Name=tag:Name,Values=turaf-private-*-${ENV}" \
    --query 'Subnets[*].SubnetId' \
    --output text 2>/dev/null | tr '\t' ',')
  
  ENV_UPPER=$(echo "$ENV" | tr '[:lower:]' '[:upper:]')
  echo ""
  echo "${ENV_UPPER} environment:"
  echo "  VPC ID: ${VPC_ID}"
  echo "  Security Group ID: ${SG_ID}"
  echo "  Private Subnet IDs: ${SUBNET_IDS}"
  
  unset AWS_ACCESS_KEY_ID
  unset AWS_SECRET_ACCESS_KEY
  unset AWS_SESSION_TOKEN
done

echo ""
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Use these security group IDs in CodeBuild project creation (Task 028)"
echo "2. Use private subnet IDs for CodeBuild VPC configuration"
echo "3. Test network connectivity with CodeBuild test project"
echo "=========================================="

echo ""
echo "✅ Setup complete!"
