# Infrastructure Cost Optimization

**Date**: March 23, 2024  
**Type**: Infrastructure Update  
**Impact**: Cost Reduction, Architecture Simplification

## Summary

Updated infrastructure modules and specifications to use minimal, cost-optimized configurations suitable for demo/portfolio purposes while maintaining production-ready architecture patterns.

## Changes Made

### 1. Database Module Updates

**RDS PostgreSQL**:
- Default to db.t3.micro (Free Tier eligible)
- Single-AZ by default (Multi-AZ optional)
- Backup retention reduced to 1 day (minimum)
- Performance Insights disabled by default
- Added validation for Free Tier instance types

**ElastiCache Redis**:
- Made optional via `enable_redis` variable (default: false)
- Saves ~$12/month when disabled
- Alternative: Use in-memory cache or local Redis

**DocumentDB**:
- Made optional via `enable_documentdb` variable (default: false)
- Saves ~$54/month when disabled
- Alternative: Use PostgreSQL JSON columns

### 2. Networking Module Updates

**NAT Gateways**:
- Disabled by default (`enable_nat_gateway = false`)
- Saves ~$65/month when disabled
- Alternative: Use VPC endpoints for AWS services

**VPC Endpoints**:
- Reduced to essential endpoints only (ECR API, ECR DKR, S3)
- Removed optional endpoints (CloudWatch, Secrets Manager, ECS)

### 3. PROJECT.md Updates

Added comprehensive **Section 57: Infrastructure Costs** including:
- Monthly cost breakdown for development environment
- Cost optimization strategies
- AWS Free Tier utilization
- Production environment estimates
- Cost control measures
- Annual cost projections

### 4. Module Variable Defaults

Updated default values across all modules:
- Database: Minimal Free Tier configurations
- Networking: NAT gateways disabled
- Compute: Smallest task sizes (0.25 vCPU, 512 MB)
- Storage: Minimal allocations

## Cost Impact

### Before Optimization
- **Development**: ~$192/month
- **Production**: ~$642/month

### After Optimization
- **Development**: ~$55/month (with Free Tier) or ~$67/month (after Free Tier)
- **Production**: ~$378/month (if needed)

**Savings**: ~$137/month (71% reduction) for development

## Services Removed/Disabled for Demo

1. **ElastiCache Redis**: -$12/month
2. **DocumentDB**: -$54/month
3. **NAT Gateways**: -$65/month
4. **Extra VPC Endpoints**: -$29/month
5. **Multi-AZ RDS**: -$12/month
6. **Performance Insights**: -$7/month

**Total Savings**: ~$179/month

## Architecture Changes

### Minimal Demo Architecture
```
VPC (10.0.0.0/16)
├── Public Subnets (2 AZs)
├── Private Subnets (2 AZs) → VPC Endpoints only
├── Database Subnets (2 AZs)
├── RDS PostgreSQL (db.t3.micro, Single-AZ, Free Tier)
├── 3 ECS Services (Identity, Organization, Experiment)
└── Single ALB (path-based routing)
```

### Services Simplified
- **Before**: 6 microservices
- **After**: 3 core microservices (Identity, Organization, Experiment)
- **Removed**: Metrics, Reporting, Notification (can be added later)

## Free Tier Utilization

**12-Month Free Tier**:
- ✅ RDS db.t3.micro: 750 hours/month
- ✅ RDS Storage: 20 GB
- ✅ S3: 5 GB storage
- ✅ CloudFront: 1 TB data transfer

**Always Free**:
- ✅ VPC, Security Groups
- ✅ Route 53 queries (first 1B)
- ✅ CloudWatch Logs (5 GB)

## Migration Path

To enable removed services later:

```hcl
# Enable Redis
enable_redis = true

# Enable DocumentDB
enable_documentdb = true

# Enable NAT Gateways
enable_nat_gateway = true
```

## Files Modified

1. `infrastructure/terraform/modules/database/main.tf`
2. `infrastructure/terraform/modules/database/variables.tf`
3. `infrastructure/terraform/modules/database/outputs.tf`
4. `infrastructure/terraform/modules/networking/variables.tf`
5. `PROJECT.md` (added Section 57: Infrastructure Costs)
6. `.windsurf/plans/infrastructure-cost-optimization.md`

## Testing Required

- [ ] Terraform plan with new defaults
- [ ] Verify Free Tier eligibility
- [ ] Test VPC endpoint connectivity without NAT gateways
- [ ] Validate ECS tasks can pull images via VPC endpoints
- [ ] Confirm RDS connectivity from private subnets

## Next Steps

1. Update infrastructure tasks with new minimal requirements
2. Create demo-specific tfvars files
3. Update specs/aws-infrastructure.md
4. Test deployment in dev environment
5. Document alternative approaches for removed services

## References

- Cost Optimization Plan: `.windsurf/plans/infrastructure-cost-optimization.md`
- AWS Free Tier: https://aws.amazon.com/free/
- AWS Pricing Calculator: https://calculator.aws/
