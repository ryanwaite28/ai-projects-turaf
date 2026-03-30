# Task 006: Create GitHub Actions Workflow

**Status**: Pending  
**Assignee**: TBD  
**Estimated Time**: 3 hours  
**Related Spec**: [Architecture Testing](../../specs/architecture-testing.md)

---

## Objective

Create GitHub Actions workflow for running architecture tests against deployed environments and publishing reports.

---

## Prerequisites

- Task 004 completed (IAM permissions)
- Task 005 completed (Infrastructure deployed)
- Understanding of GitHub Actions

---

## Tasks

### 1. Create Workflow File

Create `.github/workflows/architecture-tests.yml`:

```yaml
name: Architecture Tests

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        type: choice
        options:
          - dev
          - qa
          - prod
  schedule:
    - cron: '0 */6 * * *'  # Every 6 hours

permissions:
  id-token: write
  contents: read
  pull-requests: write

jobs:
  architecture-tests:
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment || 'dev' }}
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: us-east-1
      
      - name: Run architecture tests
        run: |
          cd services/architecture-tests
          mvn clean test -Dkarate.env=${{ github.event.inputs.environment || 'dev' }}
        continue-on-error: true
      
      - name: Generate HTML report
        if: always()
        run: |
          cd services/architecture-tests
          mvn karate:report
      
      - name: Upload report to S3
        if: always()
        run: |
          TIMESTAMP=$(date +%Y%m%d-%H%M%S)
          ENV=${{ github.event.inputs.environment || 'dev' }}
          cd services/architecture-tests
          aws s3 cp target/karate-reports/ \
            s3://turaf-architecture-test-reports-${ENV}/reports/${TIMESTAMP}/ \
            --recursive
          
          echo "REPORT_TIMESTAMP=${TIMESTAMP}" >> $GITHUB_ENV
          echo "REPORT_ENV=${ENV}" >> $GITHUB_ENV
          echo "Report URL: https://reports.${ENV}.turafapp.com/reports/${TIMESTAMP}/karate-summary.html"
      
      - name: Invalidate CloudFront cache
        if: always()
        run: |
          DISTRIBUTION_ID=$(aws cloudfront list-distributions \
            --query "DistributionList.Items[?Aliases.Items[?contains(@, 'reports.${{ env.REPORT_ENV }}.turafapp.com')]].Id | [0]" \
            --output text)
          
          if [ -n "$DISTRIBUTION_ID" ]; then
            echo "Invalidating CloudFront distribution: $DISTRIBUTION_ID"
            aws cloudfront create-invalidation \
              --distribution-id $DISTRIBUTION_ID \
              --paths "/reports/${{ env.REPORT_TIMESTAMP }}/*"
            echo "✅ CloudFront cache invalidated"
          else
            echo "⚠️ CloudFront distribution not found, skipping invalidation"
          fi
      
      - name: Publish test results
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Architecture Test Results
          path: services/architecture-tests/target/surefire-reports/*.xml
          reporter: java-junit
      
      - name: Comment PR with results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const reportPath = 'services/architecture-tests/target/karate-reports/karate-summary.json';
            
            if (fs.existsSync(reportPath)) {
              const report = JSON.parse(fs.readFileSync(reportPath));
              const reportUrl = `https://reports.${{ env.REPORT_ENV }}.turafapp.com/reports/${{ env.REPORT_TIMESTAMP }}/karate-summary.html`;
              
              github.rest.issues.createComment({
                issue_number: context.issue.number,
                owner: context.repo.owner,
                repo: context.repo.repo,
                body: `## Architecture Test Results\n\n` +
                      `✅ Passed: ${report.scenariosPassed}\n` +
                      `❌ Failed: ${report.scenariosFailed}\n` +
                      `⏱️ Duration: ${report.elapsedTime}ms\n\n` +
                      `[View Full Report](${reportUrl})`
              });
            }
      
      - name: Fail job if tests failed
        if: always()
        run: |
          if [ -f services/architecture-tests/target/karate-reports/karate-summary.json ]; then
            FAILED=$(jq '.scenariosFailed' services/architecture-tests/target/karate-reports/karate-summary.json)
            if [ "$FAILED" -gt 0 ]; then
              echo "❌ $FAILED test(s) failed"
              exit 1
            fi
          fi
```

### 2. Update GitHub Environment Secrets

Ensure these secrets are configured for each environment:

**DEV Environment**:
- `AWS_ROLE_ARN`: `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `801651112319`

**QA Environment**:
- `AWS_ROLE_ARN`: `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `965932217544`

**PROD Environment**:
- `AWS_ROLE_ARN`: `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `811783768245`

### 3. Create Workflow Documentation

Create `.github/workflows/README-architecture-tests.md`:

```markdown
# Architecture Tests Workflow

## Overview

This workflow runs full-system architecture tests against deployed environments.

## Trigger Methods

### Manual Trigger
```bash
gh workflow run architecture-tests.yml -f environment=dev
```

### Scheduled
Runs automatically every 6 hours against DEV environment.

## Workflow Steps

1. **Checkout code** - Get latest test code
2. **Setup Java** - Install JDK 17
3. **Configure AWS** - Authenticate via OIDC
4. **Run tests** - Execute Karate tests
5. **Generate report** - Create HTML report
6. **Upload to S3** - Store report in S3
7. **Invalidate cache** - Clear CloudFront cache
8. **Publish results** - Post to GitHub Actions
9. **Comment PR** - Add results to PR (if applicable)

## Report Access

Reports are accessible at:
- DEV: https://reports.dev.turafapp.com/reports/{timestamp}/
- QA: https://reports.qa.turafapp.com/reports/{timestamp}/
- PROD: https://reports.prod.turafapp.com/reports/{timestamp}/

## Troubleshooting

### Tests fail to run
- Check AWS credentials are configured
- Verify environment is deployed and accessible

### Report upload fails
- Check IAM permissions for S3
- Verify bucket exists

### CloudFront invalidation fails
- Check IAM permissions for CloudFront
- Verify distribution exists
```

---

## Acceptance Criteria

- [ ] Workflow file created
- [ ] Environment secrets configured
- [ ] Workflow can be triggered manually
- [ ] Tests execute successfully
- [ ] Reports upload to S3
- [ ] CloudFront cache invalidates
- [ ] Test results appear in GitHub Actions
- [ ] Documentation created

---

## Verification

```bash
# Trigger workflow manually
gh workflow run architecture-tests.yml -f environment=dev

# Check workflow status
gh run list --workflow=architecture-tests.yml

# View workflow logs
gh run view --log
```

---

## Notes

- Workflow runs independently and doesn't block deployments
- Reports are retained for 90 days (S3 lifecycle policy)
- Scheduled runs use DEV environment by default
- Failed tests fail the workflow job
