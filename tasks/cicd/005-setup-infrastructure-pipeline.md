# Task: Setup Infrastructure Pipeline

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 2 hours  

## Objective

Setup GitHub Actions pipeline for Terraform infrastructure deployment.

## Prerequisites

- [x] Terraform modules created

## Scope

**Files to Create**:
- `.github/workflows/terraform.yml`

## Implementation Details

### Terraform Workflow

```yaml
name: Terraform

on:
  pull_request:
    paths:
      - 'infrastructure/terraform/**'
  push:
    branches: [main]
    paths:
      - 'infrastructure/terraform/**'

jobs:
  terraform:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v2
        with:
          terraform_version: 1.5.0
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
          aws-region: us-east-1
      
      - name: Terraform Init
        run: terraform init
        working-directory: ./infrastructure/terraform
      
      - name: Terraform Plan
        run: terraform plan -var-file=environments/dev/terraform.tfvars
        working-directory: ./infrastructure/terraform
      
      - name: Terraform Apply
        if: github.ref == 'refs/heads/main'
        run: terraform apply -auto-approve -var-file=environments/dev/terraform.tfvars
        working-directory: ./infrastructure/terraform
```

## Acceptance Criteria

- [x] Terraform workflow created
- [x] Plan runs on infrastructure changes
- [x] Apply runs on main branch
- [x] Multi-environment support (DEV, QA, PROD)
- [x] Manual approval for QA and PROD
- [x] Deployment summary with outputs

## References

- Specification: `specs/ci-cd-pipelines.md` (Infrastructure Pipeline section)
- Related Tasks: 006-setup-security-scanning
