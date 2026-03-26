#!/bin/bash

set -e

# Simplified Dev Environment Deployment
# Deploys only: Networking, Security, Database, Storage
# Excludes: Compute (ECS), Lambda, Messaging, Monitoring

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"
REGION="us-east-1"

echo "=========================================="
echo "Deploy Simplified Dev Environment"
echo "=========================================="
echo ""
echo "This will deploy:"
echo "  - VPC and Networking"
echo "  - Security Groups and IAM Roles"
echo "  - RDS PostgreSQL Database"
echo "  - S3 Storage Buckets"
echo ""
echo "Excluded (will add later):"
echo "  - ECS Cluster and Services"
echo "  - Lambda Functions"
echo "  - EventBridge and SQS"
echo "  - CloudWatch Monitoring"
echo ""
echo "Estimated Cost: ~$15-25/month"
echo "Deployment Time: ~15-20 minutes"
echo ""

# Check if Terraform is installed
if ! command -v terraform &> /dev/null; then
    echo "❌ Error: Terraform is not installed"
    exit 1
fi

TERRAFORM_VERSION=$(terraform version -json | jq -r '.terraform_version')
echo "✅ Terraform installed: ${TERRAFORM_VERSION}"
echo ""

# Assume role in Dev account
echo "🔐 Assuming role in Dev account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "terraform-dev-deployment" \
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

# Initialize Terraform
echo "🔧 Initializing Terraform..."
terraform init -migrate-state -upgrade
echo ""

# Validate configuration
echo "✅ Validating Terraform configuration..."
terraform validate
echo ""

# Plan deployment
echo "📋 Planning deployment..."
terraform plan -out=tfplan \
  -var="acm_certificate_arn=arn:aws:acm:us-east-1:${DEV_ACCOUNT_ID}:certificate/placeholder" \
  -var="identity_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:latest" \
  -var="organization_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service:latest" \
  -var="experiment_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service:latest"
echo ""

# Apply deployment
echo "🚀 Applying deployment..."
terraform apply -auto-approve tfplan
echo ""

# Show outputs
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo ""
terraform output
echo ""

echo "✅ Simplified Dev environment deployed successfully!"
echo ""
echo "Next steps:"
echo "  1. Fix compute module configuration errors"
echo "  2. Re-enable compute module for ECS deployment"
echo "  3. Fix messaging module Lambda dependencies"
echo "  4. Re-enable messaging and lambda modules"
echo "  5. Add monitoring module"
echo ""
