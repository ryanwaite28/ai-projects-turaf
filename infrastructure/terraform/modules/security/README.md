# Security Module

Terraform module for creating IAM roles, security groups, and KMS keys for the Turaf platform.

## Features

- **IAM Roles**: ECS execution and task roles with least privilege permissions
- **Security Groups**: Network security for ALB, ECS, RDS, ElastiCache, and DocumentDB
- **KMS Keys**: Encryption keys for general use, RDS, and S3
- **Least Privilege**: Fine-grained permissions following AWS best practices
- **Encryption**: All data encrypted at rest and in transit

## Architecture

### IAM Roles

**ECS Execution Role**:
- Pulls container images from ECR
- Retrieves secrets from Secrets Manager
- Writes logs to CloudWatch
- Decrypts KMS-encrypted data

**ECS Task Role**:
- Access to S3 buckets
- Send/receive SQS messages
- Publish to SNS topics
- Send emails via SES
- Read secrets from Secrets Manager
- Decrypt with KMS

### Security Groups

```
Internet
    ↓
[ALB Security Group]
    ↓ (port 8080)
[ECS Tasks Security Group]
    ↓ (port 5432)     ↓ (port 6379)     ↓ (port 27017)
[RDS SG]          [ElastiCache SG]   [DocumentDB SG]
```

**ALB Security Group**:
- Ingress: 80 (HTTP), 443 (HTTPS) from internet
- Egress: All traffic

**ECS Tasks Security Group**:
- Ingress: 8080 from ALB and VPC
- Egress: All traffic

**RDS Security Group**:
- Ingress: 5432 (PostgreSQL) from ECS tasks
- Egress: All traffic

**ElastiCache Security Group**:
- Ingress: 6379 (Redis) from ECS tasks
- Egress: All traffic

**DocumentDB Security Group**:
- Ingress: 27017 (MongoDB) from ECS tasks
- Egress: All traffic

### KMS Keys

**Main KMS Key**:
- General-purpose encryption
- Secrets Manager encryption
- Application data encryption

**RDS KMS Key**:
- Database encryption at rest
- Automated backups encryption

**S3 KMS Key**:
- S3 bucket encryption
- Object encryption

## Usage

```hcl
module "security" {
  source = "../../modules/security"

  environment = "dev"
  region      = "us-east-1"
  account_id  = "123456789012"
  vpc_id      = module.networking.vpc_id
  vpc_cidr    = "10.0.0.0/16"

  tags = {
    Project   = "turaf"
    ManagedBy = "terraform"
  }
}
```

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| environment | Environment name (dev, qa, prod) | string | - | yes |
| region | AWS region | string | us-east-1 | no |
| account_id | AWS account ID | string | - | yes |
| vpc_id | VPC ID for security groups | string | - | yes |
| vpc_cidr | VPC CIDR block | string | - | yes |
| tags | Additional tags | map(string) | {} | no |

## Outputs

| Name | Description |
|------|-------------|
| ecs_execution_role_arn | ECS execution role ARN |
| ecs_task_role_arn | ECS task role ARN |
| alb_security_group_id | ALB security group ID |
| ecs_tasks_security_group_id | ECS tasks security group ID |
| rds_security_group_id | RDS security group ID |
| elasticache_security_group_id | ElastiCache security group ID |
| documentdb_security_group_id | DocumentDB security group ID |
| kms_key_arn | Main KMS key ARN |
| rds_kms_key_arn | RDS KMS key ARN |
| s3_kms_key_arn | S3 KMS key ARN |

## IAM Permissions

### ECS Execution Role Permissions

**AWS Managed Policy**:
- `AmazonECSTaskExecutionRolePolicy`

**Custom Permissions**:
- `secretsmanager:GetSecretValue` - Retrieve secrets
- `kms:Decrypt` - Decrypt secrets and data
- `logs:CreateLogGroup` - Create log groups
- `logs:CreateLogStream` - Create log streams
- `logs:PutLogEvents` - Write logs

### ECS Task Role Permissions

**S3 Access**:
- `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` - Object operations
- `s3:ListBucket` - List bucket contents
- Scoped to: `turaf-{environment}-*` buckets

**SQS Access**:
- `sqs:SendMessage`, `sqs:ReceiveMessage`, `sqs:DeleteMessage` - Message operations
- `sqs:GetQueueAttributes` - Queue metadata
- Scoped to: `turaf-{environment}-*` queues

**SNS Access**:
- `sns:Publish` - Publish messages
- Scoped to: `turaf-{environment}-*` topics

**SES Access**:
- `ses:SendEmail`, `ses:SendRawEmail` - Send emails
- Condition: From address must be `noreply@turafapp.com`

**Secrets Manager Access**:
- `secretsmanager:GetSecretValue` - Retrieve secrets
- Scoped to: `turaf/{environment}/*` secrets

**KMS Access**:
- `kms:Decrypt`, `kms:DescribeKey` - Decrypt data
- Scoped to: Environment-specific KMS key

## Security Best Practices

### Least Privilege

- ✅ IAM roles scoped to specific resources
- ✅ Security groups allow only required ports
- ✅ KMS keys separate by use case
- ✅ Conditions on SES to prevent abuse

### Encryption

- ✅ KMS key rotation enabled
- ✅ Separate keys for different data types
- ✅ 30-day deletion window for recovery
- ✅ All data encrypted at rest

### Network Security

- ✅ Security groups use least privilege
- ✅ Database access only from application tier
- ✅ No direct internet access to databases
- ✅ ALB terminates SSL/TLS

### Secrets Management

- ✅ Secrets stored in Secrets Manager
- ✅ Secrets encrypted with KMS
- ✅ Secrets scoped by environment
- ✅ No hardcoded credentials

## Cost Estimation

### KMS Keys (3 keys)
- $1/month per key
- **Total**: $3/month

### Secrets Manager
- $0.40/month per secret
- ~10 secrets per environment
- **Total**: ~$4/month

### Security Groups & IAM Roles
- **Free**

**Total Monthly Cost**: ~$7/environment

## Examples

### Development Environment

```hcl
module "security" {
  source = "../../modules/security"

  environment = "dev"
  region      = "us-east-1"
  account_id  = "801651112319"
  vpc_id      = module.networking.vpc_id
  vpc_cidr    = "10.0.0.0/16"

  tags = {
    Environment = "dev"
    CostCenter  = "engineering"
  }
}
```

### Production Environment

```hcl
module "security" {
  source = "../../modules/security"

  environment = "prod"
  region      = "us-east-1"
  account_id  = "811783768245"
  vpc_id      = module.networking.vpc_id
  vpc_cidr    = "10.2.0.0/16"

  tags = {
    Environment = "prod"
    Compliance  = "required"
  }
}
```

## Troubleshooting

### Issue: ECS tasks can't pull images

**Problem**: Tasks fail with "CannotPullContainerError"

**Solutions**:
1. Verify ECS execution role has ECR permissions
2. Check VPC endpoints for ECR are configured
3. Verify security group allows outbound HTTPS
4. Check NAT gateway is working

### Issue: Tasks can't access RDS

**Problem**: Connection timeout to database

**Solutions**:
1. Verify RDS security group allows port 5432
2. Check ECS tasks security group is allowed
3. Verify RDS is in correct subnets
4. Check route tables

### Issue: KMS decrypt errors

**Problem**: "AccessDenied" when decrypting

**Solutions**:
1. Verify IAM role has `kms:Decrypt` permission
2. Check KMS key policy allows the role
3. Verify correct KMS key is being used
4. Check resource is encrypted with expected key

### Issue: Secrets Manager access denied

**Problem**: Can't retrieve secrets

**Solutions**:
1. Verify IAM role has `secretsmanager:GetSecretValue`
2. Check secret name matches pattern `turaf/{environment}/*`
3. Verify secret exists in correct region
4. Check KMS key permissions for secret encryption

## Security Hardening

### Additional Recommendations

**IAM**:
- Enable MFA for console access
- Use IAM Access Analyzer
- Regularly rotate access keys
- Enable CloudTrail for audit logs

**Network**:
- Enable VPC Flow Logs
- Use AWS WAF on ALB
- Implement rate limiting
- Enable DDoS protection (Shield)

**Encryption**:
- Use TLS 1.2+ only
- Rotate KMS keys annually
- Enable S3 bucket encryption
- Use encrypted EBS volumes

**Monitoring**:
- Set up CloudWatch alarms
- Enable GuardDuty
- Use Security Hub
- Monitor IAM activity

## References

- [AWS IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [VPC Security Groups](https://docs.aws.amazon.com/vpc/latest/userguide/VPC_SecurityGroups.html)
- [AWS KMS](https://docs.aws.amazon.com/kms/latest/developerguide/overview.html)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html)
- [ECS Task IAM Roles](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html)
