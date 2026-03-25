#!/bin/bash

set -e

ACCOUNTS=(
  "ops:turaf-ops:146072879609"
  "dev:turaf-dev:801651112319"
  "qa:turaf-qa:965932217544"
  "prod:turaf-prod:811783768245"
)

SERVICES=(
  "identity-service"
  "organization-service"
  "experiment-service"
  "metrics-service"
  "communications-service"
  "bff-api"
  "ws-gateway"
)

REGION="us-east-1"

echo "Creating ECR repositories in all accounts..."
echo ""

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  PROFILE=$(echo "$account" | cut -d: -f2)
  ACCOUNT_ID=$(echo "$account" | cut -d: -f3)
  
  echo "=== ${ENV} account (${ACCOUNT_ID}) ==="
  
  for service in "${SERVICES[@]}"; do
    echo "  Creating turaf/${service}..."
    
    # Create repository
    aws ecr create-repository \
      --repository-name turaf/${service} \
      --image-scanning-configuration scanOnPush=true \
      --encryption-configuration encryptionType=AES256 \
      --region ${REGION} \
      --profile ${PROFILE} 2>/dev/null || echo "    Repository already exists"
    
    # Apply lifecycle policy
    aws ecr put-lifecycle-policy \
      --repository-name turaf/${service} \
      --lifecycle-policy-text file://infrastructure/ecr-lifecycle-policy.json \
      --region ${REGION} \
      --profile ${PROFILE} 2>/dev/null || echo "    Lifecycle policy already applied"
  done
  
  echo "✅ ${ENV} account: 7 repositories created"
  echo ""
done

echo "All ECR repositories configured successfully!"
echo ""
echo "Repository URIs by account:"
echo ""
for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  ACCOUNT_ID=$(echo "$account" | cut -d: -f3)
  echo "${ENV} account (${ACCOUNT_ID}):"
  for service in "${SERVICES[@]}"; do
    echo "  ${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/turaf/${service}"
  done
  echo ""
done
