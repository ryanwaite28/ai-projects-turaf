#!/bin/bash

set -e

# Deploy Development Infrastructure (Cross-Account)
# Purpose: Initialize and apply Terraform configuration for dev environment
# Uses: Root account to assume OrganizationAccountAccessRole in dev account

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform/environments/dev"

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"

echo "=========================================="
echo "Turaf Development Infrastructure Deployment"
echo "=========================================="
echo ""

# Check if terraform is installed
if ! command -v terraform &> /dev/null; then
    echo "❌ Terraform is not installed"
    echo "   Install from: https://www.terraform.io/downloads"
    exit 1
fi

echo "✅ Terraform installed: $(terraform version | head -n 1)"
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

ROOT_ACCOUNT_ID=$(aws sts get-caller-identity --profile $ROOT_PROFILE --query Account --output text)
echo "✅ AWS credentials configured"
echo "   Root Account: $ROOT_ACCOUNT_ID"
echo "   Profile: $ROOT_PROFILE"
echo ""

# Assume role in dev account
echo "Assuming role in dev account..."
CREDENTIALS=$(aws sts assume-role \
  --profile $ROOT_PROFILE \
  --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
  --role-session-name "terraform-deploy-dev" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')

echo "✅ Assumed role in dev account ($DEV_ACCOUNT_ID)"
echo ""

# Verify assumed role
ASSUMED_IDENTITY=$(aws sts get-caller-identity --output json)
echo "Current identity:"
echo "$ASSUMED_IDENTITY" | jq '.'
echo ""

# Check if terraform.tfvars exists
if [ ! -f "$TERRAFORM_DIR/terraform.tfvars" ]; then
    echo "⚠️  terraform.tfvars not found"
    echo "   Creating from terraform.tfvars.example..."
    echo ""
    
    if [ -f "$TERRAFORM_DIR/terraform.tfvars.example" ]; then
        cp "$TERRAFORM_DIR/terraform.tfvars.example" "$TERRAFORM_DIR/terraform.tfvars"
        echo "✅ Created terraform.tfvars"
        echo ""
        echo "📝 Please review and update terraform.tfvars with your values:"
        echo "   - ACM certificate ARN (if available)"
        echo "   - ECR image URIs (if services are built)"
        echo "   - Email addresses for alerts"
        echo ""
        echo "Opening terraform.tfvars for editing..."
        echo ""
        read -p "Press Enter to continue after reviewing terraform.tfvars..."
    else
        echo "❌ terraform.tfvars.example not found"
        exit 1
    fi
fi

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
echo "Generating execution plan..."
echo ""

terraform plan -out=tfplan

echo ""
echo "=========================================="
echo "Step 4: Review Plan"
echo "=========================================="
echo ""
echo "⚠️  IMPORTANT: Review the plan above carefully"
echo ""
echo "This will create:"
echo "  - VPC with public, private, and database subnets"
echo "  - RDS PostgreSQL instance (db.t3.micro)"
echo "  - Security groups and IAM roles"
echo "  - S3 buckets and SQS queues"
echo "  - ECS cluster (no services yet)"
echo ""
echo "💰 Estimated monthly cost: ~$15-30 (mostly RDS)"
echo ""

read -p "Do you want to apply this plan? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo ""
    echo "❌ Deployment cancelled"
    rm -f tfplan
    exit 0
fi

echo ""
echo "=========================================="
echo "Step 5: Terraform Apply"
echo "=========================================="
echo ""

terraform apply tfplan

rm -f tfplan

echo ""
echo "=========================================="
echo "✅ Infrastructure Deployment Complete!"
echo "=========================================="
echo ""

# Get outputs
echo "Retrieving infrastructure details..."
echo ""

VPC_ID=$(terraform output -raw vpc_id 2>/dev/null || echo "N/A")
RDS_ENDPOINT=$(terraform output -raw rds_endpoint 2>/dev/null || echo "N/A")
RDS_SECURITY_GROUP=$(terraform output -raw rds_security_group_id 2>/dev/null || echo "N/A")

echo "VPC ID: $VPC_ID"
echo "RDS Endpoint: $RDS_ENDPOINT"
echo "RDS Security Group: $RDS_SECURITY_GROUP"
echo ""

echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Run Flyway migrations to create schemas and tables"
echo "2. Run database user creation script (Task 025)"
echo "3. Run network security group setup (Task 027):"
echo "   ./infrastructure/scripts/setup-flyway-network-cross-account.sh"
echo "4. Create CodeBuild migration projects (Task 028)"
echo "=========================================="
echo ""
echo "✅ Deployment script complete!"

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN
