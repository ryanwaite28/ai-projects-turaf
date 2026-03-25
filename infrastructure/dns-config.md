# DNS Configuration

## Route 53 Hosted Zone

- **Zone ID**: Z055341020TQZLU2CKWOE
- **Zone Name**: turafapp.com
- **Account**: root (072456928432)
- **Created**: 2024-03-23
- **Status**: Active
- **Type**: Public Hosted Zone

## AWS Nameservers

**IMPORTANT**: These nameservers must be configured at whois.com registrar

1. `ns-1965.awsdns-53.co.uk`
2. `ns-567.awsdns-06.net`
3. `ns-1223.awsdns-24.org`
4. `ns-220.awsdns-27.com`

## Registrar Configuration

- **Provider**: whois.com
- **Domain**: turafapp.com
- **Status**: ✅ Nameservers updated and verified
- **Updated**: 2024-03-23
- **Delegation Status**: Active

## Manual Steps Required

1. Log into whois.com account
2. Navigate to Domain Management
3. Select turafapp.com
4. Go to DNS/Nameserver settings
5. Select "Custom Nameservers"
6. Enter the 4 AWS nameservers listed above
7. Save changes
8. Confirm the update

## DNS Propagation

- **Expected Time**: 24-48 hours for full global propagation
- **Initial Visibility**: 1-4 hours
- **Verification**: Use `dig NS turafapp.com +short` to check

## Verification Commands

```bash
# Check nameservers
dig NS turafapp.com +short

# Check SOA record
dig SOA turafapp.com +short

# Query AWS nameserver directly
dig @ns-1965.awsdns-53.co.uk turafapp.com SOA

# Check from different DNS resolvers
dig NS turafapp.com @8.8.8.8
dig NS turafapp.com @1.1.1.1
```

## Next Steps

After DNS delegation is complete:
1. Verify DNS propagation
2. Request ACM certificates (Task 005)
3. Configure email forwarding (Task 006)
4. Set up SES domain verification (Task 010)
