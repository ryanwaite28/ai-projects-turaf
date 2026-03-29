# Migrate from LocalStack to MiniStack

**Date**: 2026-03-27  
**Type**: Infrastructure Change  
**Impact**: Local development environment, CI/CD integration tests

## Summary

Replaced LocalStack with [MiniStack](https://github.com/Nahuel990/ministack) (`nahuelnucera/ministack`) as the local AWS service emulator.

## Rationale

- LocalStack moved core services behind a paid plan
- MiniStack is MIT licensed with all services free (no feature gates)
- Smaller footprint: ~150MB image vs ~1GB for LocalStack
- Faster startup: under 2 seconds
- Drop-in compatible: same port (4566), same health endpoint, same AWS SDK compatibility
- **EventBridge is now fully emulated** — no longer needs `@MockBean` in integration tests

## Changes

### docker-compose.yml
- Renamed `localstack` service to `ministack`
- Updated image from `localstack/localstack:latest` to `nahuelnucera/ministack`
- Simplified environment variables
- Updated all service `AWS_ENDPOINT` values from `http://localstack:4566` to `http://ministack:4566`
- Updated all `depends_on` references from `localstack` to `ministack`

### New Files
- `infrastructure/docker/ministack/init-aws.sh` — Init script using `aws --endpoint-url` instead of `awslocal`

### Documentation Updated
- `PROJECT.md` — Testing strategy: EventBridge moved from mocked to emulated services
- `.windsurf/workflows/project.md` — Updated testing guidance
- `.windsurf/rules/rules.md` — Updated integration test rules
- `docs/LOCAL_DEVELOPMENT.md` — Full LocalStack → MiniStack rename
- `.env.example` — Renamed config section, replaced `LOCALSTACK_DEBUG` with `MINISTACK_LOG_LEVEL`
- `scripts/local-dev-setup.sh` — Updated references
- `scripts/integration-test.sh` — Updated references

### No Changes Needed
- Spring Boot `EventBridgeConfig.java` files — already use `aws.endpoint` property from env vars
- `application-docker.yml` files — already use `${AWS_ENDPOINT:}` placeholder
