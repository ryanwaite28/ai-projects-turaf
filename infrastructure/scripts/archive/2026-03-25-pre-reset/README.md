# Archived Scripts - Pre-Infrastructure Reset

**Date**: March 25, 2026  
**Reason**: Infrastructure reset and standardization

## Context

These scripts were created during the initial infrastructure deployment and troubleshooting phase. They represent various experimental approaches, workarounds for specific issues, and ad-hoc solutions developed while learning the infrastructure requirements.

## Why Archived

1. **Inconsistency**: Multiple scripts doing similar things with slight variations
2. **Drift**: Scripts created to work around specific state issues that no longer exist
3. **Duplication**: Many overlapping functionalities across different scripts
4. **Standardization**: Moving to a smaller set of well-documented, standardized scripts

## Script Categories

### Deployment Scripts (10 variants)
- `deploy-dev-infrastructure.sh` - Basic deployment
- `deploy-dev-infrastructure-cross-account.sh` - Cross-account variant
- `deploy-dev-minimal.sh` - Minimal configuration
- `deploy-simplified-dev.sh` - Simplified (4 modules disabled)
- `deploy-standalone-dev.sh` - Standalone VPC+RDS only
- `deploy-full-dev-environment.sh` - Full environment attempt
- `destroy-standalone-dev.sh` - Cleanup standalone

### Troubleshooting Scripts (8 scripts)
- `cleanup-scheduled-deletions.sh` - Force delete Secrets Manager secrets
- `delete-db-subnet-group.sh` - Remove conflicting DB subnet group
- `delete-old-rds-instance.sh` - Remove old RDS instance
- `import-existing-resources.sh` - Import resources to Terraform state
- `unlock-terraform-state.sh` - Force unlock Terraform state
- `update-codebuild-source-dev.sh` - Update CodeBuild source
- `fix-codebuild-role-permissions-dev.sh` - Fix IAM permissions

### Flyway/Database Scripts (8 scripts)
- `create-codebuild-flyway-dev.sh` - Create CodeBuild project
- `create-db-users.sh` - Create database users manually
- `setup-flyway-iam-roles.sh` - Setup IAM roles
- `setup-flyway-iam-roles-cross-account.sh` - Cross-account variant
- `setup-flyway-network-cross-account.sh` - Network setup
- `verify-flyway-iam-roles.sh` - Verify IAM setup
- `verify-flyway-iam-roles-cross-account.sh` - Cross-account verification
- `verify-flyway-network-cross-account.sh` - Network verification
- `verify-flyway-network-dev.sh` - DEV network verification
- `test-codebuild-flyway-dev.sh` - Test CodeBuild execution
- `verify-codebuild-flyway-dev.sh` - Verify CodeBuild setup
- `validate-db-permissions.sh` - Validate database permissions

## Lessons Learned

### What Worked
- AWS role assumption pattern for cross-account access
- Terraform state management with S3 backend
- Force deletion of scheduled resources
- Incremental deployment approach

### What Didn't Work
- Too many deployment variants caused confusion
- Manual resource imports created state drift
- Standalone deployments conflicted with full deployments
- Lack of standardized error handling

### Key Issues Encountered
1. **DB Subnet Group Conflicts**: Old resources from previous deployments blocked new ones
2. **Terraform State Locks**: Interrupted operations left state locked
3. **Secrets Manager Recovery**: 30-day deletion window blocked recreations
4. **SCP Restrictions**: Some resources couldn't be deleted due to SCPs
5. **Module Configuration Errors**: ECS, Lambda, and Monitoring modules had syntax issues

## Replacement Scripts

The archived scripts are replaced by a smaller set of standardized scripts:

1. `deploy-environment.sh` - Universal deployment for all environments
2. `verify-environment.sh` - Post-deployment verification
3. `cost-estimate.sh` - Cost analysis
4. `backup-state.sh` - State backup utility
5. `assume-role.sh` - Reusable role assumption
6. `check-prerequisites.sh` - Pre-deployment checks

## Reference Value

These scripts remain valuable for:
- Understanding the evolution of the infrastructure
- Learning from troubleshooting approaches
- Referencing specific AWS CLI commands
- Understanding edge cases and workarounds

## Do Not Use

These scripts should **not** be used for new deployments. They are preserved for historical reference only. Use the new standardized scripts in the parent directory.
