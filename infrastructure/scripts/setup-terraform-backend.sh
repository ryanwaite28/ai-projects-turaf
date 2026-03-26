#!/bin/bash

# Setup Terraform Backend (S3 + DynamoDB)
# Usage: ./setup-terraform-backend.sh <env>

set -e

ENV=$1
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REGION="us-east-1"

if [ -z "$ENV" ]; then
    echo "Usage: ./setup-terraform-backend.sh <env>"
    echo "  env: dev, qa, or prod"
    exit 1
fi

echo "=========================================="
echo "Setting up Terraform Backend for $ENV"
echo "=========================================="
echo ""

# Assume role
source "${SCRIPT_DIR}/assume-role.sh" "$ENV"

# Get account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Backend resources
STATE_BUCKET="turaf-terraform-state"
LOCK_TABLE="turaf-terraform-locks"

# Create S3 bucket for state
echo "📦 Creating S3 bucket: $STATE_BUCKET"
if aws s3 ls "s3://${STATE_BUCKET}" 2>/dev/null; then
    echo "✅ Bucket already exists"
else
    aws s3api create-bucket \
        --bucket "${STATE_BUCKET}" \
        --region "${REGION}"
    
    echo "✅ Bucket created"
fi

# Enable versioning
echo "📦 Enabling versioning..."
aws s3api put-bucket-versioning \
    --bucket "${STATE_BUCKET}" \
    --versioning-configuration Status=Enabled

echo "✅ Versioning enabled"

# Enable encryption
echo "🔒 Enabling encryption..."
aws s3api put-bucket-encryption \
    --bucket "${STATE_BUCKET}" \
    --server-side-encryption-configuration '{
        "Rules": [{
            "ApplyServerSideEncryptionByDefault": {
                "SSEAlgorithm": "AES256"
            },
            "BucketKeyEnabled": true
        }]
    }'

echo "✅ Encryption enabled"

# Block public access
echo "🔒 Blocking public access..."
aws s3api put-public-access-block \
    --bucket "${STATE_BUCKET}" \
    --public-access-block-configuration \
        "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

echo "✅ Public access blocked"

# Add lifecycle policy
echo "📦 Adding lifecycle policy..."
aws s3api put-bucket-lifecycle-configuration \
    --bucket "${STATE_BUCKET}" \
    --lifecycle-configuration '{
        "Rules": [{
            "ID": "DeleteOldVersions",
            "Status": "Enabled",
            "NoncurrentVersionExpiration": {
                "NoncurrentDays": 90
            }
        }]
    }'

echo "✅ Lifecycle policy added"

# Create DynamoDB table for locking
echo "🔒 Creating DynamoDB lock table: $LOCK_TABLE"
if aws dynamodb describe-table --table-name "${LOCK_TABLE}" 2>/dev/null; then
    echo "✅ Table already exists"
else
    aws dynamodb create-table \
        --table-name "${LOCK_TABLE}" \
        --attribute-definitions AttributeName=LockID,AttributeType=S \
        --key-schema AttributeName=LockID,KeyType=HASH \
        --billing-mode PAY_PER_REQUEST \
        --tags Key=Environment,Value=${ENV} Key=ManagedBy,Value=Terraform
    
    echo "⏳ Waiting for table to be active..."
    aws dynamodb wait table-exists --table-name "${LOCK_TABLE}"
    
    echo "✅ Table created"
fi

echo ""
echo "=========================================="
echo "Backend Setup Complete!"
echo "=========================================="
echo ""
echo "✅ S3 Bucket: $STATE_BUCKET"
echo "✅ DynamoDB Table: $LOCK_TABLE"
echo ""
echo "Backend configuration:"
echo "  bucket         = \"${STATE_BUCKET}\""
echo "  key            = \"${ENV}/terraform.tfstate\""
echo "  region         = \"${REGION}\""
echo "  encrypt        = true"
echo "  dynamodb_table = \"${LOCK_TABLE}\""
echo ""
