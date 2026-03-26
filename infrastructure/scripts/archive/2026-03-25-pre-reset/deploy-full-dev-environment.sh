#!/bin/bash

set -e

# Deploy Full Dev Environment Infrastructure
# Purpose: Deploy complete Terraform infrastructure for Dev environment including ECS, ALB, Lambda, S3, CloudFront
# This addresses the CI/CD readiness blockers identified in the assessment

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TERRAFORM_DIR="${ROOT_DIR}/infrastructure/terraform/environments/dev"

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
REGION="us-east-1"

echo "=========================================="
echo "Deploy Full Dev Environment Infrastructure"
echo "=========================================="
echo ""
echo "This will deploy:"
echo "  - VPC and Networking"
echo "  - RDS PostgreSQL Database"
echo "  - ECS Cluster and Services"
echo "  - Application Load Balancer"
echo "  - Lambda Functions"
echo "  - S3 Buckets"
echo "  - CloudFront Distribution"
echo "  - EventBridge and SQS"
echo "  - CloudWatch Monitoring"
echo ""
echo "Estimated Cost: ~$32-55/month"
echo "Deployment Time: ~30-45 minutes"
echo ""

# Check if terraform is installed
if ! command -v terraform &> /dev/null; then
    echo "❌ Error: terraform is not installed"
    echo "Install: brew install terraform"
    exit 1
fi

echo "✅ Terraform installed: $(terraform version -json | jq -r '.terraform_version')"
echo ""

# Assume role in Dev account
echo "🔐 Assuming role in Dev account..."
CREDENTIALS=$(aws sts assume-role \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "deploy-full-dev-$(date +%s)" \
    --profile "${ROOT_PROFILE}" \
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

# Check for ACM certificate
echo "🔍 Checking for ACM certificate..."
CERT_ARN=$(aws acm list-certificates --region us-east-1 --query "CertificateSummaryList[?DomainName=='*.turafapp.com'].CertificateArn" --output text)

if [ -z "${CERT_ARN}" ]; then
    echo "⚠️  Warning: No ACM certificate found for *.turafapp.com in Dev account"
    echo "   Using placeholder ARN - ALB will not have HTTPS listener"
    CERT_ARN="arn:aws:acm:us-east-1:${DEV_ACCOUNT_ID}:certificate/placeholder"
else
    echo "✅ Found ACM certificate: ${CERT_ARN}"
fi
echo ""

# Check for ECR repositories
echo "🔍 Checking for ECR repositories..."
ECR_REPOS=$(aws ecr describe-repositories --region us-east-1 --query "repositories[?starts_with(repositoryName, 'turaf/')].repositoryName" --output text 2>/dev/null || echo "")

if [ -z "${ECR_REPOS}" ]; then
    echo "⚠️  Warning: No ECR repositories found"
    echo "   Using placeholder image URIs - ECS services will fail to start"
    IDENTITY_IMAGE="${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:latest"
    ORG_IMAGE="${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service:latest"
    EXP_IMAGE="${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service:latest"
else
    echo "✅ Found ECR repositories: $(echo ${ECR_REPOS} | wc -w | tr -d ' ') repos"
    IDENTITY_IMAGE="${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:latest"
    ORG_IMAGE="${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service:latest"
    EXP_IMAGE="${DEV_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service:latest"
fi
echo ""

# Navigate to Terraform directory
cd "${TERRAFORM_DIR}"

# Check if terraform.tfvars exists
if [ ! -f "terraform.tfvars" ]; then
    echo "📝 Creating terraform.tfvars from example..."
    cp terraform.tfvars.example terraform.tfvars
    
    # Update with actual values
    sed -i.bak "s|arn:aws:acm:us-east-1:801651112319:certificate/YOUR-CERT-ID|${CERT_ARN}|g" terraform.tfvars
    
    # Add ECR image URIs if not present
    if ! grep -q "identity_service_image" terraform.tfvars; then
        cat >> terraform.tfvars <<EOF

# ECR Image URIs (auto-configured)
identity_service_image     = "${IDENTITY_IMAGE}"
organization_service_image = "${ORG_IMAGE}"
experiment_service_image   = "${EXP_IMAGE}"
image_tag                  = "latest"
EOF
    fi
    
    echo "✅ Created terraform.tfvars"
else
    echo "✅ terraform.tfvars already exists"
fi
echo ""

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
terraform plan -out=tfplan
echo ""

# Show summary
echo "=========================================="
echo "Deployment Plan Summary"
echo "=========================================="
echo ""
echo "Review the plan above. This will create:"
echo "  - VPC with public/private/database subnets"
echo "  - RDS PostgreSQL (db.t3.micro)"
echo "  - ECS Cluster with 3 Fargate services"
echo "  - Application Load Balancer"
echo "  - S3 buckets for storage"
echo "  - EventBridge and SQS queues"
echo "  - Security groups and IAM roles"
echo "  - CloudWatch log groups"
echo ""
echo "⚠️  Note: Lambda and CloudFront are disabled by default for cost savings"
echo ""

# Ask for confirmation
read -p "Do you want to apply this plan? (yes/no): " CONFIRM

if [ "${CONFIRM}" != "yes" ]; then
    echo "❌ Deployment cancelled"
    rm -f tfplan
    exit 0
fi

# Apply deployment
echo ""
echo "🚀 Applying Terraform configuration..."
echo "   This will take 30-45 minutes (RDS and ALB provisioning)"
echo ""

terraform apply tfplan

# Clean up plan file
rm -f tfplan

echo ""
echo "=========================================="
echo "✅ Deployment Complete!"
echo "=========================================="
echo ""

# Get outputs
echo "📊 Infrastructure Outputs:"
echo ""
terraform output

echo ""
echo "Next Steps:"
echo "  1. Verify ECS cluster: aws ecs describe-clusters --clusters turaf-cluster-dev"
echo "  2. Verify ALB: aws elbv2 describe-load-balancers --names turaf-alb-dev"
echo "  3. Check RDS: aws rds describe-db-instances --db-instance-identifier turaf-postgres-dev"
echo "  4. Execute Task 025: Setup Database Schemas"
echo "  5. Push Docker images to ECR"
echo "  6. Test CI/CD pipelines"
echo ""
echo "Documentation updated in: infrastructure/DEPLOYED_INFRASTRUCTURE.md"
echo ""
