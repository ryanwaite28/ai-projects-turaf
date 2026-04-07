# Changelog: Infrastructure Updates - ACM, ALB, and Frontend Deployment

**Date**: 2026-03-26  
**Type**: Infrastructure & CI/CD Documentation  
**Impact**: Specifications, Tasks, Workflows

---

## Summary

Comprehensive infrastructure documentation updates covering:
1. **ACM Certificates as Shared Resources** - Documented multi-account certificate strategy
2. **ALB ACM Configuration** - Updated ALB specifications with SSL/TLS configuration
3. **Frontend Deployment Architecture** - Created S3 + CloudFront + ACM deployment spec
4. **Frontend CI/CD Workflows** - Created deployment pipelines for DEV/QA/PROD

---

## Changes Made

### 1. ACM Certificates as Shared Infrastructure Resource

**File**: `specs/aws-infrastructure.md`

**Section Added**: "Shared Infrastructure Resources → ACM SSL/TLS Certificates"

**Changes**:
- Added comprehensive ACM certificate inventory table with all account ARNs
- Documented multi-account strategy (certificates cannot be shared across accounts)
- Provided Terraform reference examples for certificate data sources
- Listed certificate properties (domain, SAN, region, validation, auto-renewal)
- Cross-referenced `infrastructure/acm-certificates.md` for complete details

**Certificate Inventory**:
| Account | ARN | Usage |
|---------|-----|-------|
| Root (072456928432) | `...c660ca8d-5584-4d6f-b75f-e5f10fc5a8ab` | CloudFront distributions |
| DEV (801651112319) | `...8b83b688-7458-4627-9fd4-ff3b2801bf70` | DEV ALB HTTPS listeners |
| QA (965932217544) | `...906b4a44-11e3-4ee7-b10d-9f715ffc0ee6` | QA ALB HTTPS listeners |
| PROD (811783768245) | `...779b5c14-8fc0-44fe-80b4-090bdee1ef62` | PROD ALB HTTPS listeners |

### 2. Application Load Balancer ACM Configuration

**File**: `specs/aws-infrastructure.md`

**Section Updated**: "Infrastructure Components → Application Load Balancer"

**Changes**:
- Expanded ALB configuration with detailed SSL/TLS settings
- Added environment-specific certificate ARN mappings
- Documented SSL policy: `ELBSecurityPolicy-TLS-1-2-2017-01`
- Added HTTP to HTTPS redirect listener configuration
- Provided complete Terraform examples for HTTPS and HTTP listeners
- Added Route 53 DNS configuration (api.{env}.turafapp.com)

**Key Additions**:
```hcl
# Data source for environment-specific ACM certificate
data "aws_acm_certificate" "main" {
  domain   = "*.turafapp.com"
  statuses = ["ISSUED"]
  most_recent = true
}

# HTTPS Listener with ACM certificate
resource "aws_lb_listener" "https" {
  certificate_arn = data.aws_acm_certificate.main.arn
  ssl_policy      = "ELBSecurityPolicy-TLS-1-2-2017-01"
  # ...
}

# HTTP to HTTPS redirect
resource "aws_lb_listener" "http" {
  default_action {
    type = "redirect"
    redirect {
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}
```

### 3. Frontend Deployment Specification

**File Created**: `specs/frontend-deployment.md`

**Content**:
- **S3 Bucket Configuration**: Per-environment buckets with versioning, encryption, public access blocking
- **CloudFront Distribution**: CDN configuration with OAI, caching, compression, custom error responses
- **ACM Certificate Integration**: Root account certificate for CloudFront (us-east-1 requirement)
- **Route 53 DNS**: Alias records for app.{env}.turafapp.com
- **Environment Configuration**: TypeScript environment files for DEV/QA/PROD
- **Security**: CSP headers, CloudFront Functions for security headers
- **Monitoring**: CloudFront access logs, CloudWatch metrics
- **Cost Optimization**: Estimated costs and optimization strategies
- **Rollback Strategy**: S3 versioning-based rollback procedure

**Architecture**:
```
User → CloudFront (app.{env}.turafapp.com) 
     → ACM Certificate (*.turafapp.com)
     → S3 Bucket (turaf-frontend-{env})
     → Static Files (index.html, *.js, *.css)
```

**Key Features**:
- CloudFront OAI for S3 bucket access (no public access)
- Separate cache policies for static assets (1 year) vs index.html (no-cache)
- Custom error responses for Angular routing (403/404 → index.html)
- TLS 1.2+ enforcement
- Gzip/Brotli compression enabled

### 4. Infrastructure Task: ALB ACM Configuration

**File Created**: `tasks/infrastructure/029-configure-alb-acm-certificates.md`

**Content**:
- Complete Terraform implementation for ALB with ACM certificates
- HTTPS listener (port 443) configuration
- HTTP to HTTPS redirect listener (port 80)
- Route 53 Alias records for API and WebSocket subdomains
- Verification procedures for SSL/TLS configuration
- Troubleshooting guide for common certificate issues
- Rollout plan for DEV → QA → PROD environments

**Acceptance Criteria**:
- ALB HTTPS listener configured with ACM certificate
- HTTP to HTTPS redirect working
- SSL policy set to TLS 1.2+
- Certificate ARN referenced via Terraform data source
- Route 53 DNS records created

### 5. CI/CD Task: Frontend Deployment Workflows

**File Created**: `tasks/cicd/009-setup-frontend-deployment-workflow.md`

**Content**:
- GitHub Actions workflows for DEV, QA, and PROD environments
- Angular build configurations per environment
- S3 sync with proper cache headers (static assets: 1 year, index.html: no-cache)
- CloudFront cache invalidation
- Deployment verification and smoke tests
- Production-specific features: bundle analysis, backup, monitoring
- Rollback procedures using S3 versioning

**Workflow Features**:

**DEV Workflow** (`frontend-dev.yml`):
- Trigger: Push to `develop` branch
- Steps: Lint → Test → Build → S3 Sync → CloudFront Invalidation → Verify
- Fast deployment for rapid iteration

**QA Workflow** (`frontend-qa.yml`):
- Trigger: Push to `release/*` branches
- Steps: Lint → Test → E2E Tests → Build → Deploy → Verify
- Includes E2E testing before deployment

**PROD Workflow** (`frontend-prod.yml`):
- Trigger: Push to `main` branch
- Steps: Full test suite → Build → Backup → Deploy → Smoke Tests → Monitoring
- Production safeguards: backup, extended verification, CloudWatch metrics

**Angular Build Scripts**:
```json
{
  "scripts": {
    "build:dev": "ng build --configuration=dev",
    "build:qa": "ng build --configuration=qa",
    "build:production": "ng build --configuration=production"
  }
}
```

---

## Rationale

### Why Document ACM as Shared Resource?

1. **Visibility**: Centralize certificate inventory across all accounts
2. **Reference**: Provide single source of truth for certificate ARNs
3. **Terraform Integration**: Show how to reference certificates in IaC
4. **Multi-Account Clarity**: Explain why each account needs its own certificate

### Why Update ALB Specification?

1. **Security**: HTTPS-only communication for all API traffic
2. **Best Practices**: TLS 1.2+ enforcement, HTTP to HTTPS redirect
3. **Implementation Guidance**: Complete Terraform examples for developers
4. **DNS Integration**: Document Route 53 alias records for ALB

### Why Create Frontend Deployment Spec?

1. **Architecture Documentation**: Define S3 + CloudFront deployment pattern
2. **Performance**: CDN caching strategy for optimal load times
3. **Security**: CloudFront OAI, CSP headers, TLS enforcement
4. **Cost Optimization**: Documented caching strategies to minimize costs
5. **Rollback Capability**: S3 versioning for quick rollbacks

### Why Create Frontend CI/CD Workflows?

1. **Automation**: Eliminate manual deployment steps
2. **Environment Parity**: Consistent deployment process across DEV/QA/PROD
3. **Quality Gates**: Automated testing before deployment
4. **Verification**: Post-deployment smoke tests
5. **Monitoring**: CloudWatch metrics integration for PROD

---

## Implementation Status

### Completed
- ✅ ACM certificates documented as shared infrastructure resource
- ✅ ALB specification updated with ACM configuration
- ✅ Frontend deployment specification created
- ✅ Infrastructure task created (029-configure-alb-acm-certificates.md)
- ✅ CI/CD task created (009-setup-frontend-deployment-workflow.md)

### Pending Implementation
- ⏳ Terraform modules for ALB ACM configuration (Task 029)
- ⏳ Terraform modules for S3 + CloudFront (new task needed)
- ⏳ GitHub Actions workflows for frontend deployment (Task 009)
- ⏳ Angular environment configuration files
- ⏳ Frontend build script updates

---

## Next Steps

### 1. Implement ALB ACM Configuration (Task 029)
```bash
cd infrastructure/terraform/modules/compute
# Update alb.tf with ACM certificate data source
# Add HTTPS and HTTP listeners
# Create route53.tf for DNS records
# Deploy to DEV environment
```

### 2. Create Frontend Infrastructure Module
```bash
# Create new Terraform module: infrastructure/terraform/modules/frontend
# Implement S3 bucket, CloudFront distribution, Route 53 records
# Deploy to DEV environment
```

### 3. Implement Frontend CI/CD Workflows (Task 009)
```bash
# Create .github/workflows/frontend-dev.yml
# Create .github/workflows/frontend-qa.yml
# Create .github/workflows/frontend-prod.yml
# Update frontend/angular.json with build configurations
# Update frontend/package.json with build scripts
```

### 4. Create Environment Configuration Files
```bash
# Create frontend/src/environments/environment.dev.ts
# Create frontend/src/environments/environment.qa.ts
# Create frontend/src/environments/environment.prod.ts
```

### 5. Test Deployment Pipeline
```bash
# Deploy to DEV environment
# Verify HTTPS access via CloudFront
# Test Angular routing
# Verify API connectivity
```

---

## Related Documentation

**Updated Files**:
- `specs/aws-infrastructure.md` - Added ACM shared resources, updated ALB spec
- `infrastructure/acm-certificates.md` - Referenced in aws-infrastructure.md
- `specs/domain-dns-management.md` - Already contains ACM certificate details

**Created Files**:
- `specs/frontend-deployment.md` - Complete frontend deployment architecture
- `tasks/infrastructure/029-configure-alb-acm-certificates.md` - ALB ACM implementation task
- `tasks/cicd/009-setup-frontend-deployment-workflow.md` - Frontend CI/CD workflows

**Related Existing Files**:
- `specs/angular-frontend.md` - Frontend application specification
- `specs/ci-cd-pipelines.md` - CI/CD architecture overview
- `tasks/infrastructure/005-request-acm-certificates.md` - ACM certificate provisioning (completed)
- `tasks/infrastructure/019-create-compute-modules.md` - Compute module creation

---

## References

- [AWS ACM Documentation](https://docs.aws.amazon.com/acm/)
- [AWS CloudFront Documentation](https://docs.aws.amazon.com/cloudfront/)
- [AWS S3 Static Website Hosting](https://docs.aws.amazon.com/AmazonS3/latest/userguide/WebsiteHosting.html)
- [ALB HTTPS Listeners](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-https-listener.html)
- [Angular Deployment](https://angular.io/guide/deployment)
