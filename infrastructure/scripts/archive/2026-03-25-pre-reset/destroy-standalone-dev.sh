#!/bin/bash

set -e

# Destroy Standalone Dev Infrastructure
# Purpose: Clean up existing standalone VPC+RDS deployment to avoid conflicts

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"
REGION="us-east-1"

echo "=========================================="
echo "Destroy Standalone Dev Infrastructure"
echo "=========================================="
echo ""
echo "⚠️  WARNING: This will destroy:"
echo "  - Existing VPC and subnets"
echo "  - RDS PostgreSQL instance"
echo "  - DB subnet groups"
echo "  - KMS keys and aliases"
echo "  - Security groups"
echo "  - Any other standalone resources"
echo ""
echo "This is necessary to clear resource conflicts"
echo "before deploying the full simplified environment."
echo ""

# Check if Terraform is installed
if ! command -v terraform &> /dev/null; then
    echo "❌ Error: Terraform is not installed"
    exit 1
fi

echo "✅ Terraform installed"
echo ""

# Assume role in Dev account
echo "🔐 Assuming role in Dev account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "terraform-dev-destroy" \
    --duration-seconds 3600 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "✅ Assumed role in Dev account (${DEV_ACCOUNT_ID})"
echo ""

# Verify credentials
CALLER_IDENTITY=$(aws sts get-caller-identity)
CURRENT_ACCOUNT=$(echo "${CALLER_IDENTITY}" | jq -r '.Account')

if [ "${CURRENT_ACCOUNT}" != "${DEV_ACCOUNT_ID}" ]; then
    echo "❌ Error: Not in correct account. Expected ${DEV_ACCOUNT_ID}, got ${CURRENT_ACCOUNT}"
    exit 1
fi

echo "✅ Verified account: ${CURRENT_ACCOUNT}"
echo ""

# Navigate to dev environment directory
cd "${SCRIPT_DIR}/../terraform/environments/dev"

# Check if Terraform state exists
if [ ! -f ".terraform/terraform.tfstate" ] && [ ! -f "terraform.tfstate" ]; then
    echo "⚠️  No Terraform state found locally"
    echo "   Checking S3 backend..."
    
    # Try to initialize to pull state from S3
    terraform init -migrate-state -upgrade 2>/dev/null || true
fi

# Show what will be destroyed
echo "📋 Planning destruction..."
terraform plan -destroy \
  -var="acm_certificate_arn=arn:aws:acm:us-east-1:${DEV_ACCOUNT_ID}:certificate/placeholder" \
  -var="identity_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:latest" \
  -var="organization_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service:latest" \
  -var="experiment_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service:latest"
echo ""

# Confirm destruction
echo "⚠️  Are you sure you want to destroy all resources? (yes/no)"
read -r CONFIRM

if [ "${CONFIRM}" != "yes" ]; then
    echo "❌ Destruction cancelled"
    exit 0
fi

# Destroy infrastructure
echo "🗑️  Destroying infrastructure..."
terraform destroy -auto-approve \
  -var="acm_certificate_arn=arn:aws:acm:us-east-1:${DEV_ACCOUNT_ID}:certificate/placeholder" \
  -var="identity_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:latest" \
  -var="organization_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service:latest" \
  -var="experiment_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service:latest"
echo ""

echo "=========================================="
echo "Destruction Complete!"
echo "=========================================="
echo ""
echo "✅ Standalone infrastructure destroyed"
echo ""
echo "Next steps:"
echo "  1. Run deploy-simplified-dev.sh to deploy clean infrastructure"
echo "  2. Verify all resources are created successfully"
echo ""
