# ADR-007: Infrastructure Restructure - Service-Managed Resources

**Date**: March 25, 2026  
**Status**: Accepted  
**Decision Makers**: Architecture Team  
**Related ADRs**: ADR-006 (Single Database Multi-Schema)

---

## Context

The initial infrastructure design managed all resources (shared infrastructure + service-specific resources) in a centralized Terraform configuration. This included:
- VPC, subnets, security groups (shared)
- ECS cluster, ALB (shared)
- ECS services, task definitions (service-specific)
- ALB target groups, listener rules (service-specific)
- CloudWatch log groups (service-specific)

This monolithic approach created several challenges:
1. **Tight Coupling**: All services deployed together, preventing independent releases
2. **Slow Iteration**: Infrastructure changes required full Terraform apply across all services
3. **Ownership Ambiguity**: Unclear whether platform team or service teams owned service resources
4. **Deployment Bottleneck**: Infrastructure team became bottleneck for service deployments
5. **Risk Amplification**: Changes to one service could affect all services
6. **State File Conflicts**: Multiple teams modifying same Terraform state

---

## Decision

We will **split infrastructure management** into two layers:

### Layer 1: Shared Infrastructure (Platform Team - Terraform)
Managed in `infrastructure/terraform/` by the platform team:
- **Networking**: VPC, subnets, NAT gateways, security groups
- **Compute**: ECS Cluster, Application Load Balancer, base HTTP/HTTPS listeners
- **Data**: RDS PostgreSQL, ElastiCache Redis, S3 buckets
- **Messaging**: EventBridge, SQS queues
- **Serverless**: Lambda functions (shared utilities)
- **Monitoring**: CloudWatch dashboards, X-Ray
- **IAM**: Execution roles, task roles, OIDC providers

### Layer 2: Service-Specific Resources (Service Teams - CI/CD)
Managed in `services/<service>/terraform/` by each service team via CI/CD:
- **ECS Task Definition**: Container configuration, environment variables, secrets
- **ECS Service**: Desired count, deployment configuration, capacity provider strategy
- **ALB Target Group**: Health checks, deregistration delay
- **ALB Listener Rule**: Path-based routing, priority
- **CloudWatch Log Group**: Service-specific logs

### Deployment Flow
1. Platform team deploys shared infrastructure via `infrastructure.yml` workflow
2. Service teams deploy their services via `service-<name>-<env>.yml` workflows
3. Service Terraform references shared infrastructure via `terraform_remote_state`
4. Each service has independent Terraform state in S3

---

## Rationale

### Why Split Responsibility?

**Independent Deployments**:
- Services can deploy without coordinating with other teams
- Faster iteration cycles for service development
- Reduced blast radius for changes

**Clear Ownership**:
- Platform team owns shared infrastructure stability
- Service teams own their service-specific configuration
- Accountability is well-defined

**Scalability**:
- Easy to add new services without modifying shared infrastructure
- Service teams can self-service their deployments
- Platform team focuses on infrastructure improvements

**Cost Efficiency**:
- Shared ALB and ECS cluster reduce costs
- Services only pay for their specific resources (tasks, target groups)
- Eliminates duplicate infrastructure per service

**State Management**:
- Separate Terraform state per service prevents conflicts
- Smaller state files are easier to manage
- State locking issues isolated to single service

### Why Not Full Service Ownership?

We considered giving services full ownership (including their own ALB, cluster), but rejected it because:
- **Cost**: Each service would need its own ALB (~$20/month) and cluster
- **Complexity**: More moving parts to manage
- **Networking**: More complex security group and routing rules
- **Operational Overhead**: More resources to monitor and maintain

The hybrid approach balances autonomy with efficiency.

---

## Consequences

### Positive

1. **Faster Deployments**: Services deploy independently in ~5-10 minutes vs 30+ minutes for full infrastructure
2. **Better Ownership**: Clear boundaries between platform and service responsibilities
3. **Reduced Risk**: Service changes don't affect other services
4. **Easier Onboarding**: New services follow established pattern
5. **Improved Velocity**: Service teams unblocked from infrastructure team
6. **Better Testing**: Services can test infrastructure changes in isolation

### Negative

1. **Increased Complexity**: More Terraform configurations to maintain (1 shared + N services)
2. **State Management**: Must manage multiple Terraform state files
3. **Coordination Required**: Platform changes to outputs require service updates
4. **Learning Curve**: Service teams must learn Terraform basics
5. **Tooling Overhead**: More CI/CD workflows to maintain
6. **Dependency Management**: Services depend on shared infrastructure outputs

### Mitigation Strategies

**For Complexity**:
- Provide service Terraform templates
- Document patterns in `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`
- Create reusable Terraform modules for common patterns

**For State Management**:
- Use S3 backend with DynamoDB locking
- Separate state files per service: `turaf-<service>-<env>-state`
- Automated state backup and recovery

**For Coordination**:
- Version shared infrastructure outputs
- Communicate breaking changes via ADRs
- Maintain backward compatibility when possible

**For Learning Curve**:
- Provide comprehensive documentation
- Offer Terraform training sessions
- Create example service deployments

---

## Implementation

### Phase 1: Infrastructure Restructure ✅ Completed
- Removed service-specific resources from `infrastructure/terraform/modules/compute/`
- Updated outputs to export shared infrastructure details
- Deployed shared infrastructure to DEV environment
- Documented in `infrastructure/docs/architecture/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`

### Phase 2: CI/CD Pattern ✅ Completed
- Created service deployment pattern documentation
- Updated CI/CD specifications
- Updated task files for DEV, QA, PROD deployments
- Documented in `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`

### Phase 3: Service Migration (Pending)
- Create service Terraform directories
- Implement service deployment workflows
- Migrate identity-service (pilot)
- Migrate remaining services
- Decommission old deployment workflows

---

## Alternatives Considered

### Alternative 1: Keep Monolithic Terraform
**Rejected**: Doesn't solve deployment bottleneck or ownership issues

### Alternative 2: Full Service Ownership (Separate ALB per service)
**Rejected**: Too expensive (~$20/month per ALB × N services) and operationally complex

### Alternative 3: Terraform Workspaces
**Rejected**: Workspaces share state file, doesn't solve state locking issues

### Alternative 4: Terragrunt
**Considered**: Adds complexity, team not familiar with tool, hybrid approach simpler

---

## References

- **Implementation Summary**: `infrastructure/docs/architecture/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- **Deployment Pattern**: `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`
- **CI/CD Specification**: `specs/ci-cd-pipelines.md`
- **Infrastructure Plan**: `infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md`

---

## Lessons Learned

1. **Start Simple**: Initial monolithic approach was good for getting started
2. **Evolve Architecture**: As team grows, split responsibilities for better velocity
3. **Document Patterns**: Clear documentation critical for service team adoption
4. **Provide Templates**: Reduce friction with ready-to-use Terraform templates
5. **Test in DEV First**: Validate pattern in DEV before rolling to QA/PROD

---

**Decision**: Accepted  
**Next Review**: After 3 services migrated to new pattern (Q2 2026)
