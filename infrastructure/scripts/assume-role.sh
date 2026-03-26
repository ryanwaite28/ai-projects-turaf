#!/bin/bash

# Assume AWS Role - Reusable Script
# Usage: source assume-role.sh <env>
# Exports AWS credentials for the specified environment

set -e

ENV=$1
ROOT_PROFILE="turaf-root"
REGION="us-east-1"

if [ -z "$ENV" ]; then
    echo "Usage: source assume-role.sh <env>"
    echo "  env: dev, qa, or prod"
    return 1 2>/dev/null || exit 1
fi

# Map environment to account ID
case "$ENV" in
    dev)
        ACCOUNT_ID="801651112319"
        ;;
    qa)
        ACCOUNT_ID="965932217544"
        ;;
    prod)
        ACCOUNT_ID="811783768245"
        ;;
    ops)
        ACCOUNT_ID="146072879609"
        ;;
    *)
        echo "❌ Invalid environment: $ENV"
        echo "   Valid options: dev, qa, prod, ops"
        return 1 2>/dev/null || exit 1
        ;;
esac

echo "🔐 Assuming role in $ENV account ($ACCOUNT_ID)..."

CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${ACCOUNT_ID}:role/OrganizationAccountAccessRole" \
    --role-session-name "terraform-${ENV}-session" \
    --duration-seconds 3600 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "✅ Assumed role in $ENV account"
echo "   Account ID: $ACCOUNT_ID"
echo "   Region: $REGION"
echo "   Session expires in 1 hour"
echo ""
