# Task: Setup Security Scanning

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 2 hours  

## Objective

Setup security scanning for dependencies, containers, and code vulnerabilities.

## Prerequisites

- [x] Task 001: CI pipeline setup

## Scope

**Files to Create**:
- `.github/workflows/security.yml`

## Implementation Details

### Security Workflow

```yaml
name: Security Scan

on:
  schedule:
    - cron: '0 0 * * *'  # Daily
  pull_request:
  push:
    branches: [main]

jobs:
  dependency-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Snyk
        uses: snyk/actions/maven@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
  
  container-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Trivy
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          severity: 'CRITICAL,HIGH'
  
  code-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v2
      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v2
```

## Acceptance Criteria

- [ ] Security workflow created
- [ ] Dependency scanning works
- [ ] Container scanning works
- [ ] Code scanning works
- [ ] Vulnerabilities reported
- [ ] Daily scans scheduled

## References

- Specification: `specs/ci-cd-pipelines.md` (Security Scanning section)
- Related Tasks: 007-configure-aws-oidc
