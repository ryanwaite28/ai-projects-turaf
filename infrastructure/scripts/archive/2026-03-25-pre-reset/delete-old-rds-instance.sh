#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
REGION="us-east-1"
DB_INSTANCE_ID="turaf-postgres-dev"
DB_SUBNET_GROUP="turaf-db-subnet-group-dev"

echo "=========================================="
echo "Delete Old RDS Instance and Subnet Group"
echo "=========================================="
echo ""

echo "🔐 Assuming role in Dev account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "delete-rds" \
    --duration-seconds 900 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "✅ Assumed role"
echo ""

# Delete RDS instance
echo "🗑️  Deleting RDS instance ${DB_INSTANCE_ID}..."
aws rds delete-db-instance \
    --db-instance-identifier "${DB_INSTANCE_ID}" \
    --skip-final-snapshot \
    --delete-automated-backups \
    --region "${REGION}" 2>/dev/null || echo "  (already deleted or doesn't exist)"
echo ""

echo "⏳ Waiting for RDS instance to be deleted (this may take 5-10 minutes)..."
aws rds wait db-instance-deleted \
    --db-instance-identifier "${DB_INSTANCE_ID}" \
    --region "${REGION}" 2>/dev/null || echo "  (already deleted)"
echo ""

# Delete DB subnet group
echo "🗑️  Deleting DB subnet group ${DB_SUBNET_GROUP}..."
aws rds delete-db-subnet-group \
    --db-subnet-group-name "${DB_SUBNET_GROUP}" \
    --region "${REGION}" 2>/dev/null || echo "  (already deleted or doesn't exist)"
echo ""

# Remove from Terraform state
echo "🧹 Cleaning up Terraform state..."
cd "${SCRIPT_DIR}/../terraform/environments/dev"
terraform state rm module.database.aws_db_subnet_group.main 2>/dev/null || true
terraform state rm module.database.aws_db_instance.main 2>/dev/null || true
echo ""

echo "=========================================="
echo "Cleanup Complete!"
echo "=========================================="
echo ""
echo "✅ Old RDS instance and subnet group deleted"
echo ""
echo "Next step:"
echo "  Run deploy-simplified-dev.sh to deploy fresh infrastructure"
echo ""
