# Operational Runbooks

**Last Updated**: March 25, 2026  
**Status**: Current  
**Related Documents**: [Deployment Runbook](../DEPLOYMENT_RUNBOOK.md), [Troubleshooting](../troubleshooting/COMMON_ISSUES.md)

This document contains operational procedures for common tasks and incident response for the Turaf platform.

---

## Table of Contents

1. [Service Deployment](#service-deployment)
2. [Rollback Procedures](#rollback-procedures)
3. [Scaling Operations](#scaling-operations)
4. [Database Operations](#database-operations)
5. [Incident Response](#incident-response)
6. [Maintenance Windows](#maintenance-windows)

---

## Service Deployment

### Deploy Service to DEV

**Prerequisites**:
- Code merged to `develop` branch
- CI tests passing
- Docker image built and pushed to ECR

**Procedure**:

1. **Trigger Deployment**:
   ```bash
   # Automatic: Push to develop branch triggers workflow
   git push origin develop
   
   # Manual: Trigger via GitHub Actions UI
   # Navigate to Actions → Service Deployment → Run workflow
   ```

2. **Monitor Deployment**:
   ```bash
   # Watch GitHub Actions workflow
   gh run watch
   
   # Monitor ECS service
   watch -n 5 'aws ecs describe-services \
     --cluster turaf-cluster-dev \
     --services identity-service-dev \
     --query "services[0].{Running:runningCount,Desired:desiredCount,Status:status}"'
   ```

3. **Verify Deployment**:
   ```bash
   # Check service health
   curl http://api.dev.turafapp.com/api/identity/actuator/health
   
   # Check logs
   aws logs tail /ecs/identity-service-dev --follow
   ```

4. **Smoke Test**:
   ```bash
   # Run basic API tests
   curl -X POST http://api.dev.turafapp.com/api/identity/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"test@example.com","password":"password"}'
   ```

**Expected Duration**: 5-10 minutes

---

### Deploy Service to QA

**Prerequisites**:
- Service tested in DEV
- Code merged to `release/*` branch
- QA environment ready

**Procedure**:

1. **Create Release Branch**:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b release/v1.2.0
   git push origin release/v1.2.0
   ```

2. **Trigger Deployment**:
   - GitHub Actions workflow triggers automatically
   - **Manual Approval Required**: 1 reviewer must approve in GitHub UI

3. **Approve Deployment**:
   - Navigate to GitHub Actions → Pending deployment
   - Review changes and approve

4. **Monitor & Verify**:
   ```bash
   # Monitor deployment
   gh run watch
   
   # Verify health
   curl http://api.qa.turafapp.com/api/identity/actuator/health
   
   # Run integration tests
   npm run test:integration:qa
   ```

**Expected Duration**: 10-15 minutes (including approval)

---

### Deploy Service to PROD

**Prerequisites**:
- Service tested in QA
- Code merged to `main` branch
- Change request approved
- Stakeholders notified

**Procedure**:

1. **Pre-Deployment Checklist**:
   - [ ] QA testing complete
   - [ ] Performance testing complete
   - [ ] Security scan passed
   - [ ] Database migrations tested
   - [ ] Rollback plan documented
   - [ ] On-call engineer available

2. **Create GitHub Release**:
   ```bash
   git checkout main
   git pull origin main
   git tag -a v1.2.0 -m "Release v1.2.0"
   git push origin v1.2.0
   
   # Create release in GitHub UI
   gh release create v1.2.0 --title "v1.2.0" --notes "Release notes..."
   ```

3. **Trigger Deployment**:
   - Workflow triggers on release creation
   - **Manual Approval Required**: 2+ reviewers must approve
   - **Wait Timer**: 10-minute cooling-off period

4. **Approve Deployment**:
   - Senior engineers review changes
   - Approve in GitHub Actions UI

5. **Monitor Blue-Green Deployment**:
   ```bash
   # Watch CodeDeploy deployment
   aws deploy get-deployment \
     --deployment-id <deployment-id> \
     --query 'deploymentInfo.status'
   
   # Monitor CloudWatch alarms
   aws cloudwatch describe-alarms \
     --alarm-names turaf-identity-service-prod-errors
   ```

6. **Verify Deployment**:
   ```bash
   # Health check
   curl https://api.turafapp.com/api/identity/actuator/health
   
   # Run E2E tests
   npm run test:e2e:prod
   
   # Monitor for 5 minutes
   watch -n 30 'aws cloudwatch get-metric-statistics \
     --namespace AWS/ApplicationELB \
     --metric-name TargetResponseTime \
     --dimensions Name=TargetGroup,Value=<tg-arn> \
     --start-time $(date -u -d "5 minutes ago" +%Y-%m-%dT%H:%M:%S) \
     --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
     --period 60 \
     --statistics Average'
   ```

7. **Post-Deployment**:
   - Update deployment log
   - Notify stakeholders
   - Monitor for 1 hour

**Expected Duration**: 20-30 minutes (including approvals and monitoring)

---

## Rollback Procedures

### Rollback Service Deployment

**When to Rollback**:
- Health checks failing
- Error rate > 5%
- Performance degradation > 50%
- Critical bugs discovered

**Procedure**:

1. **Identify Previous Version**:
   ```bash
   # Get previous image tag
   aws ecr describe-images \
     --repository-name turaf/identity-service \
     --query 'sort_by(imageDetails,& imagePushedAt)[-2].imageTags[0]'
   ```

2. **Rollback via Terraform**:
   ```bash
   cd services/identity-service/terraform
   
   # Update image tag to previous version
   terraform plan -var="image_tag=<previous-sha>"
   terraform apply -var="image_tag=<previous-sha>"
   ```

3. **Rollback via GitHub Actions**:
   ```bash
   # Re-run previous successful workflow
   gh workflow run service-identity-prod.yml \
     --ref <previous-commit-sha>
   ```

4. **Emergency Rollback (PROD only)**:
   ```bash
   # CodeDeploy auto-rollback on alarm triggers
   # Manual rollback:
   aws deploy stop-deployment \
     --deployment-id <deployment-id> \
     --auto-rollback-enabled
   ```

5. **Verify Rollback**:
   ```bash
   # Check running version
   aws ecs describe-task-definition \
     --task-definition identity-service-prod \
     --query 'taskDefinition.containerDefinitions[0].image'
   
   # Verify health
   curl https://api.turafapp.com/api/identity/actuator/health
   ```

**Expected Duration**: 5-10 minutes

---

## Scaling Operations

### Scale Service Up

**When to Scale**:
- CPU utilization > 70%
- Memory utilization > 80%
- Request latency increasing
- Anticipated traffic spike

**Procedure**:

1. **Update Desired Count**:
   ```bash
   cd services/identity-service/terraform
   
   # Update desired_count variable
   terraform plan -var="desired_count=5"
   terraform apply -var="desired_count=5"
   ```

2. **Monitor Scaling**:
   ```bash
   # Watch task count
   watch -n 5 'aws ecs describe-services \
     --cluster turaf-cluster-prod \
     --services identity-service-prod \
     --query "services[0].{Running:runningCount,Desired:desiredCount}"'
   ```

3. **Verify Load Distribution**:
   ```bash
   # Check target group targets
   aws elbv2 describe-target-health \
     --target-group-arn <arn>
   ```

**Expected Duration**: 2-5 minutes

---

### Scale Service Down

**When to Scale Down**:
- Off-peak hours
- Traffic decreased
- Cost optimization

**Procedure**:

1. **Gradual Scale Down**:
   ```bash
   # Reduce by 1-2 tasks at a time
   terraform apply -var="desired_count=3"
   
   # Wait 5 minutes and monitor
   # If stable, continue scaling down
   ```

2. **Monitor Impact**:
   ```bash
   # Watch response times
   aws cloudwatch get-metric-statistics \
     --namespace AWS/ApplicationELB \
     --metric-name TargetResponseTime \
     --dimensions Name=TargetGroup,Value=<tg-arn> \
     --start-time $(date -u -d "10 minutes ago" +%Y-%m-%dT%H:%M:%S) \
     --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
     --period 60 \
     --statistics Average
   ```

**Expected Duration**: 5-10 minutes

---

## Database Operations

### Run Database Migration

**Prerequisites**:
- Migration scripts tested in DEV/QA
- Database backup taken
- Maintenance window scheduled (PROD)

**Procedure**:

1. **Backup Database** (PROD only):
   ```bash
   # Create manual snapshot
   aws rds create-db-snapshot \
     --db-instance-identifier turaf-db-prod \
     --db-snapshot-identifier turaf-db-prod-pre-migration-$(date +%Y%m%d-%H%M%S)
   
   # Wait for snapshot to complete
   aws rds wait db-snapshot-completed \
     --db-snapshot-identifier <snapshot-id>
   ```

2. **Deploy Service with Migration**:
   ```bash
   # Migration runs automatically on service startup
   # Flyway executes pending migrations
   
   # Monitor migration in logs
   aws logs tail /ecs/identity-service-prod --follow | grep "Flyway"
   ```

3. **Verify Migration**:
   ```sql
   -- Connect to database
   psql -h <rds-endpoint> -U turaf_admin -d turaf
   
   -- Check migration history
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   
   -- Verify schema changes
   \d+ <table_name>
   ```

4. **Rollback Migration** (if needed):
   ```bash
   # Restore from snapshot (PROD)
   aws rds restore-db-instance-from-db-snapshot \
     --db-instance-identifier turaf-db-prod-restored \
     --db-snapshot-identifier <snapshot-id>
   
   # Or run down migration (DEV/QA)
   flyway undo -url=jdbc:postgresql://... -user=... -password=...
   ```

**Expected Duration**: 5-30 minutes (depending on migration complexity)

---

### Database Performance Tuning

**When to Tune**:
- Slow query performance
- High CPU/memory usage
- Connection pool exhaustion

**Procedure**:

1. **Identify Slow Queries**:
   ```sql
   -- Enable pg_stat_statements
   CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
   
   -- Find slow queries
   SELECT query, calls, total_time, mean_time
   FROM pg_stat_statements
   ORDER BY mean_time DESC
   LIMIT 10;
   ```

2. **Analyze Query Plans**:
   ```sql
   EXPLAIN ANALYZE <slow-query>;
   ```

3. **Add Indexes**:
   ```sql
   -- Create index
   CREATE INDEX CONCURRENTLY idx_users_email ON users(email);
   
   -- Verify index usage
   EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';
   ```

4. **Update Connection Pool**:
   ```yaml
   # In application.yml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20  # Increase if needed
         minimum-idle: 5
   ```

---

## Incident Response

### High Error Rate Alert

**Severity**: P1 (Critical)  
**Response Time**: Immediate

**Procedure**:

1. **Acknowledge Alert**:
   ```bash
   # Check CloudWatch alarm
   aws cloudwatch describe-alarms \
     --alarm-names turaf-identity-service-prod-errors \
     --query 'MetricAlarms[0].{State:StateValue,Reason:StateReason}'
   ```

2. **Assess Impact**:
   ```bash
   # Check error rate
   aws logs insights query \
     --log-group-name /ecs/identity-service-prod \
     --start-time $(date -u -d "15 minutes ago" +%s) \
     --end-time $(date -u +%s) \
     --query-string 'fields @timestamp, @message | filter @message like /ERROR/ | stats count() by bin(5m)'
   ```

3. **Identify Root Cause**:
   ```bash
   # Check recent deployments
   aws ecs describe-services \
     --cluster turaf-cluster-prod \
     --services identity-service-prod \
     --query 'services[0].deployments'
   
   # Check logs for errors
   aws logs tail /ecs/identity-service-prod --since 15m | grep ERROR
   ```

4. **Mitigate**:
   - If recent deployment: **Rollback immediately**
   - If database issue: **Scale up RDS or add read replicas**
   - If dependency issue: **Enable circuit breaker or fallback**

5. **Communicate**:
   - Post in #incidents Slack channel
   - Update status page
   - Notify stakeholders

6. **Post-Incident**:
   - Write incident report
   - Schedule postmortem
   - Create action items

---

### Service Outage

**Severity**: P0 (Critical)  
**Response Time**: Immediate

**Procedure**:

1. **Declare Incident**:
   ```bash
   # Post in #incidents
   # Page on-call engineer
   # Update status page
   ```

2. **Quick Assessment**:
   ```bash
   # Check service status
   aws ecs describe-services \
     --cluster turaf-cluster-prod \
     --services identity-service-prod \
     --query 'services[0].{Running:runningCount,Desired:desiredCount,Status:status}'
   
   # Check ALB health
   aws elbv2 describe-target-health \
     --target-group-arn <arn>
   ```

3. **Immediate Actions**:
   - **If all tasks stopped**: Check CloudWatch logs, restart service
   - **If ALB unhealthy**: Check security groups, health check config
   - **If database down**: Check RDS status, restore from snapshot if needed

4. **Restore Service**:
   ```bash
   # Force new deployment
   aws ecs update-service \
     --cluster turaf-cluster-prod \
     --service identity-service-prod \
     --force-new-deployment
   
   # Or scale up
   terraform apply -var="desired_count=5"
   ```

5. **Monitor Recovery**:
   ```bash
   # Watch service come back
   watch -n 5 'curl -s https://api.turafapp.com/api/identity/actuator/health | jq .'
   ```

6. **All-Clear**:
   - Verify all services healthy
   - Update status page
   - Notify stakeholders
   - Schedule postmortem

---

## Maintenance Windows

### Scheduled Maintenance

**Typical Windows**:
- **DEV**: Anytime
- **QA**: Tuesday/Thursday 10 PM - 12 AM EST
- **PROD**: Sunday 2 AM - 4 AM EST

**Procedure**:

1. **Pre-Maintenance** (1 week before):
   - [ ] Schedule maintenance window
   - [ ] Notify stakeholders
   - [ ] Update status page
   - [ ] Prepare runbook
   - [ ] Test in QA

2. **Pre-Maintenance** (1 day before):
   - [ ] Confirm maintenance window
   - [ ] Backup databases
   - [ ] Verify rollback procedures
   - [ ] Assign roles (lead, backup, communicator)

3. **During Maintenance**:
   - [ ] Post start notification
   - [ ] Execute changes
   - [ ] Monitor systems
   - [ ] Document issues
   - [ ] Test functionality

4. **Post-Maintenance**:
   - [ ] Verify all systems operational
   - [ ] Post completion notification
   - [ ] Update documentation
   - [ ] Conduct retrospective

---

## Emergency Contacts

**On-Call Rotation**: See PagerDuty schedule

**Escalation Path**:
1. On-call engineer (immediate)
2. Platform team lead (15 minutes)
3. Engineering manager (30 minutes)
4. CTO (1 hour)

**Communication Channels**:
- **Incidents**: #incidents (Slack)
- **Status**: status.turafapp.com
- **PagerDuty**: https://turaf.pagerduty.com

---

## References

- **Deployment Runbook**: `../DEPLOYMENT_RUNBOOK.md`
- **Troubleshooting**: `../troubleshooting/COMMON_ISSUES.md`
- **Infrastructure Docs**: `../../infrastructure/docs/`
- **CI/CD Specs**: `../../specs/ci-cd-pipelines.md`

---

**Maintained By**: Platform Team  
**Review Frequency**: Quarterly  
**Last Reviewed**: March 25, 2026
