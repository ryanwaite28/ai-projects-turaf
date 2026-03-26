#!/bin/bash

# Check Prerequisites for Terraform Deployment
# Usage: ./check-prerequisites.sh <env>

set -e

ENV=$1
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -z "$ENV" ]; then
    echo "Usage: ./check-prerequisites.sh <env>"
    echo "  env: dev, qa, or prod"
    exit 1
fi

echo "=========================================="
echo "Checking Prerequisites for $ENV"
echo "=========================================="
echo ""

# Check required tools
echo "📋 Checking required tools..."
MISSING_TOOLS=()

if ! command -v terraform &> /dev/null; then
    MISSING_TOOLS+=("terraform")
fi

if ! command -v aws &> /dev/null; then
    MISSING_TOOLS+=("aws-cli")
fi

if ! command -v jq &> /dev/null; then
    MISSING_TOOLS+=("jq")
fi

if [ ${#MISSING_TOOLS[@]} -gt 0 ]; then
    echo "❌ Missing required tools:"
    for tool in "${MISSING_TOOLS[@]}"; do
        echo "   - $tool"
    done
    exit 1
fi

echo "✅ All required tools installed"
echo "   - Terraform: $(terraform version | head -n1)"
echo "   - AWS CLI: $(aws --version | cut -d' ' -f1)"
echo "   - jq: $(jq --version)"
echo ""

# Check AWS credentials
echo "📋 Checking AWS credentials..."
if ! aws sts get-caller-identity --profile turaf-root &> /dev/null; then
    echo "❌ AWS credentials not configured for profile 'turaf-root'"
    echo "   Run: aws configure --profile turaf-root"
    exit 1
fi

echo "✅ AWS credentials configured"
CALLER_IDENTITY=$(aws sts get-caller-identity --profile turaf-root)
echo "   Account: $(echo $CALLER_IDENTITY | jq -r '.Account')"
echo "   User: $(echo $CALLER_IDENTITY | jq -r '.Arn')"
echo ""

# Source assume-role to get credentials for target environment
source "${SCRIPT_DIR}/assume-role.sh" "$ENV"

# Check Terraform state bucket
echo "📋 Checking Terraform state bucket..."
STATE_BUCKET="turaf-terraform-state"
if aws s3 ls "s3://${STATE_BUCKET}" &> /dev/null; then
    echo "✅ Terraform state bucket exists: ${STATE_BUCKET}"
else
    echo "❌ Terraform state bucket not found: ${STATE_BUCKET}"
    exit 1
fi
echo ""

# Check DynamoDB lock table
echo "📋 Checking DynamoDB lock table..."
LOCK_TABLE="turaf-terraform-locks"
if aws dynamodb describe-table --table-name "${LOCK_TABLE}" &> /dev/null; then
    echo "✅ DynamoDB lock table exists: ${LOCK_TABLE}"
else
    echo "❌ DynamoDB lock table not found: ${LOCK_TABLE}"
    exit 1
fi
echo ""

# Check ECR repositories
echo "📋 Checking ECR repositories..."
REQUIRED_REPOS=("turaf/identity-service" "turaf/organization-service" "turaf/experiment-service")
MISSING_REPOS=()

for repo in "${REQUIRED_REPOS[@]}"; do
    if ! aws ecr describe-repositories --repository-names "$repo" &> /dev/null; then
        MISSING_REPOS+=("$repo")
    fi
done

if [ ${#MISSING_REPOS[@]} -gt 0 ]; then
    echo "⚠️  Missing ECR repositories:"
    for repo in "${MISSING_REPOS[@]}"; do
        echo "   - $repo"
    done
    echo "   Note: Repositories will be created during deployment"
else
    echo "✅ All ECR repositories exist"
fi
echo ""

# Check ACM certificate (optional - will use placeholder if not found)
echo "📋 Checking ACM certificate..."
if aws acm list-certificates --region us-east-1 | jq -r '.CertificateSummaryList[] | select(.DomainName == "*.turafapp.com") | .CertificateArn' | grep -q .; then
    CERT_ARN=$(aws acm list-certificates --region us-east-1 | jq -r '.CertificateSummaryList[] | select(.DomainName == "*.turafapp.com") | .CertificateArn')
    echo "✅ ACM certificate found: *.turafapp.com"
    echo "   ARN: $CERT_ARN"
    echo ""
    echo "⚠️  Update terraform.tfvars with this certificate ARN:"
    echo "   acm_certificate_arn = \"$CERT_ARN\""
else
    echo "⚠️  ACM certificate not found for *.turafapp.com"
    echo "   Using placeholder in terraform.tfvars"
    echo "   Note: ALB will not work without valid certificate"
fi
echo ""

# Check terraform.tfvars exists
echo "📋 Checking Terraform configuration..."
TFVARS_FILE="${SCRIPT_DIR}/../terraform/environments/${ENV}/terraform.tfvars"
if [ -f "$TFVARS_FILE" ]; then
    echo "✅ terraform.tfvars exists"
else
    echo "⚠️  terraform.tfvars not found"
    echo "   Copy from terraform.tfvars.demo-ready or terraform.tfvars.example"
fi
echo ""

echo "=========================================="
echo "Prerequisites Check Complete"
echo "=========================================="
echo ""
echo "✅ Ready to deploy $ENV environment"
echo ""
