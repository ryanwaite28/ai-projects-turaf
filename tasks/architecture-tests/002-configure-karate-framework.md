# Task 002: Configure Karate Framework

**Status**: ✅ Completed  
**Assignee**: AI Assistant  
**Estimated Time**: 3 hours  
**Actual Time**: < 1 hour  
**Completed**: 2026-03-31  
**Related Spec**: [Architecture Testing](../../specs/architecture-testing.md)

---

## Objective

Configure Karate framework with environment-specific settings and create base configuration files.

---

## Prerequisites

- Task 001 completed
- Understanding of Karate configuration

---

## Tasks

### 1. Create karate-config.js

Create `src/test/resources/karate-config.js`:

```javascript
function fn() {
  var env = karate.env || 'local';
  karate.log('karate.env system property was:', env);
  
  var config = {
    baseUrl: 'http://localhost:8080',
    wsUrl: 'ws://localhost:8081',
    waitTimeout: 30000,
    pollInterval: 1000,
    awsRegion: 'us-east-1'
  };
  
  if (env === 'dev') {
    config.baseUrl = 'https://api.dev.turafapp.com';
    config.wsUrl = 'wss://ws.dev.turafapp.com';
    config.awsAccountId = '801651112319';
  } else if (env === 'qa') {
    config.baseUrl = 'https://api.qa.turafapp.com';
    config.wsUrl = 'wss://ws.qa.turafapp.com';
    config.awsAccountId = '965932217544';
  } else if (env === 'prod') {
    config.baseUrl = 'https://api.turafapp.com';
    config.wsUrl = 'wss://ws.turafapp.com';
    config.awsAccountId = '811783768245';
  }
  
  karate.configure('connectTimeout', 10000);
  karate.configure('readTimeout', 30000);
  
  return config;
}
```

### 2. Create Environment Properties Files

Create directory and files:
```bash
mkdir -p src/test/resources/environments
```

**local.properties**:
```properties
aws.region=us-east-1
aws.endpoint.override=http://localhost:4566
test.data.cleanup=true
parallel.threads=1
```

**dev.properties**:
```properties
aws.region=us-east-1
aws.account.id=801651112319
test.data.cleanup=true
parallel.threads=4
```

**qa.properties**:
```properties
aws.region=us-east-1
aws.account.id=965932217544
test.data.cleanup=true
parallel.threads=4
```

**prod.properties**:
```properties
aws.region=us-east-1
aws.account.id=811783768245
test.data.cleanup=false
parallel.threads=2
smoke.tests.only=true
```

### 3. Create Test Configuration Class

Create `src/test/java/com/turaf/architecture/config/TestConfig.java`:

```java
package com.turaf.architecture.config;

public class TestConfig {
    
    private static final String ENV = System.getProperty("karate.env", "local");
    
    public static String getEnvironment() {
        return ENV;
    }
    
    public static boolean isLocal() {
        return "local".equals(ENV);
    }
    
    public static boolean isDev() {
        return "dev".equals(ENV);
    }
    
    public static boolean isQa() {
        return "qa".equals(ENV);
    }
    
    public static boolean isProd() {
        return "prod".equals(ENV);
    }
}
```

### 4. Create Base Test Runner

Create `src/test/java/com/turaf/architecture/config/KarateTestRunner.java`:

```java
package com.turaf.architecture.config;

import com.intuit.karate.junit5.Karate;

public class KarateTestRunner {
    
    @Karate.Test
    Karate testAll() {
        return Karate.run().relativeTo(getClass());
    }
}
```

### 5. Create Logback Configuration

Create `src/test/resources/logback-test.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.intuit.karate" level="INFO"/>
    <logger name="com.turaf.architecture" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

---

## Acceptance Criteria

- [x] karate-config.js created with environment configurations
- [x] Environment properties files created for all environments
- [x] TestConfig class created
- [x] Base test runner created
- [x] Logback configuration created
- [x] Configuration loads correctly for each environment
- [x] Test runner executes successfully (expected failure: no feature files yet)

---

## Verification

```bash
# Test local configuration
mvn test -Dkarate.env=local

# Test dev configuration
mvn test -Dkarate.env=dev

# Should complete without errors (no tests yet)
```

---

## Notes

- Environment is set via -Dkarate.env system property
- Default environment is 'local'
- Configuration is loaded once at startup
