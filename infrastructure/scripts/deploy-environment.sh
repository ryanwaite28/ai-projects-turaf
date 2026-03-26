#!/bin/bash

# Universal Deployment Script for All Environments
# Usage: ./deploy-environment.sh <env> [plan|apply|destroy]

set -e

ENV=$1
ACTION=${2:-plan}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="${SCRIPT_DIR}/../terraform/environments/${ENV}"

if [ -z "$ENV" ]; then
    echo "Usage: ./deploy-environment.sh <env> [plan|apply|destroy]"
    echo "  env: dev, qa, or prod"
    echo "  action: plan (default), apply, or destroy"
    exit 1
fi

if [ ! -d "$TERRAFORM_DIR" ]; then
    echo "❌ Environment directory not found: $TERRAFORM_DIR"
    exit 1
fi

echo "=========================================="
echo "Terraform Deployment: $ENV"
echo "Action: $ACTION"
echo "=========================================="
echo ""

# Run prerequisites check
echo "🔍 Running prerequisites check..."
"${SCRIPT_DIR}/check-prerequisites.sh" "$ENV"

# Assume role and export credentials for Terraform
echo ""
echo "🔐 Assuming role for Terraform..."
source "${SCRIPT_DIR}/assume-role.sh" "$ENV"

# Navigate to environment directory
cd "$TERRAFORM_DIR"

# Initialize Terraform
echo "=========================================="
echo "Initializing Terraform"
echo "=========================================="
echo ""
terraform init -upgrade

# Validate configuration
echo ""
echo "=========================================="
echo "Validating Terraform Configuration"
echo "=========================================="
echo ""
terraform validate

# Format check
echo ""
echo "=========================================="
echo "Checking Terraform Formatting"
echo "=========================================="
echo ""
terraform fmt -check -recursive || {
    echo "⚠️  Formatting issues found. Run 'terraform fmt -recursive' to fix."
}

# Execute action
case "$ACTION" in
    plan)
        echo ""
        echo "=========================================="
        echo "Generating Terraform Plan"
        echo "=========================================="
        echo ""
        terraform plan -out=tfplan
        
        echo ""
        echo "=========================================="
        echo "Plan Complete"
        echo "=========================================="
        echo ""
        echo "✅ Plan saved to: tfplan"
        echo ""
        echo "To apply this plan, run:"
        echo "  ./deploy-environment.sh $ENV apply"
        echo ""
        ;;
        
    apply)
        if [ ! -f "tfplan" ]; then
            echo "❌ No plan file found. Run plan first:"
            echo "   ./deploy-environment.sh $ENV plan"
            exit 1
        fi
        
        echo ""
        echo "=========================================="
        echo "Applying Terraform Plan"
        echo "=========================================="
        echo ""
        echo "⚠️  This will create/modify infrastructure in $ENV"
        echo ""
        read -p "Are you sure you want to proceed? (type 'yes' to confirm): " confirm
        
        if [ "$confirm" != "yes" ]; then
            echo "Apply cancelled."
            exit 0
        fi
        
        terraform apply tfplan
        
        # Remove plan file after successful apply
        rm -f tfplan
        
        echo ""
        echo "=========================================="
        echo "Deployment Complete!"
        echo "=========================================="
        echo ""
        echo "✅ Infrastructure deployed to $ENV"
        echo ""
        echo "Next steps:"
        echo "  1. Run verification: ./verify-environment.sh $ENV"
        echo "  2. Check outputs: terraform output"
        echo "  3. Setup database schemas (if RDS deployed)"
        echo ""
        ;;
        
    destroy)
        echo ""
        echo "=========================================="
        echo "Destroying Infrastructure"
        echo "=========================================="
        echo ""
        echo "⚠️  WARNING: This will DESTROY all infrastructure in $ENV"
        echo ""
        read -p "Are you ABSOLUTELY SURE? (type 'destroy-$ENV' to confirm): " confirm
        
        if [ "$confirm" != "destroy-$ENV" ]; then
            echo "Destroy cancelled."
            exit 0
        fi
        
        terraform destroy -auto-approve
        
        echo ""
        echo "=========================================="
        echo "Destruction Complete"
        echo "=========================================="
        echo ""
        echo "✅ All infrastructure destroyed in $ENV"
        echo ""
        ;;
        
    *)
        echo "❌ Invalid action: $ACTION"
        echo "   Valid actions: plan, apply, destroy"
        exit 1
        ;;
esac
