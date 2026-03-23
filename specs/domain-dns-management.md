# Domain and DNS Management Specification

**Source**: INFRASTRUCTURE_PLAN.md (Phase 1), PROJECT.md (Section 11)

This specification defines the domain configuration, DNS management with Route 53, SSL/TLS certificate management with ACM, and email forwarding setup for the Turaf platform.

---

## Overview

The Turaf platform uses `turafapp.com` as its primary domain, managed through whois.com and delegated to AWS Route 53 for DNS management. SSL/TLS certificates are provisioned via AWS Certificate Manager (ACM) for secure HTTPS connections.

---

## Domain Configuration

### Primary Domain

**Domain**: `turafapp.com`  
**Registrar**: whois.com  
**DNS Provider**: AWS Route 53 (delegated)  
**DNSSEC**: Optional (recommended for production)

### Subdomain Structure

**Production**:
- `turafapp.com` - Main website/marketing (CloudFront)
- `api.turafapp.com` - Production API (ALB)
- `app.turafapp.com` - Production Frontend (CloudFront)
- `ws.turafapp.com` - Production WebSocket Gateway (ALB)

**Development**:
- `api.dev.turafapp.com` - Development API
- `app.dev.turafapp.com` - Development Frontend
- `ws.dev.turafapp.com` - Development WebSocket Gateway

**QA/Staging**:
- `api.qa.turafapp.com` - QA API
- `app.qa.turafapp.com` - QA Frontend
- `ws.qa.turafapp.com` - QA WebSocket Gateway

**Internal/Private**:
- `internal.turafapp.com` - Private hosted zone for internal services
- `*.internal.turafapp.com` - Internal service discovery

---

## Route 53 Configuration

### Public Hosted Zone

**Hosted Zone Name**: `turafapp.com`  
**Type**: Public  
**Account**: Root (072456928432)  
**Region**: Global (Route 53 is a global service)

**Name Servers** (assigned by AWS):
```
ns-xxxx.awsdns-xx.org
ns-xxxx.awsdns-xx.co.uk
ns-xxxx.awsdns-xx.com
ns-xxxx.awsdns-xx.net
```

**Configuration**:
```bash
# Create hosted zone
aws route53 create-hosted-zone \
  --name turafapp.com \
  --caller-reference $(date +%s) \
  --hosted-zone-config Comment="Public hosted zone for Turaf platform"
```

### Private Hosted Zone

**Hosted Zone Name**: `internal.turafapp.com`  
**Type**: Private  
**VPC Association**: All environment VPCs (dev, qa, prod)  
**Account**: Shared across all accounts

**Purpose**:
- Internal service discovery
- Private DNS for microservices
- Database endpoints
- Internal load balancers

**Configuration**:
```bash
# Create private hosted zone (in each account)
aws route53 create-hosted-zone \
  --name internal.turafapp.com \
  --caller-reference $(date +%s) \
  --hosted-zone-config Comment="Private hosted zone for internal services" \
  --vpc VPCRegion=us-east-1,VPCId=<VPC_ID>
```

---

## DNS Records

### A Records (Alias to AWS Resources)

**Production API**:
```
Name: api.turafapp.com
Type: A (Alias)
Target: Public ALB DNS name
Routing Policy: Simple
Evaluate Target Health: Yes
```

**Production Frontend**:
```
Name: app.turafapp.com
Type: A (Alias)
Target: CloudFront distribution
Routing Policy: Simple
```

**Production WebSocket**:
```
Name: ws.turafapp.com
Type: A (Alias)
Target: Public ALB DNS name
Routing Policy: Simple
Evaluate Target Health: Yes
```

**Development/QA** (similar pattern):
```
api.dev.turafapp.com → dev ALB
app.dev.turafapp.com → dev CloudFront
ws.dev.turafapp.com → dev ALB

api.qa.turafapp.com → qa ALB
app.qa.turafapp.com → qa CloudFront
ws.qa.turafapp.com → qa ALB
```

### CNAME Records

**Email Records** (for SES):
```
_amazonses.turafapp.com → TXT record for domain verification
<token1>._domainkey.turafapp.com → CNAME for DKIM
<token2>._domainkey.turafapp.com → CNAME for DKIM
<token3>._domainkey.turafapp.com → CNAME for DKIM
```

### TXT Records

**SPF Record**:
```
Name: turafapp.com
Type: TXT
Value: "v=spf1 include:amazonses.com ~all"
TTL: 300
```

**DMARC Record**:
```
Name: _dmarc.turafapp.com
Type: TXT
Value: "v=DMARC1; p=quarantine; rua=mailto:admin@turafapp.com"
TTL: 300
```

**Domain Verification** (SES):
```
Name: _amazonses.turafapp.com
Type: TXT
Value: "<VERIFICATION_TOKEN>"
TTL: 300
```

### MX Records (if using Route 53 for email)

**Email Routing**:
```
Name: turafapp.com
Type: MX
Priority: 10
Value: mail.turafapp.com
TTL: 300
```

**Note**: Currently using Titan Email via whois.com, so MX records managed by Titan.

---

## SSL/TLS Certificate Management (ACM)

### Wildcard Certificate

**Certificate Details**:
- **Domain**: `*.turafapp.com`
- **Subject Alternative Names**: `turafapp.com`
- **Validation Method**: DNS
- **Region**: us-east-1 (for CloudFront compatibility)
- **Account**: Root account (shared via cross-account access)

**Request Certificate**:
```bash
aws acm request-certificate \
  --domain-name "*.turafapp.com" \
  --subject-alternative-names "turafapp.com" \
  --validation-method DNS \
  --region us-east-1
```

**DNS Validation**:
```bash
# Get validation records
aws acm describe-certificate \
  --certificate-arn <CERTIFICATE_ARN> \
  --region us-east-1 \
  --query 'Certificate.DomainValidationOptions[*].[ResourceRecord.Name,ResourceRecord.Value]' \
  --output table

# Add CNAME records to Route 53 for validation
```

### Environment-Specific Certificates (Optional)

**Dev Certificate**:
- Domain: `*.dev.turafapp.com`
- Account: dev (801651112319)
- Region: us-east-1

**QA Certificate**:
- Domain: `*.qa.turafapp.com`
- Account: qa (965932217544)
- Region: us-east-1

**Prod Certificate**:
- Domain: `*.turafapp.com` (shared with root)
- Account: prod (811783768245)
- Region: us-east-1

### Certificate Renewal

**Auto-Renewal**:
- ACM automatically renews certificates
- DNS validation records must remain in place
- Renewal occurs 60 days before expiration

**Monitoring**:
```bash
# Check certificate status
aws acm describe-certificate \
  --certificate-arn <CERTIFICATE_ARN> \
  --region us-east-1 \
  --query 'Certificate.[Status,NotAfter]'
```

---

## Email Forwarding Configuration

### Email Addresses

**AWS Account Management**:
- `aws@turafapp.com` → `admin@turafapp.com`
- `aws-ops@turafapp.com` → `admin@turafapp.com`
- `aws-dev@turafapp.com` → `admin@turafapp.com`
- `aws-qa@turafapp.com` → `admin@turafapp.com`
- `aws-prod@turafapp.com` → `admin@turafapp.com`

**Application Emails** (SES):
- `noreply@turafapp.com` → `admin@turafapp.com`
- `notifications@turafapp.com` → `admin@turafapp.com`
- `support@turafapp.com` → `admin@turafapp.com`

### Titan Email Configuration

**Provider**: Titan Email (via whois.com)  
**Admin Email**: `admin@turafapp.com`

**Forwarding Rules**:
1. Log into Titan Email control panel
2. Navigate to Email Forwarding
3. Create aliases for each email address
4. Set forwarding destination to `admin@turafapp.com`

---

## DNS Propagation and TTL

### TTL Values

**Production Records**: 300 seconds (5 minutes)
- Allows for quick updates during incidents
- Balance between caching and flexibility

**Development Records**: 60 seconds (1 minute)
- Faster iteration during development
- More frequent DNS queries acceptable

**Email Records**: 300 seconds
- Standard for email-related records
- Ensures reliable email delivery

### Propagation Time

**Initial Setup**: 24-48 hours
- Nameserver delegation from whois.com to Route 53
- Global DNS propagation

**Record Updates**: 5-60 minutes
- Depends on TTL value
- Cached records expire based on TTL

**Verification**:
```bash
# Check DNS propagation
dig turafapp.com +short
dig api.turafapp.com +short
dig app.turafapp.com +short

# Check nameservers
dig NS turafapp.com +short
nslookup -type=NS turafapp.com
```

---

## Health Checks and Monitoring

### Route 53 Health Checks

**API Health Check**:
```
Name: turaf-api-prod-health
Type: HTTPS
Resource Path: /actuator/health
Port: 443
Interval: 30 seconds
Failure Threshold: 3
```

**Frontend Health Check**:
```
Name: turaf-app-prod-health
Type: HTTPS
Resource Path: /
Port: 443
Interval: 30 seconds
Failure Threshold: 3
```

**CloudWatch Alarms**:
- Alert on health check failures
- SNS notification to operations team
- Automatic failover (if multi-region)

---

## Disaster Recovery

### DNS Backup

**Backup Strategy**:
- Export Route 53 records daily
- Store in S3 with versioning
- Cross-region replication

**Backup Script**:
```bash
# Export all records
aws route53 list-resource-record-sets \
  --hosted-zone-id <HOSTED_ZONE_ID> \
  --output json > route53-backup-$(date +%Y%m%d).json

# Upload to S3
aws s3 cp route53-backup-$(date +%Y%m%d).json \
  s3://turaf-dns-backups/
```

### Recovery Procedures

**Scenario 1: Hosted Zone Deletion**
1. Recreate hosted zone with same name
2. Update nameservers at registrar
3. Restore records from S3 backup
4. Wait for DNS propagation

**Scenario 2: Certificate Expiration**
1. Request new certificate
2. Add DNS validation records
3. Wait for validation
4. Update ALB/CloudFront with new certificate

---

## Security Best Practices

### DNSSEC (Optional)

**Purpose**: Prevent DNS spoofing and cache poisoning

**Configuration**:
```bash
# Enable DNSSEC signing
aws route53 enable-hosted-zone-dnssec \
  --hosted-zone-id <HOSTED_ZONE_ID>

# Add DS records to registrar
```

### Domain Lock

**Registrar Settings**:
- Enable domain transfer lock
- Require authorization code for transfers
- Enable two-factor authentication

### Access Control

**Route 53 Permissions**:
- Limit who can modify DNS records
- Use IAM roles with least privilege
- Enable CloudTrail logging for DNS changes

**Certificate Access**:
- Restrict ACM certificate access
- Use resource-based policies
- Enable certificate transparency logging

---

## Cost Optimization

### Route 53 Costs

**Hosted Zones**: $0.50/month per hosted zone
- 1 public hosted zone: $0.50/month
- 3 private hosted zones (dev, qa, prod): $1.50/month

**Queries**: $0.40 per million queries
- Estimated: 10M queries/month = $4.00/month

**Health Checks**: $0.50/month per health check
- 6 health checks (api, app, ws × 2 envs): $3.00/month

**Total Estimated**: ~$9/month

### ACM Costs

**Public Certificates**: Free
- No charge for public SSL/TLS certificates
- Automatic renewal included

---

## Implementation Checklist

- [ ] Create Route 53 public hosted zone
- [ ] Update nameservers at whois.com
- [ ] Verify DNS delegation
- [ ] Request ACM wildcard certificate
- [ ] Add DNS validation records
- [ ] Wait for certificate validation
- [ ] Configure email forwarding in Titan
- [ ] Create DNS records for each environment
- [ ] Set up health checks
- [ ] Configure CloudWatch alarms
- [ ] Test DNS resolution
- [ ] Verify SSL/TLS certificates

---

## References

- [Route 53 Documentation](https://docs.aws.amazon.com/route53/)
- [ACM Documentation](https://docs.aws.amazon.com/acm/)
- [DNS Best Practices](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/best-practices-dns.html)
- INFRASTRUCTURE_PLAN.md (Phase 1)
