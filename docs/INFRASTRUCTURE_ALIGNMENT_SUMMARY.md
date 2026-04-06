# Infrastructure Alignment Summary

**Date**: 2026-04-03  
**Plan**: project-alignment-f9c901.md  
**Status**: Phase 7 In Progress

---

## Overview

This document summarizes the infrastructure alignment work completed to address critical networking gaps identified in the project alignment plan (Discrepancies D29-D36, D21, D22).

---

## Completed Work

### Phase 7.1: Database Migration V016 ✅

**File Created**: `services/flyway-service/migrations/V016__communications_fix_organization_id_type.sql`

**Purpose**: Fix organization_id column type inconsistency in conversations table

**Changes**:
- Altered `organization_id` column from `VARCHAR(255)` to `VARCHAR(36)` for UUID consistency
- Added column comment for documentation

**Resolves**: Discrepancy D23 (V012 migration column width inconsistency)

---

### Phase 7.2: Internal ALB for Service Discovery ✅

**Problem**: BFF had no way to communicate with backend microservices in AWS (D29, D31)

**Solution**: Added internal Application Load Balancer for service-to-service communication

#### Files Modified:

**1. Compute Module** (`infrastructure/terraform/modules/compute/main.tf`)
- Added `aws_lb.internal` resource (internal ALB in private subnets)
- Added `aws_lb_listener.internal_http` resource (HTTP listener on port 80)
- Internal ALB configured with:
  - `internal = true` (not internet-facing)
  - Placed in private subnets
  - Uses internal ALB security group
  - HTTP listener with default 404 response

**2. Compute Module Variables** (`infrastructure/terraform/modules/compute/variables.tf`)
- Added `internal_alb_security_group_id` variable

**3. Compute Module Outputs** (`infrastructure/terraform/modules/compute/outputs.tf`)
- Added `internal_alb_arn` output
- Added `internal_alb_dns_name` output (for BFF service configuration)
- Added `internal_alb_zone_id` output
- Added `internal_alb_listener_http_arn` output (for creating listener rules)

**4. Security Module** (`infrastructure/terraform/modules/security/main.tf`)
- Added `aws_security_group.internal_alb` resource:
  - Ingress: Port 80 from VPC CIDR (ECS tasks)
  - Egress: All traffic allowed
- Updated `aws_security_group.ecs_tasks`:
  - Added ingress rule from internal ALB on port 8080
  - Added ingress rule from public ALB on port 8080
  - Maintains existing VPC ingress on port 8080

**5. Security Module Outputs** (`infrastructure/terraform/modules/security/outputs.tf`)
- Added `internal_alb_security_group_id` output

**6. Environment Configurations**
- Updated `infrastructure/terraform/environments/dev/main.tf`
- Updated `infrastructure/terraform/environments/qa/main.tf`
- Updated `infrastructure/terraform/environments/prod/main.tf`
- All environments now pass `internal_alb_security_group_id` to compute module

**Resolves**: 
- D29 (No internal/private ALB)
- D31 (No service discovery for BFF → microservices)

---

## Architecture Impact

### Before
```
Frontend → Public ALB → BFF → ??? (no way to reach microservices)
```

### After
```
Frontend → Public ALB → BFF → Internal ALB → Microservices
                                    ↓
                          (path-based routing)
                          /identity/* → identity-service
                          /organization/* → organization-service
                          /experiment/* → experiment-service
                          /metrics/* → metrics-service
                          /communications/* → communications-service
```

### Security Flow
1. Public ALB security group: Allows HTTP/HTTPS from internet
2. Internal ALB security group: Allows HTTP from VPC (ECS tasks)
3. ECS tasks security group: Allows traffic from both ALBs on port 8080

---

## Remaining Infrastructure Work

### Phase 7.3: Public ALB Listener Rules (D30)
- [ ] Add target group for BFF API service
- [ ] Add listener rule: `/api/*` → BFF target group
- [ ] Add target group for WebSocket Gateway
- [ ] Add listener rule: `/ws/*` → ws-gateway target group
- [ ] Configure health checks for target groups

### Phase 7.4: Internal ALB Listener Rules
- [ ] Add target groups for each microservice
- [ ] Add listener rules for path-based routing:
  - `/identity/*` → identity-service target group
  - `/organization/*` → organization-service target group
  - `/experiment/*` → experiment-service target group
  - `/metrics/*` → metrics-service target group
  - `/communications/*` → communications-service target group

### Phase 7.5: EventBridge and Lambda Triggers (D32, D35)
- [ ] Create EventBridge event bus resource
- [ ] Add EventBridge rules to forward events to SQS queues
- [ ] Update Lambda trigger configuration

### Phase 7.6: SQS FIFO Queue (D36)
- [ ] Convert chat messages queue to FIFO
- [ ] Split into direct messages and group messages queues
- [ ] Update DLQ configuration

### Phase 7.7: Docker Compose Fixes (D21)
- [ ] Remove unused `ministack_data` volume
- [ ] Add BFF service URL environment variables
- [ ] Normalize communications-service health check path

### Phase 7.8: GitHub Actions Workflows (D22)
- [ ] Create active `service-ws-gateway.yml` workflow
- [ ] Create frontend deployment workflow
- [ ] Clean up archived per-env workflows

---

## BFF Configuration Update Required

Once Terraform is deployed, the BFF `ServiceUrlsConfig` needs to be updated to use the internal ALB DNS name:

```yaml
# Current (Docker Compose - uses service names)
identity-service-url: http://identity-service:8081

# AWS (should use internal ALB)
identity-service-url: http://${INTERNAL_ALB_DNS}/identity
organization-service-url: http://${INTERNAL_ALB_DNS}/organization
experiment-service-url: http://${INTERNAL_ALB_DNS}/experiment
metrics-service-url: http://${INTERNAL_ALB_DNS}/metrics
communications-service-url: http://${INTERNAL_ALB_DNS}/communications
```

The internal ALB DNS name will be available as a Terraform output: `module.compute.internal_alb_dns_name`

---

## Testing Plan

### Local (Docker Compose)
1. Verify services can communicate using service names
2. Test BFF → microservice calls
3. Verify health checks

### AWS (After Deployment)
1. Verify internal ALB is created in private subnets
2. Verify security groups allow correct traffic flow
3. Test BFF → internal ALB → microservice communication
4. Verify path-based routing works correctly
5. Monitor CloudWatch logs for connection issues

---

## Notes

- Internal ALB provides service discovery without requiring AWS Cloud Map
- Path-based routing on internal ALB allows services to run on same port (8080)
- Security groups enforce network isolation (internal ALB not accessible from internet)
- All services standardized on port 8080 for simplicity
- Internal ALB DNS name is stable and can be used in environment variables

---

## Related Documents

- **Plan**: `.windsurf/plans/active/project-alignment-f9c901.md`
- **Specs**: `specs/aws-infrastructure.md`, `specs/bff-api.md`
- **Tasks**: Updated in `tasks/TASK_SUMMARY.md`
