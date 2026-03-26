# Task: Setup CD DEV Pipeline

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 4 hours  

## Objective

Setup GitHub Actions CD pipeline for deploying services to DEV environment using service-specific Terraform.

**Architecture Note**: Services now manage their own infrastructure (ECS service, task definition, target group, listener rule) via Terraform in CI/CD pipelines. Shared infrastructure (cluster, ALB, databases) is managed separately.

## Prerequisites

- [x] Task 001: CI pipeline setup
- [ ] AWS OIDC configured
- [ ] Shared infrastructure deployed (ECS cluster, ALB, VPC)
- [ ] Service Terraform directories created (`services/<service>/terraform/`)

## Scope

**Files to Create**:
- `.github/workflows/service-identity-dev.yml`
- `.github/workflows/service-organization-dev.yml`
- `.github/workflows/service-experiment-dev.yml`
- `services/identity-service/terraform/` (backend.tf, data.tf, main.tf, variables.tf, outputs.tf)
- `services/organization-service/terraform/` (backend.tf, data.tf, main.tf, variables.tf, outputs.tf)
- `services/experiment-service/terraform/` (backend.tf, data.tf, main.tf, variables.tf, outputs.tf)

## Implementation Details

### Service Deployment Workflow (Example: Identity Service)

```yaml
name: Deploy Identity Service to DEV

on:
  push:
    branches: [develop]
    paths:
      - 'services/identity-service/**'
  workflow_dispatch:

env:
  AWS_REGION: us-east-1
  SERVICE_NAME: identity-service
  ENVIRONMENT: dev
  AWS_ACCOUNT_ID: 801651112319

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
            -var="desired_count=1"
      
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
      
      - name: Health check
        run: |
          ALB_DNS=$(aws elbv2 describe-load-balancers \
            --names turaf-alb-${{ env.ENVIRONMENT }} \
            --query 'LoadBalancers[0].DNSName' \
            --output text)
          
          for i in {1..30}; do
            if curl -f http://$ALB_DNS/api/${{ env.SERVICE_NAME }}/health; then
              echo "Service is healthy"
              exit 0
            fi
            sleep 10
          done
          exit 1
```

## Acceptance Criteria

- [ ] Service deployment workflows created for all services
- [ ] Service Terraform directories created with all required files
- [ ] AWS OIDC authentication works
- [ ] Docker images built and pushed to ECR
- [ ] Service-specific Terraform deploys successfully
- [ ] ECS services created and running
- [ ] ALB routing configured correctly
- [ ] Health checks pass
- [ ] Deployment circuit breaker configured
- [ ] Rollback on failure works

## Testing Requirements

**Validation**:
- Push changes to a service directory
- Verify workflow triggers based on path filter
- Check Docker image pushed to ECR
- Verify Terraform creates/updates service resources
- Check ECS service status and task count
- Verify ALB target group shows healthy targets
- Test service endpoint via ALB
- Verify CloudWatch logs are being written

## References

- Specification: `specs/ci-cd-pipelines-UPDATED.md` (Service Deployment Pattern)
- Infrastructure Pattern: `.windsurf/plans/cicd-service-deployment-pattern.md`
- Infrastructure Summary: `infrastructure/docs/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- Related Tasks: 003-setup-cd-qa-pipeline
