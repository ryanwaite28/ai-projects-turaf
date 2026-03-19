# Deployment Runbook

This runbook provides step-by-step deployment procedures for the Turaf platform across all AWS environments.

**GitHub Repository**: https://github.com/ryanwaite28/ai-projects-turaf

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Overview](#environment-overview)
3. [DEV Deployment](#dev-deployment)
4. [QA Deployment](#qa-deployment)
5. [PROD Deployment](#prod-deployment)
6. [Rollback Procedures](#rollback-procedures)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### AWS Account Access

Ensure you have access to the appropriate AWS accounts:

| Environment | AWS Account ID | Required Access |
|-------------|---------------|-----------------|
| DEV | 801651112319 | Developer/Admin |
| QA | 965932217544 | QA Team/Admin |
| PROD | 811783768245 | Operations/Admin |
| Ops | 146072879609 | DevOps Team |

### GitHub Access

- Repository: https://github.com/ryanwaite28/ai-projects-turaf
- Permissions: Write access for deployments
- Branch access: Appropriate branch permissions

### Tools Required

```bash
# Verify tool versions
aws --version          # AWS CLI 2.x
terraform --version    # Terraform 1.5+
docker --version       # Docker 20.x+
git --version          # Git 2.x+
```

### GitHub Secrets Configuration

Verify GitHub secrets are configured:

**Repository Secrets**:
- `SONAR_TOKEN`
- `SONAR_HOST_URL`
- `SLACK_WEBHOOK_URL`

**Environment Secrets**:
- DEV: `AWS_ACCOUNT_ID` = `801651112319`
- QA: `AWS_ACCOUNT_ID` = `965932217544`
- PROD: `AWS_ACCOUNT_ID` = `811783768245`

---

## Environment Overview

### Deployment Matrix

| Environment | Branch | Trigger | Approval | AWS Account |
|-------------|--------|---------|----------|-------------|
| **DEV** | `develop` | Automatic | None | 801651112319 |
| **QA** | `release/*` | Automatic | Optional | 965932217544 |
| **PROD** | `main` | Manual | Required (2) | 811783768245 |

### Deployment Components

Each deployment includes:
1. **Infrastructure** (Terraform)
2. **Backend Services** (ECS containers)
3. **Lambda Functions** (Reporting, Notifications)
4. **Frontend** (Angular SPA to S3/CloudFront)

---

## DEV Deployment

### Automatic Deployment (via GitHub Actions)

**Trigger**: Push to `develop` branch

```bash
# Make changes on feature branch
git checkout -b feature/my-feature
# ... make changes ...
git add .
git commit -m "feat: add new feature"
git push origin feature/my-feature

# Create PR and merge to develop
# Deployment starts automatically
```

**Workflow**: `.github/workflows/cd-dev.yml`

**Steps**:
1. Build Docker images
2. Push to ECR (801651112319)
3. Deploy infrastructure (Terraform)
4. Update ECS services
5. Deploy Lambda functions
6. Deploy frontend to S3
7. Invalidate CloudFront cache
8. Run smoke tests

**Monitoring**:
```bash
# Watch GitHub Actions
# https://github.com/ryanwaite28/ai-projects-turaf/actions

# Monitor ECS deployment
aws ecs describe-services \
  --cluster turaf-cluster-dev \
  --services turaf-experiment-service-dev \
  --region us-east-1 \
  --profile turaf-dev
```

### Manual Deployment (Emergency)

If GitHub Actions is unavailable:

```bash
# 1. Set AWS credentials
export AWS_PROFILE=turaf-dev

# 2. Build and push images
cd services/experiment-service
docker build -t turaf/experiment-service:manual .
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 801651112319.dkr.ecr.us-east-1.amazonaws.com
docker tag turaf/experiment-service:manual 801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service:manual
docker push 801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service:manual

# 3. Update ECS service
aws ecs update-service \
  --cluster turaf-cluster-dev \
  --service turaf-experiment-service-dev \
  --force-new-deployment \
  --region us-east-1

# 4. Wait for deployment
aws ecs wait services-stable \
  --cluster turaf-cluster-dev \
  --services turaf-experiment-service-dev \
  --region us-east-1
```

### Validation

```bash
# Health checks
curl -f https://api.dev.turaf.com/api/v1/experiments/health

# Smoke tests
curl -X POST https://api.dev.turaf.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","name":"Test User"}'

# Check logs
aws logs tail /ecs/turaf-experiment-service --follow --region us-east-1 --profile turaf-dev
```

---

## QA Deployment

### Automatic Deployment (via GitHub Actions)

**Trigger**: Push to `release/*` branch

```bash
# Create release branch from develop
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0

# Update version
echo "1.2.0" > VERSION
git add VERSION
git commit -m "chore: bump version to v1.2.0"
git push origin release/v1.2.0

# Deployment starts automatically
```

**Workflow**: `.github/workflows/cd-qa.yml`

**Steps**:
1. Build Docker images
2. Push to ECR (965932217544)
3. Deploy infrastructure (Terraform)
4. Update ECS services
5. Deploy Lambda functions
6. Deploy frontend to S3
7. Invalidate CloudFront cache
8. Run integration tests
9. Run E2E tests
10. Wait for approval (optional)

### QA Validation Checklist

**Functional Testing**:
- [ ] User registration and login
- [ ] Problem creation and management
- [ ] Hypothesis creation
- [ ] Experiment lifecycle (create, start, complete)
- [ ] Metrics recording
- [ ] Report generation
- [ ] Email notifications

**Integration Testing**:
- [ ] All microservices communicating
- [ ] Event bus processing events
- [ ] Database connections stable
- [ ] S3 report storage working
- [ ] CloudFront serving frontend

**Performance Testing**:
- [ ] API response times < 500ms (P95)
- [ ] Report generation < 60s
- [ ] Event processing < 5s
- [ ] Frontend load time < 3s

### Approval Process

If approval is required:
1. QA team validates functionality
2. Review test results in GitHub Actions
3. Approve deployment in GitHub UI
4. Deployment proceeds automatically

---

## PROD Deployment

### Manual Deployment (with Approval)

**Trigger**: Manual workflow dispatch from `main` branch

**Prerequisites**:
1. QA validation complete
2. Release notes prepared
3. Stakeholder notification sent
4. Deployment window scheduled

**Steps**:

#### 1. Prepare Release

```bash
# Merge release branch to main
git checkout main
git pull origin main
git merge --no-ff release/v1.2.0
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin main --tags
```

#### 2. Trigger Deployment

1. Navigate to GitHub Actions
2. Select "Deploy to PROD" workflow
3. Click "Run workflow"
4. Select branch: `main`
5. Enter deployment notes
6. Click "Run workflow"

#### 3. Approval Gate

**Required Approvers**: 2 from authorized list
- Team leads
- Senior engineers
- DevOps team

**Approval Checklist**:
- [ ] QA validation complete
- [ ] Release notes reviewed
- [ ] Rollback plan ready
- [ ] Monitoring dashboards prepared
- [ ] On-call team notified

#### 4. Monitor Deployment

**Workflow**: `.github/workflows/cd-prod.yml`

**Blue-Green Deployment Steps**:
1. Build and push images to ECR (811783768245)
2. Deploy new task definitions
3. Create new task set (green)
4. Run smoke tests on green
5. Shift 10% traffic to green
6. Wait 5 minutes, monitor metrics
7. Shift 50% traffic to green
8. Wait 5 minutes, monitor metrics
9. Shift 100% traffic to green
10. Decommission old task set (blue)

**Monitoring During Deployment**:

```bash
# Watch ECS deployment
aws ecs describe-services \
  --cluster turaf-cluster-prod \
  --services turaf-experiment-service-prod \
  --region us-east-1 \
  --profile turaf-prod

# Monitor CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=turaf-experiment-service-prod \
  --start-time $(date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 \
  --statistics Average \
  --region us-east-1 \
  --profile turaf-prod

# Check error rates
aws logs tail /ecs/turaf-experiment-service --follow --region us-east-1 --profile turaf-prod | grep ERROR
```

#### 5. Post-Deployment Validation

**Health Checks** (within 5 minutes):
```bash
# API health
curl -f https://api.turaf.com/api/v1/experiments/health

# Frontend health
curl -f https://app.turaf.com

# Database connectivity
# (via internal monitoring dashboard)
```

**Smoke Tests** (within 10 minutes):
- [ ] User login successful
- [ ] Create new experiment
- [ ] Record metrics
- [ ] Generate report
- [ ] Verify email notification

**Metrics Validation** (within 30 minutes):
- [ ] Error rate < 1%
- [ ] P95 latency < 500ms
- [ ] No 5xx errors
- [ ] Database connections stable
- [ ] Event processing normal

#### 6. Stakeholder Notification

```bash
# Send deployment notification
# - Slack channel: #deployments
# - Email: stakeholders@turafapp.com
# - Status: Success/Failed
# - Version: v1.2.0
# - Deployment time: [timestamp]
```

---

## Rollback Procedures

### Automatic Rollback Triggers

Deployment automatically rolls back if:
- Error rate > 5% for 5 minutes
- P95 latency > 2x baseline for 10 minutes
- Health check failures > 50%
- Critical CloudWatch alarms triggered

### Manual Rollback (PROD)

**Scenario**: Critical issue discovered post-deployment

**Steps**:

#### 1. Initiate Rollback

```bash
# Option A: Via GitHub Actions
# 1. Navigate to GitHub Actions
# 2. Select "Rollback PROD" workflow
# 3. Enter target version (e.g., v1.1.9)
# 4. Approve rollback (1 reviewer required)

# Option B: Manual ECS rollback
export AWS_PROFILE=turaf-prod

# Get previous task definition
aws ecs describe-services \
  --cluster turaf-cluster-prod \
  --services turaf-experiment-service-prod \
  --query 'services[0].deployments[1].taskDefinition' \
  --output text

# Update service to previous task definition
aws ecs update-service \
  --cluster turaf-cluster-prod \
  --service turaf-experiment-service-prod \
  --task-definition turaf-experiment-service-prod:PREVIOUS_REVISION
```

#### 2. Rollback Lambda Functions

```bash
# List function versions
aws lambda list-versions-by-function \
  --function-name turaf-reporting-service-prod \
  --region us-east-1

# Rollback to previous version
aws lambda update-alias \
  --function-name turaf-reporting-service-prod \
  --name PROD \
  --function-version PREVIOUS_VERSION \
  --region us-east-1
```

#### 3. Rollback Frontend

```bash
# Sync previous frontend version from backup
aws s3 sync s3://turaf-frontend-prod-backup/v1.1.9/ s3://turaf-frontend-prod/ --delete

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id $CLOUDFRONT_DISTRIBUTION_ID_PROD \
  --paths "/*"
```

#### 4. Verify Rollback

```bash
# Health checks
curl -f https://api.turaf.com/api/v1/experiments/health

# Check version
curl https://api.turaf.com/api/v1/version

# Monitor metrics
# Verify error rates return to normal
# Verify latency returns to baseline
```

#### 5. Post-Rollback Actions

- [ ] Notify stakeholders of rollback
- [ ] Create incident report
- [ ] Schedule post-mortem
- [ ] Fix issues in develop branch
- [ ] Plan re-deployment

---

## Troubleshooting

### Common Issues

#### Issue: ECS Service Won't Stabilize

**Symptoms**: Service stuck in "deployment in progress"

**Diagnosis**:
```bash
aws ecs describe-services \
  --cluster turaf-cluster-prod \
  --services turaf-experiment-service-prod \
  --query 'services[0].events[0:10]'
```

**Solutions**:
1. Check task definition for errors
2. Verify security group rules
3. Check ECR image availability
4. Review CloudWatch logs for startup errors

#### Issue: High Error Rate After Deployment

**Symptoms**: 5xx errors increasing

**Diagnosis**:
```bash
# Check application logs
aws logs tail /ecs/turaf-experiment-service --follow --region us-east-1

# Check ALB target health
aws elbv2 describe-target-health \
  --target-group-arn TARGET_GROUP_ARN
```

**Solutions**:
1. Verify database connectivity
2. Check environment variables
3. Verify secrets in Secrets Manager
4. Review recent code changes
5. Consider rollback if critical

#### Issue: Frontend Not Updating

**Symptoms**: Users seeing old version

**Diagnosis**:
```bash
# Check S3 sync
aws s3 ls s3://turaf-frontend-prod/ --recursive

# Check CloudFront cache
aws cloudfront get-distribution --id $DISTRIBUTION_ID
```

**Solutions**:
1. Verify S3 sync completed
2. Create CloudFront invalidation
3. Check browser cache (hard refresh)
4. Verify CloudFront distribution settings

#### Issue: Lambda Function Errors

**Symptoms**: Event processing failures

**Diagnosis**:
```bash
# Check Lambda logs
aws logs tail /aws/lambda/turaf-reporting-service-prod --follow

# Check Lambda metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Errors \
  --dimensions Name=FunctionName,Value=turaf-reporting-service-prod \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Sum
```

**Solutions**:
1. Check function code for errors
2. Verify VPC configuration
3. Check IAM permissions
4. Review EventBridge rules
5. Check DLQ for failed events

### Emergency Contacts

**On-Call Rotation**:
- Primary: [On-call engineer]
- Secondary: [Backup engineer]
- Escalation: [Team lead]

**Communication Channels**:
- Slack: #incidents
- PagerDuty: Turaf Production
- Email: oncall@turafapp.com

---

## Deployment Checklist

### Pre-Deployment

- [ ] Code reviewed and approved
- [ ] Tests passing (unit, integration, E2E)
- [ ] Security scans passed
- [ ] QA validation complete (for PROD)
- [ ] Release notes prepared
- [ ] Rollback plan ready
- [ ] Stakeholders notified
- [ ] Deployment window scheduled
- [ ] On-call team alerted

### During Deployment

- [ ] Monitor GitHub Actions workflow
- [ ] Watch CloudWatch metrics
- [ ] Check application logs
- [ ] Verify health checks
- [ ] Monitor error rates
- [ ] Check latency metrics
- [ ] Validate traffic distribution (PROD)

### Post-Deployment

- [ ] Run smoke tests
- [ ] Verify all services healthy
- [ ] Check error rates (< 1%)
- [ ] Verify latency (< 500ms P95)
- [ ] Confirm event processing
- [ ] Test critical user flows
- [ ] Send deployment notification
- [ ] Update deployment log
- [ ] Close deployment ticket

---

## References

- [PROJECT.md](../PROJECT.md) - Project specifications
- [GITHUB.md](../GITHUB.md) - CI/CD workflows
- [AWS_ACCOUNTS.md](../AWS_ACCOUNTS.md) - AWS account details
- [AWS Multi-Account Strategy](AWS_MULTI_ACCOUNT_STRATEGY.md) - Architecture details
- [CI/CD Pipelines Spec](../specs/ci-cd-pipelines.md) - Pipeline details
