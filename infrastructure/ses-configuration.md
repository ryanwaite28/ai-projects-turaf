# Amazon SES Configuration

**Account**: prod (811783768245)  
**Region**: us-east-1  
**Domain**: turafapp.com  
**Sending Address**: noreply@turafapp.com  
**Configured**: 2024-03-23

---

## Status

- ✅ Domain verified in SES
- ✅ DKIM enabled and configured
- ✅ SPF configured
- ✅ DMARC configured
- ✅ Email address verification initiated
- ⏳ Production access: **Pending approval** (requires manual request)

---

## Domain Verification

**Verification Token**: `7annHFCIRZX1cn9hoor/wKA94wwBWsSndv0X+nVZiiA=`

**DNS Record Added**:
- **Type**: TXT
- **Name**: `_amazonses.turafapp.com`
- **Value**: `"7annHFCIRZX1cn9hoor/wKA94wwBWsSndv0X+nVZiiA="`
- **TTL**: 300

---

## DKIM Configuration

**DKIM Tokens**:
1. `dyz2gbmqvtrmhgubc23flyv5z3rehk2w`
2. `yyerrwgqsxhyxmyhh5ayij3smxgutgxs`
3. `nqe4adr5qtk6bvd3ag72fyz3tbng5p4n`

**DNS Records Added**:

| Type | Name | Value | TTL |
|------|------|-------|-----|
| CNAME | dyz2gbmqvtrmhgubc23flyv5z3rehk2w._domainkey.turafapp.com | dyz2gbmqvtrmhgubc23flyv5z3rehk2w.dkim.amazonses.com | 300 |
| CNAME | yyerrwgqsxhyxmyhh5ayij3smxgutgxs._domainkey.turafapp.com | yyerrwgqsxhyxmyhh5ayij3smxgutgxs.dkim.amazonses.com | 300 |
| CNAME | nqe4adr5qtk6bvd3ag72fyz3tbng5p4n._domainkey.turafapp.com | nqe4adr5qtk6bvd3ag72fyz3tbng5p4n.dkim.amazonses.com | 300 |

---

## SPF Configuration

**DNS Record Added**:
- **Type**: TXT
- **Name**: `turafapp.com`
- **Value**: `"v=spf1 include:amazonses.com ~all"`
- **TTL**: 300

**SPF Policy Explanation**:
- `v=spf1` - SPF version 1
- `include:amazonses.com` - Allow Amazon SES to send emails
- `~all` - Soft fail for other senders (mark as suspicious but don't reject)

---

## DMARC Configuration

**DNS Record Added**:
- **Type**: TXT
- **Name**: `_dmarc.turafapp.com`
- **Value**: `"v=DMARC1; p=quarantine; rua=mailto:admin@turafapp.com; pct=100; adkim=s; aspf=s"`
- **TTL**: 300

**DMARC Policy Explanation**:
- `v=DMARC1` - DMARC version 1
- `p=quarantine` - Quarantine emails that fail authentication
- `rua=mailto:admin@turafapp.com` - Send aggregate reports to admin
- `pct=100` - Apply policy to 100% of emails
- `adkim=s` - Strict DKIM alignment
- `aspf=s` - Strict SPF alignment

---

## Verified Identities

### Domain
- **Domain**: `turafapp.com`
- **Status**: Verified
- **DKIM**: Enabled

### Email Addresses
- **Email**: `noreply@turafapp.com`
- **Status**: Verification email sent (check inbox)
- **Purpose**: Platform transactional emails

---

## Sending Limits

### Current (Sandbox Mode)
- **Max send rate**: 1 email/second
- **Max 24-hour send**: 200 emails/day
- **Restriction**: Can only send to verified email addresses

### After Production Access Approval
- **Max send rate**: 14 emails/second (default)
- **Max 24-hour send**: 50,000 emails/day (default)
- **Restriction**: Can send to any email address
- **Note**: Limits can be increased by request

---

## Production Access Request

### Required Steps

1. **Navigate to AWS Console**:
   - Go to AWS Console → Amazon SES → Account Dashboard
   - Click "Request production access"

2. **Fill Out Request Form**:

   **Use Case**: Transactional emails for SaaS platform

   **Website URL**: https://turafapp.com

   **Description**:
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
   We have implemented bounce and complaint handling via SNS.
   ```

   **Compliance**: Confirm compliance with AWS Acceptable Use Policy

3. **Submit Request**

4. **Wait for Approval**: Typically 24-48 hours

---

## Email Templates

### Transactional Email Types

1. **User Registration**
   - Welcome email
   - Email verification
   - Account activation

2. **Password Management**
   - Password reset requests
   - Password change confirmations

3. **Experiment Notifications**
   - Experiment created
   - Experiment updated
   - Experiment completed

4. **Report Notifications**
   - Report generated
   - Report ready for download

5. **System Alerts**
   - Account security alerts
   - Subscription changes
   - Service updates

---

## Testing

### Verification Commands

```bash
# Check domain verification status
aws ses get-identity-verification-attributes \
  --identities turafapp.com \
  --region us-east-1 \
  --profile turaf-prod

# Check DKIM status
aws ses get-identity-dkim-attributes \
  --identities turafapp.com \
  --region us-east-1 \
  --profile turaf-prod

# Check sending quota
aws ses get-send-quota \
  --region us-east-1 \
  --profile turaf-prod
```

### DNS Verification

```bash
# Verify domain verification TXT record
dig TXT _amazonses.turafapp.com +short

# Verify DKIM CNAME records
dig CNAME dyz2gbmqvtrmhgubc23flyv5z3rehk2w._domainkey.turafapp.com +short
dig CNAME yyerrwgqsxhyxmyhh5ayij3smxgutgxs._domainkey.turafapp.com +short
dig CNAME nqe4adr5qtk6bvd3ag72fyz3tbng5p4n._domainkey.turafapp.com +short

# Verify SPF record
dig TXT turafapp.com +short | grep spf1

# Verify DMARC record
dig TXT _dmarc.turafapp.com +short
```

### Send Test Email

```bash
# Send test email (only works after email verification)
aws ses send-email \
  --from noreply@turafapp.com \
  --destination ToAddresses=admin@turafapp.com \
  --message \
    Subject={Data="SES Test Email",Charset=utf-8},\
    Body={Text={Data="This is a test email from Amazon SES.",Charset=utf-8}} \
  --region us-east-1 \
  --profile turaf-prod
```

---

## Bounce and Complaint Handling

### SNS Topics (To Be Configured)

```bash
# Create SNS topics for notifications
aws sns create-topic --name ses-bounces --region us-east-1 --profile turaf-prod
aws sns create-topic --name ses-complaints --region us-east-1 --profile turaf-prod

# Configure SES to publish notifications
aws ses set-identity-notification-topic \
  --identity turafapp.com \
  --notification-type Bounce \
  --sns-topic arn:aws:sns:us-east-1:811783768245:ses-bounces \
  --region us-east-1 \
  --profile turaf-prod

aws ses set-identity-notification-topic \
  --identity turafapp.com \
  --notification-type Complaint \
  --sns-topic arn:aws:sns:us-east-1:811783768245:ses-complaints \
  --region us-east-1 \
  --profile turaf-prod
```

---

## Monitoring

### Key Metrics to Monitor

- **Bounce Rate**: Should be < 5%
- **Complaint Rate**: Should be < 0.1%
- **Send Rate**: Monitor against quota
- **Delivery Rate**: Should be > 95%

### CloudWatch Metrics

Available SES metrics in CloudWatch:
- `Reputation.BounceRate`
- `Reputation.ComplaintRate`
- `Send`
- `Delivery`
- `Bounce`
- `Complaint`
- `Reject`

### Recommended Alarms

```bash
# High bounce rate alarm
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
  --region us-east-1 \
  --profile turaf-prod

# High complaint rate alarm
aws cloudwatch put-metric-alarm \
  --alarm-name ses-high-complaint-rate \
  --alarm-description "Alert when SES complaint rate exceeds 0.1%" \
  --metric-name Reputation.ComplaintRate \
  --namespace AWS/SES \
  --statistic Average \
  --period 300 \
  --threshold 0.001 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --region us-east-1 \
  --profile turaf-prod
```

---

## Best Practices

### Email Sending
1. ✅ Use verified sender addresses
2. ✅ Include unsubscribe links in all marketing emails
3. ✅ Monitor bounce and complaint rates
4. ✅ Implement email throttling to stay within limits
5. ✅ Use configuration sets for tracking
6. ✅ Handle bounces and complaints promptly

### Email Content
1. Use clear, descriptive subject lines
2. Include plain text version of HTML emails
3. Avoid spam trigger words
4. Include physical mailing address (for marketing emails)
5. Provide easy unsubscribe mechanism
6. Personalize emails when possible

### Security
1. ✅ DKIM signatures enabled
2. ✅ SPF record configured
3. ✅ DMARC policy set to quarantine
4. Use HTTPS for all links
5. Don't include sensitive information in emails
6. Implement email encryption for sensitive data

---

## Cost Estimation

**SES Pricing** (us-east-1):
- First 62,000 emails/month: **Free** (if sent from EC2)
- Additional emails: $0.10 per 1,000 emails
- Attachments: $0.12 per GB
- Dedicated IP: $24.95/month (optional)

**Estimated Monthly Cost**:
- 5,000 emails/month: **$0** (within free tier)
- 100,000 emails/month: ~$4
- 1,000,000 emails/month: ~$94

**Current Expected Usage**: 1,000-5,000 emails/day = 30,000-150,000 emails/month

**Estimated Cost**: $0-$9/month

---

## Troubleshooting

### Domain Verification Pending

**Issue**: Domain verification status shows "Pending"

**Solutions**:
1. Wait 5-30 minutes for DNS propagation
2. Verify TXT record exists: `dig TXT _amazonses.turafapp.com +short`
3. Check for typos in verification token
4. Retry verification check

### DKIM Verification Failing

**Issue**: DKIM status shows "Failed" or "Pending"

**Solutions**:
1. Verify all 3 CNAME records exist
2. Check for typos in DKIM tokens
3. Wait for DNS propagation (up to 72 hours)
4. Ensure CNAME values point to `.dkim.amazonses.com`

### Test Email Not Received

**Issue**: Test emails not arriving

**Solutions**:
1. Verify email address is verified in SES
2. Check spam/junk folder
3. Verify sending quota not exceeded
4. Check CloudWatch logs for errors
5. Ensure account is not in sandbox mode (or recipient is verified)

### Production Access Denied

**Issue**: Production access request rejected

**Solutions**:
1. Provide more detailed use case description
2. Ensure website is accessible and professional
3. Clarify email opt-in process
4. Describe bounce/complaint handling procedures
5. Resubmit with additional details

---

## Integration with Application

### Spring Boot Configuration

```yaml
# application.yml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: ${AWS_SES_SMTP_USERNAME}
    password: ${AWS_SES_SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

### AWS SDK Configuration

```java
@Configuration
public class SesConfig {
    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }
}
```

---

## References

- [Amazon SES Documentation](https://docs.aws.amazon.com/ses/)
- [SES Domain Verification](https://docs.aws.amazon.com/ses/latest/dg/verify-domain-procedure.html)
- [SES DKIM](https://docs.aws.amazon.com/ses/latest/dg/send-email-authentication-dkim.html)
- [SES Production Access](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html)
- [SES Best Practices](https://docs.aws.amazon.com/ses/latest/dg/best-practices.html)
- specs/notification-service.md
- INFRASTRUCTURE_PLAN.md (Phase 2.3)
