# Infrastructure Cost Optimization Guide

**Last Updated**: March 23, 2024  
**Purpose**: Cost-optimized AWS infrastructure for demo/portfolio purposes

---

## 💰 Cost Summary

### Demo Environment (Recommended)
**Monthly Cost**: ~$54.50/month (with AWS Free Tier) or ~$66.50/month (after Free Tier)

### Production Environment (Optional)
**Monthly Cost**: ~$378/month

### Annual Costs
- **Demo**: ~$660/year (with Free Tier) or ~$798/year (after)
- **Production**: ~$4,536/year

---

## 📊 Detailed Cost Breakdown

### Demo Environment

| Service | Configuration | Monthly Cost | Notes |
|---------|--------------|--------------|-------|
| **RDS PostgreSQL** | db.t3.micro, 20GB, Single-AZ | $0 or $12 | Free Tier for 12 months |
| **ECS Fargate** | 3 services, 0.25 vCPU, 512MB, Spot | $15 | 70% savings with Spot |
| **Application Load Balancer** | Single ALB | $16 | Required for routing |
| **VPC Endpoints** | ECR API, ECR DKR | $14 | Replaces NAT Gateway |
| **S3** | Single bucket, minimal storage | $2 | Lifecycle policies |
| **ECR** | Container images | $1 | Lifecycle: keep 3 images |
| **Secrets Manager** | 5 secrets | $2 | Reduced from 10 |
| **KMS** | 1 key | $1 | Single key for all |
| **Route 53** | Hosted zone | $0.50 | DNS for turafapp.com |
| **CloudFront** | Frontend distribution | $1 | Free Tier eligible |
| **CloudWatch Logs** | Log storage | $2 | 7-day retention |
| **TOTAL** | | **$54.50** | **With Free Tier** |

---

## 🚫 Services Disabled for Cost Savings

| Service | Monthly Savings | Alternative Solution |
|---------|----------------|---------------------|
| **ElastiCache Redis** | $12 | Use in-memory cache or local Redis container |
| **DocumentDB** | $54 | Use PostgreSQL JSON columns for document storage |
| **NAT Gateways** | $65 | Use VPC endpoints for AWS service access |
| **Multi-AZ RDS** | $12 | Single-AZ sufficient for demo |
| **Performance Insights** | $7 | Use CloudWatch metrics instead |
| **Extra VPC Endpoints** | $29 | Keep only essential (ECR, S3) |
| **Metrics Service** | $5 | Use CloudWatch directly |
| **Reporting Service** | $5 | Generate reports on-demand |
| **Notification Service** | $5 | Use SES directly from services |
| **TOTAL SAVINGS** | **$194/month** | **77% cost reduction** |

---

## ✅ AWS Free Tier Utilization

### 12-Month Free Tier
- ✅ **RDS**: 750 hours/month db.t3.micro (covers 24/7 usage)
- ✅ **RDS Storage**: 20 GB
- ✅ **RDS Backups**: 20 GB
- ✅ **S3**: 5 GB storage, 20,000 GET requests, 2,000 PUT requests
- ✅ **CloudFront**: 1 TB data transfer out, 10M HTTP/HTTPS requests
- ✅ **CloudWatch Logs**: 5 GB ingestion

### Always Free
- ✅ **VPC**: Free
- ✅ **Security Groups**: Free
- ✅ **Route 53 Queries**: First 1 billion/month
- ✅ **Lambda**: 1M requests/month, 400,000 GB-seconds
- ✅ **CloudWatch Metrics**: 10 custom metrics, 10 alarms
- ✅ **S3 Gateway Endpoint**: Free

---

## 🎯 Architecture Comparison

### Demo Architecture (Cost-Optimized)
```
VPC (10.0.0.0/16)
├── Public Subnets (2 AZs)
│   └── Application Load Balancer
├── Private Subnets (2 AZs)
│   ├── ECS Tasks (3 services via VPC Endpoints)
│   │   ├── Identity Service (0.25 vCPU, 512 MB)
│   │   ├── Organization Service (0.25 vCPU, 512 MB)
│   │   └── Experiment Service (0.25 vCPU, 512 MB)
│   └── VPC Endpoints (ECR API, ECR DKR, S3)
└── Database Subnets (2 AZs)
    └── RDS PostgreSQL (db.t3.micro, Single-AZ)

Cost: ~$55/month
```

### Production Architecture (Full-Featured)
```
VPC (10.0.0.0/16)
├── Public Subnets (2 AZs)
│   ├── Application Load Balancer
│   └── NAT Gateways (2)
├── Private Subnets (2 AZs)
│   ├── ECS Tasks (6 services, 2 tasks each)
│   │   ├── Identity Service (0.5 vCPU, 1 GB)
│   │   ├── Organization Service (0.5 vCPU, 1 GB)
│   │   ├── Experiment Service (1 vCPU, 2 GB)
│   │   ├── Metrics Service (1 vCPU, 2 GB)
│   │   ├── Reporting Service (0.5 vCPU, 1 GB)
│   │   └── Notification Service (0.25 vCPU, 512 MB)
│   ├── ElastiCache Redis (2 nodes)
│   └── VPC Endpoints (6)
└── Database Subnets (2 AZs)
    ├── RDS PostgreSQL (db.t3.small, Multi-AZ)
    └── DocumentDB (2 instances)

Cost: ~$378/month
```

---

## 🔧 Configuration Files

### Terraform Variables (Demo)

**File**: `infrastructure/terraform/environments/dev/terraform.tfvars`

```hcl
# Cost-Optimized Demo Configuration
environment = "dev"

# Database - Free Tier
db_instance_class      = "db.t3.micro"
db_allocated_storage   = 20
backup_retention_days  = 1
enable_multi_az        = false
enable_redis           = false
enable_documentdb      = false

# Networking - No NAT Gateway
enable_nat_gateway     = false

# Compute - Minimal Fargate
services = {
  identity     = { cpu = 256, memory = 512, count = 1 }
  organization = { cpu = 256, memory = 512, count = 1 }
  experiment   = { cpu = 256, memory = 512, count = 1 }
}
use_fargate_spot = true
```

### Module Usage (Demo)

```hcl
module "database" {
  source = "../../modules/database"
  
  environment           = "dev"
  enable_redis          = false  # Save $12/month
  enable_documentdb     = false  # Save $54/month
  db_instance_class     = "db.t3.micro"
  backup_retention_days = 1
}

module "networking" {
  source = "../../modules/networking"
  
  environment        = "dev"
  enable_nat_gateway = false  # Save $65/month
}

module "compute" {
  source = "../../modules/compute"
  
  environment      = "dev"
  use_fargate_spot = true  # Save 70%
  services = {
    identity     = { cpu = 256, memory = 512 }
    organization = { cpu = 256, memory = 512 }
    experiment   = { cpu = 256, memory = 512 }
  }
}
```

---

## 📈 Scaling Path

### Phase 1: Demo (Current)
- **Cost**: ~$55/month
- **Services**: 3 core microservices
- **Database**: PostgreSQL only
- **Networking**: VPC endpoints only

### Phase 2: Enhanced Demo (+$30/month)
- **Cost**: ~$85/month
- **Add**: Redis for caching
- **Add**: Metrics service
- **Keep**: VPC endpoints (no NAT)

### Phase 3: Production-Ready (+$140/month)
- **Cost**: ~$225/month
- **Add**: Multi-AZ RDS
- **Add**: NAT Gateways
- **Add**: All 6 microservices
- **Upgrade**: Larger instance sizes

### Phase 4: Full Production (+$150/month)
- **Cost**: ~$375/month
- **Add**: DocumentDB
- **Add**: Auto-scaling
- **Add**: Enhanced monitoring
- **Add**: Cross-region backups

---

## 💡 Cost Control Best Practices

### 1. Resource Tagging
All resources tagged with:
- `Environment`: dev/qa/prod
- `Project`: turaf
- `ManagedBy`: terraform
- `CostCenter`: demo

### 2. Budget Alerts
```hcl
resource "aws_budgets_budget" "monthly" {
  name         = "turaf-monthly-budget"
  budget_type  = "COST"
  limit_amount = "100"
  limit_unit   = "USD"
  time_unit    = "MONTHLY"
  
  notification {
    comparison_operator = "GREATER_THAN"
    threshold           = 80
    threshold_type      = "PERCENTAGE"
    notification_type   = "ACTUAL"
  }
}
```

### 3. Lifecycle Policies

**S3**:
- Delete logs after 7 days
- Delete old backups after 7 days

**ECR**:
- Keep only last 3 images per repository

**CloudWatch Logs**:
- 7-day retention for demo
- 30-day retention for production

### 4. Auto-Scaling
- Disabled for demo (fixed 1 task per service)
- Scale down to 0 during non-business hours (future)

### 5. Spot Instances
- Use Fargate Spot for 70% savings
- Acceptable for demo/dev environments

---

## 🔍 Cost Monitoring

### CloudWatch Dashboard
Monitor costs with:
- Daily spend by service
- Month-to-date total
- Forecast vs budget

### AWS Cost Explorer
Review:
- Cost by service
- Cost by tag
- Savings opportunities

### Terraform Cost Estimation
```bash
# Use Infracost for cost estimates
infracost breakdown --path infrastructure/terraform/environments/dev
```

---

## 📚 References

- [AWS Pricing Calculator](https://calculator.aws/)
- [AWS Free Tier](https://aws.amazon.com/free/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Cost Optimization Plan](.windsurf/plans/infrastructure-cost-optimization.md)
- [Changelog](changelog/2024-03-23-infrastructure-cost-optimization.md)

---

## 🎓 Key Learnings

1. **Single Database Multi-Schema**: Saves ~$180/month vs separate databases
2. **VPC Endpoints vs NAT Gateway**: Saves $65/month for AWS service access
3. **Fargate Spot**: 70% savings for non-critical workloads
4. **Free Tier Optimization**: Maximize 12-month free tier benefits
5. **Minimal Services**: Start with 3 core services, add as needed
6. **Lifecycle Policies**: Automatic cleanup prevents storage bloat
7. **Right-Sizing**: 0.25 vCPU sufficient for demo workloads

---

**Total Infrastructure Cost Reduction**: 77% (from $248/month to $55/month)
