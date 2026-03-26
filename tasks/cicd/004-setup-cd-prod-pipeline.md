# Task: Setup CD PROD Pipeline

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 5 hours  

## Objective

Setup GitHub Actions CD pipeline for deploying services to PROD environment using service-specific Terraform with blue-green deployment strategy and strict approval gates.

**Architecture Note**: Services manage their own infrastructure via Terraform in CI/CD pipelines. Production deployments use ECS blue-green deployment with automatic rollback on failure.

## Prerequisites

- [x] Task 003: CD QA pipeline setup
- [ ] Shared infrastructure deployed to PROD (ECS cluster, ALB, VPC)
- [ ] Service Terraform directories created (`services/<service>/terraform/`)
- [ ] PROD environment protection rules configured in GitHub
- [ ] Blue-green deployment strategy tested in QA

## Scope

**Files to Create**:
- `.github/workflows/service-identity-prod.yml`
- `.github/workflows/service-organization-prod.yml`
- `.github/workflows/service-experiment-prod.yml`

## Implementation Details

### Service Deployment Workflow (Example: Identity Service to PROD)

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

## Acceptance Criteria

- [ ] Service deployment workflows created for all services
- [ ] GitHub environment protection configured for PROD (multiple approvers)
- [ ] Service Terraform configured with blue-green deployment
- [ ] AWS OIDC authentication works for PROD account
- [ ] Security scanning (Trivy) blocks vulnerable images
- [ ] Docker images built and pushed to PROD ECR
- [ ] Service-specific Terraform deploys successfully
- [ ] ECS services use CODE_DEPLOY deployment controller
- [ ] Blue-green deployment executes correctly
- [ ] ALB routing switches to new task set
- [ ] E2E tests pass after deployment
- [ ] CloudWatch alarms configured for auto-rollback
- [ ] Automatic rollback on failure works
- [ ] Manual approval workflow requires 2+ reviewers
- [ ] Deployment monitoring verifies success

## Testing Requirements

**Validation**:
- Push changes to `main` branch or create GitHub release
- Verify workflow triggers based on path filter
- Confirm manual approval is required (2+ reviewers)
- Approve deployment in GitHub UI
- Verify security scan passes (no CRITICAL/HIGH vulnerabilities)
- Check Docker image pushed to PROD ECR
- Verify Terraform creates blue-green deployment
- Monitor CodeDeploy deployment progress
- Check ECS service shows two task sets during deployment
- Verify ALB traffic shifts to new task set
- Test service endpoint via ALB
- Run E2E tests against PROD environment
- Verify CloudWatch alarms don't trigger
- Confirm old task set is terminated after success
- Test rollback by introducing failing health checks

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

- Specification: `specs/ci-cd-pipelines.md` (Service Deployment Pattern, Blue-Green section)
- Infrastructure Pattern: `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`
- Infrastructure Summary: `infrastructure/docs/architecture/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- Related Tasks: 003-setup-cd-qa-pipeline, 005-setup-infrastructure-pipeline
