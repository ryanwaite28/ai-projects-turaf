# GitHub Configuration

**Repository**: ryanwaite28/ai-projects-turaf  
**Configured**: 2024-03-23  
**Status**: Ready for CI/CD

---

## Overview

GitHub repository configured with environments, secrets, and protection rules for secure multi-environment CI/CD deployments using GitHub Actions and AWS OIDC authentication.

---

## Environments

| Environment | Protection Rules | Deployment Branch | Required Approvers | Auto-Deploy |
|-------------|-----------------|-------------------|-------------------|-------------|
| dev | None | dev | 0 | ✅ Yes |
| qa | None | qa | 0 | ✅ Yes |
| prod | Required reviewers | main | 1 | ❌ Manual approval |

### Environment Configuration

**Dev Environment**:
- **Purpose**: Development testing and integration
- **Deployment**: Automatic on push to `dev` branch
- **Protection**: None (fast iteration)
- **AWS Account**: 801651112319 (turaf-dev)

**QA Environment**:
- **Purpose**: Quality assurance and staging
- **Deployment**: Automatic on push to `qa` branch
- **Protection**: None (automated testing)
- **AWS Account**: 965932217544 (turaf-qa)

**Prod Environment**:
- **Purpose**: Production workloads
- **Deployment**: Manual approval required
- **Protection**: 1 required reviewer, main branch only
- **AWS Account**: 811783768245 (turaf-prod)

---

## Repository Secrets

Repository-level secrets (available to all workflows):

| Secret Name | Description | Required |
|-------------|-------------|----------|
| SONARQUBE_TOKEN | SonarQube authentication token for code quality scanning | Optional |
| SLACK_WEBHOOK_URL | Slack webhook for deployment notifications | Optional |

**Note**: These are optional and can be added later when SonarQube and Slack integrations are set up.

---

## Environment Secrets

Each environment has the following secrets configured:

### Dev Environment Secrets

| Secret Name | Value | Description |
|-------------|-------|-------------|
| AWS_ROLE_ARN | arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole | IAM role for OIDC authentication |
| AWS_ACCOUNT_ID | 801651112319 | Dev AWS account ID |
| AWS_REGION | us-east-1 | AWS region for deployments |
| ECR_REGISTRY | 801651112319.dkr.ecr.us-east-1.amazonaws.com | ECR registry URL |
| ECS_CLUSTER | turaf-cluster-dev | ECS cluster name |

### QA Environment Secrets

| Secret Name | Value | Description |
|-------------|-------|-------------|
| AWS_ROLE_ARN | arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole | IAM role for OIDC authentication |
| AWS_ACCOUNT_ID | 965932217544 | QA AWS account ID |
| AWS_REGION | us-east-1 | AWS region for deployments |
| ECR_REGISTRY | 965932217544.dkr.ecr.us-east-1.amazonaws.com | ECR registry URL |
| ECS_CLUSTER | turaf-cluster-qa | ECS cluster name |

### Prod Environment Secrets

| Secret Name | Value | Description |
|-------------|-------|-------------|
| AWS_ROLE_ARN | arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole | IAM role for OIDC authentication |
| AWS_ACCOUNT_ID | 811783768245 | Prod AWS account ID |
| AWS_REGION | us-east-1 | AWS region for deployments |
| ECR_REGISTRY | 811783768245.dkr.ecr.us-east-1.amazonaws.com | ECR registry URL |
| ECS_CLUSTER | turaf-cluster-prod | ECS cluster name |

---

## Environment Variables (Optional)

Non-sensitive configuration values:

### Dev Environment Variables

| Variable Name | Value | Description |
|---------------|-------|-------------|
| ENVIRONMENT | dev | Environment identifier |
| LOG_LEVEL | debug | Logging verbosity |
| ENABLE_DEBUG | true | Enable debug features |

### QA Environment Variables

| Variable Name | Value | Description |
|---------------|-------|-------------|
| ENVIRONMENT | qa | Environment identifier |
| LOG_LEVEL | info | Logging verbosity |
| ENABLE_DEBUG | false | Disable debug features |

### Prod Environment Variables

| Variable Name | Value | Description |
|---------------|-------|-------------|
| ENVIRONMENT | prod | Environment identifier |
| LOG_LEVEL | warn | Logging verbosity (warnings only) |
| ENABLE_DEBUG | false | Disable debug features |

---

## Branch Protection Rules

### Main Branch (Production)

**Branch pattern**: `main`

**Protection rules**:
- ✅ Require pull request before merging
  - Required approvals: 1
  - Dismiss stale reviews when new commits are pushed
- ✅ Require status checks to pass before merging
  - Required checks:
    - `build`
    - `test`
    - `sonarqube-scan` (when configured)
    - `security-scan`
- ✅ Require conversation resolution before merging
- ✅ Require linear history
- ✅ Do not allow bypassing the above settings
- ✅ Restrict who can push to matching branches

**Deployment**: Requires manual approval in prod environment

### QA Branch (Staging)

**Branch pattern**: `qa`

**Protection rules**:
- ✅ Require pull request before merging
  - Required approvals: 1
- ✅ Require status checks to pass before merging
  - Required checks:
    - `build`
    - `test`

**Deployment**: Automatic to QA environment

### Dev Branch (Development)

**Branch pattern**: `dev`

**Protection rules**:
- ✅ Require status checks to pass before merging (optional)
  - Required checks:
    - `build`
    - `test`

**Deployment**: Automatic to dev environment

---

## Setup Instructions

### Automated Setup (Recommended)

Use the provided automation script:

```bash
# Ensure GitHub CLI is installed
brew install gh  # macOS
# or see https://github.com/cli/cli#installation

# Authenticate with GitHub
gh auth login

# Run setup script
chmod +x scripts/setup-github-environments.sh
./scripts/setup-github-environments.sh
```

This script will:
1. Create dev, qa, and prod environments
2. Configure all environment secrets
3. Verify the configuration

### Manual Setup

#### 1. Create Environments

1. Navigate to https://github.com/ryanwaite28/ai-projects-turaf
2. Go to **Settings** → **Environments**
3. Click **New environment**
4. Create three environments: `dev`, `qa`, `prod`

#### 2. Configure Prod Environment Protection

1. Go to **Settings** → **Environments** → **prod**
2. Enable **Required reviewers**
3. Add yourself as a required reviewer
4. Set **Deployment branches** to "Selected branches"
5. Add `main` branch

#### 3. Add Environment Secrets

For each environment (dev, qa, prod):

1. Go to **Settings** → **Environments** → **[environment]**
2. Click **Add secret**
3. Add the 5 secrets listed above for each environment

#### 4. Configure Branch Protection

1. Go to **Settings** → **Branches**
2. Click **Add branch protection rule**
3. Configure rules for `main`, `qa`, and `dev` branches as specified above

---

## Usage in GitHub Actions

### Basic Workflow Structure

```yaml
name: Deploy to Environment

on:
  push:
    branches: [dev, qa, main]

permissions:
  id-token: write  # Required for OIDC
  contents: read

jobs:
  deploy-dev:
    if: github.ref == 'refs/heads/dev'
    runs-on: ubuntu-latest
    environment: dev  # Uses dev environment secrets
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ secrets.AWS_REGION }}
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      
      - name: Build and push Docker image
        env:
          ECR_REGISTRY: ${{ secrets.ECR_REGISTRY }}
          IMAGE_TAG: dev-${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/turaf/identity-service:$IMAGE_TAG .
          docker push $ECR_REGISTRY/turaf/identity-service:$IMAGE_TAG
      
      - name: Deploy to ECS
        env:
          CLUSTER: ${{ secrets.ECS_CLUSTER }}
        run: |
          aws ecs update-service \
            --cluster $CLUSTER \
            --service identity-service \
            --force-new-deployment
```

### Multi-Environment Deployment

```yaml
name: Multi-Environment Deployment

on:
  push:
    branches: [dev, qa, main]

permissions:
  id-token: write
  contents: read

jobs:
  deploy-dev:
    if: github.ref == 'refs/heads/dev'
    runs-on: ubuntu-latest
    environment: dev
    steps:
      # ... deployment steps

  deploy-qa:
    if: github.ref == 'refs/heads/qa'
    runs-on: ubuntu-latest
    environment: qa
    steps:
      # ... deployment steps

  deploy-prod:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: prod  # Requires manual approval
    steps:
      # ... deployment steps
```

---

## Verification

### Verify Environments

```bash
# List all environments
gh api repos/ryanwaite28/ai-projects-turaf/environments \
  --jq '.environments[].name'

# Expected output:
# dev
# qa
# prod
```

### Verify Secrets

```bash
# List repository secrets
gh secret list --repo ryanwaite28/ai-projects-turaf

# List environment secrets
gh secret list --env dev --repo ryanwaite28/ai-projects-turaf
gh secret list --env qa --repo ryanwaite28/ai-projects-turaf
gh secret list --env prod --repo ryanwaite28/ai-projects-turaf

# Expected: 5 secrets per environment
# AWS_ROLE_ARN, AWS_ACCOUNT_ID, AWS_REGION, ECR_REGISTRY, ECS_CLUSTER
```

### Test Secrets in Workflow

Create `.github/workflows/test-secrets.yml`:

```yaml
name: Test Secrets

on:
  workflow_dispatch:

permissions:
  id-token: write
  contents: read

jobs:
  test-dev:
    runs-on: ubuntu-latest
    environment: dev
    steps:
      - name: Test secrets
        run: |
          echo "Environment: dev"
          echo "AWS Account: ${{ secrets.AWS_ACCOUNT_ID }}"
          echo "AWS Region: ${{ secrets.AWS_REGION }}"
          echo "ECR Registry: ${{ secrets.ECR_REGISTRY }}"
          echo "ECS Cluster: ${{ secrets.ECS_CLUSTER }}"
          echo "Role ARN: ${{ secrets.AWS_ROLE_ARN }}"
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ secrets.AWS_REGION }}
      
      - name: Verify AWS access
        run: |
          aws sts get-caller-identity
          echo "✅ OIDC authentication successful!"
```

Run manually:
```bash
gh workflow run test-secrets.yml
```

---

## Security Best Practices

### 1. Secret Management

- ✅ Use environment-specific secrets (not repository-wide)
- ✅ Rotate secrets regularly (every 90 days)
- ✅ Never log secret values
- ✅ Use GitHub's secret scanning
- ✅ Enable push protection for secrets

### 2. Environment Protection

- ✅ Require approvals for production
- ✅ Limit deployment branches
- ✅ Use wait timers for critical changes
- ✅ Review deployment logs regularly

### 3. Branch Protection

- ✅ Require pull requests for all branches
- ✅ Require status checks before merge
- ✅ Require conversation resolution
- ✅ Enforce linear history
- ✅ Restrict force pushes

### 4. Access Control

- ✅ Limit repository admin access
- ✅ Use teams for permissions
- ✅ Enable two-factor authentication
- ✅ Review access logs regularly

### 5. Audit Trail

- ✅ Monitor workflow runs
- ✅ Review deployment history
- ✅ Track secret access
- ✅ Set up alerts for failures

---

## Troubleshooting

### Issue: Cannot create environment

**Error**: "Resource not accessible by integration"

**Solution**:
- Verify you have admin access to the repository
- Check organization settings allow environments
- Ensure GitHub CLI is authenticated with correct permissions

### Issue: Secrets not accessible in workflow

**Error**: Secret value is empty in workflow

**Solution**:
```yaml
jobs:
  deploy:
    environment: dev  # Must specify environment to access env secrets
    steps:
      - name: Use secret
        run: echo ${{ secrets.AWS_ROLE_ARN }}
```

### Issue: OIDC authentication fails

**Error**: "Not authorized to perform sts:AssumeRoleWithWebIdentity"

**Solution**:
- Verify `AWS_ROLE_ARN` secret is correct
- Ensure workflow has `id-token: write` permission
- Check IAM role trust policy allows GitHub repository

### Issue: Branch protection prevents merge

**Error**: "Required status checks must pass"

**Solution**:
- Ensure all required checks are passing
- Update branch protection rules if checks are outdated
- Use `workflow_dispatch` for manual testing

---

## Maintenance

### Regular Tasks

**Weekly**:
- Review workflow run history
- Check for failed deployments
- Monitor secret access logs

**Monthly**:
- Review and update branch protection rules
- Audit environment protection settings
- Check for unused secrets

**Quarterly**:
- Rotate all secrets
- Review and update required status checks
- Audit repository access permissions

---

## References

- [GitHub Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment)
- [GitHub Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Branch Protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches)
- [GitHub CLI](https://cli.github.com/)
- [AWS OIDC with GitHub Actions](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- infrastructure/github-oidc-roles.md
- specs/ci-cd-pipelines.md
- INFRASTRUCTURE_PLAN.md (Phase 4)
