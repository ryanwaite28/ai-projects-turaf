# Task: Request ACM Certificates

**Service**: Infrastructure  
**Type**: Domain and DNS Configuration  
**Priority**: High  
**Estimated Time**: 1 hour  
**Dependencies**: 017-configure-route53-hosted-zone

---

## Objective

Request SSL/TLS certificates from AWS Certificate Manager (ACM) for all domains and configure DNS validation via Route 53.

---

## Acceptance Criteria

- [x] Wildcard certificate requested for *.turafapp.com
- [x] DNS validation records created in Route 53
- [x] Certificate validated and issued
- [x] Certificate ARN documented
- [ ] Optional: Environment-specific certificates requested (deferred)

---

## Implementation

### 1. Request Wildcard Certificate

```bash
# Request certificate in us-east-1 (required for CloudFront)
aws acm request-certificate \
  --domain-name "*.turafapp.com" \
  --subject-alternative-names "turafapp.com" \
  --validation-method DNS \
  --region us-east-1

# Save certificate ARN from output
```

**Expected Output**:
```json
{
    "CertificateArn": "arn:aws:acm:us-east-1:072456928432:certificate/12345678-1234-1234-1234-123456789012"
}
```

### 2. Get DNS Validation Records

```bash
# Set certificate ARN
CERT_ARN="arn:aws:acm:us-east-1:072456928432:certificate/12345678-1234-1234-1234-123456789012"

# Get validation records
aws acm describe-certificate \
  --certificate-arn $CERT_ARN \
  --region us-east-1 \
  --query 'Certificate.DomainValidationOptions[*].[ResourceRecord.Name,ResourceRecord.Value]' \
  --output table
```

**Expected Output**:
```
-----------------------------------------------------------------
|                    DescribeCertificate                        |
+---------------------------------------------------------------+
|  _abc123.turafapp.com.  |  _xyz789.acm-validations.aws.      |
+---------------------------------------------------------------+
```

### 3. Create DNS Validation Records in Route 53

```bash
# Get hosted zone ID
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones \
  --query "HostedZones[?Name=='turafapp.com.'].Id" \
  --output text | cut -d'/' -f3)

# Get validation record details
VALIDATION_NAME=$(aws acm describe-certificate \
  --certificate-arn $CERT_ARN \
  --region us-east-1 \
  --query 'Certificate.DomainValidationOptions[0].ResourceRecord.Name' \
  --output text)

VALIDATION_VALUE=$(aws acm describe-certificate \
  --certificate-arn $CERT_ARN \
  --region us-east-1 \
  --query 'Certificate.DomainValidationOptions[0].ResourceRecord.Value' \
  --output text)

# Create change batch file
cat > acm-validation-records.json <<EOF
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "$VALIDATION_NAME",
      "Type": "CNAME",
      "TTL": 300,
      "ResourceRecords": [{"Value": "$VALIDATION_VALUE"}]
    }
  }]
}
EOF

# Add validation record to Route 53
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://acm-validation-records.json
```

### 4. Wait for Certificate Validation

```bash
# Wait for validation (can take 5-30 minutes)
aws acm wait certificate-validated \
  --certificate-arn $CERT_ARN \
  --region us-east-1

# Check status
aws acm describe-certificate \
  --certificate-arn $CERT_ARN \
  --region us-east-1 \
  --query 'Certificate.Status'
```

**Expected**: `"ISSUED"`

---

## Optional: Environment-Specific Certificates

### Dev Certificate

```bash
# Request dev certificate (in dev account)
aws acm request-certificate \
  --domain-name "*.dev.turafapp.com" \
  --validation-method DNS \
  --region us-east-1 \
  --profile turaf-dev

# Follow same validation process
```

### QA Certificate

```bash
# Request qa certificate (in qa account)
aws acm request-certificate \
  --domain-name "*.qa.turafapp.com" \
  --validation-method DNS \
  --region us-east-1 \
  --profile turaf-qa

# Follow same validation process
```

### Prod Certificate

**Note**: Production can use the wildcard certificate from root account via cross-account sharing, or request its own.

---

## Verification

### 1. Check Certificate Status

```bash
# Describe certificate
aws acm describe-certificate \
  --certificate-arn $CERT_ARN \
  --region us-east-1

# Verify:
# - Status: ISSUED
# - DomainName: *.turafapp.com
# - SubjectAlternativeNames: turafapp.com, *.turafapp.com
# - Type: AMAZON_ISSUED
```

### 2. List All Certificates

```bash
# List certificates in region
aws acm list-certificates \
  --region us-east-1 \
  --query 'CertificateSummaryList[*].[DomainName,CertificateArn,Status]' \
  --output table
```

### 3. Verify DNS Validation Records

```bash
# Check validation record in Route 53
aws route53 list-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --query "ResourceRecordSets[?Type=='CNAME']"

# Verify CNAME record exists for ACM validation
```

---

## Certificate Details

### Wildcard Certificate

**Domain**: `*.turafapp.com`  
**SAN**: `turafapp.com`  
**Covers**:
- api.turafapp.com
- app.turafapp.com
- ws.turafapp.com
- api.dev.turafapp.com
- app.dev.turafapp.com
- ws.dev.turafapp.com
- api.qa.turafapp.com
- app.qa.turafapp.com
- ws.qa.turafapp.com
- Any other subdomain

**Does NOT cover**:
- turafapp.com (root domain - added as SAN)
- *.*.turafapp.com (nested subdomains)

---

## Troubleshooting

### Issue: Certificate stuck in "Pending Validation"

**Cause**: DNS validation record not found

**Solution**:
```bash
# Verify validation record exists
dig CNAME $VALIDATION_NAME +short

# Check Route 53 record
aws route53 list-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --query "ResourceRecordSets[?Name=='$VALIDATION_NAME']"

# Recreate if missing
```

### Issue: "LimitExceededException"

**Cause**: ACM certificate limit reached (default: 2,048 per region)

**Solution**:
- Delete unused certificates
- Request limit increase
- Use different region

### Issue: Validation record already exists

**Cause**: Previous certificate request

**Solution**:
```bash
# Update existing record instead of creating
# Change "CREATE" to "UPSERT" in change batch
```

### Issue: Certificate not validating after 30+ minutes

**Cause**: DNS propagation delay or incorrect record

**Solution**:
```bash
# Verify DNS propagation
dig CNAME $VALIDATION_NAME @8.8.8.8

# Check ACM validation status
aws acm describe-certificate \
  --certificate-arn $CERT_ARN \
  --region us-east-1 \
  --query 'Certificate.DomainValidationOptions'

# Delete and recreate if needed
```

---

## Certificate Management

### Auto-Renewal

**ACM Behavior**:
- Automatically renews certificates 60 days before expiration
- Requires DNS validation records to remain in place
- No action needed if validation records exist

**Monitoring**:
```bash
# Check certificate expiration
aws acm describe-certificate \
  --certificate-arn $CERT_ARN \
  --region us-east-1 \
  --query 'Certificate.NotAfter'
```

### Certificate Sharing

**Cross-Account Access**:
- Certificates cannot be directly shared across accounts
- Options:
  1. Request separate certificates in each account
  2. Use CloudFormation StackSets for multi-account deployment
  3. Use AWS Resource Access Manager (RAM) for supported services

---

## Documentation

### Save Certificate Information

Create `infrastructure/acm-certificates.md`:

```markdown
# ACM Certificates

## Wildcard Certificate

- **ARN**: arn:aws:acm:us-east-1:072456928432:certificate/12345678-1234-1234-1234-123456789012
- **Domain**: *.turafapp.com
- **SAN**: turafapp.com
- **Region**: us-east-1
- **Account**: root (072456928432)
- **Status**: ISSUED
- **Issued**: 2024-01-01
- **Expires**: 2025-01-01 (auto-renews)

## Validation Records

- **Name**: _abc123.turafapp.com
- **Type**: CNAME
- **Value**: _xyz789.acm-validations.aws
- **TTL**: 300
```

---

## Checklist

- [x] Wildcard certificate requested
- [x] Certificate ARN saved: `c660ca8d-5584-4d6f-b75f-e5f10fc5a8ab`
- [x] DNS validation records created
- [x] Certificate validated (Status: ISSUED)
- [x] Validation records documented
- [x] Certificate expiration date noted: 2026-10-06
- [ ] Optional: Dev certificate requested (deferred)
- [ ] Optional: QA certificate requested (deferred)

---

## Next Steps

After certificate validation:
1. ✅ **COMPLETED** - Certificate validated and issued successfully
2. Proceed to **Task 006: Configure Email Forwarding**
3. Use certificate ARN in ALB/CloudFront configurations (later tasks)

## Implementation Results (2024-03-23)

### ✅ Certificate Details

- **ARN**: `arn:aws:acm:us-east-1:072456928432:certificate/c660ca8d-5584-4d6f-b75f-e5f10fc5a8ab`
- **Domain**: `*.turafapp.com`
- **Subject Alternative Names**: 
  - `*.turafapp.com`
  - `turafapp.com`
- **Status**: ISSUED ✅
- **Region**: us-east-1
- **Account**: root (072456928432)
- **Issued**: 2024-03-23 17:29:46 UTC
- **Expires**: 2026-10-06 19:59:59 UTC
- **Validation Method**: DNS
- **Key Algorithm**: RSA-2048

### ✅ DNS Validation Record

Added to Route 53 hosted zone `Z055341020TQZLU2CKWOE`:

- **Name**: `_fbf85825e8daf10df0aca23dd2320b07.turafapp.com.`
- **Type**: CNAME
- **Value**: `_d09092ac6f3e640919da44adddd8c78a.jkddzztszm.acm-validations.aws.`
- **TTL**: 300
- **Status**: Active (required for auto-renewal)

### 🎯 Certificate Coverage

**Covered Domains**:
- ✅ `turafapp.com` (root domain)
- ✅ `*.turafapp.com` (all first-level subdomains)
  - `api.turafapp.com`
  - `app.turafapp.com`
  - `ws.turafapp.com`
  - `dev.turafapp.com`, `qa.turafapp.com`, `prod.turafapp.com`
  - Any other first-level subdomain

### 📁 Documentation Created

- `infrastructure/acm-certificates.md` - Complete certificate documentation

### 🔄 Auto-Renewal

- ACM will automatically renew this certificate 60 days before expiration
- DNS validation record must remain in Route 53 for renewal to succeed
- No manual intervention required

---

## References

- [ACM Documentation](https://docs.aws.amazon.com/acm/)
- [DNS Validation](https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html)
- [Certificate Renewal](https://docs.aws.amazon.com/acm/latest/userguide/managed-renewal.html)
- specs/domain-dns-management.md
- INFRASTRUCTURE_PLAN.md (Phase 1.2)
