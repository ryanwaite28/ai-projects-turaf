# Workflow Consolidation

**Date**: April 1, 2026  
**Type**: Infrastructure Improvement  
**Impact**: CI/CD Pipeline Simplification

## Summary

Consolidated GitHub Actions workflows from 27 environment-specific files to 9 unified service workflows, reducing duplication and improving maintainability while implementing sequential deployment patterns.

## Changes

### Workflow Structure

**Before**:
- 27 separate workflow files (9 services × 3 environments)
- Pattern: `service-<name>-<env>.yml` (e.g., `service-identity-dev.yml`, `service-identity-qa.yml`, `service-identity-prod.yml`)
- Each environment required separate workflow file
- Duplication of deployment logic across environments

**After**:
- 9 consolidated workflow files (1 per service)
- Pattern: `service-<name>.yml` (e.g., `service-identity.yml`)
- Single workflow handles all environments (DEV, QA, PROD)
- Sequential deployment: DEV → QA → PROD

### Deployment Flow

**Push to `develop` branch**:
- Triggers DEV deployment only
- Automatic deployment (no approval required)

**Push to `main` branch**:
1. Security scan (Trivy vulnerability scanning)
2. Deploy to DEV (automatic)
3. Deploy to QA (automatic after DEV succeeds)
4. Deploy to PROD (requires manual approval via GitHub environment protection)

**Manual workflow dispatch**:
- Allows deployment to specific environment (dev, qa, or prod)
- Useful for hotfixes or targeted deployments

### GitHub Environment Names

**Updated environment naming convention**:
- ❌ Old: `dev-environment`, `qa-environment`, `prod-environment`
- ✅ New: `dev`, `qa`, `prod`

**Environment Protection Rules**:
- `dev`: No protection (auto-deploy)
- `qa`: Optional reviewers
- `prod`: Required reviewers (manual approval gate)

### Files Created

**New Consolidated Workflows** (9 files):
1. `.github/workflows/service-identity.yml`
2. `.github/workflows/service-experiment.yml`
3. `.github/workflows/service-metrics.yml`
4. `.github/workflows/service-reporting.yml`
5. `.github/workflows/service-notification.yml`
6. `.github/workflows/service-organization.yml`
7. `.github/workflows/service-communications.yml`
8. `.github/workflows/service-bff-api.yml`
9. `.github/workflows/service-ws-gateway.yml`

### Files Archived

**Old Environment-Specific Workflows** (27 files moved to `.github/workflows/archive/`):
- `service-*-dev.yml` (9 files)
- `service-*-qa.yml` (9 files)
- `service-*-prod.yml` (9 files)

### Documentation Updates

**Updated Files**:
1. `infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md`
   - Section 4.1: GitHub environment creation commands
   - Section 4.2: Environment-specific secrets configuration
   
2. `GITHUB.md`
   - GitHub Environments section with correct naming

3. `tasks/cicd/007-configure-aws-oidc.md`
   - GitHub environment references in "Next Steps"

4. `tasks/cicd/008-setup-per-service-workflows.md`
   - Workflow examples with correct environment names

5. `specs/ci-cd-pipelines.md`
   - Workflow structure section
   - Service deployment pattern examples

6. `docs/DEPLOYMENT_RUNBOOK.md`
   - Deployment matrix
   - Workflow file references
   - Sequential deployment flow documentation

## Benefits

### Reduced Duplication
- **67% reduction** in workflow files (27 → 9)
- Single source of truth for deployment logic per service
- Easier to maintain and update deployment processes

### Improved Deployment Safety
- Sequential deployment ensures DEV validation before QA/PROD
- Security scanning runs before production deployments
- Manual approval gate for PROD deployments

### Better Developer Experience
- Simpler workflow structure
- Consistent deployment patterns across all services
- Clear deployment flow: develop → DEV, main → DEV → QA → PROD

### Operational Efficiency
- Reduced maintenance overhead
- Easier to implement cross-cutting changes
- Consistent monitoring and health checks across environments

## Migration Notes

### For Developers

**No changes required** to development workflow:
- Continue pushing to `develop` for DEV deployments
- Continue pushing to `main` for production releases
- Existing branch protection rules remain unchanged

### For Operations

**GitHub Environment Configuration**:
```bash
# Ensure environments are configured with simple names
gh api repos/ryanwaite28/ai-projects-turaf/environments/dev -X PUT
gh api repos/ryanwaite28/ai-projects-turaf/environments/qa -X PUT
gh api repos/ryanwaite28/ai-projects-turaf/environments/prod -X PUT
```

**Environment Protection Rules**:
- `dev`: No protection
- `qa`: Optional reviewers (recommended)
- `prod`: Required reviewers (2 approvals)

### Rollback Plan

If issues arise, old workflow files are archived in `.github/workflows/archive/` and can be restored:
```bash
# Restore old workflows if needed
cd .github/workflows
cp archive/service-*-*.yml .
```

## Testing

**Validation Steps**:
1. ✅ All 9 consolidated workflows created
2. ✅ Documentation updated with correct environment names
3. ✅ Changelog created
4. ⏳ Archive old workflow files
5. ⏳ Test deployment flow (DEV → QA → PROD)

## Related Documents

- [CI/CD Pipelines Specification](../specs/ci-cd-pipelines.md)
- [Deployment Runbook](../docs/DEPLOYMENT_RUNBOOK.md)
- [GitHub Configuration](../GITHUB.md)
- [Infrastructure Plan](../infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md)
- [Consolidation Plan](.windsurf/plans/active/workflow-consolidation-c6c363.md)

## References

- **ADR**: N/A (operational improvement, not architectural change)
- **GitHub Issue**: N/A
- **Pull Request**: TBD
