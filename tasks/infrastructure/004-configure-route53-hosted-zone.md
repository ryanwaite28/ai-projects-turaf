# Task: Configure Route 53 Hosted Zone

**Service**: Infrastructure  
**Type**: Domain and DNS Configuration  
**Priority**: High  
**Estimated Time**: 1 hour  
**Dependencies**: 014-verify-aws-organization

---

## Objective

Create a Route 53 public hosted zone for turafapp.com and delegate DNS management from whois.com to AWS Route 53.

---

## Acceptance Criteria

- [ ] Route 53 public hosted zone created for turafapp.com
- [ ] Hosted zone ID documented
- [ ] 4 AWS nameservers obtained
- [ ] Nameservers updated at whois.com
- [ ] DNS delegation verified
- [ ] DNS propagation confirmed

---

## Implementation

### 1. Create Public Hosted Zone

```bash
# Create hosted zone in root account
aws route53 create-hosted-zone \
  --name turafapp.com \
  --caller-reference $(date +%s) \
  --hosted-zone-config Comment="Public hosted zone for Turaf platform"
```

**Expected Output**:
```json
{
    "HostedZone": {
        "Id": "/hostedzone/Z1234567890ABC",
        "Name": "turafapp.com.",
        "CallerReference": "1234567890",
        "Config": {
            "Comment": "Public hosted zone for Turaf platform",
            "PrivateZone": false
        },
        "ResourceRecordSetCount": 2
    },
    "DelegationSet": {
        "NameServers": [
            "ns-1234.awsdns-12.org",
            "ns-5678.awsdns-34.co.uk",
            "ns-9012.awsdns-56.com",
            "ns-3456.awsdns-78.net"
        ]
    }
}
```

### 2. Save Hosted Zone Information

```bash
# Extract hosted zone ID
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones \
  --query "HostedZones[?Name=='turafapp.com.'].Id" \
  --output text | cut -d'/' -f3)

echo "Hosted Zone ID: $HOSTED_ZONE_ID"

# Save to file
echo "HOSTED_ZONE_ID=$HOSTED_ZONE_ID" > infrastructure/route53-config.txt
```

### 3. Get Nameservers

```bash
# Get nameservers
aws route53 get-hosted-zone \
  --id $HOSTED_ZONE_ID \
  --query 'DelegationSet.NameServers' \
  --output table

# Save nameservers
aws route53 get-hosted-zone \
  --id $HOSTED_ZONE_ID \
  --query 'DelegationSet.NameServers' \
  --output text > infrastructure/nameservers.txt
```

### 4. Update Nameservers at whois.com

**Manual Steps**:

1. Log into whois.com account
2. Navigate to Domain Management
3. Select turafapp.com
4. Go to DNS/Nameserver settings
5. Select "Custom Nameservers"
6. Enter the 4 AWS nameservers:
   - `ns-xxxx.awsdns-xx.org`
   - `ns-xxxx.awsdns-xx.co.uk`
   - `ns-xxxx.awsdns-xx.com`
   - `ns-xxxx.awsdns-xx.net`
7. Save changes
8. Confirm the update

**Note**: DNS propagation can take 24-48 hours.

---

## Verification

### 1. Verify Hosted Zone Creation

```bash
# List hosted zones
aws route53 list-hosted-zones \
  --query "HostedZones[?Name=='turafapp.com.']" \
  --output table
```

### 2. Check Initial Records

```bash
# List resource record sets
aws route53 list-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID

# Should show NS and SOA records
```

### 3. Verify DNS Delegation (after propagation)

```bash
# Check nameservers via dig
dig NS turafapp.com +short

# Expected: 4 AWS nameservers
# ns-xxxx.awsdns-xx.org
# ns-xxxx.awsdns-xx.co.uk
# ns-xxxx.awsdns-xx.com
# ns-xxxx.awsdns-xx.net

# Check via nslookup
nslookup -type=NS turafapp.com

# Check SOA record
dig SOA turafapp.com +short
```

### 4. Test DNS Resolution

```bash
# Query AWS nameserver directly
dig @ns-xxxx.awsdns-xx.org turafapp.com SOA

# Should return valid SOA record
```

---

## DNS Propagation Timeline

**Immediate** (0-5 minutes):
- Hosted zone created in Route 53
- NS and SOA records available

**Short-term** (1-4 hours):
- Nameserver updates visible at registrar
- Some DNS resolvers pick up new nameservers

**Medium-term** (4-24 hours):
- Most DNS resolvers updated
- Global propagation in progress

**Complete** (24-48 hours):
- Full global DNS propagation
- All resolvers using new nameservers

---

## Troubleshooting

### Issue: Hosted zone already exists

**Cause**: Zone was previously created

**Solution**:
```bash
# List existing hosted zones
aws route53 list-hosted-zones

# Use existing zone ID
# Delete duplicate if needed (careful!)
aws route53 delete-hosted-zone --id <DUPLICATE_ZONE_ID>
```

### Issue: Cannot update nameservers at whois.com

**Cause**: Domain locked or insufficient permissions

**Solution**:
- Verify domain is unlocked
- Check account permissions
- Contact whois.com support
- Verify domain ownership

### Issue: DNS not propagating

**Cause**: TTL caching, propagation delay

**Solution**:
```bash
# Check current nameservers globally
dig NS turafapp.com @8.8.8.8
dig NS turafapp.com @1.1.1.1

# Wait for TTL to expire (usually 24-48 hours)
# Use online DNS propagation checker
```

### Issue: "AccessDeniedException"

**Cause**: Insufficient Route 53 permissions

**Solution**:
```bash
# Verify IAM permissions
aws iam get-user

# Ensure route53:* permissions
# Use root account credentials if needed
```

---

## Documentation

### Save Configuration

Create `infrastructure/dns-config.md`:

```markdown
# DNS Configuration

## Route 53 Hosted Zone

- **Zone ID**: Z1234567890ABC
- **Zone Name**: turafapp.com
- **Account**: root (072456928432)
- **Created**: 2024-01-01

## Nameservers

1. ns-1234.awsdns-12.org
2. ns-5678.awsdns-34.co.uk
3. ns-9012.awsdns-56.com
4. ns-3456.awsdns-78.net

## Registrar

- **Provider**: whois.com
- **Updated**: 2024-01-01
- **Status**: Active
```

---

## Checklist

- [ ] Route 53 hosted zone created
- [ ] Hosted zone ID saved: `Z1234567890ABC`
- [ ] 4 nameservers documented
- [ ] Nameservers updated at whois.com
- [ ] Update confirmed at registrar
- [ ] DNS delegation verified (after 24-48 hours)
- [ ] Configuration documented

---

## Next Steps

After DNS delegation is complete:
1. Proceed to task 018: Request ACM Certificates
2. Create DNS records for environments
3. Configure email forwarding

---

## References

- [Route 53 Hosted Zones](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/hosted-zones-working-with.html)
- [Migrating DNS to Route 53](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/MigratingDNS.html)
- specs/domain-dns-management.md
- INFRASTRUCTURE_PLAN.md (Phase 1.1)
