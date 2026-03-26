# DEV Environment Pre-Reset Inventory

**Date**: March 25, 2026  
**Account**: 801651112319 (dev)  
**Region**: us-east-1  
**Purpose**: Document resources before infrastructure reset

---

## VPC and Networking

- VPC ID: vpc-0eb73410956d368a8
  - CIDR: 10.0.0.0/16
  - State: available
  - DNS Hostnames: null
  - DNS Support: null

- VPC ID: vpc-04b562ab3eebfb8b5
  - CIDR: 10.0.0.0/16
  - State: available
  - DNS Hostnames: null
  - DNS Support: null


## Subnets

- Subnet ID: subnet-0fbca1c0741c511bc
  - CIDR: 10.0.10.0/24
  - AZ: us-east-1a
  - Available IPs: 251
  - Type: turaf-private-subnet-dev-1

- Subnet ID: subnet-0752f98623e6664ef
  - CIDR: 10.0.10.0/24
  - AZ: us-east-1a
  - Available IPs: 245
  - Type: turaf-private-us-east-1a-dev

- Subnet ID: subnet-0eade6db591c9d11f
  - CIDR: 10.0.21.0/24
  - AZ: us-east-1b
  - Available IPs: 251
  - Type: turaf-database-us-east-1b-dev

- Subnet ID: subnet-0a7e1733037f31e69
  - CIDR: 10.0.11.0/24
  - AZ: us-east-1b
  - Available IPs: 251
  - Type: turaf-private-subnet-dev-2

- Subnet ID: subnet-0745ca1dadd4723b4
  - CIDR: 10.0.0.0/24
  - AZ: us-east-1a
  - Available IPs: 250
  - Type: turaf-public-subnet-dev-1

- Subnet ID: subnet-0535725f234ca5bc9
  - CIDR: 10.0.11.0/24
  - AZ: us-east-1b
  - Available IPs: 245
  - Type: turaf-private-us-east-1b-dev

- Subnet ID: subnet-059a1a88c62c3e8c7
  - CIDR: 10.0.1.0/24
  - AZ: us-east-1b
  - Available IPs: 251
  - Type: turaf-public-subnet-dev-2

- Subnet ID: subnet-0451eea91e9bfd527
  - CIDR: 10.0.1.0/24
  - AZ: us-east-1b
  - Available IPs: 250
  - Type: turaf-public-us-east-1b-dev

- Subnet ID: subnet-00c61b0b47979a1f3
  - CIDR: 10.0.2.0/24
  - AZ: us-east-1c
  - Available IPs: 250
  - Type: turaf-public-us-east-1c-dev

- Subnet ID: subnet-0e5b26fc43803ca86
  - CIDR: 10.0.20.0/24
  - AZ: us-east-1a
  - Available IPs: 251
  - Type: turaf-database-subnet-dev-1

- Subnet ID: subnet-0ae5e2889493ca0ad
  - CIDR: 10.0.21.0/24
  - AZ: us-east-1b
  - Available IPs: 251
  - Type: turaf-database-subnet-dev-2

- Subnet ID: subnet-0722e1d94a2003539
  - CIDR: 10.0.0.0/24
  - AZ: us-east-1a
  - Available IPs: 250
  - Type: turaf-public-us-east-1a-dev

- Subnet ID: subnet-01dbe290097df811a
  - CIDR: 10.0.12.0/24
  - AZ: us-east-1c
  - Available IPs: 245
  - Type: turaf-private-us-east-1c-dev

- Subnet ID: subnet-0bb4be7f7afcc314c
  - CIDR: 10.0.20.0/24
  - AZ: us-east-1a
  - Available IPs: 251
  - Type: turaf-database-us-east-1a-dev

- Subnet ID: subnet-0f52fac024e2aee69
  - CIDR: 10.0.22.0/24
  - AZ: us-east-1c
  - Available IPs: 251
  - Type: turaf-database-us-east-1c-dev


## NAT Gateways

- NAT Gateway ID: nat-071665cb67dabc8f6
  - State: available
  - Subnet: subnet-0745ca1dadd4723b4
  - EIP: 34.201.165.130

- NAT Gateway ID: nat-0e07c76378f890dc9
  - State: available
  - Subnet: subnet-0722e1d94a2003539
  - EIP: 54.84.119.241

- NAT Gateway ID: nat-098bf6cc553ed9127
  - State: available
  - Subnet: subnet-0451eea91e9bfd527
  - EIP: 44.219.83.248

- NAT Gateway ID: nat-06f2dbb4598476840
  - State: available
  - Subnet: subnet-00c61b0b47979a1f3
  - EIP: 52.45.217.150


## VPC Endpoints

- Endpoint ID: vpce-033bd439d79e0e179
  - Service: com.amazonaws.us-east-1.s3
  - Type: Gateway
  - State: available

- Endpoint ID: vpce-0f1c65d7267dad553
  - Service: com.amazonaws.us-east-1.secretsmanager
  - Type: Interface
  - State: available

- Endpoint ID: vpce-0a8213fdcd586d649
  - Service: com.amazonaws.us-east-1.ecs-telemetry
  - Type: Interface
  - State: available

- Endpoint ID: vpce-0a6218f705ebb4957
  - Service: com.amazonaws.us-east-1.ecs
  - Type: Interface
  - State: available

- Endpoint ID: vpce-0138d19a5155e73a0
  - Service: com.amazonaws.us-east-1.ecr.api
  - Type: Interface
  - State: available

- Endpoint ID: vpce-0210b1b68b3d69da5
  - Service: com.amazonaws.us-east-1.logs
  - Type: Interface
  - State: available

- Endpoint ID: vpce-0ff902246acbf9ad1
  - Service: com.amazonaws.us-east-1.ecr.dkr
  - Type: Interface
  - State: available


## Security Groups

- SG ID: sg-01b1f0d32cf32bd22
  - Name: turaf-codebuild-dev-20260324134435640200000003
  - Description: Security group for CodeBuild (Flyway migrations)
  - Ingress Rules: 0
  - Egress Rules: 1

- SG ID: sg-0bcb2fe8cb2cd4ff2
  - Name: turaf-elasticache-dev-2026032504170708370000001b
  - Description: Security group for ElastiCache Redis
  - Ingress Rules: 1
  - Egress Rules: 1

- SG ID: sg-0a140341eb1a95931
  - Name: turaf-documentdb-dev-2026032504170995870000001c
  - Description: Security group for DocumentDB (MongoDB)
  - Ingress Rules: 1
  - Egress Rules: 1

- SG ID: sg-0700dfd644af580af
  - Name: turaf-rds-dev-20260324134435643500000004
  - Description: Security group for RDS PostgreSQL
  - Ingress Rules: 1
  - Egress Rules: 1

- SG ID: sg-0fc1dca77f0c67878
  - Name: turaf-ecs-tasks-dev-2026032504170423920000001a
  - Description: Security group for ECS tasks
  - Ingress Rules: 1
  - Egress Rules: 1

- SG ID: sg-0cb7e7e4a2141b193
  - Name: turaf-vpc-endpoints-dev-2026032504153803120000000d
  - Description: Security group for VPC endpoints
  - Ingress Rules: 1
  - Egress Rules: 1

- SG ID: sg-04faa193e1084074c
  - Name: turaf-rds-dev-2026032504171270370000001d
  - Description: Security group for RDS PostgreSQL databases
  - Ingress Rules: 1
  - Egress Rules: 1

- SG ID: sg-05a4aff2579041639
  - Name: turaf-alb-dev-20260325041638011600000016
  - Description: Security group for Application Load Balancer
  - Ingress Rules: 2
  - Egress Rules: 1


## RDS Instances


## DB Subnet Groups

- Name: turaf-db-subnet-group-dev
  - VPC: vpc-0eb73410956d368a8
  - Subnets: 2
  - Status: Complete


## ElastiCache Clusters


## ECS Clusters


## ECS Services


## Application Load Balancers


## Lambda Functions


## S3 Buckets

- Bucket: turaf-dev-801651112319
  - Created: 2026-03-25T03:37:38+00:00

- Bucket: turaf-terraform-state-dev
  - Created: 2026-03-23T22:46:39+00:00


## Secrets Manager

- Secret: turaf/dev/rds/admin-20260324134423738900000001
  - ARN: arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/rds/admin-20260324134423738900000001-Wtw0q2
  - Last Changed: 2026-03-24T10:09:28.604000-04:00

- Secret: turaf/dev/db/experiment-user
  - ARN: arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/db/experiment-user-S7hUV4
  - Last Changed: 2026-03-25T00:19:49.279000-04:00

- Secret: turaf/dev/db/organization-user
  - ARN: arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/db/organization-user-H7kGm3
  - Last Changed: 2026-03-25T00:19:49.279000-04:00

- Secret: turaf/dev/db/admin-password
  - ARN: arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/db/admin-password-r54By2
  - Last Changed: 2026-03-25T00:19:49.283000-04:00

- Secret: turaf/dev/db/metrics-user
  - ARN: arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/db/metrics-user-P3aB32
  - Last Changed: 2026-03-25T00:19:49.284000-04:00

- Secret: turaf/dev/db/identity-user
  - ARN: arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/db/identity-user-g5mWq3
  - Last Changed: 2026-03-25T00:19:49.311000-04:00


## EventBridge Rules


## SQS Queues


## CloudWatch Log Groups

- Log Group: /aws/codebuild/turaf-flyway-migrations-dev
  - Retention: Never expire days
  - Stored Bytes: 0

- Log Group: /aws/rds/instance/turaf-postgres-dev/postgresql
  - Retention: Never expire days
  - Stored Bytes: 0


## Estimated Monthly Costs

Based on current running resources:\n
- NAT Gateways: 4 × $32/month = $128
- RDS Instances: 0 × $15/month (db.t3.micro) = $0
- ALB: 0 × $16/month = $0
- VPC Endpoints: ~$14/month (estimated)
- Other services: ~$10/month (estimated)

**Estimated Total: ~$152/month**
