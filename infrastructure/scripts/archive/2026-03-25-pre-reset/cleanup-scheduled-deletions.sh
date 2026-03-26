#!/bin/bash

set -e

# Cleanup Scheduled Deletions
# Purpose: Force delete AWS resources scheduled for deletion to clear conflicts

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"
REGION="us-east-1"

echo "=========================================="
echo "Cleanup Scheduled Deletions"
echo "=========================================="
echo ""

# Assume role in Dev account
echo "🔐 Assuming role in Dev account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "cleanup-deletions" \
    --duration-seconds 900 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "✅ Assumed role in Dev account (${DEV_ACCOUNT_ID})"
echo ""

# Force delete Secrets Manager secrets
echo "🗑️  Force deleting Secrets Manager secrets..."
SECRETS=(
    "turaf/dev/db/admin-password"
    "turaf/dev/db/identity-user"
    "turaf/dev/db/organization-user"
    "turaf/dev/db/experiment-user"
    "turaf/dev/db/metrics-user"
)

for SECRET in "${SECRETS[@]}"; do
    echo "  - Deleting ${SECRET}..."
    aws secretsmanager delete-secret \
        --secret-id "${SECRET}" \
        --force-delete-without-recovery \
        --region "${REGION}" 2>/dev/null || echo "    (already deleted or doesn't exist)"
done
echo ""

# Delete DB subnet group
echo "🗑️  Deleting DB subnet group..."
aws rds delete-db-subnet-group \
    --db-subnet-group-name "turaf-db-subnet-group-${ENV}" \
    --region "${REGION}" 2>/dev/null || echo "  (already deleted or doesn't exist)"
echo ""

# Delete KMS alias (note: can't delete the key itself due to SCP)
echo "🗑️  Deleting KMS alias..."
aws kms delete-alias \
    --alias-name "alias/turaf-rds-${ENV}" \
    --region "${REGION}" 2>/dev/null || echo "  (already deleted or doesn't exist)"
echo ""

echo "=========================================="
echo "Cleanup Complete!"
echo "=========================================="
echo ""
echo "✅ Scheduled deletions cleaned up"
echo ""
echo "Next step:"
echo "  Run deploy-simplified-dev.sh to deploy infrastructure"
echo ""
