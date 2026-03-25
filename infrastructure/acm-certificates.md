# ACM Certificates

## Wildcard Certificate

- **ARN**: `arn:aws:acm:us-east-1:072456928432:certificate/c660ca8d-5584-4d6f-b75f-e5f10fc5a8ab`
- **Domain**: `*.turafapp.com`
- **Subject Alternative Names**: 
  - `*.turafapp.com`
  - `turafapp.com`
- **Region**: us-east-1
- **Account**: root (072456928432)
- **Status**: ISSUED ✅
- **Issued**: 2024-03-23
- **Expires**: 2026-10-06 (auto-renews 60 days before expiration)
- **Validation Method**: DNS
- **Key Algorithm**: RSA-2048
- **Signature Algorithm**: SHA256WITHRSA

## Validation Records

### DNS CNAME Record (Route 53)

- **Name**: `_fbf85825e8daf10df0aca23dd2320b07.turafapp.com.`
- **Type**: CNAME
- **Value**: `_d09092ac6f3e640919da44adddd8c78a.jkddzztszm.acm-validations.aws.`
- **TTL**: 300
- **Status**: Active (required for auto-renewal)

## Certificate Coverage

This wildcard certificate covers:

### Covered Domains
- `turafapp.com` (root domain - via SAN)
- `*.turafapp.com` (all first-level subdomains)
  - `api.turafapp.com`
  - `app.turafapp.com`
  - `ws.turafapp.com`
  - `dev.turafapp.com`
  - `qa.turafapp.com`
  - `prod.turafapp.com`
  - Any other first-level subdomain

### NOT Covered
- `*.*.turafapp.com` (nested subdomains like `api.dev.turafapp.com`)
  - Note: These are covered by the wildcard pattern `*.turafapp.com`

## Usage

This certificate can be used with:
- Application Load Balancers (ALB)
- CloudFront distributions
- API Gateway custom domains
- Elastic Beanstalk environments
- Any AWS service requiring SSL/TLS in us-east-1

## Auto-Renewal

- **ACM automatically renews** this certificate 60 days before expiration
- **No action required** as long as the DNS validation record remains in Route 53
- **Monitoring**: Set up CloudWatch alarms for certificate expiration (optional)

## Important Notes

1. **DNS Validation Record**: Do NOT delete the CNAME validation record from Route 53, as it's required for automatic renewal
2. **Region**: Certificate is in us-east-1 (required for CloudFront, can be used for ALB in any region)
3. **Cross-Account**: Certificates cannot be shared across AWS accounts; request separate certificates in dev/qa/prod accounts if needed
4. **Renewal**: ACM handles renewal automatically; no manual intervention required

## Verification Commands

```bash
# Check certificate status
aws acm describe-certificate \
  --certificate-arn "arn:aws:acm:us-east-1:072456928432:certificate/c660ca8d-5584-4d6f-b75f-e5f10fc5a8ab" \
  --region us-east-1 \
  --profile turaf-root

# List all certificates
aws acm list-certificates \
  --region us-east-1 \
  --profile turaf-root

# Verify DNS validation record
dig CNAME _fbf85825e8daf10df0aca23dd2320b07.turafapp.com +short
```

## Next Steps

1. Use this certificate ARN in:
   - ALB listener configurations (Task 016+)
   - CloudFront distributions (if applicable)
   - API Gateway custom domain configurations
2. Consider requesting environment-specific certificates in dev/qa/prod accounts (optional)
3. Set up certificate expiration monitoring (optional)
