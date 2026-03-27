# Task: PROD Deployment Pattern Documentation

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 1 hour  
**Type**: Pattern Documentation

## Objective

Document the GitHub Actions deployment pattern for PROD environment. This task describes the workflow structure, triggers, security scanning, blue-green deployment strategy, and strict approval gates that services follow when deploying to production.

**Note**: This task documents the PATTERN. For actual workflow implementation, see **Task 008: Setup Per-Service Deployment Workflows**.

**Architecture Note**: Services manage their own infrastructure via Terraform in CI/CD pipelines. Production deployments use ECS blue-green deployment with automatic rollback on failure.

## Prerequisites

- [x] Task 001: CI pipeline setup
- [x] Task 002: DEV deployment pattern documented
- [x] Task 003: QA deployment pattern documented
- [x] Task 007: AWS OIDC configured
- [ ] Task 005: Shared infrastructure deployed to PROD (ECS cluster, ALB, VPC)
- [ ] Task 008: Per-service workflows implemented
- [ ] PROD environment protection rules configured in GitHub
- [ ] Blue-green deployment strategy tested in QA

## Scope

**This task documents the pattern for**:
- PROD environment workflow structure
- Trigger conditions (main branch, releases, path filters)
- Security scanning requirements (Trivy)
- Manual approval requirements (2+ reviewers)
- Blue-green deployment strategy
- Build and push steps
- Terraform deployment steps
- E2E testing and monitoring steps

**Actual workflow files are created in Task 008**.

## PROD Deployment Pattern

### Workflow Structure

Each service follows this pattern for PROD deployments:

**Trigger**: Push to `main` branch or GitHub release with changes to service directory  
**Environment**: PROD (AWS Account: 811783768245)  
**Approval**: Required (2+ reviewers via GitHub environment protection)  
**Security Scanning**: Mandatory (Trivy - blocks on CRITICAL/HIGH vulnerabilities)  
**Deployment Strategy**: Blue-Green via ECS CODE_DEPLOY  
**E2E Tests**: Required after deployment  
**Monitoring**: 5-minute observation period with auto-rollback on alarms

### Example Workflow Pattern (Identity Service)

```yaml
name: Deploy Identity Service to PROD

on:
  push:
    branches: [main]
    paths:
      - 'services/identity-service/**'
  release:
    types: [published]
  workflow_dispatch:

env:
  AWS_REGION: us-east-1
  SERVICE_NAME: identity-service
  ENVIRONMENT: prod
  AWS_ACCOUNT_ID: 811783768245

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
      
      - name: Run security scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ steps.login-ecr.outputs.registry }}/turaf/${{ env.SERVICE_NAME }}:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'
      
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
    environment: production  # Requires manual approval + stricter rules
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
            -var="desired_count=3" \
            -var="deployment_type=blue_green"
      
      - name: Terraform Apply (Blue-Green)
        working-directory: services/${{ env.SERVICE_NAME }}/terraform
        run: terraform apply -auto-approve tfplan
        # ECS deployment_controller = CODE_DEPLOY enables blue-green

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
      
      - name: Run E2E tests
        run: |
          ALB_DNS=$(aws elbv2 describe-load-balancers \
            --names turaf-alb-${{ env.ENVIRONMENT }} \
            --query 'LoadBalancers[0].DNSName' \
            --output text)
          
          # Health check
          curl -f http://$ALB_DNS/api/${{ env.SERVICE_NAME }}/health
          
          # Run production smoke tests
          # npm run test:e2e:prod
      
      - name: Monitor deployment
        run: |
          # Monitor CloudWatch metrics for 5 minutes
          # Check error rates, latency, etc.
          # CodeDeploy will auto-rollback on alarm triggers
          sleep 300
          
          # Verify deployment succeeded
          DEPLOYMENT_STATUS=$(aws ecs describe-services \
            --cluster turaf-cluster-${{ env.ENVIRONMENT }} \
            --services ${{ env.SERVICE_NAME }}-${{ env.ENVIRONMENT }} \
            --query 'services[0].deployments[0].rolloutState' \
            --output text)
          
          if [ "$DEPLOYMENT_STATUS" != "COMPLETED" ]; then
            echo "Deployment did not complete successfully"
            exit 1
          fi
```

## Pattern Characteristics

### PROD-Specific Features
- **Strict Approval**: 2+ senior reviewers required
- **Branch**: `main` (or GitHub releases)
- **Frequency**: Controlled releases only
- **Image Tags**: `${github.sha}` and `prod-latest`
- **Desired Count**: 3 tasks (high availability)
- **Security Scanning**: Trivy (blocks CRITICAL/HIGH)
- **Deployment Strategy**: Blue-Green (CODE_DEPLOY)
- **Traffic Shifting**: Linear or Canary
- **Auto-Rollback**: Enabled on CloudWatch alarms
- **E2E Tests**: Required post-deployment
- **Monitoring Period**: 5 minutes
- **Wait Timer**: 10-minute cooling-off period

### Workflow Jobs
1. **build-and-push**: Security scan, build Docker image, push to ECR
2. **deploy-service**: Run Terraform for blue-green deployment (requires approval)
3. **verify-deployment**: Wait for stability, run E2E tests, monitor metrics

## Acceptance Criteria

- [x] PROD deployment pattern documented
- [x] Workflow structure defined
- [x] Trigger conditions specified
- [x] Security scanning requirements documented
- [x] Blue-green deployment strategy documented
- [x] Manual approval process documented (2+ reviewers)
- [x] Example workflow provided
- [ ] Pattern implemented in Task 008

## Validation Approach

**When implemented (Task 008), validate by**:
- Pushing changes to `main` branch or creating GitHub release
- Verifying workflow triggers based on path filter
- Confirming manual approval is required (2+ reviewers)
- Approving deployment in GitHub UI
- Verifying security scan passes (no CRITICAL/HIGH vulnerabilities)
- Checking Docker image pushed to PROD ECR with correct tags
- Verifying Terraform creates blue-green deployment
- Monitoring CodeDeploy deployment progress
- Checking ECS service shows two task sets during deployment
- Verifying ALB traffic shifts to new task set
- Testing service endpoint via ALB
- Running E2E tests against PROD environment
- Verifying CloudWatch alarms don't trigger
- Confirming old task set is terminated after success
- Testing rollback by introducing failing health checks

## GitHub Environment Protection

Configure in GitHub repository settings:
- **Environment name**: `production`
- **Required reviewers**: 2+ senior team members
- **Wait timer**: 10 minutes (cooling-off period)
- **Deployment branches**: `main` only
- **Prevent self-review**: Enabled
- **Required status checks**: All CI tests must pass

## Blue-Green Deployment Configuration

**ECS Service Terraform**:
```hcl
deployment_controller {
  type = "CODE_DEPLOY"
}

deployment_configuration {
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }
}
```

**CodeDeploy Configuration**:
- Traffic shifting: Linear10PercentEvery1Minute or Canary10Percent5Minutes
- Automatic rollback on CloudWatch alarms
- Termination wait time: 5 minutes

## References

- **Specification**: `specs/ci-cd-pipelines.md` (Service Deployment Pattern, Blue-Green section)
- **Implementation**: Task 008 (Setup Per-Service Deployment Workflows)
- **Related Patterns**: 
  - Task 002: DEV Deployment Pattern
  - Task 003: QA Deployment Pattern
- **Infrastructure**: `infrastructure/docs/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- **IAM Roles**: `docs/IAM_ROLES.md`
