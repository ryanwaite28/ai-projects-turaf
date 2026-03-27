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

- [x] Root account wildcard certificate requested for *.turafapp.com
- [x] Root account DNS validation records created in Route 53
- [x] Root account certificate validated and issued
- [x] Root account certificate ARN documented
- [x] DEV account certificate requested (801651112319)
- [x] QA account certificate requested (965932217544)
- [x] PROD account certificate requested (811783768245)
- [x] All environment certificates validated and issued
- [x] All certificate ARNs documented

---

## Implementation

### 1. Request Root Account Wildcard Certificate

```bash
# Authenticate to root account
aws sso login --profile turaf-root

# Request certificate in us-east-1 (required for CloudFront)
aws acm request-certificate \
  --domain-name "*.turafapp.com" \
  --subject-alternative-names "turafapp.com" \
  --validation-method DNS \
  --region us-east-1 \
  --profile turaf-root

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

### 4. Wait for Root Certificate Validation

```bash
# Wait for validation (can take 5-30 minutes)
aws acm wait certificate-validated \
  --certificate-arn $CERT_ARN \
  --region us-east-1 \
  --profile turaf-root

# Check status
aws acm describe-certificate \
  --certificate-arn $CERT_ARN \
  --region us-east-1 \
  --profile turaf-root \
  --query 'Certificate.Status'
```

**Expected**: `"ISSUED"`

---

## Multi-Account Certificate Provisioning

**Objective**: Request certificates in DEV, QA, and PROD accounts for ALB HTTPS listeners.

**Note**: ACM certificates cannot be shared across AWS accounts. Each environment requires its own certificate.

### 5. Request DEV Account Certificate

```bash
# Authenticate to DEV account
aws sso login --profile turaf-dev

# Request certificate
aws acm request-certificate \
  --domain-name "*.turafapp.com" \
  --subject-alternative-names "turafapp.com" \
  --validation-method DNS \
  --region us-east-1 \
  --profile turaf-dev

# Save DEV certificate ARN
DEV_CERT_ARN="<ARN_FROM_OUTPUT>"
```

### 6. Request QA Account Certificate

```bash
# Authenticate to QA account
aws sso login --profile turaf-qa

# Request certificate
aws acm request-certificate \
  --domain-name "*.turafapp.com" \
  --subject-alternative-names "turafapp.com" \
  --validation-method DNS \
  --region us-east-1 \
  --profile turaf-qa

# Save QA certificate ARN
QA_CERT_ARN="<ARN_FROM_OUTPUT>"
```

### 7. Request PROD Account Certificate

```bash
# Authenticate to PROD account
aws sso login --profile turaf-prod

# Request certificate
aws acm request-certificate \
  --domain-name "*.turafapp.com" \
  --subject-alternative-names "turafapp.com" \
  --validation-method DNS \
  --region us-east-1 \
  --profile turaf-prod

# Save PROD certificate ARN
PROD_CERT_ARN="<ARN_FROM_OUTPUT>"
```

### 8. Add Validation Records for All Certificates

```bash
# For each certificate, get validation CNAME record
# DEV
aws acm describe-certificate \
  --certificate-arn $DEV_CERT_ARN \
  --region us-east-1 \
  --profile turaf-dev \
  --query 'Certificate.DomainValidationOptions[0].ResourceRecord'

# QA
aws acm describe-certificate \
  --certificate-arn $QA_CERT_ARN \
  --region us-east-1 \
  --profile turaf-qa \
  --query 'Certificate.DomainValidationOptions[0].ResourceRecord'

# PROD
aws acm describe-certificate \
  --certificate-arn $PROD_CERT_ARN \
  --region us-east-1 \
  --profile turaf-prod \
  --query 'Certificate.DomainValidationOptions[0].ResourceRecord'

# Add all validation CNAME records to Route 53 (root account)
# Each certificate will have a unique validation record
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://acm-validation-all-accounts.json \
  --profile turaf-root
```

### 9. Wait for All Certificates to Validate

```bash
# DEV
aws acm wait certificate-validated \
  --certificate-arn $DEV_CERT_ARN \
  --region us-east-1 \
  --profile turaf-dev

# QA
aws acm wait certificate-validated \
  --certificate-arn $QA_CERT_ARN \
  --region us-east-1 \
  --profile turaf-qa

# PROD
aws acm wait certificate-validated \
  --certificate-arn $PROD_CERT_ARN \
  --region us-east-1 \
  --profile turaf-prod
```

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

## Implementation Results

### ✅ Certificate Requests Completed (2026-03-26)

**DEV Account Certificate**:
- **ARN**: `arn:aws:acm:us-east-1:801651112319:certificate/8b83b688-7458-4627-9fd4-ff3b2801bf70`
- **Account**: 801651112319
- **Status**: ISSUED ✅
- **Validation**: DNS (CNAME)
- **Issued**: 2026-03-26

**QA Account Certificate**:
- **ARN**: `arn:aws:acm:us-east-1:965932217544:certificate/906b4a44-11e3-4ee7-b10d-9f715ffc0ee6`
- **Account**: 965932217544
- **Status**: ISSUED ✅
- **Validation**: DNS (CNAME)
- **Issued**: 2026-03-26

**PROD Account Certificate**:
- **ARN**: `arn:aws:acm:us-east-1:811783768245:certificate/779b5c14-8fc0-44fe-80b4-090bdee1ef62`
- **Account**: 811783768245
- **Status**: ISSUED ✅
- **Validation**: DNS (CNAME)
- **Issued**: 2026-03-26

**Validation Records Added to Route 53**:
- All three validation CNAME records added to hosted zone `Z055341020TQZLU2CKWOE`
- Validation completed successfully in < 2 minutes
- Records file: `infrastructure/acm-validation-all-accounts.json`

**Documentation Updated**:
- ✅ `infrastructure/acm-certificates.md` - All certificate ARNs documented
- ✅ `specs/domain-dns-management.md` - Certificate status updated to ISSUED
- ✅ Task acceptance criteria marked complete

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
