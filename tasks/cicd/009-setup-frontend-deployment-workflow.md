# Task: Setup Frontend Deployment Workflow

**Service**: CI/CD  
**Type**: Frontend Deployment Pipeline  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 005-request-acm-certificates, 012-configure-github-environments-secrets

---

## Objective

Create GitHub Actions workflows to build, test, and deploy the Angular frontend application to S3 + CloudFront across DEV, QA, and PROD environments.

---

## Acceptance Criteria

- [ ] Frontend build workflow created for all environments
- [ ] S3 bucket sync configured with proper cache headers
- [ ] CloudFront invalidation integrated
- [ ] Environment-specific configurations applied
- [ ] Deployment tested in DEV environment
- [ ] Rollback procedure documented

---

## Implementation

### 1. Create Frontend Deployment Workflow (DEV)

**File**: `.github/workflows/frontend-dev.yml`

```yaml
name: Deploy Frontend - DEV

on:
  push:
    branches:
      - develop
    paths:
      - 'frontend/**'
  workflow_dispatch:

env:
  ENVIRONMENT: dev
  AWS_REGION: us-east-1
  NODE_VERSION: '18'

permissions:
  id-token: write
  contents: read

jobs:
  build-and-deploy:
    name: Build and Deploy to DEV
    runs-on: ubuntu-latest
    environment: dev
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: ./frontend
        run: npm ci

      - name: Lint
        working-directory: ./frontend
        run: npm run lint

      - name: Run unit tests
        working-directory: ./frontend
        run: npm run test:ci

      - name: Build Angular app
        working-directory: ./frontend
        run: npm run build:dev
        env:
          NODE_OPTIONS: '--max_old_space_size=4096'

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_DEV }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Get S3 bucket name
        id: s3-bucket
        run: |
          ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
          BUCKET_NAME="turaf-frontend-${{ env.ENVIRONMENT }}-${ACCOUNT_ID}"
          echo "bucket_name=${BUCKET_NAME}" >> $GITHUB_OUTPUT

      - name: Sync static assets to S3 (with caching)
        working-directory: ./frontend
        run: |
          aws s3 sync dist/turaf-frontend/ s3://${{ steps.s3-bucket.outputs.bucket_name }}/ \
            --delete \
            --cache-control "public, max-age=31536000, immutable" \
            --exclude "index.html" \
            --exclude "*.txt" \
            --exclude "*.json"

      - name: Upload index.html (no cache)
        working-directory: ./frontend
        run: |
          aws s3 cp dist/turaf-frontend/index.html s3://${{ steps.s3-bucket.outputs.bucket_name }}/index.html \
            --cache-control "no-cache, no-store, must-revalidate" \
            --metadata-directive REPLACE

      - name: Upload config files (no cache)
        working-directory: ./frontend
        run: |
          for file in dist/turaf-frontend/*.{txt,json}; do
            if [ -f "$file" ]; then
              aws s3 cp "$file" s3://${{ steps.s3-bucket.outputs.bucket_name }}/$(basename "$file") \
                --cache-control "no-cache, no-store, must-revalidate"
            fi
          done

      - name: Get CloudFront distribution ID
        id: cloudfront
        run: |
          DISTRIBUTION_ID=$(aws cloudfront list-distributions \
            --query "DistributionList.Items[?Aliases.Items[?contains(@, 'app.${{ env.ENVIRONMENT }}.turafapp.com')]].Id | [0]" \
            --output text)
          echo "distribution_id=${DISTRIBUTION_ID}" >> $GITHUB_OUTPUT

      - name: Invalidate CloudFront cache
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ steps.cloudfront.outputs.distribution_id }} \
            --paths "/*"

      - name: Wait for invalidation
        run: |
          INVALIDATION_ID=$(aws cloudfront list-invalidations \
            --distribution-id ${{ steps.cloudfront.outputs.distribution_id }} \
            --query 'InvalidationList.Items[0].Id' \
            --output text)
          
          aws cloudfront wait invalidation-completed \
            --distribution-id ${{ steps.cloudfront.outputs.distribution_id }} \
            --id $INVALIDATION_ID

      - name: Verify deployment
        run: |
          echo "Waiting for CloudFront propagation..."
          sleep 30
          
          RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://app.${{ env.ENVIRONMENT }}.turafapp.com)
          
          if [ "$RESPONSE" -eq 200 ]; then
            echo "✅ Deployment successful! Frontend is accessible."
          else
            echo "❌ Deployment verification failed. HTTP status: $RESPONSE"
            exit 1
          fi

      - name: Post deployment summary
        if: always()
        run: |
          echo "## Deployment Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "- **Environment**: ${{ env.ENVIRONMENT }}" >> $GITHUB_STEP_SUMMARY
          echo "- **S3 Bucket**: ${{ steps.s3-bucket.outputs.bucket_name }}" >> $GITHUB_STEP_SUMMARY
          echo "- **CloudFront Distribution**: ${{ steps.cloudfront.outputs.distribution_id }}" >> $GITHUB_STEP_SUMMARY
          echo "- **URL**: https://app.${{ env.ENVIRONMENT }}.turafapp.com" >> $GITHUB_STEP_SUMMARY
```

### 2. Create Frontend Deployment Workflow (QA)

**File**: `.github/workflows/frontend-qa.yml`

```yaml
name: Deploy Frontend - QA

on:
  push:
    branches:
      - 'release/*'
    paths:
      - 'frontend/**'
  workflow_dispatch:

env:
  ENVIRONMENT: qa
  AWS_REGION: us-east-1
  NODE_VERSION: '18'

permissions:
  id-token: write
  contents: read

jobs:
  build-and-deploy:
    name: Build and Deploy to QA
    runs-on: ubuntu-latest
    environment: qa
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: ./frontend
        run: npm ci

      - name: Lint
        working-directory: ./frontend
        run: npm run lint

      - name: Run unit tests
        working-directory: ./frontend
        run: npm run test:ci

      - name: Run E2E tests
        working-directory: ./frontend
        run: npm run e2e:ci

      - name: Build Angular app
        working-directory: ./frontend
        run: npm run build:qa
        env:
          NODE_OPTIONS: '--max_old_space_size=4096'

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_QA }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Get S3 bucket name
        id: s3-bucket
        run: |
          ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
          BUCKET_NAME="turaf-frontend-${{ env.ENVIRONMENT }}-${ACCOUNT_ID}"
          echo "bucket_name=${BUCKET_NAME}" >> $GITHUB_OUTPUT

      - name: Sync static assets to S3 (with caching)
        working-directory: ./frontend
        run: |
          aws s3 sync dist/turaf-frontend/ s3://${{ steps.s3-bucket.outputs.bucket_name }}/ \
            --delete \
            --cache-control "public, max-age=31536000, immutable" \
            --exclude "index.html" \
            --exclude "*.txt" \
            --exclude "*.json"

      - name: Upload index.html (no cache)
        working-directory: ./frontend
        run: |
          aws s3 cp dist/turaf-frontend/index.html s3://${{ steps.s3-bucket.outputs.bucket_name }}/index.html \
            --cache-control "no-cache, no-store, must-revalidate" \
            --metadata-directive REPLACE

      - name: Get CloudFront distribution ID
        id: cloudfront
        run: |
          DISTRIBUTION_ID=$(aws cloudfront list-distributions \
            --query "DistributionList.Items[?Aliases.Items[?contains(@, 'app.${{ env.ENVIRONMENT }}.turafapp.com')]].Id | [0]" \
            --output text)
          echo "distribution_id=${DISTRIBUTION_ID}" >> $GITHUB_OUTPUT

      - name: Invalidate CloudFront cache
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ steps.cloudfront.outputs.distribution_id }} \
            --paths "/*"

      - name: Verify deployment
        run: |
          sleep 30
          RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://app.${{ env.ENVIRONMENT }}.turafapp.com)
          if [ "$RESPONSE" -ne 200 ]; then
            echo "❌ Deployment verification failed. HTTP status: $RESPONSE"
            exit 1
          fi
```

### 3. Create Frontend Deployment Workflow (PROD)

**File**: `.github/workflows/frontend-prod.yml`

```yaml
name: Deploy Frontend - PROD

on:
  push:
    branches:
      - main
    paths:
      - 'frontend/**'
  workflow_dispatch:

env:
  ENVIRONMENT: prod
  AWS_REGION: us-east-1
  NODE_VERSION: '18'

permissions:
  id-token: write
  contents: read

jobs:
  build-and-deploy:
    name: Build and Deploy to PROD
    runs-on: ubuntu-latest
    environment: 
      name: prod
      url: https://app.turafapp.com
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: ./frontend
        run: npm ci

      - name: Lint
        working-directory: ./frontend
        run: npm run lint

      - name: Run unit tests
        working-directory: ./frontend
        run: npm run test:ci

      - name: Run E2E tests
        working-directory: ./frontend
        run: npm run e2e:ci

      - name: Build Angular app
        working-directory: ./frontend
        run: npm run build:production
        env:
          NODE_OPTIONS: '--max_old_space_size=4096'

      - name: Analyze bundle size
        working-directory: ./frontend
        run: |
          npm run analyze || true
          
          BUNDLE_SIZE=$(du -sh dist/turaf-frontend | cut -f1)
          echo "📦 Bundle size: $BUNDLE_SIZE" >> $GITHUB_STEP_SUMMARY

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_PROD }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Get S3 bucket name
        id: s3-bucket
        run: |
          ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
          BUCKET_NAME="turaf-frontend-${ACCOUNT_ID}"
          echo "bucket_name=${BUCKET_NAME}" >> $GITHUB_OUTPUT

      - name: Backup current version
        run: |
          TIMESTAMP=$(date +%Y%m%d-%H%M%S)
          aws s3 cp s3://${{ steps.s3-bucket.outputs.bucket_name }}/index.html \
            s3://${{ steps.s3-bucket.outputs.bucket_name }}/backups/${TIMESTAMP}/index.html \
            --metadata "backup-timestamp=${TIMESTAMP}" || true

      - name: Sync static assets to S3 (with caching)
        working-directory: ./frontend
        run: |
          aws s3 sync dist/turaf-frontend/ s3://${{ steps.s3-bucket.outputs.bucket_name }}/ \
            --delete \
            --cache-control "public, max-age=31536000, immutable" \
            --exclude "index.html" \
            --exclude "*.txt" \
            --exclude "*.json"

      - name: Upload index.html (no cache)
        working-directory: ./frontend
        run: |
          aws s3 cp dist/turaf-frontend/index.html s3://${{ steps.s3-bucket.outputs.bucket_name }}/index.html \
            --cache-control "no-cache, no-store, must-revalidate" \
            --metadata-directive REPLACE

      - name: Get CloudFront distribution ID
        id: cloudfront
        run: |
          DISTRIBUTION_ID=$(aws cloudfront list-distributions \
            --query "DistributionList.Items[?Aliases.Items[?contains(@, 'app.turafapp.com')]].Id | [0]" \
            --output text)
          echo "distribution_id=${DISTRIBUTION_ID}" >> $GITHUB_OUTPUT

      - name: Invalidate CloudFront cache
        run: |
          INVALIDATION_ID=$(aws cloudfront create-invalidation \
            --distribution-id ${{ steps.cloudfront.outputs.distribution_id }} \
            --paths "/*" \
            --query 'Invalidation.Id' \
            --output text)
          
          echo "invalidation_id=${INVALIDATION_ID}" >> $GITHUB_OUTPUT

      - name: Wait for invalidation
        run: |
          aws cloudfront wait invalidation-completed \
            --distribution-id ${{ steps.cloudfront.outputs.distribution_id }} \
            --id ${{ steps.cloudfront.outputs.invalidation_id }}

      - name: Smoke test
        run: |
          sleep 60
          
          # Test homepage
          RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://app.turafapp.com)
          if [ "$RESPONSE" -ne 200 ]; then
            echo "❌ Homepage check failed. HTTP status: $RESPONSE"
            exit 1
          fi
          
          # Test API connectivity
          API_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://api.turafapp.com/actuator/health)
          if [ "$API_RESPONSE" -ne 200 ]; then
            echo "⚠️ API health check failed. HTTP status: $API_RESPONSE"
          fi
          
          echo "✅ Smoke tests passed"

      - name: Monitor CloudWatch metrics
        run: |
          # Get CloudFront error rate
          ERROR_RATE=$(aws cloudwatch get-metric-statistics \
            --namespace AWS/CloudFront \
            --metric-name 4xxErrorRate \
            --dimensions Name=DistributionId,Value=${{ steps.cloudfront.outputs.distribution_id }} \
            --start-time $(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S) \
            --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
            --period 300 \
            --statistics Average \
            --query 'Datapoints[0].Average' \
            --output text)
          
          echo "📊 CloudFront 4xx Error Rate: ${ERROR_RATE}%" >> $GITHUB_STEP_SUMMARY

      - name: Post deployment summary
        if: always()
        run: |
          echo "## 🚀 Production Deployment Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "- **Environment**: PROD" >> $GITHUB_STEP_SUMMARY
          echo "- **S3 Bucket**: ${{ steps.s3-bucket.outputs.bucket_name }}" >> $GITHUB_STEP_SUMMARY
          echo "- **CloudFront Distribution**: ${{ steps.cloudfront.outputs.distribution_id }}" >> $GITHUB_STEP_SUMMARY
          echo "- **URL**: https://app.turafapp.com" >> $GITHUB_STEP_SUMMARY
          echo "- **Commit**: ${{ github.sha }}" >> $GITHUB_STEP_SUMMARY
```

### 4. Update Angular package.json Scripts

**File**: `frontend/package.json`

Add build scripts:
```json
{
  "scripts": {
    "build:dev": "ng build --configuration=dev",
    "build:qa": "ng build --configuration=qa",
    "build:production": "ng build --configuration=production",
    "test:ci": "ng test --watch=false --code-coverage --browsers=ChromeHeadless",
    "e2e:ci": "ng e2e --headless",
    "analyze": "ng build --configuration=production --stats-json && webpack-bundle-analyzer dist/turaf-frontend/stats.json"
  }
}
```

### 5. Create Angular Build Configurations

**File**: `frontend/angular.json`

```json
{
  "projects": {
    "turaf-frontend": {
      "architect": {
        "build": {
          "configurations": {
            "dev": {
              "fileReplacements": [
                {
                  "replace": "src/environments/environment.ts",
                  "with": "src/environments/environment.dev.ts"
                }
              ],
              "optimization": true,
              "outputHashing": "all",
              "sourceMap": false,
              "namedChunks": false,
              "aot": true,
              "extractLicenses": true,
              "buildOptimizer": true
            },
            "qa": {
              "fileReplacements": [
                {
                  "replace": "src/environments/environment.ts",
                  "with": "src/environments/environment.qa.ts"
                }
              ],
              "optimization": true,
              "outputHashing": "all",
              "sourceMap": false,
              "namedChunks": false,
              "aot": true,
              "extractLicenses": true,
              "buildOptimizer": true
            },
            "production": {
              "fileReplacements": [
                {
                  "replace": "src/environments/environment.ts",
                  "with": "src/environments/environment.prod.ts"
                }
              ],
              "optimization": true,
              "outputHashing": "all",
              "sourceMap": false,
              "namedChunks": false,
              "aot": true,
              "extractLicenses": true,
              "buildOptimizer": true,
              "budgets": [
                {
                  "type": "initial",
                  "maximumWarning": "2mb",
                  "maximumError": "5mb"
                },
                {
                  "type": "anyComponentStyle",
                  "maximumWarning": "6kb",
                  "maximumError": "10kb"
                }
              ]
            }
          }
        }
      }
    }
  }
}
```

---

## Verification

### 1. Test DEV Deployment

```bash
# Trigger workflow manually
gh workflow run frontend-dev.yml

# Monitor workflow
gh run watch

# Verify deployment
curl -I https://app.dev.turafapp.com

# Check SSL certificate
openssl s_client -connect app.dev.turafapp.com:443 -servername app.dev.turafapp.com < /dev/null 2>&1 | grep -A 2 "Certificate chain"
```

### 2. Verify S3 Bucket Contents

```bash
# List S3 objects
aws s3 ls s3://turaf-frontend-dev-801651112319/ --recursive --profile turaf-dev

# Check cache headers
aws s3api head-object \
  --bucket turaf-frontend-dev-801651112319 \
  --key index.html \
  --profile turaf-dev \
  --query 'CacheControl'

# Should return: "no-cache, no-store, must-revalidate"
```

### 3. Verify CloudFront Distribution

```bash
# Get distribution details
aws cloudfront get-distribution \
  --id <DISTRIBUTION_ID> \
  --profile turaf-dev \
  --query 'Distribution.DistributionConfig.{Aliases:Aliases.Items,Certificate:ViewerCertificate.ACMCertificateArn}'
```

---

## Rollback Procedure

### Manual Rollback

```bash
# 1. List previous versions
aws s3api list-object-versions \
  --bucket turaf-frontend-prod-811783768245 \
  --prefix index.html \
  --profile turaf-prod

# 2. Restore previous version
VERSION_ID="<PREVIOUS_VERSION_ID>"
aws s3api copy-object \
  --bucket turaf-frontend-prod-811783768245 \
  --copy-source "turaf-frontend-prod-811783768245/index.html?versionId=${VERSION_ID}" \
  --key index.html \
  --cache-control "no-cache, no-store, must-revalidate" \
  --metadata-directive REPLACE \
  --profile turaf-prod

# 3. Invalidate CloudFront
aws cloudfront create-invalidation \
  --distribution-id <DISTRIBUTION_ID> \
  --paths "/*" \
  --profile turaf-prod

# 4. Verify
curl -I https://app.turafapp.com
```

---

## Documentation

Update after completion:
- `specs/frontend-deployment.md` - Mark CI/CD workflows as implemented
- `specs/ci-cd-pipelines.md` - Add frontend deployment workflow documentation
- `.github/workflows/README.md` - Document frontend workflows

---

## Related Tasks

- Task 005: Request ACM Certificates (prerequisite)
- Task 012: Configure GitHub Environments and Secrets (prerequisite)
- Frontend tasks 001-014: Angular application implementation
