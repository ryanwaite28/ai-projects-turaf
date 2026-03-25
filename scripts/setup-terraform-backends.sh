#!/bin/bash

set -e

ACCOUNTS=("ops:turaf-ops" "dev:turaf-dev" "qa:turaf-qa" "prod:turaf-prod")
REGION="us-east-1"

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  PROFILE="${account##*:}"
  BUCKET="turaf-terraform-state-${ENV}"
  
  echo "Setting up Terraform backend for ${ENV} account..."
  
  # Create S3 bucket
  aws s3api create-bucket \
    --bucket ${BUCKET} \
    --region ${REGION} \
    --profile ${PROFILE} || echo "Bucket already exists"
  
  # Enable versioning
  aws s3api put-bucket-versioning \
    --bucket ${BUCKET} \
    --versioning-configuration Status=Enabled \
    --profile ${PROFILE}
  
  # Enable encryption
  aws s3api put-bucket-encryption \
    --bucket ${BUCKET} \
    --server-side-encryption-configuration '{
      "Rules": [{
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        }
      }]
    }' \
    --profile ${PROFILE}
  
  # Block public access
  aws s3api put-public-access-block \
    --bucket ${BUCKET} \
    --public-access-block-configuration \
      BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true \
    --profile ${PROFILE}
  
  # Create DynamoDB table
  aws dynamodb create-table \
    --table-name turaf-terraform-locks \
    --attribute-definitions AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --region ${REGION} \
    --profile ${PROFILE} || echo "Table already exists"
  
  echo "✅ ${ENV} backend setup complete"
done

echo ""
echo "All Terraform backends configured successfully!"
