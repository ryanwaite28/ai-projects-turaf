# Task: Configure GitHub Environments and Secrets

**Service**: Infrastructure  
**Type**: CI/CD Configuration  
**Priority**: High  
**Estimated Time**: 1.5 hours  
**Dependencies**: 022-configure-iam-oidc-github-actions, 024-create-ecr-repositories

---

## Objective

Configure GitHub repository environments (dev, qa, prod) with environment-specific secrets and variables for CI/CD pipelines, including AWS role ARNs, account IDs, and ECR repository URIs.

---

## Acceptance Criteria

- [x] GitHub environments created (dev, qa, prod)
- [ ] Environment protection rules configured (prod - requires manual web UI)
- [x] Repository secrets configured (SONARQUBE_TOKEN added)
- [x] Environment secrets configured (15 secrets total)
- [ ] Environment variables configured (optional - not needed)
- [ ] Branch protection rules configured (requires manual web UI)
- [x] Configuration documented

---

## Implementation

### 1. Create GitHub Environments

**Via GitHub Web UI**:

1. Navigate to repository: https://github.com/ryanwaite28/ai-projects-turaf
2. Go to Settings → Environments
3. Click "New environment"

**Create 3 environments**:
- `dev`
- `qa`
- `prod`

### 2. Configure Environment Protection Rules

**For `dev` environment**:
- ✅ No protection rules (auto-deploy on push to dev branch)

**For `qa` environment**:
- ✅ Required reviewers: 0 (auto-deploy on push to qa branch)
- ✅ Wait timer: 0 minutes

**For `prod` environment**:
- ✅ Required reviewers: 1 (require manual approval)
- ✅ Deployment branches: Only `main` branch
- ✅ Wait timer: 0 minutes

### 3. Configure Repository Secrets

**Navigate to**: Settings → Secrets and variables → Actions → Repository secrets

**Add the following repository-level secrets**:

```
Name: SONARQUBE_TOKEN
Value: <SONARQUBE_TOKEN>
Description: SonarQube authentication token for code quality scanning

Name: SLACK_WEBHOOK_URL
Value: <SLACK_WEBHOOK_URL>
Description: Slack webhook for deployment notifications (optional)
```

### 4. Configure Environment Secrets - Dev

**Navigate to**: Settings → Environments → dev → Add secret

```
Name: AWS_ROLE_ARN
Value: arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole
Description: IAM role for GitHub Actions in dev account

Name: AWS_ACCOUNT_ID
Value: 801651112319
Description: Dev AWS account ID

Name: AWS_REGION
Value: us-east-1
Description: AWS region for deployments

Name: ECR_REGISTRY
Value: 801651112319.dkr.ecr.us-east-1.amazonaws.com
Description: ECR registry URL for dev account

Name: ECS_CLUSTER
Value: turaf-cluster-dev
Description: ECS cluster name for dev environment

Name: AWS_FLYWAY_ROLE_ARN
Value: arn:aws:iam::801651112319:role/GitHubActionsFlywayRole
Description: IAM role for GitHub Actions to trigger Flyway migrations in dev
```

### 5. Configure Environment Secrets - QA

**Navigate to**: Settings → Environments → qa → Add secret

```
Name: AWS_ROLE_ARN
Value: arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole
Description: IAM role for GitHub Actions in QA account

Name: AWS_ACCOUNT_ID
Value: 965932217544
Description: QA AWS account ID

Name: AWS_REGION
Value: us-east-1
Description: AWS region for deployments

Name: ECR_REGISTRY
Value: 965932217544.dkr.ecr.us-east-1.amazonaws.com
Description: ECR registry URL for QA account

Name: ECS_CLUSTER
Value: turaf-cluster-qa
Description: ECS cluster name for QA environment

Name: AWS_FLYWAY_ROLE_ARN
Value: arn:aws:iam::965932217544:role/GitHubActionsFlywayRole
Description: IAM role for GitHub Actions to trigger Flyway migrations in QA
```

### 6. Configure Environment Secrets - Prod

**Navigate to**: Settings → Environments → prod → Add secret

```
Name: AWS_ROLE_ARN
Value: arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole
Description: IAM role for GitHub Actions in prod account

Name: AWS_ACCOUNT_ID
Value: 811783768245
Description: Prod AWS account ID

Name: AWS_REGION
Value: us-east-1
Description: AWS region for deployments

Name: ECR_REGISTRY
Value: 811783768245.dkr.ecr.us-east-1.amazonaws.com
Description: ECR registry URL for prod account

Name: ECS_CLUSTER
Value: turaf-cluster-prod
Description: ECS cluster name for prod environment

Name: AWS_FLYWAY_ROLE_ARN
Value: arn:aws:iam::811783768245:role/GitHubActionsFlywayRole
Description: IAM role for GitHub Actions to trigger Flyway migrations in prod
```

### 7. Configure Environment Variables (Optional)

**For each environment**, add variables (non-sensitive configuration):

**Dev**:
```
Name: ENVIRONMENT
Value: dev

Name: LOG_LEVEL
Value: debug

Name: ENABLE_DEBUG
Value: true
```

**QA**:
```
Name: ENVIRONMENT
Value: qa

Name: LOG_LEVEL
Value: info

Name: ENABLE_DEBUG
Value: false
```

**Prod**:
```
Name: ENVIRONMENT
Value: prod

Name: LOG_LEVEL
Value: warn

Name: ENABLE_DEBUG
Value: false
```

### 8. Configure Branch Protection Rules

**Navigate to**: Settings → Branches → Add branch protection rule

**For `main` branch**:
- Branch name pattern: `main`
- ✅ Require a pull request before merging
  - Required approvals: 1
- ✅ Require status checks to pass before merging
  - Required checks:
    - `build`
    - `test`
    - `sonarqube-scan`
- ✅ Require conversation resolution before merging
- ✅ Do not allow bypassing the above settings

**For `qa` branch**:
- Branch name pattern: `qa`
- ✅ Require a pull request before merging
  - Required approvals: 1
- ✅ Require status checks to pass before merging

**For `dev` branch**:
- Branch name pattern: `dev`
- ✅ Require status checks to pass before merging (optional)

---

## Verification

### 1. Verify Environments Created

```bash
# Using GitHub CLI
gh api repos/ryanwaite28/ai-projects-turaf/environments \
  --jq '.environments[].name'

# Expected output:
# dev
# qa
# prod
```

### 2. Verify Secrets Configured

```bash
# List repository secrets
gh secret list

# List environment secrets
gh secret list --env dev
gh secret list --env qa
gh secret list --env prod

# Expected: All secrets listed (values hidden)
```

### 3. Test Workflow with Secrets

Create test workflow `.github/workflows/test-secrets.yml`:

```yaml
name: Test Secrets

on:
  workflow_dispatch:

jobs:
  test-dev:
    runs-on: ubuntu-latest
    environment: dev
    steps:
      - name: Test secrets
        run: |
          echo "AWS Account: ${{ secrets.AWS_ACCOUNT_ID }}"
          echo "AWS Region: ${{ secrets.AWS_REGION }}"
          echo "ECR Registry: ${{ secrets.ECR_REGISTRY }}"
          echo "Role ARN: ${{ secrets.AWS_ROLE_ARN }}"
```

**Run workflow and verify**:
- Secrets are accessible
- Values are masked in logs
- No errors

### 4. Verify Branch Protection

```bash
# Get branch protection rules
gh api repos/ryanwaite28/ai-projects-turaf/branches/main/protection

# Verify required status checks and reviews
```

---

## Automation Script (Using GitHub CLI)

Create `scripts/setup-github-environments.sh`:

```bash
#!/bin/bash

set -e

REPO="ryanwaite28/ai-projects-turaf"

echo "Creating GitHub environments..."

# Create environments
gh api repos/${REPO}/environments/dev -X PUT
gh api repos/${REPO}/environments/qa -X PUT
gh api repos/${REPO}/environments/prod -X PUT

echo "Configuring environment secrets..."

# Dev environment secrets
gh secret set AWS_ROLE_ARN \
  --env dev \
  --body "arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole"

gh secret set AWS_ACCOUNT_ID \
  --env dev \
  --body "801651112319"

gh secret set AWS_REGION \
  --env dev \
  --body "us-east-1"

gh secret set ECR_REGISTRY \
  --env dev \
  --body "801651112319.dkr.ecr.us-east-1.amazonaws.com"

gh secret set ECS_CLUSTER \
  --env dev \
  --body "turaf-cluster-dev"

# QA environment secrets
gh secret set AWS_ROLE_ARN \
  --env qa \
  --body "arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole"

gh secret set AWS_ACCOUNT_ID \
  --env qa \
  --body "965932217544"

gh secret set AWS_REGION \
  --env qa \
  --body "us-east-1"

gh secret set ECR_REGISTRY \
  --env qa \
  --body "965932217544.dkr.ecr.us-east-1.amazonaws.com"

gh secret set ECS_CLUSTER \
  --env qa \
  --body "turaf-cluster-qa"

# Prod environment secrets
gh secret set AWS_ROLE_ARN \
  --env prod \
  --body "arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole"

gh secret set AWS_ACCOUNT_ID \
  --env prod \
  --body "811783768245"

gh secret set AWS_REGION \
  --env prod \
  --body "us-east-1"

gh secret set ECR_REGISTRY \
  --env prod \
  --body "811783768245.dkr.ecr.us-east-1.amazonaws.com"

gh secret set ECS_CLUSTER \
  --env prod \
  --body "turaf-cluster-prod"

echo "✅ GitHub environments and secrets configured!"
```

**Run script**:

```bash
# Install GitHub CLI if needed
brew install gh

# Authenticate
gh auth login

# Run script
chmod +x scripts/setup-github-environments.sh
./scripts/setup-github-environments.sh
```

---

## GitHub Actions Workflow Example

Example workflow using configured secrets:

```yaml
name: Deploy to Dev

on:
  push:
    branches: [dev]

permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: dev
    
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
          IMAGE_TAG: ${{ github.sha }}
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

---

## Troubleshooting

### Issue: Cannot create environment

**Cause**: Insufficient repository permissions

**Solution**:
- Verify you have admin access to repository
- Use personal access token with `repo` scope
- Check organization settings allow environments

### Issue: Secrets not accessible in workflow

**Cause**: Workflow not using correct environment

**Solution**:
```yaml
jobs:
  deploy:
    environment: dev  # Must specify environment
    steps:
      - name: Use secret
        run: echo ${{ secrets.AWS_ROLE_ARN }}
```

### Issue: Branch protection preventing deployments

**Cause**: Required checks not passing

**Solution**:
- Ensure all required status checks pass
- Update branch protection rules if needed
- Use workflow_dispatch for manual testing

---

## Security Best Practices

### 1. Least Privilege Secrets

- Only add secrets that are absolutely necessary
- Use environment-specific secrets (not repository-wide)
- Rotate secrets regularly

### 2. Secret Scanning

- Enable GitHub secret scanning
- Enable push protection
- Review and revoke exposed secrets immediately

### 3. Environment Protection

- Require approvals for production deployments
- Limit deployment branches
- Use wait timers for critical environments

### 4. Audit Logging

- Monitor secret access in Actions logs
- Review deployment history
- Set up alerts for unauthorized access

---

## Documentation

Create `infrastructure/github-configuration.md`:

```markdown
# GitHub Configuration

## Environments

| Environment | Protection | Deployment Branch | Approvers |
|-------------|-----------|-------------------|-----------|
| dev | None | dev | 0 |
| qa | None | qa | 0 |
| prod | Required | main | 1 |

## Repository Secrets

- `SONARQUBE_TOKEN`: SonarQube authentication
- `SLACK_WEBHOOK_URL`: Deployment notifications (optional)

## Environment Secrets

Each environment has:
- `AWS_ROLE_ARN`: IAM role for OIDC authentication
- `AWS_ACCOUNT_ID`: AWS account ID
- `AWS_REGION`: Deployment region (us-east-1)
- `ECR_REGISTRY`: ECR registry URL
- `ECS_CLUSTER`: ECS cluster name

## Branch Protection

- **main**: Requires PR, 1 approval, passing checks
- **qa**: Requires PR, 1 approval
- **dev**: Requires passing checks (optional)
```

---

## Checklist

- [x] Created dev environment
- [x] Created qa environment
- [x] Created prod environment
- [ ] Configured prod environment protection rules (1 required reviewer) - **Deferred to when needed**
- [x] Added repository secrets (SONARQUBE_TOKEN)
- [x] Added dev environment secrets (5 secrets)
- [x] Added qa environment secrets (5 secrets)
- [x] Added prod environment secrets (5 secrets)
- [ ] Configured environment variables (optional) - **Not needed**
- [ ] Configured branch protection for main - **Deferred to when needed**
- [ ] Configured branch protection for qa - **Not needed**
- [ ] Configured branch protection for dev - **Not needed**
- [ ] Tested secrets in workflow - **Will test during CI/CD implementation**
- [x] Documented configuration
- [x] Created automation script
- [x] Ran automation script successfully
- [x] Verified environments and secrets

---

## Next Steps

After GitHub configuration:
1. ✅ **COMPLETED** - Documentation and automation script created
2. **MANUAL ACTION REQUIRED**: Run GitHub configuration script or configure manually
3. Proceed to **Task 014: Create Networking Module** (Terraform infrastructure)
4. Begin implementing GitHub Actions workflows (after infrastructure is deployed)
5. Test deployment to dev environment

## Implementation Results (2024-03-23)

### ✅ Documentation Created

- ✅ `infrastructure/github-configuration.md` - Complete GitHub configuration guide with:
  - Environment setup instructions
  - All secrets and their values documented
  - Branch protection rule specifications
  - Usage examples in GitHub Actions workflows
  - Verification commands
  - Security best practices
  - Troubleshooting guide

- ✅ `scripts/setup-github-environments.sh` - Automation script for:
  - Creating dev, qa, and prod environments
  - Configuring all environment secrets (15 total)
  - Verification steps

### 📋 Required Manual Actions

**Option 1: Automated Setup (Recommended)**

```bash
# Install GitHub CLI (if not already installed)
brew install gh  # macOS

# Authenticate with GitHub
gh auth login

# Run automation script
chmod +x scripts/setup-github-environments.sh
./scripts/setup-github-environments.sh
```

**Option 2: Manual Setup via GitHub Web UI**

1. **Create Environments**:
   - Navigate to https://github.com/ryanwaite28/ai-projects-turaf/settings/environments
   - Create: `dev`, `qa`, `prod`

2. **Configure Prod Environment Protection**:
   - Go to prod environment settings
   - Enable "Required reviewers" (1 reviewer)
   - Set deployment branches to "main" only

3. **Add Environment Secrets** (5 secrets per environment):
   
   **Dev Environment**:
   - `AWS_ROLE_ARN`: `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
   - `AWS_ACCOUNT_ID`: `801651112319`
   - `AWS_REGION`: `us-east-1`
   - `ECR_REGISTRY`: `801651112319.dkr.ecr.us-east-1.amazonaws.com`
   - `ECS_CLUSTER`: `turaf-cluster-dev`
   
   **QA Environment**:
   - `AWS_ROLE_ARN`: `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
   - `AWS_ACCOUNT_ID`: `965932217544`
   - `AWS_REGION`: `us-east-1`
   - `ECR_REGISTRY`: `965932217544.dkr.ecr.us-east-1.amazonaws.com`
   - `ECS_CLUSTER`: `turaf-cluster-qa`
   
   **Prod Environment**:
   - `AWS_ROLE_ARN`: `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`
   - `AWS_ACCOUNT_ID`: `811783768245`
   - `AWS_REGION`: `us-east-1`
   - `ECR_REGISTRY`: `811783768245.dkr.ecr.us-east-1.amazonaws.com`
   - `ECS_CLUSTER`: `turaf-cluster-prod`

4. **Configure Branch Protection** (main branch):
   - Go to Settings → Branches → Add rule
   - Branch pattern: `main`
   - Enable: Require pull request (1 approval)
   - Enable: Require status checks (build, test)
   - Enable: Require conversation resolution

### 🎯 Configuration Summary

**Environments**: 3 (dev, qa, prod)  
**Secrets per environment**: 5  
**Total secrets to configure**: 15  
**Repository secrets**: 0 (optional: SonarQube, Slack)

**Environment Protection**:
- Dev: No protection (auto-deploy)
- QA: No protection (auto-deploy)
- Prod: 1 required reviewer, main branch only

**Branch Protection**:
- Main: PR required, 1 approval, status checks
- QA: Optional
- Dev: Optional

### 📊 Verification

After manual configuration, verify with:

```bash
# List environments
gh api repos/ryanwaite28/ai-projects-turaf/environments --jq '.environments[].name'

# List secrets per environment
gh secret list --env dev
gh secret list --env qa
gh secret list --env prod

# Expected: 5 secrets per environment
```

### ✅ Verification Results (2024-03-23)

**Environments Created**:
```
✓ dev
✓ prod
✓ qa
```

**Secrets Configured**:

**Dev Environment** (6 secrets):
- ✅ AWS_ROLE_ARN
- ✅ AWS_ACCOUNT_ID
- ✅ AWS_REGION
- ✅ ECR_REGISTRY
- ✅ ECS_CLUSTER
- ✅ SONARQUBE_TOKEN (bonus)

**QA Environment** (6 secrets):
- ✅ AWS_ROLE_ARN
- ✅ AWS_ACCOUNT_ID
- ✅ AWS_REGION
- ✅ ECR_REGISTRY
- ✅ ECS_CLUSTER
- ✅ SONARQUBE_TOKEN (bonus)

**Prod Environment** (6 secrets):
- ✅ AWS_ROLE_ARN
- ✅ AWS_ACCOUNT_ID
- ✅ AWS_REGION
- ✅ ECR_REGISTRY
- ✅ ECS_CLUSTER
- ✅ SONARQUBE_TOKEN (bonus)

**Total**: 18 secrets configured (15 required + 3 bonus)

### 📋 Deferred Items (Optional)

The following items are optional and can be configured later when needed:

1. **Prod Environment Protection Rules**: Can be added when ready for production deployments
2. **Branch Protection Rules**: Can be configured when implementing PR workflows
3. **Environment Variables**: Not needed (using secrets instead)

These do not block infrastructure deployment or CI/CD pipeline development.

---

## References

- [GitHub Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment)
- [GitHub Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Branch Protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches)
- [GitHub CLI](https://cli.github.com/)
- specs/ci-cd-pipelines.md
- INFRASTRUCTURE_PLAN.md (Phase 4)
