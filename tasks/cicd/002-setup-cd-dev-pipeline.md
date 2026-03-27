# Task: DEV Deployment Pattern Documentation

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 1 hour  
**Type**: Pattern Documentation

## Objective

Document the GitHub Actions deployment pattern for DEV environment. This task describes the workflow structure, triggers, and deployment steps that services follow when deploying to DEV.

**Note**: This task documents the PATTERN. For actual workflow implementation, see **Task 008: Setup Per-Service Deployment Workflows**.

**Architecture Note**: Services now manage their own infrastructure (ECS service, task definition, target group, listener rule) via Terraform in CI/CD pipelines. Shared infrastructure (cluster, ALB, databases) is managed separately.

## Prerequisites

- [x] Task 001: CI pipeline setup
- [x] Task 007: AWS OIDC configured
- [ ] Task 005: Shared infrastructure deployed (ECS cluster, ALB, VPC)
- [ ] Task 008: Per-service workflows implemented

## Scope

**This task documents the pattern for**:
- DEV environment workflow structure
- Trigger conditions (develop branch, path filters)
- Build and push steps
- Terraform deployment steps
- Verification and health check steps

**Actual workflow files are created in Task 008**.

## DEV Deployment Pattern

### Workflow Structure

Each service follows this pattern for DEV deployments:

**Trigger**: Push to `develop` branch with changes to service directory  
**Environment**: DEV (AWS Account: 801651112319)  
**Approval**: None (auto-deploy)  
**Security Scanning**: Basic (in CI pipeline)

### Example Workflow Pattern (Identity Service)

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

## Pattern Characteristics

### DEV-Specific Features
- **Auto-deployment**: No manual approval required
- **Branch**: `develop`
- **Frequency**: On every push to service directory
- **Image Tags**: `${github.sha}` and `dev-latest`
- **Desired Count**: 1 task (cost optimization)
- **Circuit Breaker**: Enabled for auto-rollback
- **Health Checks**: 30 attempts with 10s intervals

### Workflow Jobs
1. **build-and-push**: Build Docker image and push to ECR
2. **deploy-service**: Run Terraform to deploy/update service infrastructure
3. **verify-deployment**: Wait for stability and run health checks

## Acceptance Criteria

- [x] DEV deployment pattern documented
- [x] Workflow structure defined
- [x] Trigger conditions specified
- [x] Example workflow provided
- [ ] Pattern implemented in Task 008

## Validation Approach

**When implemented (Task 008), validate by**:
- Pushing changes to a service directory on `develop` branch
- Verifying workflow triggers based on path filter
- Checking Docker image pushed to ECR with correct tags
- Verifying Terraform creates/updates service resources
- Checking ECS service status and task count
- Verifying ALB target group shows healthy targets
- Testing service endpoint via ALB
- Verifying CloudWatch logs are being written

## References

- **Specification**: `specs/ci-cd-pipelines.md` (Service Deployment Pattern)
- **Implementation**: Task 008 (Setup Per-Service Deployment Workflows)
- **Related Patterns**: 
  - Task 003: QA Deployment Pattern
  - Task 004: PROD Deployment Pattern
- **Infrastructure**: `infrastructure/docs/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- **IAM Roles**: `docs/IAM_ROLES.md`
