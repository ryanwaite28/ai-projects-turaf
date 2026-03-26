# Archived Specifications

This directory contains previous versions of specifications that have been superseded by updated versions.

## Archived Files

### ci-cd-pipelines-v1-monolithic.md
**Archived**: March 25, 2026  
**Reason**: Superseded by new service-managed infrastructure pattern  
**Current Version**: `../ci-cd-pipelines.md`

**Key Differences**:
- **Old Pattern**: All services deployed via central Terraform, CD pipeline used `aws ecs update-service`
- **New Pattern**: Services manage their own infrastructure via CI/CD pipelines with service-specific Terraform

**Migration Guide**: See `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`

---

## Versioning Convention

When archiving specifications:
1. Move old version to this directory
2. Rename with descriptive suffix (e.g., `-v1-monolithic`)
3. Update this README with archive reason and pointer to current version
4. Update all references in tasks and documentation
