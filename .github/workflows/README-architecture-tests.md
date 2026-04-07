# Architecture Tests Workflow

**Created**: 2026-03-31  
**Workflow File**: `architecture-tests.yml`  
**Purpose**: Run full-system architecture tests against deployed environments

---

## Overview

This workflow runs comprehensive architecture tests using the Karate framework against deployed environments (DEV, QA, PROD). Tests validate API contracts, event-driven workflows, and cross-service integrations.

---

## Trigger Methods

### Manual Trigger (Recommended)

Via GitHub CLI:
```bash
# Test DEV environment
gh workflow run architecture-tests.yml -f environment=dev

# Test QA environment
gh workflow run architecture-tests.yml -f environment=qa

# Test PROD environment (smoke tests only)
gh workflow run architecture-tests.yml -f environment=prod
```

Via GitHub UI:
1. Go to **Actions** tab
2. Select **Architecture Tests** workflow
3. Click **Run workflow**
4. Select target environment
5. Click **Run workflow**

### Scheduled Execution

Runs automatically every 6 hours against the **DEV** environment:
- **Schedule**: `0 */6 * * *` (00:00, 06:00, 12:00, 18:00 UTC)
- **Environment**: DEV (default)
- **Purpose**: Continuous validation of DEV environment

---

## Workflow Steps

### 1. Checkout Code
- Fetches latest test code from repository
- Uses `actions/checkout@v4`

### 2. Set up JDK 17
- Installs Java Development Kit 17
- Uses Temurin distribution
- Caches Maven dependencies for faster builds

### 3. Configure AWS Credentials
- Authenticates with AWS using OIDC
- Assumes `GitHubActionsDeploymentRole`
- No long-lived credentials stored

### 4. Run Architecture Tests
- Executes Karate tests with Maven
- Environment-specific configuration via `-Dkarate.env`
- Continues even if tests fail (for reporting)

### 5. Generate HTML Report
- Creates comprehensive HTML test report
- Uses Karate's built-in reporting
- Always runs (even if tests fail)

### 6. Upload Report to S3
- Stores report in environment-specific S3 bucket
- Timestamped directory structure
- Accessible via CloudFront CDN

### 7. Invalidate CloudFront Cache
- Clears CloudFront cache for new report
- Ensures immediate availability
- Skips if distribution not found

### 8. Publish Test Results
- Posts JUnit-format results to GitHub Actions
- Uses `dorny/test-reporter@v1`
- Provides summary in Actions UI

### 9. Comment PR with Results
- Adds test results comment to pull requests
- Includes pass/fail counts and report link
- Only runs for PR-triggered workflows

### 10. Fail Job if Tests Failed
- Checks test results JSON
- Fails workflow if any tests failed
- Ensures CI/CD pipeline integrity

---

## Report Access

Test reports are published to CloudFront-backed S3 buckets:

### DEV Environment
- **URL**: `https://reports.dev.turafapp.com/reports/{timestamp}/karate-summary.html`
- **Bucket**: `turaf-architecture-test-reports-dev`
- **Retention**: 90 days

### QA Environment
- **URL**: `https://reports.qa.turafapp.com/reports/{timestamp}/karate-summary.html`
- **Bucket**: `turaf-architecture-test-reports-qa`
- **Retention**: 90 days

### PROD Environment
- **URL**: `https://reports.prod.turafapp.com/reports/{timestamp}/karate-summary.html`
- **Bucket**: `turaf-architecture-test-reports-prod`
- **Retention**: 180 days

**Report Structure**:
```
/reports/{timestamp}/
  ├── karate-summary.html      # Main report
  ├── karate-summary.json      # JSON results
  ├── karate-timeline.html     # Timeline view
  └── {feature-name}.html      # Individual feature reports
```

---

## Environment Configuration

### Required GitHub Secrets

Each environment must have these secrets configured:

#### DEV Environment
- `AWS_ROLE_ARN`: `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `801651112319`

#### QA Environment
- `AWS_ROLE_ARN`: `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `965932217544`

#### PROD Environment
- `AWS_ROLE_ARN`: `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `811783768245`

### Configuring Secrets

Via GitHub UI:
1. Go to **Settings** → **Environments**
2. Select environment (dev, qa, or prod)
3. Add secrets under **Environment secrets**

Via GitHub CLI:
```bash
# DEV
gh secret set AWS_ROLE_ARN -e dev -b "arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole"
gh secret set AWS_ACCOUNT_ID -e dev -b "801651112319"

# QA
gh secret set AWS_ROLE_ARN -e qa -b "arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole"
gh secret set AWS_ACCOUNT_ID -e qa -b "965932217544"

# PROD
gh secret set AWS_ROLE_ARN -e prod -b "arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole"
gh secret set AWS_ACCOUNT_ID -e prod -b "811783768245"
```

---

## Monitoring Workflow Runs

### List Recent Runs
```bash
gh run list --workflow=architecture-tests.yml --limit 10
```

### View Specific Run
```bash
gh run view <run-id>
```

### View Logs
```bash
gh run view <run-id> --log
```

### Download Artifacts
```bash
gh run download <run-id>
```

---

## Troubleshooting

### Issue: "Error: Not authorized to perform sts:AssumeRoleWithWebIdentity"

**Cause**: OIDC authentication failed

**Solutions**:
1. Verify `AWS_ROLE_ARN` secret is correct
2. Check workflow has `id-token: write` permission
3. Verify trust policy allows repository

### Issue: "Tests fail to run"

**Causes**:
- Environment not deployed
- Services not accessible
- Network connectivity issues

**Solutions**:
1. Verify environment is deployed and healthy
2. Check service endpoints are accessible
3. Review test logs for specific errors

### Issue: "Report upload fails"

**Causes**:
- S3 bucket doesn't exist
- IAM permissions insufficient
- Network issues

**Solutions**:
1. Verify S3 bucket exists: `aws s3 ls s3://turaf-architecture-test-reports-dev/`
2. Check IAM permissions include `s3:PutObject`
3. Review workflow logs for specific error

### Issue: "CloudFront invalidation fails"

**Causes**:
- CloudFront distribution doesn't exist
- IAM permissions insufficient
- Invalid distribution ID

**Solutions**:
1. Verify CloudFront distribution exists
2. Check IAM permissions include `cloudfront:CreateInvalidation`
3. Workflow will skip invalidation if distribution not found (non-fatal)

### Issue: "Test results not appearing in GitHub Actions"

**Cause**: JUnit XML files not generated

**Solutions**:
1. Verify tests ran successfully
2. Check `target/surefire-reports/` directory exists
3. Review Maven test execution logs

---

## Best Practices

### When to Run Tests

- **DEV**: After every deployment (automatic via schedule)
- **QA**: Before promoting to production
- **PROD**: After production deployments (smoke tests only)

### Interpreting Results

- **All Passed**: Environment is healthy, APIs working correctly
- **Some Failed**: Investigate failures, may indicate deployment issues
- **All Failed**: Environment likely down or misconfigured

### Report Retention

- Reports are automatically deleted after retention period
- Download important reports before expiration
- Consider archiving critical test runs

---

## Integration with CI/CD

### Deployment Pipeline Integration

```yaml
# Example: Run tests after deployment
jobs:
  deploy:
    # ... deployment steps ...
  
  verify:
    needs: deploy
    uses: ./.github/workflows/architecture-tests.yml
    with:
      environment: ${{ github.event.inputs.environment }}
```

### Pull Request Validation

Tests can be triggered on PRs to validate changes:

```yaml
on:
  pull_request:
    branches: [main, develop]
```

---

## Performance Considerations

### Execution Time

- **Typical run**: 5-15 minutes
- **Factors**: Number of tests, API response times, network latency
- **Optimization**: Run critical tests first, parallelize where possible

### Cost Implications

- **GitHub Actions**: Free for public repos, metered for private
- **AWS**: S3 storage, CloudFront bandwidth, API calls
- **Estimated**: ~$5-10/month for regular testing

---

## Security Considerations

1. **No Credentials in Code**: Uses OIDC, no long-lived credentials
2. **Environment Isolation**: Each environment has separate secrets
3. **Least Privilege**: IAM role has minimal required permissions
4. **Audit Trail**: All runs logged in GitHub Actions and CloudTrail

---

## Related Documentation

- [Architecture Testing Specification](../../specs/architecture-testing.md)
- [Task 006: Create GitHub Actions Workflow](../../tasks/architecture-tests/006-create-github-actions-workflow.md)
- [IAM Permissions](../../infrastructure/github-oidc-roles.md)
- [Terraform Infrastructure](../../services/architecture-tests/terraform/README.md)

---

## Maintenance

### Updating the Workflow

1. Edit `.github/workflows/architecture-tests.yml`
2. Test changes in DEV environment
3. Commit and push changes
4. Workflow updates automatically

### Changing Schedule

Edit the cron expression in workflow file:
```yaml
schedule:
  - cron: '0 */6 * * *'  # Every 6 hours
```

Common schedules:
- Every hour: `'0 * * * *'`
- Every 12 hours: `'0 */12 * * *'`
- Daily at midnight: `'0 0 * * *'`
- Weekdays at 9 AM: `'0 9 * * 1-5'`

### Disabling Scheduled Runs

Comment out or remove the `schedule` trigger:
```yaml
# schedule:
#   - cron: '0 */6 * * *'
```

---

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review workflow logs in GitHub Actions
3. Check AWS CloudWatch logs for service issues
4. Consult architecture testing documentation
