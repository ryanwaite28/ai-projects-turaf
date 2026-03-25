#!/bin/bash

set -e

# Deploy Minimal Development Infrastructure (Networking + Database Only)
# Purpose: Deploy only VPC and RDS needed for Task 027

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform/environments/dev"

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"

echo "=========================================="
echo "Turaf Minimal Infrastructure Deployment"
echo "Networking + Database Only"
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
  --role-session-name "terraform-deploy-minimal" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')

echo "✅ Assumed role in dev account ($DEV_ACCOUNT_ID)"
echo ""

# Navigate to terraform directory
cd "$TERRAFORM_DIR"

# Use minimal configuration
echo "Using minimal configuration (networking + database only)..."
echo ""

echo "=========================================="
echo "Step 1: Terraform Init"
echo "=========================================="
echo ""

terraform init -reconfigure

echo ""
echo "=========================================="
echo "Step 2: Terraform Validate"
echo "=========================================="
echo ""

terraform validate -no-color

echo ""
echo "=========================================="
echo "Step 3: Terraform Plan"
echo "=========================================="
echo ""

terraform plan \
  -var-file="terraform.tfvars" \
  -out=tfplan-minimal \
  main-minimal.tf

echo ""
echo "=========================================="
echo "Review Plan"
echo "=========================================="
echo ""
echo "This will create:"
echo "  ✅ VPC with subnets (public, private, database)"
echo "  ✅ RDS PostgreSQL (db.t3.micro)"
echo "  ✅ Security groups"
echo "  ✅ KMS keys"
echo ""
echo "💰 Estimated cost: ~$15/month (RDS Free Tier eligible)"
echo ""

read -p "Apply this plan? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "❌ Deployment cancelled"
    rm -f tfplan-minimal
    exit 0
fi

echo ""
echo "=========================================="
echo "Step 4: Terraform Apply"
echo "=========================================="
echo ""

terraform apply tfplan-minimal

rm -f tfplan-minimal

echo ""
echo "=========================================="
echo "✅ Minimal Infrastructure Deployed!"
echo "=========================================="
echo ""

# Get outputs
VPC_ID=$(terraform output -raw vpc_id 2>/dev/null || echo "N/A")
RDS_ENDPOINT=$(terraform output -raw rds_endpoint 2>/dev/null || echo "N/A")
RDS_SG=$(terraform output -raw rds_security_group_id 2>/dev/null || echo "N/A")

echo "VPC ID: $VPC_ID"
echo "RDS Endpoint: $RDS_ENDPOINT"
echo "RDS Security Group: $RDS_SG"
echo ""

echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Run Task 027 network setup:"
echo "   ./infrastructure/scripts/setup-flyway-network-cross-account.sh"
echo ""
echo "2. Verify network configuration:"
echo "   ./infrastructure/scripts/verify-flyway-network-cross-account.sh"
echo "=========================================="

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN
