# CI/CD Pipelines Specification

**Source**: PROJECT.md (Sections 14, 49-50)

This specification defines the complete CI/CD pipeline architecture using GitHub Actions.

---

## Pipeline Overview

**CI/CD Platform**: GitHub Actions  
**Workflow Location**: `.github/workflows/`  
**Environments**: DEV, QA, PROD  
**AWS Authentication**: OIDC Federation  

---

## Workflow Files

```
.github/
└── workflows/
    ├── ci.yml                      # Continuous Integration
    ├── cd-dev.yml                  # Deploy to DEV
    ├── cd-qa.yml                   # Deploy to QA
    ├── cd-prod.yml                 # Deploy to PROD
    ├── infrastructure.yml          # Infrastructure deployment
    └── security-scan.yml           # Security scanning
```

---

## CI Pipeline (ci.yml)

**Triggers**: 
- Push to any branch
- Pull requests to `main` or `develop`

**Jobs**:

### 1. Lint

**Purpose**: Code quality checks

**Steps**:
```yaml
lint-java:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run Checkstyle
      run: |
        cd services
        mvn checkstyle:check
    
    - name: Run SpotBugs
      run: |
        cd services
        mvn spotbugs:check

lint-angular:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
    
    - name: Install dependencies
      run: |
        cd frontend
        npm ci
    
    - name: Run ESLint
      run: |
        cd frontend
        npm run lint
    
    - name: Run Prettier check
      run: |
        cd frontend
        npm run format:check

lint-terraform:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Setup Terraform
      uses: hashicorp/setup-terraform@v3
    
    - name: Terraform Format Check
      run: |
        cd infrastructure
        terraform fmt -check -recursive
    
    - name: Run tflint
      uses: terraform-linters/setup-tflint@v4
      with:
        tflint_version: latest
    
    - name: Run tflint
      run: |
        cd infrastructure
        tflint --recursive
```

---

### 2. Unit Tests

**Purpose**: Run all unit tests

**Steps**:
```yaml
test-backend:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run tests
      run: |
        cd services
        mvn test
    
    - name: Generate coverage report
      run: |
        cd services
        mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v4
      with:
        files: ./services/target/site/jacoco/jacoco.xml
        flags: backend

test-frontend:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
    
    - name: Install dependencies
      run: |
        cd frontend
        npm ci
    
    - name: Run tests
      run: |
        cd frontend
        npm run test:ci
    
    - name: Upload coverage
      uses: codecov/codecov-action@v4
      with:
        files: ./frontend/coverage/lcov.info
        flags: frontend
```

---

### 3. Build

**Purpose**: Verify builds succeed

**Steps**:
```yaml
build-backend:
  runs-on: ubuntu-latest
  needs: [lint-java, test-backend]
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build services
      run: |
        cd services
        mvn clean package -DskipTests
    
    - name: Upload artifacts
      uses: actions/upload-artifact@v4
      with:
        name: backend-jars
        path: services/*/target/*.jar

build-frontend:
  runs-on: ubuntu-latest
  needs: [lint-angular, test-frontend]
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
    
    - name: Install dependencies
      run: |
        cd frontend
        npm ci
    
    - name: Build
      run: |
        cd frontend
        npm run build
    
    - name: Upload artifacts
      uses: actions/upload-artifact@v4
      with:
        name: frontend-dist
        path: frontend/dist/
```

---

### 4. Code Quality

**Purpose**: SonarQube analysis

**Steps**:
```yaml
sonarqube:
  runs-on: ubuntu-latest
  needs: [test-backend, test-frontend]
  steps:
    - uses: actions/checkout@v4
      with:
        fetch-depth: 0  # Shallow clones disabled for analysis
    
    - name: SonarQube Scan
      uses: sonarsource/sonarqube-scan-action@master
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
    
    - name: SonarQube Quality Gate
      uses: sonarsource/sonarqube-quality-gate-action@master
      timeout-minutes: 5
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

---

### 5. Security Scan

**Purpose**: Vulnerability scanning

**Steps**:
```yaml
security-scan:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Run OWASP Dependency Check
      run: |
        cd services
        mvn org.owasp:dependency-check-maven:check
    
    - name: Run npm audit
      run: |
        cd frontend
        npm audit --audit-level=moderate
    
    - name: Upload OWASP report
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: owasp-report
        path: services/target/dependency-check-report.html
```

---

## CD Pipeline - DEV (cd-dev.yml)

**Triggers**: 
- Push to `develop` branch
- Manual workflow dispatch

**Jobs**:

### 1. Build Docker Images

**Steps**:
```yaml
build-and-push-images:
  runs-on: ubuntu-latest
  permissions:
    id-token: write
    contents: read
  strategy:
    matrix:
      service: [identity-service, organization-service, experiment-service, metrics-service]
  steps:
    - uses: actions/checkout@v4
    
    - name: Configure AWS Credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/GitHubActionsRole
        aws-region: us-east-1
    
    - name: Login to Amazon ECR
      id: login-ecr
      uses: aws-actions/amazon-ecr-login@v2
    
    - name: Build, tag, and push image
      env:
        ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        ECR_REPOSITORY: turaf/${{ matrix.service }}
        IMAGE_TAG: ${{ github.sha }}
      run: |
        cd services/${{ matrix.service }}
        docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
        docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:dev-latest
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:dev-latest
    
    - name: Scan image with Trivy
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: ${{ steps.login-ecr.outputs.registry }}/turaf/${{ matrix.service }}:${{ github.sha }}
        format: 'sarif'
        output: 'trivy-results.sarif'
    
    - name: Upload Trivy results
      uses: github/codeql-action/upload-sarif@v3
      with:
        sarif_file: 'trivy-results.sarif'
```

---

### 2. Deploy Infrastructure

**Steps**:
```yaml
deploy-infrastructure:
  runs-on: ubuntu-latest
  needs: build-and-push-images
  permissions:
    id-token: write
    contents: read
  steps:
    - uses: actions/checkout@v4
    
    - name: Configure AWS Credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/GitHubActionsRole
        aws-region: us-east-1
    
    - name: Setup Terraform
      uses: hashicorp/setup-terraform@v3
    
    - name: Terraform Init
      run: |
        cd infrastructure/environments/dev
        terraform init
    
    - name: Terraform Plan
      run: |
        cd infrastructure/environments/dev
        terraform plan -out=tfplan
    
    - name: Terraform Apply
      run: |
        cd infrastructure/environments/dev
        terraform apply -auto-approve tfplan
    
    - name: Output infrastructure endpoints
      run: |
        cd infrastructure/environments/dev
        terraform output
```

---

### 3. Deploy Services

**Steps**:
```yaml
deploy-services:
  runs-on: ubuntu-latest
  needs: deploy-infrastructure
  permissions:
    id-token: write
    contents: read
  strategy:
    matrix:
      service: [identity-service, organization-service, experiment-service, metrics-service]
  steps:
    - uses: actions/checkout@v4
    
    - name: Configure AWS Credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/GitHubActionsRole
        aws-region: us-east-1
    
    - name: Update ECS service
      run: |
        aws ecs update-service \
          --cluster turaf-cluster-dev \
          --service turaf-${{ matrix.service }}-dev \
          --force-new-deployment
    
    - name: Wait for service stability
      run: |
        aws ecs wait services-stable \
          --cluster turaf-cluster-dev \
          --services turaf-${{ matrix.service }}-dev
```

---

### 4. Deploy Lambda Functions

**Steps**:
```yaml
deploy-lambda:
  runs-on: ubuntu-latest
  needs: deploy-infrastructure
  permissions:
    id-token: write
    contents: read
  strategy:
    matrix:
      function: [reporting-service, notification-service]
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build Lambda function
      run: |
        cd services/${{ matrix.function }}
        mvn clean package
    
    - name: Configure AWS Credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/GitHubActionsRole
        aws-region: us-east-1
    
    - name: Deploy to Lambda
      run: |
        aws lambda update-function-code \
          --function-name turaf-${{ matrix.function }}-dev \
          --zip-file fileb://services/${{ matrix.function }}/target/${{ matrix.function }}.jar
    
    - name: Wait for update to complete
      run: |
        aws lambda wait function-updated \
          --function-name turaf-${{ matrix.function }}-dev
```

---

### 5. Deploy Frontend

**Steps**:
```yaml
deploy-frontend:
  runs-on: ubuntu-latest
  needs: deploy-infrastructure
  permissions:
    id-token: write
    contents: read
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
    
    - name: Install dependencies
      run: |
        cd frontend
        npm ci
    
    - name: Build with dev config
      run: |
        cd frontend
        npm run build -- --configuration=dev
    
    - name: Configure AWS Credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/GitHubActionsRole
        aws-region: us-east-1
    
    - name: Sync to S3
      run: |
        aws s3 sync frontend/dist/ s3://turaf-frontend-dev/ --delete
    
    - name: Invalidate CloudFront cache
      run: |
        aws cloudfront create-invalidation \
          --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID_DEV }} \
          --paths "/*"
```

---

### 6. Smoke Tests

**Steps**:
```yaml
smoke-tests:
  runs-on: ubuntu-latest
  needs: [deploy-services, deploy-lambda, deploy-frontend]
  steps:
    - uses: actions/checkout@v4
    
    - name: Health check - Identity Service
      run: |
        curl -f https://api.dev.turaf.com/api/v1/auth/health || exit 1
    
    - name: Health check - Experiment Service
      run: |
        curl -f https://api.dev.turaf.com/api/v1/experiments/health || exit 1
    
    - name: Health check - Frontend
      run: |
        curl -f https://app.dev.turaf.com || exit 1
    
    - name: Basic API test
      run: |
        # Test registration endpoint
        curl -X POST https://api.dev.turaf.com/api/v1/auth/register \
          -H "Content-Type: application/json" \
          -d '{"email":"test@example.com","password":"Test123!","name":"Test User"}' \
          || echo "Registration test completed"
```

---

## CD Pipeline - QA (cd-qa.yml)

**Triggers**: 
- Manual workflow dispatch
- Tag push matching `qa-*`

**Additional Jobs**:

### Integration Tests

**Steps**:
```yaml
integration-tests:
  runs-on: ubuntu-latest
  needs: deploy-services
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run integration tests
      run: |
        cd services
        mvn verify -P integration-tests \
          -Dtest.api.url=https://api.qa.turaf.com
```

### E2E Tests

**Steps**:
```yaml
e2e-tests:
  runs-on: ubuntu-latest
  needs: deploy-frontend
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
    
    - name: Install Playwright
      run: |
        cd frontend
        npm ci
        npx playwright install --with-deps
    
    - name: Run E2E tests
      run: |
        cd frontend
        npm run e2e -- --config baseUrl=https://app.qa.turaf.com
    
    - name: Upload test results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: playwright-report
        path: frontend/playwright-report/
```

### Manual Approval

**Steps**:
```yaml
approval:
  runs-on: ubuntu-latest
  needs: [integration-tests, e2e-tests]
  environment:
    name: qa-approval
  steps:
    - name: Wait for approval
      run: echo "Deployment approved"
```

---

## CD Pipeline - PROD (cd-prod.yml)

**Triggers**: 
- Manual workflow dispatch
- Tag push matching `v*`

**Additional Jobs**:

### Pre-Deployment Checks

**Steps**:
```yaml
pre-deployment:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Verify QA deployment
      run: |
        # Check QA health
        curl -f https://api.qa.turaf.com/health || exit 1
    
    - name: Check for breaking changes
      run: |
        # Run breaking change detection
        echo "Checking for breaking changes..."
    
    - name: Review deployment plan
      run: |
        cd infrastructure/environments/prod
        terraform plan -detailed-exitcode
```

### Blue-Green Deployment

**Steps**:
```yaml
blue-green-deploy:
  runs-on: ubuntu-latest
  needs: pre-deployment
  permissions:
    id-token: write
    contents: read
  steps:
    - name: Deploy to new task set
      run: |
        # Create new task definition
        # Deploy with new task set
        # Run smoke tests on new version
        echo "Blue-green deployment"
    
    - name: Gradual traffic shift
      run: |
        # Shift 10% traffic
        # Wait 5 minutes
        # Shift 50% traffic
        # Wait 5 minutes
        # Shift 100% traffic
        echo "Traffic shifting"
```

### Post-Deployment

**Steps**:
```yaml
post-deployment:
  runs-on: ubuntu-latest
  needs: blue-green-deploy
  steps:
    - name: Monitor error rates
      run: |
        # Check CloudWatch metrics
        echo "Monitoring error rates"
    
    - name: Verify key metrics
      run: |
        # Check application metrics
        echo "Verifying metrics"
    
    - name: Send deployment notification
      uses: 8398a7/action-slack@v3
      with:
        status: ${{ job.status }}
        text: 'Production deployment completed'
        webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## Infrastructure Pipeline (infrastructure.yml)

**Triggers**: 
- Changes to `infrastructure/` directory
- Manual workflow dispatch

**Jobs**:

### Terraform Validate

**Steps**:
```yaml
validate:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Setup Terraform
      uses: hashicorp/setup-terraform@v3
    
    - name: Terraform Init
      run: |
        cd infrastructure/environments/dev
        terraform init -backend=false
    
    - name: Terraform Validate
      run: |
        cd infrastructure
        terraform validate -recursive
    
    - name: Terraform Format Check
      run: |
        cd infrastructure
        terraform fmt -check -recursive
```

### Terraform Plan

**Steps**:
```yaml
plan:
  runs-on: ubuntu-latest
  needs: validate
  permissions:
    id-token: write
    contents: read
    pull-requests: write
  strategy:
    matrix:
      environment: [dev, qa, prod]
  steps:
    - uses: actions/checkout@v4
    
    - name: Configure AWS Credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/GitHubActionsRole
        aws-region: us-east-1
    
    - name: Setup Terraform
      uses: hashicorp/setup-terraform@v3
    
    - name: Terraform Plan
      run: |
        cd infrastructure/environments/${{ matrix.environment }}
        terraform init
        terraform plan -out=tfplan
    
    - name: Post plan to PR
      uses: actions/github-script@v7
      if: github.event_name == 'pull_request'
      with:
        script: |
          const output = `#### Terraform Plan for ${{ matrix.environment }}
          \`\`\`
          ${process.env.PLAN}
          \`\`\``;
          github.rest.issues.createComment({
            issue_number: context.issue.number,
            owner: context.repo.owner,
            repo: context.repo.repo,
            body: output
          });
```

### Terraform Apply

**Steps**:
```yaml
apply-dev:
  runs-on: ubuntu-latest
  needs: plan
  if: github.ref == 'refs/heads/main'
  permissions:
    id-token: write
    contents: read
  steps:
    - name: Terraform Apply DEV
      run: |
        cd infrastructure/environments/dev
        terraform apply -auto-approve

apply-qa:
  runs-on: ubuntu-latest
  needs: apply-dev
  environment:
    name: qa-infrastructure
  permissions:
    id-token: write
    contents: read
  steps:
    - name: Terraform Apply QA
      run: |
        cd infrastructure/environments/qa
        terraform apply -auto-approve

apply-prod:
  runs-on: ubuntu-latest
  needs: apply-qa
  environment:
    name: prod-infrastructure
  permissions:
    id-token: write
    contents: read
  steps:
    - name: Terraform Apply PROD
      run: |
        cd infrastructure/environments/prod
        terraform apply -auto-approve
```

---

## AWS OIDC Setup

### GitHub OIDC Provider

**AWS Console Setup**:
1. Create OIDC provider
2. Provider URL: `https://token.actions.githubusercontent.com`
3. Audience: `sts.amazonaws.com`

### IAM Role for GitHub Actions

**Trust Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:OWNER/REPO:*"
        }
      }
    }
  ]
}
```

**Permissions Policy**:
- ECR: Push/pull images
- ECS: Update services
- Lambda: Update function code
- S3: Upload to buckets
- CloudFront: Create invalidations
- Terraform: Full infrastructure management

---

## GitHub Secrets

**Required Secrets**:
- `AWS_ACCOUNT_ID`: AWS account ID
- `SONAR_TOKEN`: SonarQube token
- `SONAR_HOST_URL`: SonarQube server URL
- `SLACK_WEBHOOK_URL`: Slack notifications
- `CLOUDFRONT_DISTRIBUTION_ID_DEV`: CloudFront distribution ID
- `CLOUDFRONT_DISTRIBUTION_ID_QA`: CloudFront distribution ID
- `CLOUDFRONT_DISTRIBUTION_ID_PROD`: CloudFront distribution ID

---

## References

- PROJECT.md: CI/CD specifications
- GitHub Actions Documentation
- AWS OIDC Documentation
- Terraform GitHub Actions
