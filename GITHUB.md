# GitHub Configuration

This document outlines the GitHub configuration and rules for this project.

## Repository Links

- **Repository**: https://github.com/ryanwaite28/ai-projects-turaf



## Repository Structure

- **Main Branch**: `main`
- **Feature Branches**: `feature/*`
- **Release Branches**: `release/*`
- **Hotfix Branches**: `hotfix/*`
- **Documentation Branches**: `docs/*`
- **Testing Branches**: `test/*`
- **Performance Branches**: `perf/*`
- **Security Branches**: `security/*`
- **Refactor Branches**: `refactor/*`
- **Chore Branches**: `chore/*`
- **Miscellaneous Branches**: `misc/*`



## Branch Protection Rules

- **Main**: 
  - Require status checks to pass before merging
  - Require branches to be up to date before merging
  - Require pull request reviews before merging
  - Require code review
  - Dismiss stale pull request approvals when new commits are pushed
  - Require linear history
  - Include administrators

- **Feature Branches**: 
  - Require status checks to pass before merging
  - Require branches to be up to date before merging
  - Require pull request reviews before merging
  - Require code review
  - Dismiss stale pull request approvals when new commits are pushed
  - Require linear history
  - Include administrators
  - Pull request triggers CI/CD pipeline

---

## CI/CD Integration

### GitHub Actions Workflows

All CI/CD pipelines are implemented using GitHub Actions and stored in `.github/workflows/`.

**Workflow Files**:
- `ci.yml` - Continuous Integration (runs on all PRs and pushes)
- `cd-dev.yml` - Deploy to DEV environment
- `cd-qa.yml` - Deploy to QA environment
- `cd-prod.yml` - Deploy to PROD environment
- `infrastructure.yml` - Infrastructure deployment via Terraform
- `security-scan.yml` - Security vulnerability scanning

### Branch-to-Environment Deployment Mapping

**Branch Strategy**:
- `main` branch → Production deployments (manual approval required)
- `develop` branch → DEV environment (automatic deployment)
- `release/*` branches → QA environment (automatic deployment)
- Feature branches → CI validation only (no deployment)

**Environment Triggers**:
- **DEV**: Automatic deployment on push to `develop`
- **QA**: Automatic deployment on push to `release/*` or manual trigger
- **PROD**: Manual workflow dispatch only, requires approval

### GitHub Environments

**Environment Configuration**:

1. **dev**
   - Protection rules: None (auto-deploy)
   - Secrets: DEV-specific configuration
   - Reviewers: Not required

2. **qa**
   - Protection rules: Wait timer (5 minutes)
   - Secrets: QA-specific configuration
   - Reviewers: Optional

3. **prod**
   - Protection rules: Required reviewers (2 approvals)
   - Secrets: PROD-specific configuration
   - Reviewers: Team leads and senior engineers
   - Deployment branches: `main` only

### Required Status Checks

**All Pull Requests to `main` must pass**:
- Lint (Java, Angular, Terraform)
- Unit Tests (Backend, Frontend)
- Build (All services)
- SonarQube Quality Gate
- Security Scan (OWASP, npm audit)
- Terraform Validate (if infrastructure changes)

---

## AWS OIDC Authentication

### OIDC Provider Configuration

GitHub Actions authenticates to AWS using OpenID Connect (OIDC) federation, eliminating the need for long-lived AWS credentials.

**OIDC Provider Details**:
- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`
- Thumbprint: `6938fd4d98bab03faadb97b34396831e3780aea1`

### IAM Roles per AWS Account

**DEV Account (801651112319)**:
- Role Name: `GitHubActionsDeploymentRole`
- Role ARN: `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
- Trust Policy: Repository `ryanwaite28/ai-projects-turaf` with branch `develop`
- Permissions: ECR, ECS, Lambda, S3, CloudFront, Terraform state

**QA Account (965932217544)**:
- Role Name: `GitHubActionsDeploymentRole`
- Role ARN: `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
- Trust Policy: Repository `ryanwaite28/ai-projects-turaf` with branches `release/*`
- Permissions: ECR, ECS, Lambda, S3, CloudFront, Terraform state

**PROD Account (811783768245)**:
- Role Name: `GitHubActionsDeploymentRole`
- Role ARN: `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`
- Trust Policy: Repository `ryanwaite28/ai-projects-turaf` with branch `main`
- Permissions: ECR, ECS, Lambda, S3, CloudFront, Terraform state (read-heavy, write-restricted)

**Ops Account (146072879609)**:
- Role Name: `GitHubActionsDeploymentRole`
- Role ARN: `arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole`
- Trust Policy: Repository `ryanwaite28/ai-projects-turaf` (all branches)
- Permissions: Cross-account access for centralized DevOps tooling

### Trust Policy Template

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::{ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:ryanwaite28/ai-projects-turaf:*"
        }
      }
    }
  ]
}
```

### Workflow Authentication Example

```yaml
- name: Configure AWS Credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole
    aws-region: us-east-1
    role-session-name: GitHubActions-Deploy-Dev
```

### Permission Boundaries

All GitHub Actions IAM roles enforce least-privilege access:
- No IAM user/role creation permissions
- No root account access
- Scoped to specific resource prefixes (`turaf-*`)
- Time-limited sessions (1 hour maximum)
- CloudTrail logging enabled for all actions

---

## Repository Security & Compliance

### Code Review Requirements

**Pull Request Rules**:
- This is a solo repository, so I'm not requiring multiple approvals
<!-- - Minimum 1 approval required for feature branches
- Minimum 2 approvals required for `main` branch
- Code owners must approve changes to critical paths
- Stale reviews dismissed on new commits
- Review from code owner required for:
  - `/infrastructure/**`
  - `/services/*/src/main/**`
  - `/.github/workflows/**` -->

### Security Scanning Integration

**Automated Security Tools**:

1. **Dependabot**
   - Automatic dependency updates
   - Security vulnerability alerts
   - Weekly scan schedule
   - Auto-merge for patch updates (dev only)

2. **CodeQL Analysis**
   - Runs on every PR and push to `main`
   - Languages: Java, JavaScript/TypeScript
   - Custom queries for Spring Boot security patterns
   - Results uploaded to GitHub Security tab

3. **Secret Scanning**
   - Push protection enabled
   - Scans for AWS keys, API tokens, private keys
   - Automatic alerts to repository admins
   - Partner patterns enabled

4. **Container Scanning**
   - Trivy scans all Docker images
   - Critical/High vulnerabilities block deployment
   - SARIF results uploaded to GitHub Security

### Secrets Management

**GitHub Secrets Organization**:

**Repository Secrets**:
- `SONAR_TOKEN` - SonarQube authentication
- `SONAR_HOST_URL` - SonarQube server URL
- `SLACK_WEBHOOK_URL` - Deployment notifications

**Environment Secrets** (per environment):
- `AWS_ACCOUNT_ID` - Target AWS account
- `CLOUDFRONT_DISTRIBUTION_ID` - Frontend CDN
- `DATABASE_MIGRATION_TOKEN` - Flyway migrations
- `DATADOG_API_KEY` - Monitoring integration

**Secret Rotation Policy**:
- Secrets rotated every 90 days
- Automated rotation via AWS Secrets Manager
- GitHub secrets updated via API automation
- Audit log maintained in Ops account

### Compliance & Audit

**Audit Trail**:
- All deployments logged to CloudTrail
- GitHub Actions logs retained for 90 days
- Deployment artifacts stored in S3 with versioning
- Change tracking via Git commits and PR history

**Compliance Checks**:
- No direct commits to `main` (enforced)
- All changes via pull requests
- Signed commits required for production
- Branch protection cannot be bypassed

---

## Multi-Environment Strategy

### Environment-to-Account Mapping

**AWS Account Assignments**:

| Environment | AWS Account ID | Account Name | Purpose |
|-------------|---------------|--------------|---------|
| DEV | 801651112319 | dev | Development and feature testing |
| QA | 965932217544 | qa | Integration testing and staging |
| PROD | 811783768245 | prod | Production workloads |
| Ops | 146072879609 | Ops | DevOps tooling and CI/CD infrastructure |

### Promotion Workflows

**DEV → QA Promotion**:
1. Feature branch merged to `develop`
2. Automatic deployment to DEV
3. Smoke tests pass in DEV
4. Create `release/vX.Y.Z` branch from `develop`
5. Automatic deployment to QA
6. Integration tests run in QA
7. Manual QA validation

**QA → PROD Promotion**:
1. QA validation complete
2. Create Git tag `vX.Y.Z` from release branch
3. Merge release branch to `main`
4. Manual workflow dispatch for PROD deployment
5. Requires 2 approvals from authorized personnel
6. Blue-green deployment with gradual traffic shift
7. Automated rollback on error threshold breach

### Approval Gates

**DEV Environment**:
- No approval required
- Automatic deployment on merge to `develop`

**QA Environment**:
- Optional approval (can be bypassed)
- 5-minute wait timer before deployment
- Notification to QA team

**PROD Environment**:
- **Required approvals**: 2 reviewers
- **Authorized approvers**: Team leads, senior engineers, DevOps team
- **Approval timeout**: 72 hours
- **Deployment window**: Business hours only (configurable)

### Rollback Procedures

**Automated Rollback Triggers**:
- Error rate > 5% for 5 minutes
- P95 latency > 2x baseline for 10 minutes
- Health check failures > 50%
- Critical CloudWatch alarms

**Manual Rollback Process**:
1. Trigger rollback workflow in GitHub Actions
2. Specify target version/commit
3. Approval required (1 reviewer)
4. Revert ECS task definitions
5. Restore previous Lambda versions
6. Invalidate CloudFront cache
7. Verify rollback success

---

## Monorepo Management

### Repository Organization

```
ai-projects-turaf/
├── .github/
│   ├── workflows/          # CI/CD pipelines
│   ├── CODEOWNERS          # Code ownership rules
│   └── PULL_REQUEST_TEMPLATE.md
├── services/               # Backend microservices
│   ├── identity-service/
│   ├── organization-service/
│   ├── experiment-service/
│   ├── metrics-service/
│   ├── reporting-service/
│   └── notification-service/
├── frontend/               # Angular application
├── infrastructure/         # Terraform IaC
├── libs/                   # Shared libraries
│   ├── domain-model/
│   ├── event-models/
│   └── shared-utils/
├── docs/                   # Documentation
├── specs/                  # Technical specifications
└── tasks/                  # Implementation tasks
```

### CODEOWNERS Configuration

**File**: `.github/CODEOWNERS`

```
# Global owners
* @ryanwaite28

# Infrastructure requires DevOps approval
/infrastructure/** @ryanwaite28 @devops-team

# CI/CD workflows require senior approval
/.github/workflows/** @ryanwaite28 @senior-engineers

# Backend services
/services/** @ryanwaite28 @backend-team

# Frontend
/frontend/** @ryanwaite28 @frontend-team

# Security-sensitive files
/services/identity-service/** @ryanwaite28 @security-team
```

### Path-Based Workflow Triggers

**Selective CI/CD Execution**:

```yaml
on:
  push:
    paths:
      - 'services/identity-service/**'
      - 'libs/domain-model/**'
      - '.github/workflows/ci.yml'
```

**Service-Specific Workflows**:
- Changes to `services/identity-service/` trigger identity-service build
- Changes to `infrastructure/` trigger Terraform validation
- Changes to `frontend/` trigger Angular build and tests
- Changes to shared `libs/` trigger all dependent service builds

### Versioning Strategy

**Monorepo Versioning**:
- Single version number for entire platform
- Semantic versioning: `MAJOR.MINOR.PATCH`
- Version tracked in root `package.json` or `VERSION` file
- All services deployed together with same version

**Individual Service Versions**:
- Services maintain internal version for compatibility
- Docker images tagged with both platform version and commit SHA
- Example: `turaf/identity-service:v1.2.3` and `turaf/identity-service:abc1234`

---

## Release Management

### Semantic Versioning Strategy

**Version Format**: `vMAJOR.MINOR.PATCH`

**Version Increment Rules**:
- **MAJOR**: Breaking API changes, database schema breaking changes
- **MINOR**: New features, backward-compatible API changes
- **PATCH**: Bug fixes, security patches, performance improvements

**Examples**:
- `v1.0.0` - Initial production release
- `v1.1.0` - New experiment workflow feature
- `v1.1.1` - Bug fix for report generation
- `v2.0.0` - Breaking change to event schema

### Release Branch Workflow

**Creating a Release**:

1. **Branch Creation**:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b release/v1.2.0
   ```

2. **Version Bump**:
   - Update version in `VERSION` file
   - Update `package.json` versions
   - Update `pom.xml` versions for Java services
   - Commit: `chore: bump version to v1.2.0`

3. **Release Preparation**:
   - Run full test suite
   - Update CHANGELOG.md
   - Update documentation
   - Fix any release-blocking bugs

4. **Deployment to QA**:
   - Push release branch to GitHub
   - Automatic deployment to QA environment
   - QA team validates release

5. **Finalize Release**:
   ```bash
   git checkout main
   git merge --no-ff release/v1.2.0
   git tag -a v1.2.0 -m "Release version 1.2.0"
   git push origin main --tags
   ```

6. **Backmerge to Develop**:
   ```bash
   git checkout develop
   git merge --no-ff release/v1.2.0
   git push origin develop
   ```

### Changelog Generation

**CHANGELOG.md Format**:

```markdown
# Changelog

## [1.2.0] - 2024-03-15

### Added
- New experiment metrics dashboard
- Email notifications for experiment completion
- Export reports to PDF

### Changed
- Improved report generation performance by 40%
- Updated Angular to v17

### Fixed
- Fixed bug in hypothesis validation
- Corrected timezone handling in metrics

### Security
- Updated dependencies with security vulnerabilities
- Enhanced JWT token validation
```

**Automated Changelog**:
- Use conventional commits for automatic generation
- GitHub Actions workflow generates changelog from commits
- Categorizes by commit type (feat, fix, chore, etc.)

### Tagging Strategy

**Tag Naming**:
- Production releases: `v1.2.3`
- Release candidates: `v1.2.3-rc.1`
- Beta releases: `v1.2.3-beta.1`
- Hotfixes: `v1.2.4` (patch increment)

**Tag Annotations**:
```bash
git tag -a v1.2.0 -m "Release v1.2.0

Features:
- Experiment metrics dashboard
- Email notifications

Bug Fixes:
- Hypothesis validation
- Timezone handling
"
```

### Deployment Artifact Management

**Artifact Storage**:
- Docker images stored in Amazon ECR
- Build artifacts stored in S3 bucket `turaf-artifacts-{env}`
- Terraform state in S3 with versioning enabled
- Release notes stored in GitHub Releases

**Artifact Retention**:
- **DEV**: Last 10 images, 30-day artifact retention
- **QA**: Last 20 images, 60-day artifact retention
- **PROD**: All tagged releases, indefinite retention

**Artifact Promotion**:
- DEV images tagged: `dev-latest`, `dev-{commit-sha}`
- QA images tagged: `qa-latest`, `qa-{commit-sha}`, `v{version}-rc.{n}`
- PROD images tagged: `prod-latest`, `prod-{commit-sha}`, `v{version}`

### Hotfix Workflow

**Critical Production Issues**:

1. **Create Hotfix Branch**:
   ```bash
   git checkout main
   git checkout -b hotfix/v1.2.1
   ```

2. **Implement Fix**:
   - Make minimal changes to fix critical issue
   - Add regression test
   - Update version to patch increment

3. **Fast-Track Deployment**:
   - Deploy directly to PROD (with approval)
   - Skip QA environment for critical fixes
   - Monitor closely post-deployment

4. **Backmerge**:
   ```bash
   git checkout main
   git merge --no-ff hotfix/v1.2.1
   git tag v1.2.1
   git checkout develop
   git merge --no-ff hotfix/v1.2.1
   ```

---

## Best Practices Summary

### Development Workflow
1. Create feature branch from `develop`
2. Implement feature with tests
3. Open PR with descriptive title and description
4. Address code review feedback
5. Merge to `develop` after approval and CI passes
6. Automatic deployment to DEV environment

### Release Workflow
1. Create release branch from `develop`
2. Bump version and update changelog
3. Deploy to QA for validation
4. Merge to `main` after QA approval
5. Tag release with version number
6. Deploy to PROD with manual approval
7. Backmerge to `develop`

### Security Practices
1. Never commit secrets or credentials
2. Use GitHub Secrets for sensitive data
3. Rotate secrets every 90 days
4. Enable all security scanning tools
5. Review and address security alerts promptly
6. Use OIDC for AWS authentication (no long-lived keys)

### Code Quality
1. Write tests for all new features
2. Maintain >80% code coverage
3. Pass SonarQube quality gates
4. Follow language-specific style guides
5. Document public APIs and complex logic
6. Keep PRs focused and reasonably sized

---

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [AWS OIDC with GitHub Actions](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [Semantic Versioning](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [IAM Roles Reference](docs/IAM_ROLES.md) - Authoritative source for all IAM role names and ARNs
- PROJECT.md - Overall project specifications
- AWS_ACCOUNTS.md - AWS account details and organization structure

