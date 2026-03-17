# Task: Setup CI Pipeline

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 3 hours  

## Objective

Setup GitHub Actions CI pipeline for linting, testing, building, and code quality checks.

## Prerequisites

- [ ] GitHub repository created
- [ ] Understanding of GitHub Actions

## Scope

**Files to Create**:
- `.github/workflows/ci.yml`

## Implementation Details

### CI Workflow

```yaml
name: CI

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main, develop]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Lint Java code
        run: mvn checkstyle:check
        working-directory: ./services

  test:
    runs-on: ubuntu-latest
    needs: lint
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: mvn test
        working-directory: ./services
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./services/target/site/jacoco/jacoco.xml

  build:
    runs-on: ubuntu-latest
    needs: test
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean package -DskipTests
        working-directory: ./services

  security-scan:
    runs-on: ubuntu-latest
    needs: lint
    steps:
      - uses: actions/checkout@v3
      - name: Run Snyk security scan
        uses: snyk/actions/maven@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        with:
          args: --severity-threshold=high
```

## Acceptance Criteria

- [ ] CI workflow created
- [ ] Linting runs on PRs
- [ ] Tests run on PRs
- [ ] Build succeeds
- [ ] Code coverage reported
- [ ] Security scanning enabled
- [ ] Workflow triggers correctly

## Testing Requirements

**Validation**:
- Create test PR
- Verify all jobs run
- Check status checks

## References

- Specification: `specs/ci-cd-pipelines.md` (CI Pipeline section)
- Related Tasks: 002-setup-cd-dev-pipeline
