# Task: Configure Amazon SES

**Service**: Infrastructure  
**Type**: Email Service Configuration  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 017-configure-route53-hosted-zone, 019-configure-email-forwarding

---

## Objective

Configure Amazon Simple Email Service (SES) in the production account for sending platform notification emails, including domain verification, DKIM, SPF, DMARC, and production access request.

---

## Acceptance Criteria

- [x] SES configured in prod account (us-east-1)
- [x] Domain turafapp.com verified in SES
- [x] DKIM records added to Route 53
- [x] SPF record configured
- [x] DMARC record configured
- [x] Email addresses verified (noreply@turafapp.com - verification email sent)
- [ ] Production access requested and approved (requires manual AWS Console request)
- [ ] Test email sent successfully (pending email verification)

---

## Implementation

### 1. Verify Domain in SES

**In prod account (811783768245)**:

```bash
# Switch to prod account
export AWS_PROFILE=turaf-prod

# Verify domain
aws ses verify-domain-identity \
  --domain turafapp.com \
  --region us-east-1

# Save verification token from output
VERIFICATION_TOKEN="<TOKEN_FROM_OUTPUT>"
```

**Expected Output**:
```json
{
    "VerificationToken": "abc123xyz789..."
}
```

### 2. Add Domain Verification TXT Record to Route 53

```bash
# Get hosted zone ID (in root account)
export AWS_PROFILE=default
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones \
  --query "HostedZones[?Name=='turafapp.com.'].Id" \
  --output text | cut -d'/' -f3)

# Create verification record
cat > ses-verification-record.json <<EOF
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "_amazonses.turafapp.com",
      "Type": "TXT",
      "TTL": 300,
      "ResourceRecords": [{"Value": "\"${VERIFICATION_TOKEN}\""}]
    }
  }]
}
EOF

# Add record to Route 53
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://ses-verification-record.json
```

### 3. Wait for Domain Verification

```bash
# Switch back to prod account
export AWS_PROFILE=turaf-prod

# Check verification status (may take 5-30 minutes)
aws ses get-identity-verification-attributes \
  --identities turafapp.com \
  --region us-east-1 \
  --query 'VerificationAttributes.*.VerificationStatus'

# Expected: "Success"
```

### 4. Enable DKIM for Domain

```bash
# Enable DKIM
aws ses set-identity-dkim-enabled \
  --identity turafapp.com \
  --dkim-enabled \
  --region us-east-1

# Get DKIM tokens
aws ses verify-domain-dkim \
  --domain turafapp.com \
  --region us-east-1

# Save the 3 DKIM tokens from output
```

**Expected Output**:
```json
{
    "DkimTokens": [
        "token1abc123",
        "token2def456",
        "token3ghi789"
    ]
}
```

### 5. Add DKIM CNAME Records to Route 53

```bash
# Switch to root account for Route 53
export AWS_PROFILE=default

# Create DKIM records (replace tokens with actual values)
cat > ses-dkim-records.json <<EOF
{
  "Changes": [
    {
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "token1abc123._domainkey.turafapp.com",
        "Type": "CNAME",
        "TTL": 300,
        "ResourceRecords": [{"Value": "token1abc123.dkim.amazonses.com"}]
      }
    },
    {
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "token2def456._domainkey.turafapp.com",
        "Type": "CNAME",
        "TTL": 300,
        "ResourceRecords": [{"Value": "token2def456.dkim.amazonses.com"}]
      }
    },
    {
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "token3ghi789._domainkey.turafapp.com",
        "Type": "CNAME",
        "TTL": 300,
        "ResourceRecords": [{"Value": "token3ghi789.dkim.amazonses.com"}]
      }
    }
  ]
}
EOF

# Add DKIM records
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://ses-dkim-records.json
```

### 6. Configure SPF Record

```bash
# Create SPF record
cat > ses-spf-record.json <<EOF
{
  "Changes": [{
    "Action": "UPSERT",
    "ResourceRecordSet": {
      "Name": "turafapp.com",
      "Type": "TXT",
      "TTL": 300,
      "ResourceRecords": [{"Value": "\"v=spf1 include:amazonses.com ~all\""}]
    }
  }]
}
EOF

# Add SPF record
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://ses-spf-record.json
```

### 7. Configure DMARC Record

```bash
# Create DMARC record
cat > ses-dmarc-record.json <<EOF
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "_dmarc.turafapp.com",
      "Type": "TXT",
      "TTL": 300,
      "ResourceRecords": [{"Value": "\"v=DMARC1; p=quarantine; rua=mailto:admin@turafapp.com; pct=100; adkim=s; aspf=s\""}]
    }
  }]
}
EOF

# Add DMARC record
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://ses-dmarc-record.json
```

### 8. Verify Email Address

```bash
# Switch to prod account
export AWS_PROFILE=turaf-prod

# Verify noreply email address
aws ses verify-email-identity \
  --email-address noreply@turafapp.com \
  --region us-east-1

# Check email inbox for verification link
# Click verification link in email
```

### 9. Configure SES Sending Limits

```bash
# Check current sending limits
aws ses get-send-quota \
  --region us-east-1

# Expected (sandbox):
# MaxSendRate: 1 email/second
# Max24HourSend: 200 emails/day
```

### 10. Request Production Access

**Create production access request**:

1. Go to AWS Console → SES → Account Dashboard
2. Click "Request production access"
3. Fill out form:
   - **Use case**: Transactional emails for SaaS platform
   - **Website URL**: https://turafapp.com
   - **Description**: 
     ```
     Turaf is a SaaS platform for problem tracking and solution validation.
     We need to send transactional emails including:
     - User registration confirmations
     - Password reset emails
     - Experiment notifications
     - Report generation notifications
     - System alerts
     
     Expected volume: 1,000-5,000 emails/day
     All emails are opt-in and include unsubscribe links.
     ```
   - **Compliance**: Confirm compliance with AWS policies
4. Submit request
5. Wait for approval (typically 24-48 hours)

---

## Verification

### 1. Verify Domain Status

```bash
# Check domain verification
aws ses get-identity-verification-attributes \
  --identities turafapp.com \
  --region us-east-1

# Expected: VerificationStatus: "Success"
```

### 2. Verify DKIM Status

```bash
# Check DKIM status
aws ses get-identity-dkim-attributes \
  --identities turafapp.com \
  --region us-east-1

# Expected: DkimEnabled: true, DkimVerificationStatus: "Success"
```

### 3. Verify DNS Records

```bash
# Check TXT record (domain verification)
dig TXT _amazonses.turafapp.com +short

# Check CNAME records (DKIM)
dig CNAME token1abc123._domainkey.turafapp.com +short
dig CNAME token2def456._domainkey.turafapp.com +short
dig CNAME token3ghi789._domainkey.turafapp.com +short

# Check SPF record
dig TXT turafapp.com +short | grep spf1

# Check DMARC record
dig TXT _dmarc.turafapp.com +short
```

### 4. Send Test Email

```bash
# Send test email
aws ses send-email \
  --from noreply@turafapp.com \
  --destination ToAddresses=admin@turafapp.com \
  --message \
    Subject={Data="SES Test Email",Charset=utf-8},\
    Body={Text={Data="This is a test email from Amazon SES.",Charset=utf-8}} \
  --region us-east-1

# Check admin@turafapp.com inbox for test email
```

### 5. Check Email Deliverability

**Test with mail-tester.com**:

```bash
# Send email to mail-tester address
aws ses send-email \
  --from noreply@turafapp.com \
  --destination ToAddresses=test-xxxxx@mail-tester.com \
  --message \
    Subject={Data="Deliverability Test",Charset=utf-8},\
    Body={Text={Data="Testing email deliverability.",Charset=utf-8}} \
  --region us-east-1

# Visit mail-tester.com to see score (aim for 10/10)
```

---

## SES Configuration Summary

### DNS Records Added

| Record Type | Name | Value | Purpose |
|-------------|------|-------|---------|
| TXT | _amazonses.turafapp.com | {verification-token} | Domain verification |
| CNAME | {token1}._domainkey.turafapp.com | {token1}.dkim.amazonses.com | DKIM signature |
| CNAME | {token2}._domainkey.turafapp.com | {token2}.dkim.amazonses.com | DKIM signature |
| CNAME | {token3}._domainkey.turafapp.com | {token3}.dkim.amazonses.com | DKIM signature |
| TXT | turafapp.com | v=spf1 include:amazonses.com ~all | SPF policy |
| TXT | _dmarc.turafapp.com | v=DMARC1; p=quarantine; ... | DMARC policy |

### Verified Identities

- **Domain**: turafapp.com
- **Email**: noreply@turafapp.com

### Sending Limits

**Sandbox** (initial):
- 1 email/second
- 200 emails/day
- Can only send to verified addresses

**Production** (after approval):
- 14 emails/second (default, can request increase)
- 50,000 emails/day (default, can request increase)
- Can send to any address

---

## Troubleshooting

### Issue: Domain verification pending

**Cause**: DNS propagation delay

**Solution**:
```bash
# Wait 5-30 minutes for DNS propagation
# Check DNS record exists
dig TXT _amazonses.turafapp.com +short

# Retry verification check
aws ses get-identity-verification-attributes \
  --identities turafapp.com \
  --region us-east-1
```

### Issue: DKIM verification failing

**Cause**: CNAME records not propagated or incorrect

**Solution**:
```bash
# Verify CNAME records exist
dig CNAME {token}._domainkey.turafapp.com +short

# Check for typos in DKIM token
# Wait for DNS propagation
```

### Issue: Test email not received

**Cause**: Email in sandbox mode or verification pending

**Solution**:
```bash
# Verify email address is verified
aws ses get-identity-verification-attributes \
  --identities noreply@turafapp.com \
  --region us-east-1

# Check spam/junk folder
# Verify sending quota not exceeded
```

### Issue: Production access request denied

**Cause**: Insufficient information or compliance concerns

**Solution**:
- Provide more detailed use case description
- Ensure website is accessible
- Clarify email opt-in process
- Describe bounce/complaint handling
- Resubmit request with additional details

---

## SES Best Practices

### 1. Bounce and Complaint Handling

```bash
# Set up SNS topics for bounces and complaints
aws sns create-topic \
  --name ses-bounces \
  --region us-east-1

aws sns create-topic \
  --name ses-complaints \
  --region us-east-1

# Configure SES to publish to SNS
aws ses set-identity-notification-topic \
  --identity turafapp.com \
  --notification-type Bounce \
  --sns-topic arn:aws:sns:us-east-1:811783768245:ses-bounces \
  --region us-east-1

aws ses set-identity-notification-topic \
  --identity turafapp.com \
  --notification-type Complaint \
  --sns-topic arn:aws:sns:us-east-1:811783768245:ses-complaints \
  --region us-east-1
```

### 2. Email Sending Best Practices

- Use verified sender addresses
- Include unsubscribe links
- Monitor bounce and complaint rates
- Implement email throttling
- Use configuration sets for tracking
- Handle bounces and complaints promptly

### 3. Monitoring

```bash
# Create CloudWatch alarms for bounce rate
aws cloudwatch put-metric-alarm \
  --alarm-name ses-high-bounce-rate \
  --alarm-description "Alert when SES bounce rate exceeds 5%" \
  --metric-name Reputation.BounceRate \
  --namespace AWS/SES \
  --statistic Average \
  --period 300 \
  --threshold 0.05 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --region us-east-1
```

---

## Cost Estimation

**SES Pricing** (us-east-1):
- First 62,000 emails/month: Free (if sent from EC2)
- Additional emails: $0.10 per 1,000 emails
- Attachments: $0.12 per GB

**Estimated Monthly Cost**:
- 5,000 emails/month: **Free**
- 100,000 emails/month: ~$4
- 1,000,000 emails/month: ~$94

---

## Documentation

Create `infrastructure/ses-configuration.md`:

```markdown
# Amazon SES Configuration

**Account**: prod (811783768245)  
**Region**: us-east-1  
**Domain**: turafapp.com  
**Sending Address**: noreply@turafapp.com

## Status

- ✅ Domain verified
- ✅ DKIM enabled
- ✅ SPF configured
- ✅ DMARC configured
- ⏳ Production access: Pending/Approved

## DNS Records

- TXT: _amazonses.turafapp.com
- CNAME: {token1}._domainkey.turafapp.com
- CNAME: {token2}._domainkey.turafapp.com
- CNAME: {token3}._domainkey.turafapp.com
- TXT: turafapp.com (SPF)
- TXT: _dmarc.turafapp.com

## Sending Limits

- Max send rate: 14 emails/second
- Max 24-hour send: 50,000 emails

## Monitoring

- Bounce rate: <5%
- Complaint rate: <0.1%
- CloudWatch alarms: Enabled
```

---

## Checklist

- [x] Domain verified in SES
- [x] Domain verification TXT record added to Route 53
- [x] DKIM enabled for domain
- [x] 3 DKIM CNAME records added to Route 53
- [x] SPF TXT record added to Route 53
- [x] DMARC TXT record added to Route 53
- [x] Email address noreply@turafapp.com verification initiated
- [ ] Verify noreply@turafapp.com (check email inbox and click verification link)
- [ ] Test email sent successfully (after email verification)
- [ ] Production access requested (manual - AWS Console)
- [ ] Production access approved (wait 24-48 hours after request)
- [ ] SNS topics configured for bounces/complaints (deferred)
- [ ] CloudWatch alarms configured (deferred)
- [x] SES configuration documented

---

## Next Steps

After SES configuration:
1. ✅ **COMPLETED** - SES configured with domain verification, DKIM, SPF, and DMARC
2. **MANUAL ACTION REQUIRED**: 
   - Check inbox for noreply@turafapp.com verification email and click verification link
   - Request production access via AWS Console (SES → Account Dashboard → Request production access)
3. Proceed to **Task 011: Create ECR Repositories**
4. Update Notification Service to use SES (notification-service tasks)
5. Configure email templates for platform notifications

## Implementation Results (2024-03-23)

### ✅ Domain Verification

**Domain**: `turafapp.com`
- **Status**: ✅ Verified
- **Verification Token**: `7annHFCIRZX1cn9hoor/wKA94wwBWsSndv0X+nVZiiA=`
- **DNS Record**: TXT `_amazonses.turafapp.com`

### ✅ DKIM Configuration

**Status**: ✅ Enabled and Verified

**DKIM Tokens**:
1. `dyz2gbmqvtrmhgubc23flyv5z3rehk2w`
2. `yyerrwgqsxhyxmyhh5ayij3smxgutgxs`
3. `nqe4adr5qtk6bvd3ag72fyz3tbng5p4n`

**DNS Records Added**: 3 CNAME records for DKIM signatures

### ✅ SPF Configuration

**DNS Record**: TXT `turafapp.com`
- **Value**: `"v=spf1 include:amazonses.com ~all"`
- **Status**: ✅ Configured

### ✅ DMARC Configuration

**DNS Record**: TXT `_dmarc.turafapp.com`
- **Value**: `"v=DMARC1; p=quarantine; rua=mailto:admin@turafapp.com; pct=100; adkim=s; aspf=s"`
- **Status**: ✅ Configured
- **Policy**: Quarantine failed emails, send reports to admin@turafapp.com

### ✅ Email Address Verification

**Email**: `noreply@turafapp.com`
- **Status**: ⏳ Verification email sent
- **Action Required**: Check inbox and click verification link

### 📊 Current Sending Limits (Sandbox Mode)

- **Max send rate**: 1 email/second
- **Max 24-hour send**: 200 emails/day
- **Restriction**: Can only send to verified email addresses
- **Sent last 24 hours**: 0 emails

### 📁 Documentation Created

- ✅ `infrastructure/ses-configuration.md` - Complete SES configuration with:
  - Domain and DKIM verification details
  - DNS records (verification, DKIM, SPF, DMARC)
  - Production access request instructions
  - Testing and verification commands
  - Bounce/complaint handling setup
  - Monitoring and CloudWatch alarms
  - Integration examples for Spring Boot
  - Cost estimation

### 🎯 Benefits

- ✅ **Domain authenticated** - DKIM, SPF, and DMARC configured
- ✅ **Email deliverability** - Proper authentication improves inbox placement
- ✅ **Security** - DMARC policy protects against email spoofing
- ✅ **Monitoring ready** - DNS records in place for tracking
- ✅ **Production ready** - Pending manual production access request

### ⚠️ Manual Actions Required

**1. Verify Email Address** (5 minutes):
- Check inbox for `noreply@turafapp.com`
- Click verification link in email from Amazon SES
- Verify email is confirmed in SES console

**2. Request Production Access** (10 minutes + 24-48 hours approval):
1. Log into AWS Console → prod account (811783768245)
2. Navigate to Amazon SES → Account Dashboard
3. Click "Request production access"
4. Fill out form:
   - **Use case**: Transactional emails for SaaS platform
   - **Website URL**: https://turafapp.com
   - **Description**: (see infrastructure/ses-configuration.md for template)
   - **Expected volume**: 1,000-5,000 emails/day
5. Submit request
6. Wait for AWS approval (typically 24-48 hours)

**3. After Production Access Approved**:
- Test email sending to any address
- Configure SNS topics for bounce/complaint handling
- Set up CloudWatch alarms for monitoring

---

## References

- [Amazon SES Documentation](https://docs.aws.amazon.com/ses/)
- [SES Domain Verification](https://docs.aws.amazon.com/ses/latest/dg/verify-domain-procedure.html)
- [SES DKIM](https://docs.aws.amazon.com/ses/latest/dg/send-email-authentication-dkim.html)
- [SES Production Access](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html)
- specs/notification-service.md
- INFRASTRUCTURE_PLAN.md (Phase 2.3)
