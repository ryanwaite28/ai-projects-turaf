# Task: Setup CD PROD Pipeline

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 3 hours  

## Objective

Setup GitHub Actions CD pipeline for deploying to PROD environment with blue-green deployment.

## Prerequisites

- [x] Task 003: CD QA pipeline setup

## Scope

**Files to Create**:
- `.github/workflows/cd-prod.yml`

## Implementation Details

### CD PROD Workflow

```yaml
name: CD - PROD

on:
  release:
    types: [published]

env:
  AWS_REGION: us-east-1

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
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
      
      - name: Blue-Green Deployment
        run: |
          # Create new task definition
          # Update target group
          # Switch traffic
          # Monitor health
          # Rollback if needed
```

## Acceptance Criteria

- [ ] CD PROD workflow created
- [ ] Blue-green deployment implemented
- [ ] Health checks verified
- [ ] Automatic rollback on failure
- [ ] Production approval required

## References

- Specification: `specs/ci-cd-pipelines.md` (Blue-Green Deployment section)
- Related Tasks: 005-setup-infrastructure-pipeline
