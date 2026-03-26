# AWS Multi-Account Strategy

This document outlines the multi-account AWS architecture strategy for the Turaf platform.

---

## Overview

The Turaf platform uses a **multi-account AWS architecture** to provide security isolation, blast radius limitation, and clear separation of concerns across environments.

**Organization ID**: `o-l3zk5a91yj`  
**Root Account**: `072456928432`  
**GitHub Repository**: https://github.com/ryanwaite28/ai-projects-turaf

---

## Account Structure

### AWS Organization Hierarchy

```
AWS Organization (o-l3zk5a91yj)
├── root (072456928432) - Management Account
├── Ops (146072879609) - DevOps Tooling
├── dev (801651112319) - Development Environment
├── qa (965932217544) - QA/Staging Environment
└── prod (811783768245) - Production Environment
```

### Account Details

| Account Name | Account ID | Email | Purpose | Workloads |
|--------------|------------|-------|---------|-----------|
| **root** | 072456928432 | aws@turafapp.com | AWS Organizations management | Billing, CloudTrail aggregation, SCPs |
| **Ops** | 146072879609 | aws-ops@turafapp.com | DevOps infrastructure | CI/CD tooling, centralized logging, monitoring |
| **dev** | 801651112319 | aws-dev@turafapp.com | Development environment | Feature development, testing |
| **qa** | 965932217544 | aws-qa@turafapp.com | QA/Staging environment | Integration testing, pre-production validation |
| **prod** | 811783768245 | aws-prod@turafapp.com | Production environment | Live customer workloads |

---

## Rationale for Multi-Account Architecture

### Security Isolation

**Blast Radius Limitation**:
- Security incidents contained to single account
- Compromised credentials limited to one environment
- Reduced risk of production data exposure

**IAM Boundary Enforcement**:
- Separate IAM policies per account
- No cross-environment privilege escalation
- Clear permission boundaries

**Network Isolation**:
- Separate VPCs per account
- No VPC peering between environments
- Independent network security groups

### Compliance and Governance

**Audit Trail Separation**:
- CloudTrail logs per account
- Environment-specific compliance requirements
- Clear accountability per environment

**Service Control Policies (SCPs)**:
- Organization-wide security controls
- Prevent accidental resource deletion
- Enforce encryption and security standards

### Cost Management

**Billing Separation**:
- Clear cost allocation per environment
- Budget alerts per account
- Chargeback to appropriate teams

**Resource Tagging**:
- Consistent tagging enforced via SCPs
- Cost tracking by environment
- Resource optimization per account

### Operational Efficiency

**Independent Deployments**:
- Deploy to environments without affecting others
- Parallel infrastructure changes
- Environment-specific configurations

**Failure Isolation**:
- Infrastructure failures contained
- Testing doesn't impact production
- Independent scaling per environment

---

## Account-Level Security Controls

### Service Control Policies (SCPs)

**Organization-Wide SCPs**:

1. **Prevent CloudTrail Deletion**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Deny",
         "Action": [
           "cloudtrail:DeleteTrail",
           "cloudtrail:StopLogging"
         ],
         "Resource": "*"
       }
     ]
   }
   ```

2. **Enforce Encryption at Rest**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Deny",
         "Action": [
           "s3:PutObject"
         ],
         "Resource": "*",
         "Condition": {
           "StringNotEquals": {
             "s3:x-amz-server-side-encryption": ["AES256", "aws:kms"]
           }
         }
       }
     ]
   }
   ```

3. **Restrict to us-east-1 Region**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Deny",
         "NotAction": [
           "iam:*",
           "organizations:*",
           "route53:*",
           "cloudfront:*",
           "support:*"
         ],
         "Resource": "*",
         "Condition": {
           "StringNotEquals": {
             "aws:RequestedRegion": "us-east-1"
           }
         }
       }
     ]
   }
   ```

4. **Block Public S3 Buckets**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Deny",
         "Action": [
           "s3:PutBucketPublicAccessBlock"
         ],
         "Resource": "*",
         "Condition": {
           "Bool": {
             "s3:BlockPublicAcls": "false"
           }
         }
       }
     ]
   }
   ```

### Cross-Account IAM Roles

**GitHub Actions OIDC Roles**:
- DEV: `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
- QA: `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
- PROD: `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`
- Ops: `arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole`

**Centralized Logging Role** (all accounts → Ops):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "arn:aws:iam::801651112319:root",
          "arn:aws:iam::965932217544:root",
          "arn:aws:iam::811783768245:root"
        ]
      },
      "Action": "sts:AssumeRole",
      "Resource": "arn:aws:iam::146072879609:role/CentralizedLoggingRole"
    }
  ]
}
```

**Audit Role** (all accounts → root):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::072456928432:root"
      },
      "Action": "sts:AssumeRole",
      "Resource": "arn:aws:iam::*:role/OrganizationAuditRole"
    }
  ]
}
```

---

## Environment-Specific Configurations

### DEV Account (801651112319)

**Purpose**: Feature development and testing

**Characteristics**:
- Cost-optimized configurations
- Single-AZ deployments acceptable
- Fargate Spot instances enabled
- Smaller instance sizes
- Automatic deployments from `develop` branch

**Infrastructure**:
- VPC CIDR: `10.0.0.0/16`
- RDS: db.t3.micro, Single-AZ
- ECS: 1 task per service
- Lambda: Lower memory allocations
- S3: 30-day lifecycle policies

**Access**:
- GitHub Actions: `develop` branch only
- Developers: Full access via SSO
- No production data

### QA Account (965932217544)

**Purpose**: Integration testing and pre-production validation

**Characteristics**:
- Production-like configuration
- Multi-AZ for availability testing
- Automatic deployments from `release/*` branches
- Integration and E2E test execution
- Performance testing environment

**Infrastructure**:
- VPC CIDR: `10.1.0.0/16`
- RDS: db.t3.small, Multi-AZ
- ECS: 2 tasks per service
- Lambda: Production-equivalent memory
- S3: 60-day lifecycle policies

**Access**:
- GitHub Actions: `release/*` branches
- QA Team: Full access
- Developers: Read-only access
- Synthetic production data

### PROD Account (811783768245)

**Purpose**: Live customer workloads

**Characteristics**:
- High availability (Multi-AZ)
- Auto-scaling enabled
- Manual deployments with approval gates
- Enhanced monitoring and alerting
- Disaster recovery enabled

**Infrastructure**:
- VPC CIDR: `10.2.0.0/16`
- RDS: db.t3.medium, Multi-AZ, Read Replicas
- ECS: 2-10 tasks per service (auto-scaling)
- Lambda: Reserved concurrency
- S3: Indefinite retention, versioning, MFA delete

**Access**:
- GitHub Actions: `main` branch only, requires approval
- Operations Team: Limited access via SSO
- Developers: No direct access
- Real customer data

### Ops Account (146072879609)

**Purpose**: Centralized DevOps tooling and monitoring

**Characteristics**:
- Cross-account access for tooling
- Centralized log aggregation
- Monitoring dashboards
- CI/CD infrastructure
- Security scanning tools

**Infrastructure**:
- VPC CIDR: `10.3.0.0/16`
- CloudWatch Log aggregation
- Centralized metrics dashboards
- Security scanning infrastructure
- Artifact storage

**Access**:
- GitHub Actions: All branches
- DevOps Team: Full access
- Cross-account roles for log collection

---

## Cross-Account Access Patterns

### ECR Image Sharing (Optional)

If needed, ECR images can be shared across accounts:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCrossAccountPull",
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "arn:aws:iam::801651112319:root",
          "arn:aws:iam::965932217544:root",
          "arn:aws:iam::811783768245:root"
        ]
      },
      "Action": [
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:BatchCheckLayerAvailability"
      ]
    }
  ]
}
```

### CloudTrail Log Aggregation

All accounts send CloudTrail logs to root account:

**Root Account S3 Bucket Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AWSCloudTrailAclCheck",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudtrail.amazonaws.com"
      },
      "Action": "s3:GetBucketAcl",
      "Resource": "arn:aws:s3:::turaf-cloudtrail-logs"
    },
    {
      "Sid": "AWSCloudTrailWrite",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudtrail.amazonaws.com"
      },
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::turaf-cloudtrail-logs/*",
      "Condition": {
        "StringEquals": {
          "s3:x-amz-acl": "bucket-owner-full-control"
        }
      }
    }
  ]
}
```

### Centralized Logging to Ops Account

All accounts send application logs to Ops account:

**CloudWatch Logs Subscription**:
```bash
# From each account, create subscription filter
aws logs put-subscription-filter \
  --log-group-name "/ecs/turaf-experiment-service" \
  --filter-name "CentralizedLogging" \
  --filter-pattern "" \
  --destination-arn "arn:aws:logs:us-east-1:146072879609:destination:CentralizedLogs"
```

---

## Cost Allocation Strategy

### Tagging Strategy

**Required Tags** (enforced via SCPs):
- `Project`: `Turaf`
- `Environment`: `dev|qa|prod|ops`
- `Service`: Service name (e.g., `experiment-service`)
- `ManagedBy`: `Terraform`
- `CostCenter`: Cost allocation code

### Budget Alerts

**Per-Account Budgets**:
- DEV: $100/month
- QA: $200/month
- PROD: $1000/month
- Ops: $150/month

**Alert Thresholds**:
- 50% of budget: Email notification
- 80% of budget: Email + Slack notification
- 100% of budget: Email + Slack + PagerDuty

### Cost Optimization

**DEV Account**:
- Fargate Spot (70% cost savings)
- Single-AZ RDS
- Aggressive S3 lifecycle policies
- Auto-shutdown during off-hours (optional)

**QA Account**:
- On-demand pricing
- Multi-AZ for testing
- Moderate retention policies

**PROD Account**:
- Reserved capacity for baseline
- Auto-scaling for peaks
- Long-term retention
- Savings Plans for predictable workloads

---

## Disaster Recovery

### Backup Strategy

**DEV Account**:
- RDS: 7-day retention
- S3: Versioning enabled
- No cross-region replication

**QA Account**:
- RDS: 7-day retention
- S3: Versioning enabled
- No cross-region replication

**PROD Account**:
- RDS: 30-day retention, cross-region backup
- S3: Versioning, MFA delete, cross-region replication
- Infrastructure: Terraform state versioned

### Recovery Procedures

**Infrastructure Recovery**:
1. Terraform state stored in S3 with versioning
2. Infrastructure can be recreated from code
3. RTO: 2 hours for full environment rebuild

**Data Recovery**:
1. RDS automated backups (point-in-time recovery)
2. S3 versioning for object recovery
3. RTO: 1 hour for database restore
4. RPO: 5 minutes (automated backups)

---

## Compliance and Audit

### CloudTrail Configuration

**Per-Account CloudTrail**:
- All API calls logged
- Log file validation enabled
- Logs sent to root account S3 bucket
- 90-day retention in CloudWatch Logs

**Root Account Aggregation**:
- Organization trail enabled
- All member account events captured
- Indefinite retention in S3
- Glacier transition after 90 days

### Compliance Requirements

**Data Residency**:
- All data in us-east-1 region
- No cross-region data transfer
- Enforced via SCPs

**Encryption**:
- All data encrypted at rest (KMS)
- All data encrypted in transit (TLS)
- Enforced via SCPs

**Access Logging**:
- All S3 bucket access logged
- All API calls logged via CloudTrail
- Centralized log analysis in Ops account

---

## Migration Path

### Adding New Accounts

1. **Create Account in AWS Organizations**:
   ```bash
   aws organizations create-account \
     --email aws-staging@turafapp.com \
     --account-name staging
   ```

2. **Apply SCPs**:
   - Attach organization-wide SCPs
   - Configure account-specific policies

3. **Setup Infrastructure**:
   - Create Terraform state bucket
   - Deploy base networking
   - Configure IAM roles

4. **Configure Access**:
   - Setup GitHub Actions OIDC
   - Configure SSO access
   - Setup cross-account roles

### Account Consolidation (if needed)

If consolidation becomes necessary:
1. Export data from source account
2. Import to target account
3. Update DNS and routing
4. Decommission source account

---

## Best Practices

### Account Management

1. **Never use root account credentials** - Use IAM users/roles
2. **Enable MFA on root accounts** - Additional security layer
3. **Use AWS SSO** - Centralized access management
4. **Regular access reviews** - Quarterly audit of permissions
5. **Automated compliance checks** - AWS Config rules

### Security

1. **Least privilege access** - Grant minimum required permissions
2. **Regular credential rotation** - 90-day rotation policy
3. **Monitor for anomalies** - CloudWatch alarms and GuardDuty
4. **Incident response plan** - Documented procedures per account
5. **Regular security audits** - Quarterly reviews

### Cost Management

1. **Tag all resources** - Enable cost allocation
2. **Review budgets monthly** - Adjust as needed
3. **Optimize unused resources** - Regular cleanup
4. **Use cost anomaly detection** - AWS Cost Anomaly Detection
5. **Reserved capacity planning** - Annual review

---

## References

- [AWS Organizations Best Practices](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_best-practices.html)
- [AWS Multi-Account Strategy](https://aws.amazon.com/organizations/getting-started/best-practices/)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- PROJECT.md - Overall project specifications
- AWS_ACCOUNTS.md - Account details
- GITHUB.md - CI/CD and deployment workflows
