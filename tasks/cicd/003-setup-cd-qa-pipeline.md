# Task: Setup CD QA Pipeline

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 4 hours  

## Objective

Setup GitHub Actions CD pipeline for deploying services to QA environment using service-specific Terraform with manual approval.

**Architecture Note**: Services now manage their own infrastructure (ECS service, task definition, target group, listener rule) via Terraform in CI/CD pipelines. Shared infrastructure (cluster, ALB, databases) is managed separately.

## Prerequisites

- [x] Task 002: CD DEV pipeline setup
- [ ] Shared infrastructure deployed to QA (ECS cluster, ALB, VPC)
- [ ] Service Terraform directories created (`services/<service>/terraform/`)
- [ ] QA environment protection rules configured in GitHub

## Scope

**Files to Create**:
- `.github/workflows/service-identity-qa.yml`
- `.github/workflows/service-organization-qa.yml`
- `.github/workflows/service-experiment-qa.yml`

## Implementation Details

### Service Deployment Workflow (Example: Identity Service to QA)

```yaml
name: Deploy Identity Service to QA

on:
  push:
    branches: [release/*]
    paths:
      - 'services/identity-service/**'
  workflow_dispatch:

env:
  AWS_REGION: us-east-1
  SERVICE_NAME: identity-service
  ENVIRONMENT: qa
  AWS_ACCOUNT_ID: 965932217544

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      
      - name: Build and push
        working-directory: services/${{ env.SERVICE_NAME }}
        run: |
          docker build -t ${{ steps.login-ecr.outputs.registry }}/turaf/${{ env.SERVICE_NAME }}:${{ github.sha }} .
          docker tag ${{ steps.login-ecr.outputs.registry }}/turaf/${{ env.SERVICE_NAME }}:${{ github.sha }} \
            ${{ steps.login-ecr.outputs.registry }}/turaf/${{ env.SERVICE_NAME }}:${{ env.ENVIRONMENT }}-latest
          docker push ${{ steps.login-ecr.outputs.registry }}/turaf/${{ env.SERVICE_NAME }}:${{ github.sha }}
          docker push ${{ steps.login-ecr.outputs.registry }}/turaf/${{ env.SERVICE_NAME }}:${{ env.ENVIRONMENT }}-latest

  deploy-service:
    runs-on: ubuntu-latest
    needs: build-and-push
    environment: qa  # Requires manual approval
    permissions:
      id-token: write
      contents: read
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
      
      - name: Terraform Init
        working-directory: services/${{ env.SERVICE_NAME }}/terraform
        run: terraform init
      
      - name: Terraform Plan
        working-directory: services/${{ env.SERVICE_NAME }}/terraform
        run: |
          terraform plan -out=tfplan \
            -var="environment=${{ env.ENVIRONMENT }}" \
            -var="image_tag=${{ github.sha }}" \
            -var="desired_count=2"
      
      - name: Terraform Apply
        working-directory: services/${{ env.SERVICE_NAME }}/terraform
        run: terraform apply -auto-approve tfplan

  verify-deployment:
    runs-on: ubuntu-latest
    needs: deploy-service
    permissions:
      id-token: write
      contents: read
    
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Wait for service stability
        run: |
          aws ecs wait services-stable \
            --cluster turaf-cluster-${{ env.ENVIRONMENT }} \
            --services ${{ env.SERVICE_NAME }}-${{ env.ENVIRONMENT }} \
            --region ${{ env.AWS_REGION }}
      
      - name: Run integration tests
        run: |
          # Run environment-specific integration tests
          ALB_DNS=$(aws elbv2 describe-load-balancers \
            --names turaf-alb-${{ env.ENVIRONMENT }} \
            --query 'LoadBalancers[0].DNSName' \
            --output text)
          
          # Health check
          curl -f http://$ALB_DNS/api/${{ env.SERVICE_NAME }}/health
          
          # Run smoke tests
          # npm run test:integration:qa
```

## Acceptance Criteria

- [ ] Service deployment workflows created for all services
- [ ] GitHub environment protection configured for QA (manual approval)
- [ ] Service Terraform directories configured for QA environment
- [ ] AWS OIDC authentication works for QA account
- [ ] Docker images built and pushed to QA ECR
- [ ] Service-specific Terraform deploys successfully
- [ ] ECS services created and running in QA
- [ ] ALB routing configured correctly
- [ ] Integration tests pass after deployment
- [ ] Manual approval workflow functions correctly
- [ ] Deployment circuit breaker configured
- [ ] Rollback on failure works

## Testing Requirements

**Validation**:
- Push changes to `release/*` branch
- Verify workflow triggers based on path filter
- Confirm manual approval is required
- Approve deployment in GitHub UI
- Check Docker image pushed to QA ECR
- Verify Terraform creates/updates service resources
- Check ECS service status and task count (desired: 2)
- Verify ALB target group shows healthy targets
- Test service endpoint via ALB
- Run integration tests against QA environment
- Verify CloudWatch logs are being written

## GitHub Environment Protection

Configure in GitHub repository settings:
- **Environment name**: `qa`
- **Required reviewers**: 1-2 team members
- **Wait timer**: Optional (e.g., 5 minutes)
- **Deployment branches**: `release/*` only

## References

- Specification: `specs/ci-cd-pipelines.md` (Service Deployment Pattern)
- Infrastructure Pattern: `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`
- Infrastructure Summary: `infrastructure/docs/architecture/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- Related Tasks: 002-setup-cd-dev-pipeline, 004-setup-cd-prod-pipeline
