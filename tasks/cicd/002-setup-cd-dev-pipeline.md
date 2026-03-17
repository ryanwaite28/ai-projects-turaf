# Task: Setup CD DEV Pipeline

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 3 hours  

## Objective

Setup GitHub Actions CD pipeline for deploying to DEV environment.

## Prerequisites

- [x] Task 001: CI pipeline setup
- [ ] AWS OIDC configured

## Scope

**Files to Create**:
- `.github/workflows/cd-dev.yml`

## Implementation Details

### CD DEV Workflow

```yaml
name: CD - DEV

on:
  push:
    branches: [develop]

env:
  AWS_REGION: us-east-1
  ECR_REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.us-east-1.amazonaws.com

jobs:
  deploy:
    runs-on: ubuntu-latest
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
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1
      
      - name: Build and push identity-service
        working-directory: ./services/identity-service
        run: |
          docker build -t $ECR_REGISTRY/identity-service:${{ github.sha }} .
          docker push $ECR_REGISTRY/identity-service:${{ github.sha }}
      
      - name: Update ECS service
        run: |
          aws ecs update-service \
            --cluster turaf-cluster-dev \
            --service identity-service-dev \
            --force-new-deployment
      
      - name: Wait for deployment
        run: |
          aws ecs wait services-stable \
            --cluster turaf-cluster-dev \
            --services identity-service-dev
```

## Acceptance Criteria

- [ ] CD DEV workflow created
- [ ] AWS OIDC authentication works
- [ ] Docker images built and pushed
- [ ] ECS services updated
- [ ] Deployment verification works
- [ ] Rollback on failure

## Testing Requirements

**Validation**:
- Push to develop branch
- Verify deployment to DEV
- Check ECS service status

## References

- Specification: `specs/ci-cd-pipelines.md` (CD Pipeline section)
- Related Tasks: 003-setup-cd-qa-pipeline
