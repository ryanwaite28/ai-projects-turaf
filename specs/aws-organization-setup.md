# AWS Organization Setup Specification

**Source**: INFRASTRUCTURE_PLAN.md (Phase 2.1), PROJECT.md (Section 10)

This specification defines the AWS Organization structure, organizational units, service control policies, and cross-account configurations for the Turaf platform.

---

## Overview

The Turaf platform uses a multi-account AWS architecture managed through AWS Organizations. This provides security isolation, cost allocation, and environment separation across development, QA, and production workloads.

---

## AWS Organization Structure

### Organization Details

**Organization ID**: `o-l3zk5a91yj`  
**Root Account**: `072456928432`  
**Management Account ARN**: `arn:aws:organizations::072456928432:root/o-l3zk5a91yj/r-gs6r`  
**Root ID**: `r-gs6r`  
**Primary Region**: `us-east-1`

### Member Accounts

| Account Name | Account ID | Email | ARN | Purpose |
|--------------|------------|-------|-----|---------|
| **root** | 072456928432 | aws@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/072456928432` | Management account, billing, organization-wide services |
| **Ops** | 146072879609 | aws-ops@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/146072879609` | DevOps tooling, centralized logging, CI/CD artifacts |
| **dev** | 801651112319 | aws-dev@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/801651112319` | Development environment, single-AZ, cost-optimized |
| **qa** | 965932217544 | aws-qa@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/965932217544` | QA/Staging environment, multi-AZ, production-like |
| **prod** | 811783768245 | aws-prod@turafapp.com | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/811783768245` | Production environment, multi-AZ, high availability |

---

## Organizational Units (OUs)

**Note**: All accounts are members of the same AWS Organization (o-l3zk5a91yj), with the root account (072456928432) serving as the management account. The organizational units provide logical grouping for policy management and billing organization.

### OU Structure

```
AWS Organization (o-l3zk5a91yj)
└── Root (r-gs6r) - Management Account: 072456928432
    └── Workloads OU
        ├── Ops (146072879609)
        ├── dev (801651112319)
        ├── qa (965932217544)
        └── prod (811783768245)
```

### Workloads OU

**Purpose**: Contains all workload accounts including operations and environment-specific accounts  
**Members**: Ops, dev, qa, prod  
**Policies**: Environment-specific SCPs, cost controls, security policies

**Characteristics**:
- Ops account: DevOps tooling, centralized logging, CI/CD infrastructure
- Dev account: Development environment, single-AZ, cost-optimized
- QA account: QA/Staging environment, multi-AZ, production-like
- Prod account: Production environment, multi-AZ, high availability
- Separate billing and cost allocation per account
- Environment-specific resource quotas
- Shared security and compliance policies

---

## Service Control Policies (SCPs)

### Organization-Wide SCPs

#### 1. Deny CloudTrail Deletion

**Purpose**: Prevent deletion or modification of CloudTrail logs  
**Applied To**: All accounts

**Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyCloudTrailDeletion",
      "Effect": "Deny",
      "Action": [
        "cloudtrail:DeleteTrail",
        "cloudtrail:StopLogging",
        "cloudtrail:UpdateTrail"
      ],
      "Resource": "*",
      "Condition": {
        "StringNotEquals": {
          "aws:PrincipalAccount": "072456928432"
        }
      }
    }
  ]
}
```

#### 2. Enforce Encryption at Rest

**Purpose**: Require encryption for all storage services  
**Applied To**: All accounts

**Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyUnencryptedStorage",
      "Effect": "Deny",
      "Action": [
        "s3:PutObject",
        "rds:CreateDBInstance",
        "dynamodb:CreateTable",
        "ebs:CreateVolume"
      ],
      "Resource": "*",
      "Condition": {
        "StringNotEquals": {
          "s3:x-amz-server-side-encryption": ["AES256", "aws:kms"],
          "rds:StorageEncrypted": "true",
          "dynamodb:Encryption": "true",
          "ebs:Encrypted": "true"
        }
      }
    }
  ]
}
```

#### 3. Restrict Regions

**Purpose**: Limit resource creation to us-east-1  
**Applied To**: All accounts except root

**Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyAllOutsideUSEast1",
      "Effect": "Deny",
      "NotAction": [
        "iam:*",
        "organizations:*",
        "route53:*",
        "cloudfront:*",
        "support:*",
        "budgets:*"
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

#### 4. Require MFA for Sensitive Operations

**Purpose**: Enforce MFA for destructive actions  
**Applied To**: All accounts

**Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyWithoutMFA",
      "Effect": "Deny",
      "Action": [
        "ec2:TerminateInstances",
        "rds:DeleteDBInstance",
        "s3:DeleteBucket",
        "dynamodb:DeleteTable"
      ],
      "Resource": "*",
      "Condition": {
        "BoolIfExists": {
          "aws:MultiFactorAuthPresent": "false"
        }
      }
    }
  ]
}
```

#### 5. Block Public S3 Bucket Access

**Purpose**: Prevent public S3 buckets by default  
**Applied To**: All accounts

**Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyPublicS3Buckets",
      "Effect": "Deny",
      "Action": [
        "s3:PutBucketPublicAccessBlock",
        "s3:PutAccountPublicAccessBlock"
      ],
      "Resource": "*",
      "Condition": {
        "StringNotEquals": {
          "s3:PublicAccessBlockConfiguration": "true"
        }
      }
    }
  ]
}
```

---

## Cross-Account IAM Roles

### GitHub Actions OIDC Roles

**Purpose**: Allow GitHub Actions to deploy to each environment  
**Trust Policy**: OIDC provider for token.actions.githubusercontent.com

**Roles**:
- `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
- `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
- `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`
- `arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole`

**Trust Policy Template**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:ryanwaite28/ai-projects-turaf:*"
        }
      }
    }
  ]
}
```

### Cross-Account Logging Role

**Purpose**: Allow all accounts to send logs to Ops account  
**Source Accounts**: dev, qa, prod  
**Destination Account**: Ops (146072879609)

**Role ARN**: `arn:aws:iam::146072879609:role/CrossAccountLoggingRole`

**Trust Policy**:
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
      "Action": "sts:AssumeRole"
    }
  ]
}
```

**Permissions Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogStreams"
      ],
      "Resource": "arn:aws:logs:us-east-1:146072879609:log-group:/aws/centralized/*"
    }
  ]
}
```

### Cross-Account Audit Role

**Purpose**: Allow root account to audit all member accounts  
**Source Account**: root (072456928432)  
**Destination Accounts**: Ops, dev, qa, prod

**Role ARN Template**: `arn:aws:iam::<ACCOUNT_ID>:role/CrossAccountAuditRole`

**Trust Policy**:
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
      "Condition": {
        "StringEquals": {
          "sts:ExternalId": "turaf-audit-2024"
        }
      }
    }
  ]
}
```

**Permissions Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "iam:Get*",
        "iam:List*",
        "cloudtrail:LookupEvents",
        "config:Describe*",
        "config:Get*",
        "config:List*"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## AWS Services Enabled Organization-Wide

### CloudTrail

**Purpose**: Audit logging for all accounts  
**Configuration**: Organization trail in root account  
**Log Destination**: S3 bucket in root account  
**Retention**: 90 days in CloudWatch Logs, 7 years in S3

**Service Principal**: `cloudtrail.amazonaws.com`

### AWS Config

**Purpose**: Configuration compliance and change tracking  
**Configuration**: Aggregator in root account  
**Delivery Channel**: S3 bucket in root account

**Service Principal**: `config.amazonaws.com`

### GuardDuty

**Purpose**: Threat detection and security monitoring  
**Configuration**: Delegated administrator in Ops account  
**Auto-enable**: New accounts automatically enrolled

**Service Principal**: `guardduty.amazonaws.com`

### Security Hub

**Purpose**: Centralized security findings  
**Configuration**: Aggregator in Ops account  
**Standards**: AWS Foundational Security Best Practices, CIS AWS Foundations Benchmark

**Service Principal**: `securityhub.amazonaws.com`

---

## Account-Level Security Controls

### Root Account (072456928432)

**Security Requirements**:
- MFA enabled on root user
- Root user access keys deleted
- CloudTrail enabled
- AWS Config enabled
- Billing alerts configured
- Cost anomaly detection enabled

**Allowed Actions**:
- Organization management
- Billing and cost management
- Account creation
- SCP management
- Organization-wide service configuration

### Workload Accounts (dev, qa, prod)

**Security Requirements**:
- No root user access (use IAM roles)
- CloudTrail enabled
- AWS Config enabled
- GuardDuty enabled
- Security Hub enabled
- VPC Flow Logs enabled

**Resource Quotas**:
- **DEV**: Lower limits for cost control
- **QA**: Medium limits for testing
- **PROD**: Higher limits for production workloads

### Ops Account (146072879609)

**Security Requirements**:
- Enhanced monitoring
- Centralized log aggregation
- Security scanning tools
- Compliance reporting

**Special Permissions**:
- Cross-account log access
- Security Hub aggregation
- GuardDuty delegated administrator

---

## Cost Allocation and Tagging

### Required Tags

All resources must be tagged with:

```json
{
  "Environment": "dev|qa|prod",
  "Project": "turaf",
  "ManagedBy": "terraform",
  "CostCenter": "engineering",
  "Owner": "platform-team"
}
```

### Cost Allocation Tags

**Activated Tags**:
- `Environment`
- `Project`
- `Service` (microservice name)
- `CostCenter`

### Billing Alerts

**Root Account Alerts**:
- Total monthly spend > $2,000
- Anomaly detection enabled

**Per-Account Alerts**:
- **DEV**: Monthly spend > $250
- **QA**: Monthly spend > $500
- **PROD**: Monthly spend > $1,500
- **Ops**: Monthly spend > $300

---

## Implementation Steps

### 1. Verify Organization Structure

```bash
# Verify organization
aws organizations describe-organization

# List all accounts
aws organizations list-accounts

# Verify root ID
aws organizations list-roots
```

### 2. Create Organizational Units

```bash
# Create Workloads OU
aws organizations create-organizational-unit \
  --parent-id r-gs6r \
  --name "Workloads"

# Create Security OU
aws organizations create-organizational-unit \
  --parent-id r-gs6r \
  --name "Security"
```

### 3. Move Accounts to OUs

```bash
# Move dev to Workloads OU
aws organizations move-account \
  --account-id 801651112319 \
  --source-parent-id r-gs6r \
  --destination-parent-id <WORKLOADS_OU_ID>

# Move qa to Workloads OU
aws organizations move-account \
  --account-id 965932217544 \
  --source-parent-id r-gs6r \
  --destination-parent-id <WORKLOADS_OU_ID>

# Move prod to Workloads OU
aws organizations move-account \
  --account-id 811783768245 \
  --source-parent-id r-gs6r \
  --destination-parent-id <WORKLOADS_OU_ID>

# Move Ops to Security OU
aws organizations move-account \
  --account-id 146072879609 \
  --source-parent-id r-gs6r \
  --destination-parent-id <SECURITY_OU_ID>
```

### 4. Enable AWS Services

```bash
# Enable CloudTrail
aws organizations enable-aws-service-access \
  --service-principal cloudtrail.amazonaws.com

# Enable Config
aws organizations enable-aws-service-access \
  --service-principal config.amazonaws.com

# Enable GuardDuty
aws organizations enable-aws-service-access \
  --service-principal guardduty.amazonaws.com

# Enable Security Hub
aws organizations enable-aws-service-access \
  --service-principal securityhub.amazonaws.com
```

### 5. Create and Attach SCPs

```bash
# Create SCP
aws organizations create-policy \
  --name DenyCloudTrailDeletion \
  --description "Prevent deletion of CloudTrail logs" \
  --type SERVICE_CONTROL_POLICY \
  --content file://scp-deny-cloudtrail-deletion.json

# Attach SCP to OU
aws organizations attach-policy \
  --policy-id <POLICY_ID> \
  --target-id <OU_ID>
```

---

## Monitoring and Compliance

### CloudWatch Dashboards

**Organization Dashboard**:
- Account-level cost metrics
- SCP compliance status
- Service usage across accounts
- Security findings summary

### Compliance Checks

**Automated Checks**:
- MFA enabled on root accounts
- CloudTrail enabled in all accounts
- Encryption enabled on all storage
- No public S3 buckets
- Resources in allowed regions only

### Alerting

**SNS Topics**:
- `turaf-organization-alerts` - Organization-wide alerts
- `turaf-security-alerts` - Security findings
- `turaf-cost-alerts` - Cost anomalies

---

## Disaster Recovery

### Organization Backup

**Backup Strategy**:
- Daily export of organization structure
- SCP policies backed up to S3
- Account metadata exported
- Cross-region replication for backups

**Recovery Procedures**:
1. Recreate organization structure
2. Restore SCPs from backup
3. Re-enable AWS services
4. Restore cross-account roles

---

## References

- [AWS Organizations Best Practices](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_best-practices.html)
- [Service Control Policies](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps.html)
- [Multi-Account Strategy](https://docs.aws.amazon.com/whitepapers/latest/organizing-your-aws-environment/organizing-your-aws-environment.html)
