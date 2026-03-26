# Infrastructure Documentation

**Last Updated**: March 25, 2026

This directory contains all infrastructure-related documentation for the Turaf platform.

---

## Quick Links

### Planning
- [Infrastructure Plan](planning/INFRASTRUCTURE_PLAN.md) - Complete infrastructure architecture and design
- [Infrastructure Costs](planning/INFRASTRUCTURE_COSTS.md) - Cost analysis and optimization

### Deployment
- [Deployment Guide](deployment/DEPLOYMENT_GUIDE.md) - How to deploy infrastructure
- [Deployment Summary](deployment/DEPLOYMENT_SUMMARY.md) - Recent deployment status
- [DEV Infrastructure Status](deployment/DEV_INFRASTRUCTURE_STATUS.md) - Current DEV environment state
- [Deployed Infrastructure](deployment/DEPLOYED_INFRASTRUCTURE.md) - Inventory of deployed resources

### CI/CD
- [CI/CD Infrastructure Guide](cicd/CICD_INFRASTRUCTURE_GUIDE.md) - CI/CD prerequisites and setup
- [CI/CD Prerequisites Status](cicd/CICD_PREREQUISITES_STATUS.md) - Readiness checklist

### Architecture
- [Compute Infrastructure Analysis](architecture/COMPUTE_INFRASTRUCTURE_ANALYSIS.md) - ECS/ALB architecture
- [Infrastructure Restructure Summary](architecture/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md) - Recent refactoring details

---

## Related Documentation

- **Specifications**: See `/specs/aws-infrastructure.md` and `/specs/terraform-structure.md`
- **Tasks**: See `/tasks/infrastructure/`
- **Plans**: See `/.windsurf/plans/completed/infrastructure-restructure/`

---

## Documentation Organization

```
infrastructure/docs/
├── README.md                           # This file
├── planning/                           # Architecture and cost planning
│   ├── INFRASTRUCTURE_PLAN.md
│   └── INFRASTRUCTURE_COSTS.md
├── deployment/                         # Deployment guides and status
│   ├── DEPLOYMENT_GUIDE.md
│   ├── DEPLOYMENT_SUMMARY.md
│   ├── DEV_INFRASTRUCTURE_STATUS.md
│   └── DEPLOYED_INFRASTRUCTURE.md
├── cicd/                              # CI/CD infrastructure
│   ├── CICD_INFRASTRUCTURE_GUIDE.md
│   └── CICD_PREREQUISITES_STATUS.md
└── architecture/                       # Architecture analysis
    ├── COMPUTE_INFRASTRUCTURE_ANALYSIS.md
    └── INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md
```

---

## Infrastructure Overview

### Multi-Account Architecture
- **DEV**: 801651112319
- **QA**: 965932217544
- **PROD**: 811783768245
- **Ops**: 146072879609

### Key Components
- **Networking**: VPC, subnets, NAT gateways, security groups
- **Compute**: ECS Cluster (Fargate), Application Load Balancer
- **Database**: RDS PostgreSQL, ElastiCache Redis
- **Storage**: S3 buckets
- **Messaging**: EventBridge, SQS
- **Serverless**: Lambda functions
- **Monitoring**: CloudWatch, X-Ray

### Infrastructure Management
- **Shared Infrastructure**: Managed by Terraform in `/infrastructure/terraform/`
- **Service-Specific Resources**: Managed by CI/CD pipelines per service
- **State Management**: S3 backend with DynamoDB locking

---

## Getting Started

1. **Planning**: Start with [Infrastructure Plan](planning/INFRASTRUCTURE_PLAN.md)
2. **Deployment**: Follow [Deployment Guide](deployment/DEPLOYMENT_GUIDE.md)
3. **CI/CD Setup**: See [CI/CD Infrastructure Guide](cicd/CICD_INFRASTRUCTURE_GUIDE.md)
4. **Current Status**: Check [DEV Infrastructure Status](deployment/DEV_INFRASTRUCTURE_STATUS.md)

---

**For the complete system design, see [PROJECT.md](/PROJECT.md)**
