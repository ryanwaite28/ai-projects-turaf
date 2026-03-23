# Infrastructure Prerequisites Specification

**Source**: INFRASTRUCTURE_PLAN.md (Prerequisites Section)

This specification defines all prerequisite resources and access requirements needed before deploying the Turaf platform infrastructure.

---

## Overview

Before beginning infrastructure deployment, specific resources must be acquired and configured. This document outlines all prerequisites, their purposes, and verification steps.

---

## Required Resources

### Domain Name

**Resource**: `turafapp.com`  
**Registrar**: whois.com  
**Purpose**: Primary domain for all platform services and environments

**Subdomains Required**:
- `api.turafapp.com` - Production API (ALB)
- `api.dev.turafapp.com` - Development API
- `api.qa.turafapp.com` - QA API
- `app.turafapp.com` - Production Frontend (CloudFront)
- `app.dev.turafapp.com` - Development Frontend
- `app.qa.turafapp.com` - QA Frontend
- `ws.turafapp.com` - Production WebSocket Gateway
- `ws.dev.turafapp.com` - Development WebSocket Gateway
- `ws.qa.turafapp.com` - QA WebSocket Gateway

**Verification**:
```bash
whois turafapp.com
# Verify ownership and registrar details
```

---

### Email Hosting

**Provider**: Titan Email (via whois.com)  
**Primary Email**: `admin@turafapp.com`  
**Purpose**: Infrastructure management and platform notifications

**Required Email Addresses**:

**AWS Account Management**:
- `aws@turafapp.com` - Root account
- `aws-ops@turafapp.com` - Ops account
- `aws-dev@turafapp.com` - Dev account
- `aws-qa@turafapp.com` - QA account
- `aws-prod@turafapp.com` - Prod account

**Application Emails** (SES):
- `noreply@turafapp.com` - Transactional emails
- `notifications@turafapp.com` - Experiment notifications
- `support@turafapp.com` - User support

**Email Forwarding Configuration**:
All emails should forward to `admin@turafapp.com` for centralized management.

**Verification**:
```bash
# Test email delivery
echo "Test" | mail -s "Test Email" admin@turafapp.com
```

---

### AWS Organization

**Organization ID**: `o-l3zk5a91yj`  
**Root Account**: `072456928432`  
**Management Account ARN**: `arn:aws:organizations::072456928432:root/o-l3zk5a91yj/r-gs6r`

**Member Accounts**:

| Account Name | Account ID | Email | Purpose |
|--------------|------------|-------|---------|
| **root** | 072456928432 | aws@turafapp.com | Management account |
| **Ops** | 146072879609 | aws-ops@turafapp.com | DevOps tooling, centralized logging |
| **dev** | 801651112319 | aws-dev@turafapp.com | Development environment |
| **qa** | 965932217544 | aws-qa@turafapp.com | QA/Staging environment |
| **prod** | 811783768245 | aws-prod@turafapp.com | Production environment |

**Verification**:
```bash
aws organizations describe-organization
aws organizations list-accounts
```

---

### GitHub Repository

**Repository**: https://github.com/ryanwaite28/ai-projects-turaf  
**Owner**: ryanwaite28  
**Visibility**: Private  
**Purpose**: Source code, CI/CD workflows, infrastructure as code

**Required Access**:
- Repository admin access
- Ability to create environments
- Ability to manage secrets
- Ability to configure branch protection rules

**Verification**:
```bash
gh repo view ryanwaite28/ai-projects-turaf
```

---

## Required Access

### AWS Root Account Access

**Account**: 072456928432  
**Email**: aws@turafapp.com  
**Purpose**: Organization management, account creation, root-level operations

**Required Permissions**:
- Full administrative access
- Ability to create/manage AWS Organization
- Ability to create member accounts
- Ability to configure SCPs

**Security Requirements**:
- MFA enabled
- Strong password policy
- Access keys rotated regularly
- CloudTrail enabled

---

### Domain Registrar Access

**Provider**: whois.com  
**Purpose**: DNS nameserver management, domain configuration

**Required Permissions**:
- Update nameservers
- Manage DNS records
- Configure email forwarding

---

### Email Provider Access

**Provider**: Titan Email (via whois.com)  
**Purpose**: Email alias configuration, forwarding rules

**Required Permissions**:
- Create email aliases
- Configure forwarding rules
- Manage mailboxes

---

### GitHub Repository Admin Access

**Repository**: ryanwaite28/ai-projects-turaf  
**Purpose**: CI/CD configuration, secrets management, workflow deployment

**Required Permissions**:
- Create/manage environments
- Create/manage secrets (repository and environment-level)
- Configure branch protection rules
- Manage GitHub Actions workflows
- Configure OIDC providers

---

## Local Development Environment

### Required Tools

**AWS CLI**:
```bash
# Version 2.x required
aws --version
# Expected: aws-cli/2.x.x

# Configure profiles for each account
aws configure --profile turaf-root
aws configure --profile turaf-ops
aws configure --profile turaf-dev
aws configure --profile turaf-qa
aws configure --profile turaf-prod
```

**Terraform**:
```bash
# Version 1.5+ required
terraform --version
# Expected: Terraform v1.5.x or higher
```

**Git**:
```bash
# Version 2.x required
git --version
# Expected: git version 2.x.x
```

**GitHub CLI** (optional but recommended):
```bash
# For managing GitHub resources
gh --version
# Expected: gh version 2.x.x
```

**jq** (for JSON parsing):
```bash
jq --version
# Expected: jq-1.x
```

**PostgreSQL Client** (for database migrations):
```bash
psql --version
# Expected: psql (PostgreSQL) 14.x or higher
```

---

## AWS CLI Configuration

### Profile Setup

**Root Account**:
```bash
aws configure --profile turaf-root
# AWS Access Key ID: <ROOT_ACCESS_KEY>
# AWS Secret Access Key: <ROOT_SECRET_KEY>
# Default region name: us-east-1
# Default output format: json
```

**Ops Account**:
```bash
aws configure --profile turaf-ops
# AWS Access Key ID: <OPS_ACCESS_KEY>
# AWS Secret Access Key: <OPS_SECRET_KEY>
# Default region name: us-east-1
# Default output format: json
```

**Dev Account**:
```bash
aws configure --profile turaf-dev
# AWS Access Key ID: <DEV_ACCESS_KEY>
# AWS Secret Access Key: <DEV_SECRET_KEY>
# Default region name: us-east-1
# Default output format: json
```

**QA Account**:
```bash
aws configure --profile turaf-qa
# AWS Access Key ID: <QA_ACCESS_KEY>
# AWS Secret Access Key: <QA_SECRET_KEY>
# Default region name: us-east-1
# Default output format: json
```

**Prod Account**:
```bash
aws configure --profile turaf-prod
# AWS Access Key ID: <PROD_ACCESS_KEY>
# AWS Secret Access Key: <PROD_SECRET_KEY>
# Default region name: us-east-1
# Default output format: json
```

### Verification

```bash
# Test each profile
for profile in turaf-root turaf-ops turaf-dev turaf-qa turaf-prod; do
  echo "Testing $profile..."
  aws sts get-caller-identity --profile $profile
done
```

---

## Pre-Deployment Checklist

### Domain and Email

- [ ] Domain registered and accessible
- [ ] Domain registrar credentials available
- [ ] Email hosting configured
- [ ] Primary admin email verified
- [ ] Email forwarding rules configured

### AWS Accounts

- [ ] AWS Organization created
- [ ] Root account accessible with MFA
- [ ] All 5 member accounts created
- [ ] Account emails verified
- [ ] AWS CLI configured for all accounts
- [ ] Cross-account access tested

### GitHub

- [ ] Repository accessible
- [ ] Admin access confirmed
- [ ] GitHub CLI authenticated
- [ ] Personal access token created (if needed)

### Local Environment

- [ ] AWS CLI v2 installed
- [ ] Terraform 1.5+ installed
- [ ] Git installed and configured
- [ ] GitHub CLI installed (optional)
- [ ] jq installed
- [ ] PostgreSQL client installed
- [ ] Docker installed (for Testcontainers)

### Security

- [ ] MFA enabled on root account
- [ ] Strong passwords configured
- [ ] Access keys secured
- [ ] SSH keys configured for Git
- [ ] GPG signing configured (optional)

---

## Cost Considerations

### Initial Setup Costs

**Domain Registration**: ~$10-15/year  
**Email Hosting**: ~$5-10/month  
**AWS Accounts**: No cost for account creation

### Ongoing Infrastructure Costs

**DEV Environment**: ~$150-200/month
- Single-AZ deployment
- Fargate Spot instances
- db.t3.micro RDS instance
- Minimal data transfer

**QA Environment**: ~$300-400/month
- Multi-AZ deployment
- On-demand Fargate
- db.t3.small RDS instance
- Moderate data transfer

**PROD Environment**: ~$800-1200/month
- Multi-AZ deployment
- Auto-scaling Fargate
- db.r5.large RDS instance
- High availability configuration
- Enhanced monitoring

**Total Estimated Monthly Cost**: ~$1,250-1,800

---

## Security Best Practices

### AWS Account Security

1. **Enable MFA** on all accounts
2. **Rotate access keys** every 90 days
3. **Use IAM roles** instead of access keys where possible
4. **Enable CloudTrail** for audit logging
5. **Configure AWS Config** for compliance monitoring
6. **Enable GuardDuty** for threat detection

### Domain Security

1. **Enable domain lock** to prevent unauthorized transfers
2. **Use strong registrar password**
3. **Enable two-factor authentication** on registrar account
4. **Configure DNSSEC** for DNS security (optional)

### Email Security

1. **Configure SPF records** for email authentication
2. **Enable DKIM** for email signing
3. **Set up DMARC** for email policy
4. **Monitor bounce/complaint rates**

### GitHub Security

1. **Enable two-factor authentication**
2. **Use SSH keys** for Git operations
3. **Sign commits** with GPG (optional)
4. **Limit repository access** to necessary personnel
5. **Enable security alerts** for dependencies

---

## Troubleshooting

### AWS CLI Issues

**Issue**: "Unable to locate credentials"  
**Solution**:
```bash
# Verify AWS credentials file
cat ~/.aws/credentials

# Reconfigure profile
aws configure --profile turaf-dev
```

**Issue**: "Access Denied"  
**Solution**:
```bash
# Verify IAM permissions
aws iam get-user --profile turaf-dev

# Check assumed role
aws sts get-caller-identity --profile turaf-dev
```

### Domain Access Issues

**Issue**: Cannot access domain registrar  
**Solution**:
- Verify login credentials
- Check for account lockout
- Contact registrar support
- Verify email access for password reset

### Email Delivery Issues

**Issue**: Emails not being received  
**Solution**:
- Check spam/junk folders
- Verify email forwarding rules
- Test with different email providers
- Check email hosting status

---

## Next Steps

After verifying all prerequisites:

1. Proceed to **Phase 1: Domain and DNS Configuration**
2. Configure Route 53 hosted zones
3. Request ACM certificates
4. Set up email forwarding aliases

---

## References

- [AWS Organizations Documentation](https://docs.aws.amazon.com/organizations/)
- [AWS CLI Configuration](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)
- [Terraform Installation](https://developer.hashicorp.com/terraform/downloads)
- [GitHub CLI Documentation](https://cli.github.com/)
