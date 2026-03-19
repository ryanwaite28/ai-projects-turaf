# Task: Setup BFF API Project

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 2-3 hours  

## Objective

Setup Spring Boot project structure for the BFF API service following Clean Architecture pattern.

## Prerequisites

- [ ] Java 17 installed
- [ ] Maven configured
- [ ] Internal ALB infrastructure planned
- [ ] All microservices (Identity, Organization, Experiment, Metrics) implemented

## Scope

**Files to Create**:
- `services/bff-api/pom.xml`
- `services/bff-api/src/main/java/com/turaf/bff/BffApiApplication.java`
- `services/bff-api/src/main/resources/application.yml`
- `services/bff-api/src/main/resources/application-dev.yml`
- `services/bff-api/src/main/resources/application-qa.yml`
- `services/bff-api/src/main/resources/application-prod.yml`
- `services/bff-api/README.md`

## Implementation Details

### Maven POM (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.turaf</groupId>
        <artifactId>turaf-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>bff-api</artifactId>
    <name>BFF API</name>
    <description>Backend for Frontend API Service</description>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot WebFlux (for WebClient) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Spring Boot Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Resilience4j -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.1.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-circuitbreaker</artifactId>
            <version>2.1.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-ratelimiter</artifactId>
            <version>2.1.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-retry</artifactId>
            <version>2.1.0</version>
        </dependency>

        <!-- Micrometer (Metrics) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>mockwebserver</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### Application Main Class

```java
package com.turaf.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BffApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BffApiApplication.class, args);
    }
}
```

### Application Configuration (application.yml)

```yaml
spring:
  application:
    name: bff-api
  
server:
  port: 8080
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.turaf.bff: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

# Internal ALB URL (overridden per environment)
internal:
  alb:
    url: http://localhost:8080

# CORS (overridden per environment)
cors:
  allowed-origins: http://localhost:4200

# JWT Configuration
jwt:
  secret-key: ${JWT_SECRET_KEY}
  expiration: 86400000 # 24 hours

# Resilience4j defaults
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
  
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - org.springframework.web.client.ResourceAccessException
          - java.net.SocketTimeoutException
  
  ratelimiter:
    configs:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 1m
        timeoutDuration: 5s
```

### Environment-Specific Configuration

**application-dev.yml**:
```yaml
internal:
  alb:
    url: http://internal-alb.dev.turafapp.com

cors:
  allowed-origins: http://localhost:4200,https://app.dev.turafapp.com

logging:
  level:
    com.turaf.bff: DEBUG
```

**application-qa.yml**:
```yaml
internal:
  alb:
    url: http://internal-alb.qa.turafapp.com

cors:
  allowed-origins: https://app.qa.turafapp.com
```

**application-prod.yml**:
```yaml
internal:
  alb:
    url: http://internal-alb.prod.turafapp.com

cors:
  allowed-origins: https://app.turafapp.com

logging:
  level:
    com.turaf.bff: WARN
```

### Directory Structure

```
services/bff-api/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/turaf/bff/
│   │   │   ├── BffApiApplication.java
│   │   │   ├── config/
│   │   │   ├── controllers/
│   │   │   ├── clients/
│   │   │   ├── security/
│   │   │   ├── dto/
│   │   │   └── exception/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-qa.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/turaf/bff/
└── target/
```

## Acceptance Criteria

- [x] Maven project created with correct parent POM reference
- [x] All required dependencies added to pom.xml
- [x] BffApiApplication.java created with @SpringBootApplication
- [x] application.yml configured with defaults
- [x] Environment-specific configurations created (dev, qa, prod)
- [x] Directory structure follows Clean Architecture pattern
- [x] README.md created with service overview
- [ ] Maven build succeeds: `mvn clean install`
- [ ] Application starts successfully: `mvn spring-boot:run`
- [ ] Actuator health endpoint accessible: `http://localhost:8080/actuator/health`

## Testing Requirements

**Verification**:
- Run `mvn clean install` from bff-api directory
- Verify build succeeds with no errors
- Run `mvn spring-boot:run`
- Verify application starts on port 8080
- Test health endpoint: `curl http://localhost:8080/actuator/health`
- Verify response: `{"status":"UP"}`

## References

- Specification: `specs/bff-api.md`
- PROJECT.md: Section 40 (BFF API Service)
- Parent POM: `services/pom.xml`
