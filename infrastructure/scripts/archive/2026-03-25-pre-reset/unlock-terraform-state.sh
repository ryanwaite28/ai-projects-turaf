#!/bin/bash

set -e

# Unlock Terraform State
# Purpose: Force unlock Terraform state when locked from previous operations

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
REGION="us-east-1"
LOCK_ID="b0f285a2-4cbd-d7d7-14a9-f4ca9ae2a547"

echo "=========================================="
echo "Unlock Terraform State"
echo "=========================================="
echo ""

# Assume role in Dev account
echo "🔐 Assuming role in Dev account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "terraform-unlock" \
    --duration-seconds 900 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "✅ Assumed role in Dev account"
echo ""

# Navigate to dev environment directory
cd "${SCRIPT_DIR}/../terraform/environments/dev"

# Force unlock
echo "🔓 Force unlocking state..."
terraform force-unlock -force "${LOCK_ID}"
echo ""

echo "✅ State unlocked successfully"
