#!/bin/bash

set -e

# Deploy Standalone VPC + RDS Infrastructure
# Purpose: Minimal infrastructure for Task 027 (no modules, no complexity)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform/standalone/dev-vpc-rds"

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"

echo "=========================================="
echo "Turaf Standalone Infrastructure Deployment"
echo "VPC + RDS PostgreSQL (No Modules)"
echo "=========================================="
echo ""

# Check prerequisites
if ! command -v terraform &> /dev/null; then
    echo "❌ Terraform is not installed"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo "❌ jq is not installed"
    exit 1
fi

echo "✅ Prerequisites installed"
echo ""

# Check AWS credentials
echo "Checking AWS credentials..."
if ! aws sts get-caller-identity --profile $ROOT_PROFILE &> /dev/null; then
    echo "❌ AWS credentials not configured for $ROOT_PROFILE profile"
    echo "   Run: aws sso login --profile $ROOT_PROFILE"
    exit 1
fi

ROOT_ACCOUNT_ID=$(aws sts get-caller-identity --profile $ROOT_PROFILE --query Account --output text)
echo "✅ AWS credentials configured"
echo "   Root Account: $ROOT_ACCOUNT_ID"
echo ""

# Assume role in dev account
echo "Assuming role in dev account..."
CREDENTIALS=$(aws sts assume-role \
  --profile $ROOT_PROFILE \
  --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
  --role-session-name "terraform-deploy-standalone" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')

echo "✅ Assumed role in dev account ($DEV_ACCOUNT_ID)"
echo ""

# Navigate to terraform directory
cd "$TERRAFORM_DIR"

echo "=========================================="
echo "Step 1: Terraform Init"
echo "=========================================="
echo ""

terraform init

echo ""
echo "=========================================="
echo "Step 2: Terraform Validate"
echo "=========================================="
echo ""

terraform validate

echo ""
echo "=========================================="
echo "Step 3: Terraform Plan"
echo "=========================================="
echo ""

terraform plan -out=tfplan

echo ""
echo "=========================================="
echo "Review Plan"
echo "=========================================="
echo ""
echo "This will create:"
echo "  ✅ VPC (10.0.0.0/16)"
echo "  ✅ 2 Public Subnets"
echo "  ✅ 2 Private Subnets"
echo "  ✅ 2 Database Subnets"
echo "  ✅ NAT Gateway + Internet Gateway"
echo "  ✅ RDS PostgreSQL (db.t3.micro, 20GB)"
echo "  ✅ Security Groups (RDS, CodeBuild)"
echo "  ✅ KMS Key for encryption"
echo "  ✅ Secrets Manager for DB credentials"
echo ""
echo "💰 Estimated cost: ~$15/month"
echo "   - RDS: ~$12/month (Free Tier eligible)"
echo "   - NAT Gateway: ~$3/month"
echo ""

read -p "Apply this plan? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "❌ Deployment cancelled"
    rm -f tfplan
    exit 0
fi

echo ""
echo "=========================================="
echo "Step 4: Terraform Apply"
echo "=========================================="
echo ""

terraform apply tfplan

rm -f tfplan

echo ""
echo "=========================================="
echo "✅ Infrastructure Deployed Successfully!"
echo "=========================================="
echo ""

# Get outputs
VPC_ID=$(terraform output -raw vpc_id 2>/dev/null || echo "N/A")
RDS_ENDPOINT=$(terraform output -raw rds_endpoint 2>/dev/null || echo "N/A")
RDS_ADDRESS=$(terraform output -raw rds_address 2>/dev/null || echo "N/A")
RDS_SG=$(terraform output -raw rds_security_group_id 2>/dev/null || echo "N/A")
CODEBUILD_SG=$(terraform output -raw codebuild_security_group_id 2>/dev/null || echo "N/A")
SECRET_ARN=$(terraform output -raw rds_master_secret_arn 2>/dev/null || echo "N/A")

echo "📋 Infrastructure Details:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "VPC ID:                 $VPC_ID"
echo "RDS Endpoint:           $RDS_ENDPOINT"
echo "RDS Address:            $RDS_ADDRESS"
echo "RDS Security Group:     $RDS_SG"
echo "CodeBuild Security Group: $CODEBUILD_SG"
echo "Master Secret ARN:      $SECRET_ARN"
echo ""

echo "=========================================="
echo "Next Steps for Task 027:"
echo "=========================================="
echo "1. ✅ VPC and RDS are now deployed"
echo ""
echo "2. Configure CodeBuild network access:"
echo "   - Use VPC ID: $VPC_ID"
echo "   - Use Private Subnets for CodeBuild"
echo "   - Use CodeBuild SG: $CODEBUILD_SG"
echo ""
echo "3. Retrieve DB credentials:"
echo "   aws secretsmanager get-secret-value \\"
echo "     --secret-id $SECRET_ARN \\"
echo "     --query SecretString --output text | jq ."
echo ""
echo "4. Create Flyway CodeBuild project (Task 028)"
echo "=========================================="

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN
