#!/bin/bash

set -e

# Import Existing Resources
# Purpose: Import conflicting resources into Terraform state

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"
REGION="us-east-1"

echo "=========================================="
echo "Import Existing Resources"
echo "=========================================="
echo ""

# Assume role in Dev account
echo "🔐 Assuming role in Dev account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "terraform-import" \
    --duration-seconds 900 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "✅ Assumed role in Dev account (${DEV_ACCOUNT_ID})"
echo ""

# Navigate to dev environment directory
cd "${SCRIPT_DIR}/../terraform/environments/dev"

# Import DB subnet group
echo "📥 Importing DB subnet group..."
terraform import \
  -var="acm_certificate_arn=arn:aws:acm:us-east-1:${DEV_ACCOUNT_ID}:certificate/placeholder" \
  -var="identity_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:latest" \
  -var="organization_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service:latest" \
  -var="experiment_service_image=${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service:latest" \
  module.database.aws_db_subnet_group.main \
  turaf-db-subnet-group-${ENV} 2>/dev/null || echo "  (already imported or doesn't exist)"
echo ""

echo "=========================================="
echo "Import Complete!"
echo "=========================================="
echo ""
echo "✅ Resources imported into Terraform state"
echo ""
echo "Next step:"
echo "  Run deploy-simplified-dev.sh to complete deployment"
echo ""
