# /run-tests — Run Tests for a Service

Arguments: `$ARGUMENTS` (service name or path, e.g. `bff-api`, `common`, `architecture-tests`)

Run tests for: **$ARGUMENTS**

## Steps

1. Determine the service directory: `services/$ARGUMENTS`
2. Run the appropriate test commands:

**Unit tests only** (no Docker required):
```bash
cd services/$ARGUMENTS && mvn test -Dtest="!*IntegrationTest" -Dtest="!*ArchTest"
```

**Integration tests** (requires Docker + running containers):
```bash
cd services/$ARGUMENTS && mvn test -Dtest="*IntegrationTest"
```

**Architecture tests** (requires all services running via docker-compose):
```bash
cd services/architecture-tests && mvn test
```

3. Parse the test output and report:
   - Total tests run / passed / failed / skipped
   - For each failure: test name, failure message, and file:line reference
   - Coverage summary if JaCoCo output is available

4. For each failure, diagnose the root cause:
   - Is it a compilation error? → show the error
   - Is it an assertion failure? → show expected vs actual
   - Is it an integration/connection error? → check if required services are running
   - Is it a missing bean/config error? → identify the misconfigured component

5. If there are failures, suggest targeted fixes without implementing them (unless asked).

## Notes

- Do NOT run `docker-compose up` automatically — check if services are running first with `docker ps`
- Integration tests require MiniStack (LocalStack alternative) to be running at port 4566
- Architecture tests require ALL services healthy at their respective ports
