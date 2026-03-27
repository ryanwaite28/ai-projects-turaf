# Changelog: Multi-Account ACM Certificate Strategy

**Date**: 2026-03-26  
**Type**: Infrastructure Documentation Update  
**Impact**: Infrastructure Plan, Specifications, Tasks

---

## Summary

Updated infrastructure documentation to reflect **multi-account ACM certificate provisioning strategy**. Each AWS account (DEV, QA, PROD) now requires its own ACM certificate for ALB HTTPS listeners, as certificates cannot be shared across accounts.

---

## Changes Made

### 1. Infrastructure Plan (`infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md`)

**Section Updated**: Phase 1.2 - Request ACM Certificates

**Changes**:
- Added multi-account certificate provisioning strategy
- Documented certificate request process for each account:
  - Root Account (072456928432)
  - DEV Account (801651112319)
  - QA Account (965932217544)
  - PROD Account (811783768245)
- Added AWS SSO authentication steps for each account
- Clarified DNS validation process using shared Route 53 hosted zone
- Each certificate has unique validation CNAME record

**Key Addition**:
```bash
# Per-account certificate request pattern
aws sso login --profile turaf-<env>
aws acm request-certificate \
  --domain-name "*.turafapp.com" \
  --subject-alternative-names "turafapp.com" \
  --validation-method DNS \
  --region us-east-1 \
  --profile turaf-<env>
```

### 2. Domain DNS Management Spec (`specs/domain-dns-management.md`)

**Section Updated**: SSL/TLS Certificate Management (ACM)

**Changes**:
- Replaced single wildcard certificate approach with multi-account strategy
- Added certificate provisioning details per account:
  - Root: ✅ Issued (ARN documented)
  - DEV: ⏳ Pending provisioning
  - QA: ⏳ Pending provisioning
  - PROD: ⏳ Pending provisioning
- Documented usage for each certificate (CloudFront vs ALB)
- Added shared Route 53 DNS validation process
- Clarified that each certificate has unique validation CNAME

**Architecture Note Added**:
> Each AWS account requires its own ACM certificate because certificates cannot be shared across accounts. All certificates use the same wildcard domain (`*.turafapp.com`) but are validated via separate CNAME records in the shared Route 53 hosted zone.

### 3. ACM Certificate Task (`tasks/infrastructure/005-request-acm-certificates.md`)

**Acceptance Criteria Updated**:
- [x] Root account wildcard certificate requested
- [x] Root account certificate validated and issued
- [x] Root account certificate ARN documented
- [ ] DEV account certificate requested (801651112319)
- [ ] QA account certificate requested (965932217544)
- [ ] PROD account certificate requested (811783768245)
- [ ] All environment certificates validated and issued
- [ ] All certificate ARNs documented

**Implementation Steps Added**:
- Section 5: Request DEV Account Certificate
- Section 6: Request QA Account Certificate
- Section 7: Request PROD Account Certificate
- Section 8: Add Validation Records for All Certificates
- Section 9: Wait for All Certificates to Validate

**Key Improvements**:
- AWS SSO authentication commands for each account
- Separate certificate ARN variables (DEV_CERT_ARN, QA_CERT_ARN, PROD_CERT_ARN)
- Batch validation record creation for all accounts
- Parallel certificate validation waiting

---

## Rationale

### Why Multi-Account Certificates?

1. **AWS Limitation**: ACM certificates cannot be shared across AWS accounts
2. **ALB Requirement**: Each environment's ALB requires a certificate in the same account
3. **Security Isolation**: Maintains account boundary separation
4. **Compliance**: Follows AWS multi-account best practices

### Why Same Wildcard Domain?

- All environments use `*.turafapp.com` for consistency
- Simplifies DNS management (single hosted zone)
- Each certificate validated independently via unique CNAME records
- No nested subdomains needed (e.g., `*.dev.turafapp.com`)

---

## Implementation Status

| Account | Certificate Status | ARN Documented |
|---------|-------------------|----------------|
| Root (072456928432) | ✅ Issued | ✅ Yes |
| DEV (801651112319) | ⏳ Pending | ❌ No |
| QA (965932217544) | ⏳ Pending | ❌ No |
| PROD (811783768245) | ⏳ Pending | ❌ No |

---

## Next Steps

1. **Authenticate to each account** via AWS SSO:
   ```bash
   aws sso login --profile turaf-dev
   aws sso login --profile turaf-qa
   aws sso login --profile turaf-prod
   ```

2. **Request certificates** in DEV, QA, and PROD accounts following Task 005

3. **Add validation CNAME records** to Route 53 (root account)

4. **Wait for validation** (5-30 minutes per certificate)

5. **Document certificate ARNs** in:
   - `infrastructure/acm-certificates.md`
   - `specs/domain-dns-management.md`
   - Task 005 implementation results

6. **Update Terraform modules** to use environment-specific certificate ARNs:
   ```hcl
   # terraform/environments/dev/main.tf
   certificate_arn = "arn:aws:acm:us-east-1:801651112319:certificate/<DEV_CERT_ID>"
   ```

---

## Related Documentation

- **Infrastructure Plan**: `infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md` (Phase 1.2)
- **Spec**: `specs/domain-dns-management.md` (ACM section)
- **Task**: `tasks/infrastructure/005-request-acm-certificates.md`
- **Certificate Config**: `infrastructure/acm-certificates.md`
- **AWS Accounts**: `AWS_ACCOUNTS.md`

---

## References

- [AWS ACM Documentation](https://docs.aws.amazon.com/acm/)
- [ACM Cross-Account Limitations](https://docs.aws.amazon.com/acm/latest/userguide/acm-limits.html)
- [DNS Validation](https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html)
- [Multi-Account Best Practices](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_best-practices.html)
