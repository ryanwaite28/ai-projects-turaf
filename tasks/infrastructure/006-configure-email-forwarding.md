# Task: Configure Email Forwarding

**Service**: Infrastructure  
**Type**: Domain and Email Configuration  
**Priority**: Medium  
**Estimated Time**: 30 minutes  
**Dependencies**: 017-configure-route53-hosted-zone

---

## Objective

Configure email forwarding aliases at whois.com/titan.email to route all platform and AWS account emails to the primary admin email address.

---

## Acceptance Criteria

- [ ] All AWS account emails forwarded to admin@turafapp.com
- [ ] All application emails forwarded to admin@turafapp.com
- [ ] Email forwarding tested and verified
- [ ] Configuration documented

---

## Implementation

### 1. Access Titan Email Control Panel

**Steps**:
1. Log into whois.com account
2. Navigate to turafapp.com domain management
3. Access Titan Email control panel
4. Go to Email Forwarding/Aliases section

### 2. Configure AWS Account Email Aliases

**Create the following forwarding rules**:

```
Source Email                    → Destination
─────────────────────────────────────────────────
aws@turafapp.com               → admin@turafapp.com
aws-ops@turafapp.com           → admin@turafapp.com
aws-dev@turafapp.com           → admin@turafapp.com
aws-qa@turafapp.com            → admin@turafapp.com
aws-prod@turafapp.com          → admin@turafapp.com
```

**Purpose**: Receive AWS account notifications, billing alerts, and service updates

### 3. Configure Application Email Aliases

**Create the following forwarding rules**:

```
Source Email                    → Destination
─────────────────────────────────────────────────
noreply@turafapp.com           → admin@turafapp.com
notifications@turafapp.com     → admin@turafapp.com
support@turafapp.com           → admin@turafapp.com
```

**Purpose**: 
- `noreply@turafapp.com` - SES sending address for platform notifications
- `notifications@turafapp.com` - Alternative notification address
- `support@turafapp.com` - Customer support inquiries

### 4. Titan Email Configuration Steps

**For each alias**:

1. Click "Add Email Alias" or "Email Forwarding"
2. Enter source email address (e.g., `aws@turafapp.com`)
3. Select "Forward to external address"
4. Enter destination: `admin@turafapp.com`
5. Enable forwarding
6. Save configuration

---

## Verification

### 1. Test Email Forwarding

**Send test emails to each alias**:

```bash
# From external email account, send test emails to:
# - aws@turafapp.com
# - aws-ops@turafapp.com
# - aws-dev@turafapp.com
# - aws-qa@turafapp.com
# - aws-prod@turafapp.com
# - noreply@turafapp.com
# - notifications@turafapp.com
# - support@turafapp.com

# Verify all emails arrive at admin@turafapp.com
```

### 2. Check Forwarding Status

**In Titan Email control panel**:
- Verify all aliases show "Active" status
- Check forwarding rules are correctly configured
- Ensure no delivery errors

### 3. Verify Email Headers

**Check forwarded email headers**:
- Original recipient should show the alias address
- Forwarded-To should show admin@turafapp.com
- No delivery failures or bounces

---

## Alternative Configuration (if needed)

### Option 1: Catch-All Forwarding

If Titan supports catch-all forwarding:

```
*@turafapp.com → admin@turafapp.com
```

**Pros**: Automatically forwards all emails  
**Cons**: May receive spam, less granular control

### Option 2: Individual Mailboxes

Create separate mailboxes instead of aliases:

```
Create mailbox: aws@turafapp.com
Create mailbox: notifications@turafapp.com
Create mailbox: support@turafapp.com
```

**Pros**: Better organization, separate inboxes  
**Cons**: Need to check multiple mailboxes or set up client-side forwarding

---

## Troubleshooting

### Issue: Emails not being forwarded

**Cause**: Forwarding rule not active or misconfigured

**Solution**:
- Verify forwarding rule is enabled
- Check destination email is correct
- Ensure no typos in email addresses
- Check spam/junk folder at destination

### Issue: Forwarding delayed

**Cause**: Email server propagation or processing delay

**Solution**:
- Wait 5-10 minutes for email delivery
- Check Titan Email logs for delivery status
- Verify no email quotas exceeded

### Issue: Cannot create alias

**Cause**: Alias limit reached or naming conflict

**Solution**:
- Check Titan Email plan limits
- Verify alias doesn't conflict with existing mailbox
- Contact Titan support if needed

---

## Documentation

### Save Configuration Details

Create `infrastructure/email-forwarding-config.md`:

```markdown
# Email Forwarding Configuration

**Provider**: Titan Email (via whois.com)  
**Domain**: turafapp.com  
**Primary Email**: admin@turafapp.com  
**Configured**: 2024-01-01

## AWS Account Emails

| Alias | Forwards To | Purpose |
|-------|-------------|---------|
| aws@turafapp.com | admin@turafapp.com | Root account |
| aws-ops@turafapp.com | admin@turafapp.com | Ops account |
| aws-dev@turafapp.com | admin@turafapp.com | Dev account |
| aws-qa@turafapp.com | admin@turafapp.com | QA account |
| aws-prod@turafapp.com | admin@turafapp.com | Prod account |

## Application Emails

| Alias | Forwards To | Purpose |
|-------|-------------|---------|
| noreply@turafapp.com | admin@turafapp.com | SES sending address |
| notifications@turafapp.com | admin@turafapp.com | Platform notifications |
| support@turafapp.com | admin@turafapp.com | Customer support |

## Testing

Last tested: 2024-01-01  
Status: ✅ All aliases working
```

---

## Security Considerations

### Email Security Best Practices

1. **Enable SPF/DKIM/DMARC** (configured in SES task)
2. **Monitor for spoofing** - watch for unauthorized use of aliases
3. **Regular testing** - verify forwarding works monthly
4. **Backup access** - ensure admin@turafapp.com is accessible

### Access Control

- Limit who has access to Titan Email control panel
- Use strong password for whois.com account
- Enable two-factor authentication if available
- Document who has access to email configuration

---

## Cost

**Titan Email**: Included with domain registration at whois.com  
**Email Forwarding**: Free (included in email hosting plan)

---

## Checklist

- [ ] Logged into whois.com/Titan Email
- [ ] Created aws@turafapp.com forwarding rule
- [ ] Created aws-ops@turafapp.com forwarding rule
- [ ] Created aws-dev@turafapp.com forwarding rule
- [ ] Created aws-qa@turafapp.com forwarding rule
- [ ] Created aws-prod@turafapp.com forwarding rule
- [ ] Created noreply@turafapp.com forwarding rule
- [ ] Created notifications@turafapp.com forwarding rule
- [ ] Created support@turafapp.com forwarding rule
- [ ] Tested all forwarding rules
- [ ] Verified emails arrive at admin@turafapp.com
- [ ] Documented configuration

---

## Next Steps

After email forwarding is configured:
1. Proceed to task 020: Create Service Control Policies
2. Use configured emails for AWS account setup
3. Configure SES to send from noreply@turafapp.com (task 023)

---

## References

- [Titan Email Documentation](https://www.titan.email/help)
- specs/domain-dns-management.md (Section: Email Forwarding)
- INFRASTRUCTURE_PLAN.md (Phase 1.3)
