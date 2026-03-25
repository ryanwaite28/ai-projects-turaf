#!/bin/bash

set -e

REPO="ryanwaite28/ai-projects-turaf"

echo "========================================="
echo "GitHub Environments & Secrets Setup"
echo "========================================="
echo ""
echo "Repository: ${REPO}"
echo ""

# Check if GitHub CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed"
    echo ""
    echo "Install with:"
    echo "  macOS: brew install gh"
    echo "  Linux: See https://github.com/cli/cli#installation"
    echo ""
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "❌ Not authenticated with GitHub CLI"
    echo ""
    echo "Authenticate with:"
    echo "  gh auth login"
    echo ""
    exit 1
fi

echo "✅ GitHub CLI installed and authenticated"
echo ""

# Create environments
echo "Creating GitHub environments..."
echo ""

gh api repos/${REPO}/environments/dev -X PUT \
  --silent 2>/dev/null && echo "  ✅ dev environment created" || echo "  ℹ️  dev environment already exists"

gh api repos/${REPO}/environments/qa -X PUT \
  --silent 2>/dev/null && echo "  ✅ qa environment created" || echo "  ℹ️  qa environment already exists"

gh api repos/${REPO}/environments/prod -X PUT \
  --silent 2>/dev/null && echo "  ✅ prod environment created" || echo "  ℹ️  prod environment already exists"

echo ""
echo "Configuring environment secrets..."
echo ""

# Dev environment secrets
echo "  Configuring dev environment..."
gh secret set AWS_ROLE_ARN \
  --env dev \
  --body "arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole" \
  --repo ${REPO}

gh secret set AWS_ACCOUNT_ID \
  --env dev \
  --body "801651112319" \
  --repo ${REPO}

gh secret set AWS_REGION \
  --env dev \
  --body "us-east-1" \
  --repo ${REPO}

gh secret set ECR_REGISTRY \
  --env dev \
  --body "801651112319.dkr.ecr.us-east-1.amazonaws.com" \
  --repo ${REPO}

gh secret set ECS_CLUSTER \
  --env dev \
  --body "turaf-cluster-dev" \
  --repo ${REPO}

echo "  ✅ dev environment secrets configured (5 secrets)"

# QA environment secrets
echo "  Configuring qa environment..."
gh secret set AWS_ROLE_ARN \
  --env qa \
  --body "arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole" \
  --repo ${REPO}

gh secret set AWS_ACCOUNT_ID \
  --env qa \
  --body "965932217544" \
  --repo ${REPO}

gh secret set AWS_REGION \
  --env qa \
  --body "us-east-1" \
  --repo ${REPO}

gh secret set ECR_REGISTRY \
  --env qa \
  --body "965932217544.dkr.ecr.us-east-1.amazonaws.com" \
  --repo ${REPO}

gh secret set ECS_CLUSTER \
  --env qa \
  --body "turaf-cluster-qa" \
  --repo ${REPO}

echo "  ✅ qa environment secrets configured (5 secrets)"

# Prod environment secrets
echo "  Configuring prod environment..."
gh secret set AWS_ROLE_ARN \
  --env prod \
  --body "arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole" \
  --repo ${REPO}

gh secret set AWS_ACCOUNT_ID \
  --env prod \
  --body "811783768245" \
  --repo ${REPO}

gh secret set AWS_REGION \
  --env prod \
  --body "us-east-1" \
  --repo ${REPO}

gh secret set ECR_REGISTRY \
  --env prod \
  --body "811783768245.dkr.ecr.us-east-1.amazonaws.com" \
  --repo ${REPO}

gh secret set ECS_CLUSTER \
  --env prod \
  --body "turaf-cluster-prod" \
  --repo ${REPO}

echo "  ✅ prod environment secrets configured (5 secrets)"

echo ""
echo "========================================="
echo "✅ GitHub Environments & Secrets Setup Complete!"
echo "========================================="
echo ""
echo "Environments created:"
echo "  - dev"
echo "  - qa"
echo "  - prod"
echo ""
echo "Secrets configured per environment:"
echo "  - AWS_ROLE_ARN"
echo "  - AWS_ACCOUNT_ID"
echo "  - AWS_REGION"
echo "  - ECR_REGISTRY"
echo "  - ECS_CLUSTER"
echo ""
echo "Next steps:"
echo "  1. Configure prod environment protection rules (requires manual setup)"
echo "  2. Configure branch protection rules (requires manual setup)"
echo "  3. Optionally add repository secrets (SONARQUBE_TOKEN, SLACK_WEBHOOK_URL)"
echo ""
echo "Verify configuration:"
echo "  gh secret list --env dev"
echo "  gh secret list --env qa"
echo "  gh secret list --env prod"
echo ""
