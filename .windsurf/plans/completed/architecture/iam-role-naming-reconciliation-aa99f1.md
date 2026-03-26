# IAM Role Naming Reconciliation Plan

Reconcile inconsistent IAM role naming across documentation to establish `GitHubActionsDeploymentRole` as the single source of truth.

**Created**: 2026-03-25  
**Status**: Active  
**Related Docs**: GITHUB.md, specs/ci-cd-pipelines.md, infrastructure/github-oidc-roles.md, BEST_PRACTICES.md

---

## Problem Statement

IAM role names for GitHub Actions OIDC authentication are inconsistent across documentation:

**Currently Deployed in AWS** (Source of Truth):
- Role Name: `GitHubActionsDeploymentRole` (same name in all accounts)
- Accounts: Ops, Dev, QA, Prod

**Incorrect References in Docs**:
- `GitHubActionsRole-Dev`
- `GitHubActionsRole-QA`
- `GitHubActionsRole-Prod`
- `GitHubActionsRole-Ops`
- `GitHubActionsRole` (without suffix)

---

## Decision

**Use `GitHubActionsDeploymentRole` as the authoritative standard** because:
1. ✅ Already deployed in all AWS accounts
2. ✅ No AWS infrastructure changes required
3. ✅ Environment distinguished by AWS account ID
4. ✅ Simpler naming convention
5. ✅ Documented in infrastructure/github-oidc-roles.md

---

## Scope of Changes

### Files with Incorrect References

**High Priority** (User-facing docs):
1. `GITHUB.md` - 9 references to `GitHubActionsRole-<env>`
2. `specs/ci-cd-pipelines.md` - 6 references to `GitHubActionsRole-<env>`
3. `tasks/cicd/002-setup-cd-dev-pipeline.md` - 3 references
4. `tasks/cicd/003-setup-cd-qa-pipeline.md` - 3 references
5. `tasks/cicd/004-setup-cd-prod-pipeline.md` - 3 references
6. `tasks/cicd/005-setup-infrastructure-pipeline.md` - 1 reference

**Medium Priority** (Archive/reference):
7. `specs/archive/ci-cd-pipelines-v1-monolithic.md` - 14 references (archived, lower priority)

**Low Priority** (Other):
8. `docs/troubleshooting/COMMON_ISSUES.md` - 1 reference
9. Various infrastructure docs - already correct

### Files Already Correct

✅ `infrastructure/github-oidc-roles.md` - Uses `GitHubActionsDeploymentRole`  
✅ `tasks/infrastructure/009-configure-iam-oidc-github-actions.md` - Uses `GitHubActionsDeploymentRole`

---

## Implementation Plan

### Step 1: Create IAM Roles Source-of-Truth Document

**Action**: Create `docs/IAM_ROLES.md` as the authoritative reference

**Content**:
- Complete list of all IAM roles used in the project
- GitHub Actions OIDC roles with ARNs for all accounts
- ECS task execution/task roles
- Lambda execution roles
- Cross-account access roles
- Usage examples and references

**Purpose**: Single source of truth for all IAM role names and ARNs

---

### Step 2: Update GITHUB.md

**File**: `GITHUB.md`

**Changes** (9 occurrences):
- Line 123: `GitHubActionsRole-Dev` → `GitHubActionsDeploymentRole`
- Line 124: ARN update
- Line 129: `GitHubActionsRole-QA` → `GitHubActionsDeploymentRole`
- Line 130: ARN update
- Line 135: `GitHubActionsRole-Prod` → `GitHubActionsDeploymentRole`
- Line 136: ARN update
- Line 141: `GitHubActionsRole-Ops` → `GitHubActionsDeploymentRole`
- Line 142: ARN update
- Line 177: Example workflow update

**Add Reference**: Link to `docs/IAM_ROLES.md` in References section

---

### Step 3: Update specs/ci-cd-pipelines.md

**File**: `specs/ci-cd-pipelines.md`

**Changes** (6 occurrences):
- Update all workflow examples to use `GitHubActionsDeploymentRole`
- Update role ARN references
- Add note about role naming convention

**Add Reference**: Link to `docs/IAM_ROLES.md`

---

### Step 4: Update CI/CD Task Files

**Files**:
- `tasks/cicd/002-setup-cd-dev-pipeline.md` (3 occurrences)
- `tasks/cicd/003-setup-cd-qa-pipeline.md` (3 occurrences)
- `tasks/cicd/004-setup-cd-prod-pipeline.md` (3 occurrences)
- `tasks/cicd/005-setup-infrastructure-pipeline.md` (1 occurrence)

**Changes**: Update all role references in workflow examples

---

### Step 5: Update BEST_PRACTICES.md

**File**: `BEST_PRACTICES.md`

**Changes**:
- Add `docs/IAM_ROLES.md` to "Single Source of Truth" table
- Reference IAM roles document in relevant sections

---

### Step 6: Update Troubleshooting Guide

**File**: `docs/troubleshooting/COMMON_ISSUES.md`

**Changes**: Update OIDC authentication troubleshooting example

---

### Step 7: Update Archive (Optional)

**File**: `specs/archive/ci-cd-pipelines-v1-monolithic.md`

**Decision**: Add deprecation notice at top, optionally update references

---

### Step 8: Verification

**Actions**:
1. Search for all remaining `GitHubActionsRole` references (without "Deployment")
2. Verify all ARNs are correct
3. Check cross-references are working
4. Update DOCUMENTATION_INDEX.md if needed

---

## Acceptance Criteria

- [ ] `docs/IAM_ROLES.md` created as source-of-truth
- [ ] GITHUB.md updated (9 references)
- [ ] specs/ci-cd-pipelines.md updated (6 references)
- [ ] All CI/CD task files updated (10 total references)
- [ ] BEST_PRACTICES.md references IAM_ROLES.md
- [ ] Troubleshooting guide updated
- [ ] No remaining incorrect role name references
- [ ] All ARNs verified correct
- [ ] DOCUMENTATION_INDEX.md updated (if needed)

---

## IAM Role Reference Table

**Authoritative Role Names**:

| Account | Role Name | Role ARN |
|---------|-----------|----------|
| Ops (146072879609) | `GitHubActionsDeploymentRole` | `arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole` |
| Dev (801651112319) | `GitHubActionsDeploymentRole` | `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole` |
| QA (965932217544) | `GitHubActionsDeploymentRole` | `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole` |
| Prod (811783768245) | `GitHubActionsDeploymentRole` | `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole` |

**Key Points**:
- Same role name in all accounts
- Environment distinguished by AWS account ID
- No environment suffix in role name
- Consistent with infrastructure/github-oidc-roles.md

---

## References

- **Current Source of Truth**: `infrastructure/github-oidc-roles.md`
- **Implementation Task**: `tasks/infrastructure/009-configure-iam-oidc-github-actions.md`
- **AWS Accounts**: `AWS_ACCOUNTS.md`
- **Best Practices**: `BEST_PRACTICES.md`

---

## Estimated Time

- Create IAM_ROLES.md: 30 minutes
- Update GITHUB.md: 15 minutes
- Update specs/ci-cd-pipelines.md: 15 minutes
- Update CI/CD task files: 20 minutes
- Update other docs: 10 minutes
- Verification: 10 minutes

**Total**: ~1.5 hours
