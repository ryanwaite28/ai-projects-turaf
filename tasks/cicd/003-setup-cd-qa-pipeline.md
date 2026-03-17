# Task: Setup CD QA Pipeline

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 2 hours  

## Objective

Setup GitHub Actions CD pipeline for deploying to QA environment with approval.

## Prerequisites

- [x] Task 002: CD DEV pipeline setup

## Scope

**Files to Create**:
- `.github/workflows/cd-qa.yml`

## Implementation Details

### CD QA Workflow

```yaml
name: CD - QA

on:
  workflow_dispatch:
  push:
    tags:
      - 'v*-qa'

env:
  AWS_REGION: us-east-1
  ECR_REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.us-east-1.amazonaws.com

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: qa
    permissions:
      id-token: write
      contents: read
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/GitHubActionsRole
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Deploy to QA
        run: |
          # Similar to DEV but for QA environment
          aws ecs update-service \
            --cluster turaf-cluster-qa \
            --service identity-service-qa \
            --force-new-deployment
```

## Acceptance Criteria

- [ ] CD QA workflow created
- [ ] Manual approval required
- [ ] Deployment to QA works
- [ ] Environment protection rules set

## References

- Specification: `specs/ci-cd-pipelines.md`
- Related Tasks: 004-setup-cd-prod-pipeline
