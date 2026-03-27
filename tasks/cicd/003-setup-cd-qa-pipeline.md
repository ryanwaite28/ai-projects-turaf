# Task: QA Deployment Pattern Documentation

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 1 hour  
**Type**: Pattern Documentation

## Objective

Document the GitHub Actions deployment pattern for QA environment. This task describes the workflow structure, triggers, manual approval process, and deployment steps that services follow when deploying to QA.

**Note**: This task documents the PATTERN. For actual workflow implementation, see **Task 008: Setup Per-Service Deployment Workflows**.

**Architecture Note**: Services now manage their own infrastructure (ECS service, task definition, target group, listener rule) via Terraform in CI/CD pipelines. Shared infrastructure (cluster, ALB, databases) is managed separately.

## Prerequisites

- [x] Task 001: CI pipeline setup
- [x] Task 002: DEV deployment pattern documented
- [x] Task 007: AWS OIDC configured
- [ ] Task 005: Shared infrastructure deployed to QA (ECS cluster, ALB, VPC)
- [ ] Task 008: Per-service workflows implemented
- [ ] QA environment protection rules configured in GitHub

## Scope

**This task documents the pattern for**:
- QA environment workflow structure
- Trigger conditions (release/* branches, path filters)
- Manual approval requirements
- Build and push steps
- Terraform deployment steps
- Integration testing steps

**Actual workflow files are created in Task 008**.

## QA Deployment Pattern

### Workflow Structure

Each service follows this pattern for QA deployments:

**Trigger**: Push to `release/*` branches with changes to service directory  
**Environment**: QA (AWS Account: 965932217544)  
**Approval**: Required (manual approval via GitHub environment protection)  
**Security Scanning**: Enhanced (in CI pipeline)  
**Integration Tests**: Required after deployment

### Example Workflow Pattern (Identity Service)

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

## Pattern Characteristics

### QA-Specific Features
- **Manual Approval**: Required before deployment
- **Branch**: `release/*`
- **Frequency**: On push to release branches
- **Image Tags**: `${github.sha}` and `qa-latest`
- **Desired Count**: 2 tasks (high availability testing)
- **Circuit Breaker**: Enabled for auto-rollback
- **Integration Tests**: Run after deployment
- **Reviewers**: 1-2 team members required

### Workflow Jobs
1. **build-and-push**: Build Docker image and push to ECR
2. **deploy-service**: Run Terraform to deploy/update service infrastructure (requires approval)
3. **verify-deployment**: Wait for stability and run integration tests

## Acceptance Criteria

- [x] QA deployment pattern documented
- [x] Workflow structure defined
- [x] Trigger conditions specified
- [x] Manual approval process documented
- [x] Example workflow provided
- [ ] Pattern implemented in Task 008

## Validation Approach

**When implemented (Task 008), validate by**:
- Pushing changes to `release/*` branch
- Verifying workflow triggers based on path filter
- Confirming manual approval is required
- Approving deployment in GitHub UI
- Checking Docker image pushed to QA ECR with correct tags
- Verifying Terraform creates/updates service resources
- Checking ECS service status and task count (desired: 2)
- Verifying ALB target group shows healthy targets
- Testing service endpoint via ALB
- Running integration tests against QA environment
- Verifying CloudWatch logs are being written

## GitHub Environment Protection

Configure in GitHub repository settings:
- **Environment name**: `qa`
- **Required reviewers**: 1-2 team members
- **Wait timer**: Optional (e.g., 5 minutes)
- **Deployment branches**: `release/*` only

## References

- **Specification**: `specs/ci-cd-pipelines.md` (Service Deployment Pattern)
- **Implementation**: Task 008 (Setup Per-Service Deployment Workflows)
- **Related Patterns**: 
  - Task 002: DEV Deployment Pattern
  - Task 004: PROD Deployment Pattern
- **Infrastructure**: `infrastructure/docs/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- **IAM Roles**: `docs/IAM_ROLES.md`
