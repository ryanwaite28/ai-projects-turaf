# Task: Setup Communications Service Project Structure

**Service**: Communications Service  
**Type**: Spring Boot Microservice  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: None

---

## Objective

Initialize the Maven project structure for the Communications Service with all necessary dependencies, configuration files, and directory layout following Clean Architecture principles.

---

## Acceptance Criteria

- [x] Maven project created with correct groupId and artifactId
- [x] `pom.xml` includes all required dependencies
- [x] Clean Architecture directory structure created
- [x] `application.yml` configuration file created
- [x] Docker support files created (Dockerfile, .dockerignore)
- [x] Project builds successfully with `mvn clean install`

---

## Implementation Steps

### 1. Create Maven Project

**Directory**: `services/communications-service/`

**pom.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.turaf</groupId>
    <artifactId>communications-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>Communications Service</name>
    <description>Real-time messaging service for Turaf platform</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- AWS SDK -->
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter-sqs</artifactId>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>eventbridge</artifactId>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.awspring.cloud</groupId>
                <artifactId>spring-cloud-aws-dependencies</artifactId>
                <version>3.1.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>bom</artifactId>
                <version>2.20.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 2. Create Directory Structure

```
services/communications-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── turaf/
│   │   │           └── communications/
│   │   │               ├── CommunicationsServiceApplication.java
│   │   │               ├── domain/
│   │   │               │   ├── model/
│   │   │               │   ├── event/
│   │   │               │   ├── exception/
│   │   │               │   └── repository/
│   │   │               ├── application/
│   │   │               │   ├── service/
│   │   │               │   └── dto/
│   │   │               ├── infrastructure/
│   │   │               │   ├── persistence/
│   │   │               │   ├── messaging/
│   │   │               │   └── config/
│   │   │               └── interfaces/
│   │   │                   ├── rest/
│   │   │                   └── mapper/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── migration/
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── turaf/
│       │           └── communications/
│       └── resources/
│           └── application-test.yml
├── Dockerfile
├── .dockerignore
└── README.md
```

---

### 3. Create Main Application Class

**File**: `src/main/java/com/turaf/communications/CommunicationsServiceApplication.java`

```java
package com.turaf.communications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class CommunicationsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationsServiceApplication.class, args);
    }
}
```

---

### 4. Create Configuration Files

**File**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: communications-service
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:turaf}
    username: ${DB_USER:communications_user}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: communications_schema
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    schemas: communications_schema
    locations: classpath:db/migration

server:
  port: 8084
  servlet:
    context-path: /communications

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

aws:
  region: ${AWS_REGION:us-east-1}
  sqs:
    direct-messages-queue: ${SQS_DIRECT_QUEUE:communications-direct-messages.fifo}
    group-messages-queue: ${SQS_GROUP_QUEUE:communications-group-messages.fifo}
  eventbridge:
    bus-name: ${EVENTBRIDGE_BUS:turaf-event-bus}

logging:
  level:
    com.turaf.communications: INFO
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

**File**: `src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/turaf
  jpa:
    show-sql: true

aws:
  region: us-east-1
  sqs:
    endpoint: http://localhost:4566
  eventbridge:
    endpoint: http://localhost:4566

logging:
  level:
    com.turaf.communications: DEBUG
```

**File**: `src/main/resources/application-prod.yml`

```yaml
spring:
  jpa:
    show-sql: false

logging:
  level:
    com.turaf.communications: INFO
    org.hibernate.SQL: WARN
```

**File**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        default_schema: communications_schema
  flyway:
    enabled: false
```

---

### 5. Create Dockerfile

**File**: `Dockerfile`

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/communications-service-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**File**: `.dockerignore`

```
target/
.mvn/
*.iml
.idea/
.git/
.gitignore
README.md
```

---

### 6. Create README

**File**: `README.md`

```markdown
# Communications Service

Real-time messaging service for the Turaf platform.

## Features

- Direct messaging (1-on-1)
- Group chat conversations
- Unread message tracking
- Message persistence
- Event-driven architecture

## Tech Stack

- Java 17
- Spring Boot 3.2
- PostgreSQL
- AWS SQS (FIFO)
- AWS EventBridge
- Flyway

## Build

```bash
mvn clean install
```

## Run Locally

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Docker Build

```bash
mvn clean package
docker build -t turaf/communications-service:latest .
```

## Environment Variables

- `DB_HOST`: PostgreSQL host
- `DB_PORT`: PostgreSQL port
- `DB_NAME`: Database name
- `DB_USER`: Database user
- `DB_PASSWORD`: Database password
- `AWS_REGION`: AWS region
- `SQS_DIRECT_QUEUE`: Direct messages queue name
- `SQS_GROUP_QUEUE`: Group messages queue name
- `EVENTBRIDGE_BUS`: EventBridge bus name
```

---

## Verification

1. Build the project:
   ```bash
   cd services/communications-service
   mvn clean install
   ```

2. Verify build success (no errors)

3. Check directory structure exists

4. Verify all configuration files are present

---

## References

- **Spec**: `specs/communications-service.md`
- **PROJECT.md**: Section 11 (Service Boundaries)
- **Example**: `services/experiment-service/` (similar structure)

---

## Notes

- This task only sets up the project structure
- No business logic implementation yet
- Database migrations will be added in task 010
- Follow existing service patterns from experiment-service
