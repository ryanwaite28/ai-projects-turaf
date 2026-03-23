# Task: Create Networking Module

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 3 hours  

## Objective

Create Terraform module for VPC, subnets, route tables, NAT gateways, and internet gateway.

## Prerequisites

- [x] Task 001: Terraform structure setup

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/networking/main.tf`
- `infrastructure/terraform/modules/networking/variables.tf`
- `infrastructure/terraform/modules/networking/outputs.tf`

## Implementation Details

### VPC and Subnets

```hcl
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = {
    Name        = "turaf-vpc-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_subnet" "public" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone = var.availability_zones[count.index]
  
  map_public_ip_on_launch = true
  
  tags = {
    Name = "turaf-public-${var.availability_zones[count.index]}-${var.environment}"
  }
}

resource "aws_subnet" "private" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = var.availability_zones[count.index]
  
  tags = {
    Name = "turaf-private-${var.availability_zones[count.index]}-${var.environment}"
  }
}

resource "aws_subnet" "database" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 20)
  availability_zone = var.availability_zones[count.index]
  
  tags = {
    Name = "turaf-database-${var.availability_zones[count.index]}-${var.environment}"
  }
}
```

### Internet Gateway and NAT

```hcl
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  
  tags = {
    Name = "turaf-igw-${var.environment}"
  }
}

resource "aws_eip" "nat" {
  count  = length(var.availability_zones)
  domain = "vpc"
  
  tags = {
    Name = "turaf-nat-eip-${var.availability_zones[count.index]}-${var.environment}"
  }
}

resource "aws_nat_gateway" "main" {
  count         = length(var.availability_zones)
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id
  
  tags = {
    Name = "turaf-nat-${var.availability_zones[count.index]}-${var.environment}"
  }
}
```

## Acceptance Criteria

- [ ] VPC created
- [ ] Public, private, and database subnets created
- [ ] Internet gateway attached
- [ ] NAT gateways created
- [ ] Route tables configured
- [ ] terraform plan succeeds

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify subnet CIDR calculations
- Check route table associations

## References

- Specification: `specs/aws-infrastructure.md` (Networking section)
- Related Tasks: 003-create-compute-modules
