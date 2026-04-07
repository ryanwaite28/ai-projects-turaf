# Task 027: Database Migration Network Access - Implementation Summary

**Status**: ✅ Complete  
**Environment**: Development (dev)  
**Completion Date**: March 24, 2026

---

## Overview

Network access for AWS CodeBuild to connect to RDS PostgreSQL for Flyway migrations has been configured as part of the standalone infrastructure deployment.

---

## Deployed Configuration

### VPC and Networking

- **VPC ID**: `vpc-0eb73410956d368a8`
- **VPC CIDR**: `10.0.0.0/16`
- **Private Subnets** (for CodeBuild):
  - `subnet-0fbca1c0741c511bc` (us-east-1a)
  - `subnet-0a7e1733037f31e69` (us-east-1b)
- **Database Subnets** (for RDS):
  - `subnet-0e5b26fc43803ca86` (us-east-1a)
  - `subnet-0ae5e2889493ca0ad` (us-east-1b)

### Security Groups

#### CodeBuild Security Group
- **Security Group ID**: `sg-01b1f0d32cf32bd22`
- **Name**: `turaf-codebuild-dev-*`
- **Purpose**: For CodeBuild Flyway migration projects
- **VPC**: `vpc-0eb73410956d368a8`

**Egress Rules**:
- Allow all outbound traffic (0.0.0.0/0) on all ports
  - This includes PostgreSQL (5432) to RDS
  - This includes HTTPS (443) for package downloads
  - This includes HTTP (80) for package downloads

**Ingress Rules**:
- None (CodeBuild doesn't need inbound connections)

#### RDS Security Group
- **Security Group ID**: `sg-0700dfd644af580af`
- **Name**: `turaf-rds-sg-dev`
- **Purpose**: For RDS PostgreSQL database
- **VPC**: `vpc-0eb73410956d368a8`

**Ingress Rules**:
- Allow PostgreSQL (TCP 5432) from VPC CIDR (10.0.0.0/16)
- Allow PostgreSQL (TCP 5432) from CodeBuild SG (`sg-01b1f0d32cf32bd22`)

**Egress Rules**:
- Allow all outbound traffic (default)

---

## Network Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     VPC: vpc-0eb73410956d368a8              │
│                        CIDR: 10.0.0.0/16                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────┐                              │
│  │  Private Subnet          │                              │
│  │  subnet-0fbca1c0741c511bc│                              │
│  │                          │                              │
│  │  ┌────────────────────┐  │                              │
│  │  │  CodeBuild         │  │         ┌──────────────────┐ │
│  │  │  Flyway Migrations │──┼────────>│  RDS PostgreSQL  │ │
│  │  │                    │  │  5432   │                  │ │
│  │  │  SG: sg-01b1f...   │  │         │  SG: sg-0700d... │ │
│  │  └────────────────────┘  │         │                  │ │
│  │                          │         │  Database Subnet │ │
│  └──────────────────────────┘         └──────────────────┘ │
│           │                                                 │
│           │ HTTPS/HTTP                                      │
│           ▼                                                 │
│  ┌──────────────────────────┐                              │
│  │  NAT Gateway             │                              │
│  │  nat-071665cb67dabc8f6   │                              │
│  └──────────────────────────┘                              │
│           │                                                 │
└───────────┼─────────────────────────────────────────────────┘
            │
            ▼
     Internet (Package Downloads)
```

---

## CodeBuild VPC Configuration

When creating CodeBuild projects for Flyway migrations, use the following configuration:

```yaml
VPC Configuration:
  VPC ID: vpc-0eb73410956d368a8
  Subnets:
    - subnet-0fbca1c0741c511bc  # Private subnet 1 (us-east-1a)
    - subnet-0a7e1733037f31e69  # Private subnet 2 (us-east-1b)
  Security Groups:
    - sg-01b1f0d32cf32bd22      # CodeBuild security group
```

**Important**: Use **private subnets**, not database subnets. CodeBuild needs internet access via NAT Gateway for package downloads.

---

## Database Connection Details

For CodeBuild environment variables:

```yaml
Environment Variables:
  DB_HOST: turaf-postgres-dev.cm7cimwey834.us-east-1.rds.amazonaws.com
  DB_PORT: 5432
  DB_NAME: turaf
  DB_USER: turaf_admin
  DB_PASSWORD_SECRET_ARN: arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/rds/admin-20260324134423738900000001-Wtw0q2
```

---

## Verification

### Security Group Rules Verification

Run the verification script:

```bash
./infrastructure/scripts/verify-flyway-network-dev.sh
```

**Expected Results**:
- ✅ VPC exists
- ✅ CodeBuild security group exists
- ✅ RDS security group exists
- ✅ RDS accepts connections from CodeBuild on port 5432
- ✅ CodeBuild can reach internet (via NAT Gateway)

### Manual Verification

```bash
# Verify CodeBuild security group
aws ec2 describe-security-groups \
  --group-ids sg-01b1f0d32cf32bd22 \
  --query 'SecurityGroups[0].{Name:GroupName,Egress:IpPermissionsEgress}' \
  --profile turaf-root

# Verify RDS security group
aws ec2 describe-security-groups \
  --group-ids sg-0700dfd644af580af \
  --query 'SecurityGroups[0].{Name:GroupName,Ingress:IpPermissions}' \
  --profile turaf-root
```

---

## Acceptance Criteria Status

- [x] CodeBuild security group created in dev environment
- [x] RDS security group updated to allow CodeBuild access
- [x] Automation script created (verify-flyway-network-dev.sh)
- [x] Verification script tested
- [x] Security group rules verified
- [x] Network access documented

---

## Next Steps

1. **Task 028**: Create CodeBuild Migration Projects
   - Use VPC configuration documented above
   - Use security group `sg-01b1f0d32cf32bd22`
   - Use private subnets for CodeBuild
   - Configure database connection using Secrets Manager

2. **Test Network Connectivity**:
   - Create test CodeBuild project
   - Verify connection to RDS
   - Verify package downloads work

---

## Troubleshooting

### Issue: CodeBuild cannot connect to RDS

**Check**:
1. CodeBuild is using private subnets (not database subnets)
2. CodeBuild security group is `sg-01b1f0d32cf32bd22`
3. RDS security group allows ingress from CodeBuild SG

**Verify**:
```bash
# Check CodeBuild project VPC config
aws codebuild batch-get-projects \
  --names <project-name> \
  --query 'projects[0].vpcConfig'
```

### Issue: CodeBuild cannot download packages

**Check**:
1. CodeBuild is in private subnets (with NAT Gateway route)
2. CodeBuild security group allows egress to 0.0.0.0/0
3. NAT Gateway exists and is available

**Verify**:
```bash
# Check NAT Gateway status
aws ec2 describe-nat-gateways \
  --nat-gateway-ids nat-071665cb67dabc8f6
```

---

## References

- Infrastructure Deployment: `infrastructure/DEPLOYED_INFRASTRUCTURE.md`
- Terraform Configuration: `infrastructure/terraform/standalone/dev-vpc-rds/`
- Task Specification: `tasks/infrastructure/027-configure-database-migration-network-access.md`
