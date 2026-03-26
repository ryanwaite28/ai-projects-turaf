#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
REGION="us-east-1"

echo "🔐 Assuming role in Dev account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "delete-db-subnet" \
    --duration-seconds 900 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "🗑️  Deleting DB subnet group..."
aws rds delete-db-subnet-group \
    --db-subnet-group-name turaf-db-subnet-group-dev \
    --region "${REGION}"

echo "✅ DB subnet group deleted"

# Remove from Terraform state
cd "${SCRIPT_DIR}/../terraform/environments/dev"
terraform state rm module.database.aws_db_subnet_group.main 2>/dev/null || true
echo "✅ Removed from Terraform state"
