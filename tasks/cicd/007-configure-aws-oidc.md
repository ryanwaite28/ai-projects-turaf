# Task: Configure AWS OIDC

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 4 hours (across 4 AWS accounts)  

## Objective

Configure AWS OIDC federation for GitHub Actions to authenticate without long-lived credentials across all AWS accounts (DEV, QA, PROD, Ops).

## Prerequisites

- [ ] AWS account access for all accounts (801651112319, 965932217544, 811783768245, 146072879609)
- [ ] GitHub repository created: https://github.com/ryanwaite28/ai-projects-turaf
- [ ] Admin permissions in each AWS account

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/cicd/github-oidc.tf`
- `infrastructure/environments/dev/oidc.tf`
- `infrastructure/environments/qa/oidc.tf`
- `infrastructure/environments/prod/oidc.tf`
- `infrastructure/environments/ops/oidc.tf`

## Implementation Details

### OIDC Provider (Create in Each Account)

**Module**: `infrastructure/terraform/modules/cicd/github-oidc.tf`

```hcl
resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
  
  client_id_list = ["sts.amazonaws.com"]
  
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1"
  ]
  
  tags = {
    Name        = "GitHub-OIDC-Provider"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

resource "aws_iam_role" "github_actions" {
  name = "GitHubActionsDeploymentRole"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          "token.actions.githubusercontent.com:sub" = var.github_repo_branch_pattern
        }
      }
    }]
  })
  
  tags = {
    Name        = "GitHubActionsDeploymentRole"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

resource "aws_iam_role_policy_attachment" "github_actions_ecr" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser"
}

resource "aws_iam_role_policy_attachment" "github_actions_ecs" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonECS_FullAccess"
}

resource "aws_iam_role_policy" "github_actions_custom" {
  name = "GitHubActionsCustomPolicy"
  role = aws_iam_role.github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "lambda:UpdateFunctionCode",
          "lambda:GetFunction",
          "lambda:PublishVersion"
        ]
        Resource = "arn:aws:lambda:*:*:function:turaf-*"
      },
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:DeleteObject"
        ]
        Resource = [
          "arn:aws:s3:::turaf-*",
          "arn:aws:s3:::turaf-*/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "cloudfront:CreateInvalidation",
          "cloudfront:GetInvalidation"
        ]
        Resource = "*"
      }
    ]
  })
}

variable "environment" {
  type        = string
  description = "Environment name (dev, qa, prod, ops)"
}

variable "github_repo_branch_pattern" {
  type        = string
  description = "GitHub repository and branch pattern for OIDC trust policy"
}

output "oidc_provider_arn" {
  value = aws_iam_openid_connect_provider.github.arn
}

output "github_actions_role_arn" {
  value = aws_iam_role.github_actions.arn
}
```

### DEV Account Configuration (801651112319)

**File**: `infrastructure/environments/dev/oidc.tf`

```hcl
module "github_oidc" {
  source = "../../modules/cicd"
  
  environment = "Dev"
  github_repo_branch_pattern = "repo:ryanwaite28/ai-projects-turaf:ref:refs/heads/develop"
}

output "dev_github_actions_role_arn" {
  value       = module.github_oidc.github_actions_role_arn
  description = "ARN: arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole"
}
```

### QA Account Configuration (965932217544)

**File**: `infrastructure/environments/qa/oidc.tf`

```hcl
module "github_oidc" {
  source = "../../modules/cicd"
  
  environment = "QA"
  github_repo_branch_pattern = "repo:ryanwaite28/ai-projects-turaf:ref:refs/heads/release/*"
}

output "qa_github_actions_role_arn" {
  value       = module.github_oidc.github_actions_role_arn
  description = "ARN: arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole"
}
```

### PROD Account Configuration (811783768245)

**File**: `infrastructure/environments/prod/oidc.tf`

```hcl
module "github_oidc" {
  source = "../../modules/cicd"
  
  environment = "Prod"
  github_repo_branch_pattern = "repo:ryanwaite28/ai-projects-turaf:ref:refs/heads/main"
}

output "prod_github_actions_role_arn" {
  value       = module.github_oidc.github_actions_role_arn
  description = "ARN: arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole"
}
```

### Ops Account Configuration (146072879609)

**File**: `infrastructure/environments/ops/oidc.tf`

```hcl
module "github_oidc" {
  source = "../../modules/cicd"
  
  environment = "Ops"
  github_repo_branch_pattern = "repo:ryanwaite28/ai-projects-turaf:*"
}

output "ops_github_actions_role_arn" {
  value       = module.github_oidc.github_actions_role_arn
  description = "ARN: arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole"
}
```

## Deployment Steps

### Step 1: Deploy to DEV Account (801651112319)

```bash
# Configure AWS credentials for DEV account
export AWS_PROFILE=turaf-dev

# Navigate to DEV environment
cd infrastructure/environments/dev

# Initialize Terraform
terraform init

# Plan OIDC configuration
terraform plan -target=module.github_oidc

# Apply OIDC configuration
terraform apply -target=module.github_oidc
```

### Step 2: Deploy to QA Account (965932217544)

```bash
# Configure AWS credentials for QA account
export AWS_PROFILE=turaf-qa

# Navigate to QA environment
cd infrastructure/environments/qa

# Initialize and apply
terraform init
terraform apply -target=module.github_oidc
```

### Step 3: Deploy to PROD Account (811783768245)

```bash
# Configure AWS credentials for PROD account
export AWS_PROFILE=turaf-prod

# Navigate to PROD environment
cd infrastructure/environments/prod

# Initialize and apply
terraform init
terraform apply -target=module.github_oidc
```

### Step 4: Deploy to Ops Account (146072879609)

```bash
# Configure AWS credentials for Ops account
export AWS_PROFILE=turaf-ops

# Navigate to Ops environment (create if needed)
cd infrastructure/environments/ops

# Initialize and apply
terraform init
terraform apply -target=module.github_oidc
```

## Acceptance Criteria

- [ ] OIDC provider created in DEV account (801651112319)
- [ ] OIDC provider created in QA account (965932217544)
- [ ] OIDC provider created in PROD account (811783768245)
- [ ] OIDC provider created in Ops account (146072879609)
- [ ] IAM role `GitHubActionsDeploymentRole` configured with correct trust policy in DEV
- [ ] IAM role `GitHubActionsDeploymentRole` configured with correct trust policy in QA
- [ ] IAM role `GitHubActionsDeploymentRole` configured with correct trust policy in PROD
- [ ] IAM role `GitHubActionsDeploymentRole` configured with correct trust policy in Ops
- [ ] Trust policies scoped to repository: `ryanwaite28/ai-projects-turaf`
- [ ] DEV role restricted to `develop` branch
- [ ] QA role restricted to `release/*` branches
- [ ] PROD role restricted to `main` branch
- [ ] Ops role allows all branches
- [ ] Permissions granted for ECR, ECS, Lambda, S3, CloudFront
- [ ] GitHub Actions can assume roles in all accounts
- [ ] No long-lived credentials needed

## Testing Requirements

**Validation for Each Account**:

1. **Verify OIDC Provider**:
   ```bash
   aws iam list-open-id-connect-providers
   ```

2. **Verify IAM Role**:
   ```bash
   aws iam get-role --role-name GitHubActionsDeploymentRole
   ```

3. **Test GitHub Actions Authentication**:
   - Create test workflow in GitHub Actions
   - Attempt to assume role from appropriate branch
   - Verify successful authentication
   - Check CloudTrail logs for AssumeRoleWithWebIdentity events

4. **Verify Trust Policy Restrictions**:
   - Attempt to assume DEV role from `main` branch (should fail)
   - Attempt to assume PROD role from `develop` branch (should fail)
   - Verify only correct branch patterns can assume each role

## GitHub Secrets Configuration

After OIDC setup, configure GitHub environment secrets:

**DEV Environment** (`dev-environment`):
- `AWS_ACCOUNT_ID`: `801651112319`

**QA Environment** (`qa-environment`):
- `AWS_ACCOUNT_ID`: `965932217544`

**PROD Environment** (`prod-environment`):
- `AWS_ACCOUNT_ID`: `811783768245`

## References

- Specification: `specs/ci-cd-pipelines.md` (AWS OIDC section)
- GitHub Repository: https://github.com/ryanwaite28/ai-projects-turaf
- AWS Documentation: [Configuring OpenID Connect in Amazon Web Services](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc.html)
- Related Tasks: All CI/CD tasks
