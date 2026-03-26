# Common Issues and Solutions

**Last Updated**: March 25, 2026  
**Status**: Current  
**Related Documents**: [Deployment Runbook](../DEPLOYMENT_RUNBOOK.md), [Local Development](../LOCAL_DEVELOPMENT.md)

This guide provides solutions to common issues encountered during development, deployment, and operations of the Turaf platform.

---

## Table of Contents

1. [Infrastructure Issues](#infrastructure-issues)
2. [CI/CD Issues](#cicd-issues)
3. [Database Issues](#database-issues)
4. [Service Issues](#service-issues)
5. [Local Development Issues](#local-development-issues)
6. [Networking Issues](#networking-issues)

---

## Infrastructure Issues

### Terraform State Lock

**Problem**: Terraform state is locked and cannot be modified

**Symptoms**:
```
Error: Error acquiring the state lock
Lock ID: 318d4783-7c3a-8071-cb26-a2d254d77552
```

**Solutions**:

1. **Wait for lock to release** (if another operation is running):
   ```bash
   # Check if operation is still running
   ps aux | grep terraform
   ```

2. **Force unlock** (if operation failed):
   ```bash
   cd infrastructure/terraform/environments/dev
   terraform force-unlock 318d4783-7c3a-8071-cb26-a2d254d77552
   ```

3. **Verify DynamoDB lock table**:
   ```bash
   aws dynamodb scan \
     --table-name turaf-terraform-locks-dev \
     --region us-east-1
   ```

**Prevention**:
- Always use `terraform apply` with proper error handling
- Don't interrupt Terraform operations
- Use separate state files per service

---

### ECS Service Won't Start

**Problem**: ECS tasks failing to start or immediately stopping

**Symptoms**:
- Tasks in STOPPED state
- Service shows 0 running tasks
- Deployment stuck

**Common Causes & Solutions**:

1. **Docker image doesn't exist in ECR**:
   ```bash
   # Check if image exists
   aws ecr describe-images \
     --repository-name turaf/identity-service \
     --image-ids imageTag=dev-latest \
     --region us-east-1
   
   # If missing, build and push
   cd services/identity-service
   docker build -t <ecr-url>/turaf/identity-service:dev-latest .
   docker push <ecr-url>/turaf/identity-service:dev-latest
   ```

2. **Insufficient CPU/Memory**:
   ```bash
   # Check task definition limits
   aws ecs describe-task-definition \
     --task-definition identity-service-dev \
     --query 'taskDefinition.{CPU:cpu,Memory:memory}'
   
   # Update if needed
   # Edit services/identity-service/terraform/main.tf
   # Increase cpu and memory values
   ```

3. **Missing environment variables or secrets**:
   ```bash
   # Check CloudWatch logs for errors
   aws logs tail /ecs/identity-service-dev --follow
   
   # Common missing vars:
   # - DATABASE_URL
   # - REDIS_URL
   # - JWT_SECRET
   ```

4. **Security group blocking traffic**:
   ```bash
   # Verify security group allows traffic from ALB
   aws ec2 describe-security-groups \
     --group-ids sg-xxxxx \
     --query 'SecurityGroups[0].IpPermissions'
   ```

5. **Health check failing**:
   ```bash
   # Check target group health
   aws elbv2 describe-target-health \
     --target-group-arn <target-group-arn>
   
   # Common issues:
   # - Wrong health check path
   # - Service not listening on correct port
   # - Health endpoint returning non-200 status
   ```

**Debugging Steps**:
```bash
# 1. Get stopped task ARN
TASK_ARN=$(aws ecs list-tasks \
  --cluster turaf-cluster-dev \
  --desired-status STOPPED \
  --query 'taskArns[0]' \
  --output text)

# 2. Describe stopped task
aws ecs describe-tasks \
  --cluster turaf-cluster-dev \
  --tasks $TASK_ARN \
  --query 'tasks[0].{StoppedReason:stoppedReason,Containers:containers[0].reason}'

# 3. Check CloudWatch logs
aws logs tail /ecs/identity-service-dev --since 10m
```

---

### ALB Target Group Unhealthy

**Problem**: ALB target group shows all targets as unhealthy

**Symptoms**:
- 503 Service Unavailable errors
- Target group health check failing
- No traffic reaching service

**Solutions**:

1. **Verify health check configuration**:
   ```bash
   aws elbv2 describe-target-groups \
     --target-group-arns <arn> \
     --query 'TargetGroups[0].HealthCheckPath'
   
   # Ensure path matches service health endpoint
   # Default: /actuator/health
   ```

2. **Check service is responding**:
   ```bash
   # Get task private IP
   TASK_IP=$(aws ecs describe-tasks \
     --cluster turaf-cluster-dev \
     --tasks <task-arn> \
     --query 'tasks[0].containers[0].networkInterfaces[0].privateIpv4Address' \
     --output text)
   
   # Test health endpoint (from bastion or VPC)
   curl http://$TASK_IP:8080/actuator/health
   ```

3. **Verify security group rules**:
   ```bash
   # ECS security group must allow traffic from ALB security group
   # on the container port (usually 8080)
   ```

4. **Check health check thresholds**:
   ```hcl
   # In service Terraform:
   health_check {
     healthy_threshold   = 2  # Too high?
     unhealthy_threshold = 3
     timeout             = 5  # Too low?
     interval            = 30
   }
   ```

---

## CI/CD Issues

### GitHub Actions OIDC Authentication Failure

**Problem**: Cannot assume AWS IAM role from GitHub Actions

**Symptoms**:
```
Error: Could not assume role with OIDC
```

**Solutions**:

1. **Verify OIDC provider exists**:
   ```bash
   aws iam list-open-id-connect-providers
   # Should show: arn:aws:iam::ACCOUNT:oidc-provider/token.actions.githubusercontent.com
   ```

2. **Check IAM role trust policy**:
   ```bash
   aws iam get-role --role-name GitHubActionsDeploymentRole \
     --query 'Role.AssumeRolePolicyDocument'
   
   # Should include:
   # - Correct GitHub repository
   # - Correct branch/tag pattern
   # - token.actions.githubusercontent.com as principal
   ```

3. **Verify workflow permissions**:
   ```yaml
   # In workflow file:
   permissions:
     id-token: write  # Required for OIDC
     contents: read
   ```

4. **Check branch/tag matches trust policy**:
   ```json
   // Trust policy condition:
   "StringLike": {
     "token.actions.githubusercontent.com:sub": [
       "repo:ryanwaite28/ai-projects-turaf:ref:refs/heads/develop",
       "repo:ryanwaite28/ai-projects-turaf:ref:refs/heads/main"
     ]
   }
   ```

---

### Docker Build Fails in GitHub Actions

**Problem**: Docker build succeeds locally but fails in CI

**Common Causes**:

1. **Missing build context**:
   ```yaml
   # Ensure correct context path
   - name: Build
     working-directory: services/identity-service
     run: docker build -t image:tag .
   ```

2. **Multi-stage build issues**:
   ```dockerfile
   # Ensure all stages have required files
   FROM maven:3.9-eclipse-temurin-17 AS build
   COPY pom.xml .
   COPY src ./src
   RUN mvn clean package -DskipTests
   ```

3. **Platform mismatch**:
   ```yaml
   # For ARM-based images on x86 runners
   - name: Set up QEMU
     uses: docker/setup-qemu-action@v3
   
   - name: Set up Docker Buildx
     uses: docker/setup-buildx-action@v3
   ```

---

### Terraform Apply Fails in CI/CD

**Problem**: Terraform plan succeeds but apply fails

**Solutions**:

1. **Check for resource conflicts**:
   ```bash
   # Resource already exists
   # Solution: Import existing resource or remove from AWS
   terraform import aws_ecs_service.service <cluster>/<service-name>
   ```

2. **Verify AWS credentials**:
   ```bash
   # In workflow, add debug step:
   - name: Verify AWS credentials
     run: |
       aws sts get-caller-identity
       aws sts get-session-token
   ```

3. **Check Terraform state**:
   ```bash
   # State might be corrupted or out of sync
   terraform state list
   terraform state show <resource>
   ```

---

## Database Issues

### Connection Refused to RDS

**Problem**: Cannot connect to RDS PostgreSQL database

**Symptoms**:
```
Connection refused: connect
FATAL: no pg_hba.conf entry for host
```

**Solutions**:

1. **Verify security group allows traffic**:
   ```bash
   # RDS security group must allow traffic from ECS security group
   aws ec2 describe-security-groups \
     --group-ids <rds-sg-id> \
     --query 'SecurityGroups[0].IpPermissions'
   ```

2. **Check RDS instance is running**:
   ```bash
   aws rds describe-db-instances \
     --db-instance-identifier turaf-db-dev \
     --query 'DBInstances[0].DBInstanceStatus'
   ```

3. **Verify connection string**:
   ```bash
   # Format: postgresql://username:password@host:5432/database
   # Get endpoint:
   aws rds describe-db-instances \
     --db-instance-identifier turaf-db-dev \
     --query 'DBInstances[0].Endpoint.Address' \
     --output text
   ```

4. **Test connection from ECS task**:
   ```bash
   # Use ECS Exec to connect to running task
   aws ecs execute-command \
     --cluster turaf-cluster-dev \
     --task <task-id> \
     --container identity-service \
     --interactive \
     --command "/bin/bash"
   
   # Inside container:
   psql -h <rds-endpoint> -U turaf_admin -d turaf
   ```

---

### Redis Connection Timeout

**Problem**: Cannot connect to ElastiCache Redis

**Solutions**:

1. **Verify Redis cluster is available**:
   ```bash
   aws elasticache describe-replication-groups \
     --replication-group-id turaf-redis-dev \
     --query 'ReplicationGroups[0].Status'
   ```

2. **Check security group**:
   ```bash
   # ElastiCache security group must allow traffic from ECS
   # on port 6379
   ```

3. **Get Redis endpoint**:
   ```bash
   aws elasticache describe-replication-groups \
     --replication-group-id turaf-redis-dev \
     --query 'ReplicationGroups[0].NodeGroups[0].PrimaryEndpoint.Address'
   ```

---

### Database Migration Fails

**Problem**: Flyway migration fails during service startup

**Solutions**:

1. **Check migration scripts**:
   ```bash
   # Ensure migrations are in correct location
   ls -la services/identity-service/src/main/resources/db/migration/
   
   # Naming: V1__description.sql, V2__description.sql
   ```

2. **Verify database schema exists**:
   ```sql
   -- Connect to database
   \c turaf
   \dn
   -- Should show: identity, organization, experiment schemas
   ```

3. **Check Flyway history**:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
   ```

4. **Reset migration (DEV only)**:
   ```bash
   # WARNING: Deletes all data
   flyway clean -url=jdbc:postgresql://... -user=... -password=...
   ```

---

## Service Issues

### Service Returns 500 Internal Server Error

**Problem**: API requests return 500 errors

**Debugging Steps**:

1. **Check CloudWatch logs**:
   ```bash
   aws logs tail /ecs/identity-service-dev --follow --format short
   ```

2. **Look for stack traces**:
   ```bash
   # Common issues:
   # - NullPointerException
   # - Database connection errors
   # - Missing configuration
   ```

3. **Verify environment variables**:
   ```bash
   # Check task definition
   aws ecs describe-task-definition \
     --task-definition identity-service-dev \
     --query 'taskDefinition.containerDefinitions[0].environment'
   ```

4. **Check application health**:
   ```bash
   curl http://api.dev.turafapp.com/api/identity/actuator/health
   ```

---

### Event Not Being Consumed

**Problem**: Events published to EventBridge but not consumed by service

**Solutions**:

1. **Verify EventBridge rule exists**:
   ```bash
   aws events list-rules --name-prefix turaf
   ```

2. **Check SQS queue has messages**:
   ```bash
   aws sqs get-queue-attributes \
     --queue-url <queue-url> \
     --attribute-names ApproximateNumberOfMessages
   ```

3. **Verify SQS consumer is running**:
   ```bash
   # Check service logs for SQS polling
   aws logs filter-log-events \
     --log-group-name /ecs/identity-service-dev \
     --filter-pattern "SQS"
   ```

4. **Check IAM permissions**:
   ```bash
   # Task role must have sqs:ReceiveMessage, sqs:DeleteMessage
   ```

---

## Local Development Issues

### Docker Compose Services Won't Start

**Problem**: `docker-compose up` fails or services crash

**Solutions**:

1. **Check port conflicts**:
   ```bash
   # Ensure ports 5432, 6379, 8080-8083 are free
   lsof -i :5432
   lsof -i :6379
   ```

2. **Verify Docker resources**:
   ```bash
   # Ensure Docker has enough memory (4GB+)
   docker system df
   docker system prune  # Clean up if needed
   ```

3. **Check environment variables**:
   ```bash
   # Ensure .env file exists
   cp .env.example .env
   # Edit .env with correct values
   ```

4. **View service logs**:
   ```bash
   docker-compose logs -f <service-name>
   ```

---

### Maven Build Fails

**Problem**: `mvn clean package` fails

**Common Issues**:

1. **Missing dependencies**:
   ```bash
   # Clear local Maven cache
   rm -rf ~/.m2/repository
   mvn clean install
   ```

2. **Test failures**:
   ```bash
   # Skip tests temporarily
   mvn clean package -DskipTests
   
   # Run specific test
   mvn test -Dtest=UserServiceTest
   ```

3. **Java version mismatch**:
   ```bash
   # Ensure Java 17
   java -version
   # Should show: openjdk version "17.x.x"
   ```

---

## Networking Issues

### Cannot Access Service via ALB

**Problem**: ALB returns 503 or connection timeout

**Solutions**:

1. **Check ALB listener rules**:
   ```bash
   aws elbv2 describe-rules \
     --listener-arn <listener-arn> \
     --query 'Rules[*].{Priority:Priority,PathPattern:Conditions[0].Values}'
   ```

2. **Verify target group has healthy targets**:
   ```bash
   aws elbv2 describe-target-health \
     --target-group-arn <arn>
   ```

3. **Check ALB security group**:
   ```bash
   # Must allow inbound traffic on ports 80/443
   aws ec2 describe-security-groups \
     --group-ids <alb-sg-id>
   ```

4. **Test from within VPC**:
   ```bash
   # Use bastion host or ECS Exec
   curl http://internal-alb.dev.turafapp.com/api/identity/health
   ```

---

## Getting Help

If issues persist:

1. **Check CloudWatch Logs**: Most issues show up in logs
2. **Review Recent Changes**: Check git history and deployments
3. **Consult Documentation**: [Architecture](../../specs/architecture.md), [Infrastructure](../../infrastructure/docs/README.md)
4. **Ask Team**: Post in Slack #turaf-support channel
5. **Create Issue**: Document the problem in GitHub Issues

---

## References

- **Deployment Runbook**: `../DEPLOYMENT_RUNBOOK.md`
- **Local Development**: `../LOCAL_DEVELOPMENT.md`
- **Infrastructure Docs**: `../../infrastructure/docs/`
- **Service Specs**: `../../specs/`

---

**Maintained By**: Platform Team  
**Last Reviewed**: March 25, 2026
