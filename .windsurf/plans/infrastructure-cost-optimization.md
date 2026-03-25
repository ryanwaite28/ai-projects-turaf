# Infrastructure Cost Optimization for Demo

**Date**: 2024-03-23  
**Purpose**: Update infrastructure to use cheapest options for demo/portfolio purposes

## Current State

Infrastructure modules created with production-ready configurations:
- RDS PostgreSQL (db.t3.micro, Multi-AZ optional)
- ElastiCache Redis (cache.t3.micro)
- DocumentDB (db.t3.medium)
- ECS Fargate (0.5-1 vCPU per service)
- NAT Gateways (one per AZ)

**Current Dev Cost**: ~$192/month
**Current Prod Cost**: ~$642/month

## Target State: Minimal Demo Configuration

### Goals
1. Reduce infrastructure costs to minimum viable for demo
2. Use AWS Free Tier where possible
3. Single environment (dev) only for initial demo
4. Eliminate unnecessary high-cost services
5. Update all specs and tasks to reflect minimal architecture

### Proposed Changes

#### 1. Database Layer (Current: ~$84/month → Target: ~$25/month)

**RDS PostgreSQL**:
- ✅ Keep: db.t3.micro (Free Tier eligible: 750 hours/month)
- ✅ Keep: Single-AZ only
- ✅ Keep: 20 GB storage (Free Tier: 20 GB)
- ❌ Remove: Performance Insights
- ❌ Remove: Multi-AZ
- **Cost**: ~$0/month (Free Tier) or ~$12/month after

**ElastiCache Redis**:
- ❌ Remove: Use Redis in Docker for local dev
- ❌ Remove: Not essential for demo
- **Alternative**: Redis container in ECS or local only
- **Cost**: $0/month (removed)

**DocumentDB**:
- ❌ Remove: Too expensive for demo ($54/month minimum)
- **Alternative**: MongoDB in Docker or use PostgreSQL JSON
- **Cost**: $0/month (removed)

**New Database Cost**: ~$12/month (after Free Tier)

#### 2. Compute Layer (Current: ~$50/month → Target: ~$15/month)

**ECS Fargate**:
- Reduce to 2-3 services only (Identity, Organization, Experiment)
- Use smallest task sizes: 0.25 vCPU, 512 MB
- Single task per service (no HA for demo)
- Use Fargate Spot for 70% savings
- **Cost**: ~$15/month

**Remove Services for Demo**:
- Metrics Service (can be added later)
- Reporting Service (can be added later)
- Notification Service (use SES directly)

#### 3. Networking (Current: ~$65/month → Target: ~$0/month)

**NAT Gateways**:
- ❌ Remove: Use VPC endpoints only
- ❌ Remove: Not needed if services use VPC endpoints
- **Alternative**: Public subnets with security groups
- **Cost**: $0/month (removed)

**VPC Endpoints**:
- Keep only essential: S3 (free), ECR API, ECR DKR
- Remove: CloudWatch, Secrets Manager, ECS endpoints
- **Cost**: ~$14/month (2 interface endpoints)

#### 4. Load Balancing (Current: ~$16/month → Target: ~$16/month)

**ALB**:
- Keep single ALB for all services
- Use path-based routing
- **Cost**: ~$16/month (unavoidable)

#### 5. Storage (Current: ~$5/month → Target: ~$2/month)

**S3**:
- Single bucket for all environments
- Lifecycle policies for cleanup
- **Cost**: ~$2/month

**ECR**:
- Keep for container images
- Lifecycle policy: keep last 3 images
- **Cost**: ~$1/month

#### 6. Secrets & Encryption (Current: ~$6/month → Target: ~$2/month)

**Secrets Manager**:
- Reduce to 5 secrets (down from 10)
- Use SSM Parameter Store for non-sensitive config
- **Cost**: ~$2/month

**KMS**:
- Single KMS key for all encryption
- **Cost**: ~$1/month

## Updated Cost Breakdown

### Development Environment (Single Environment for Demo)

| Service | Configuration | Monthly Cost |
|---------|--------------|--------------|
| **RDS PostgreSQL** | db.t3.micro, 20GB, Single-AZ | $0 (Free Tier) or $12 |
| **ECS Fargate** | 3 services, 0.25 vCPU, 512MB, Spot | $15 |
| **ALB** | Single ALB, path-based routing | $16 |
| **VPC Endpoints** | ECR API, ECR DKR (2 endpoints) | $14 |
| **S3** | Single bucket, minimal storage | $2 |
| **ECR** | Container images, lifecycle policy | $1 |
| **Secrets Manager** | 5 secrets | $2 |
| **KMS** | 1 key | $1 |
| **Route 53** | Hosted zone | $0.50 |
| **CloudFront** | Frontend distribution | $1 |

**Total Monthly Cost**: ~$52.50/month (or ~$40.50 with Free Tier)

### Services Removed for Demo
- ElastiCache Redis: -$12/month
- DocumentDB: -$54/month
- NAT Gateways: -$65/month
- Extra VPC Endpoints: -$29/month
- Metrics Service: -$5/month
- Multi-AZ RDS: -$12/month

**Total Savings**: ~$177/month (77% reduction)

## Implementation Plan

1. ✅ Update database module variables defaults
2. ✅ Update networking module to remove NAT gateways option
3. ✅ Update compute module for minimal Fargate config
4. ✅ Update infrastructure specs with new architecture
5. ✅ Update all task files with new requirements
6. ✅ Add cost section to PROJECT.md
7. ✅ Create environment-specific tfvars for demo

## Architecture Changes

### Before (Production-Ready)
```
VPC
├── Public Subnets (2 AZs)
├── Private Subnets (2 AZs) → NAT Gateways
├── Database Subnets (2 AZs)
├── RDS Multi-AZ
├── Redis Cluster (2 nodes)
├── DocumentDB Cluster (2 instances)
└── 6 Microservices (2 tasks each)
```

### After (Demo-Optimized)
```
VPC
├── Public Subnets (2 AZs)
├── Private Subnets (2 AZs) → VPC Endpoints only
├── Database Subnets (2 AZs)
├── RDS Single-AZ (Free Tier)
└── 3 Microservices (1 task each, Fargate Spot)
```

## Free Tier Utilization

**AWS Free Tier (12 months)**:
- RDS: 750 hours/month db.t3.micro
- RDS Storage: 20 GB
- ECS Fargate: Not in Free Tier
- ALB: Not in Free Tier
- S3: 5 GB storage, 20,000 GET requests
- CloudFront: 1 TB data transfer out

**Always Free**:
- VPC: Free
- Security Groups: Free
- Route 53 Queries: First 1 billion/month
- CloudWatch Logs: 5 GB ingestion

## Next Steps

1. Update module default variables
2. Update specs/aws-infrastructure.md
3. Update PROJECT.md with cost section
4. Update task requirements
5. Create demo-optimized tfvars files
