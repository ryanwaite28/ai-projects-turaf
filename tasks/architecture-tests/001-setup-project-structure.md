# Task 001: Setup Project Structure

**Status**: Pending  
**Assignee**: TBD  
**Estimated Time**: 2 hours  
**Related Spec**: [Architecture Testing](../../specs/architecture-testing.md)

---

## Objective

Create the architecture-tests service directory structure with Maven configuration and basic Karate framework setup.

---

## Prerequisites

- Java 17 installed
- Maven 3.8+ installed
- Understanding of Karate framework basics

---

## Tasks

### 1. Create Service Directory Structure

```bash
cd services
mkdir -p architecture-tests/src/test/java/com/turaf/architecture
mkdir -p architecture-tests/src/test/resources/features
mkdir -p architecture-tests/terraform
```

### 2. Create Maven POM File

Create `services/architecture-tests/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.turaf</groupId>
    <artifactId>architecture-tests</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Turaf Architecture Tests</name>
    <description>Full-system architecture tests using Karate framework</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <karate.version>1.4.1</karate.version>
        <awaitility.version>4.2.0</awaitility.version>
        <aws.sdk.version>2.20.0</aws.sdk.version>
    </properties>

    <dependencies>
        <!-- Karate Framework -->
        <dependency>
            <groupId>com.intuit.karate</groupId>
            <artifactId>karate-junit5</artifactId>
            <version>${karate.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- AWS SDK for validation -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>${aws.sdk.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>sqs</artifactId>
            <version>${aws.sdk.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>eventbridge</artifactId>
            <version>${aws.sdk.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Awaitility for waiting -->
        <dependency>
            <groupId>org.awaitility</groupId>
            <artifactId>awaitility</artifactId>
            <version>${awaitility.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.11</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <testResources>
            <testResource>
                <directory>src/test/resources</directory>
                <excludes>
                    <exclude>**/*.java</exclude>
                </excludes>
            </testResource>
        </testResources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
                <configuration>
                    <includes>
                        <include>**/*TestRunner.java</include>
                    </includes>
                    <systemProperties>
                        <karate.env>${karate.env}</karate.env>
                    </systemProperties>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. Create .gitignore

Create `services/architecture-tests/.gitignore`:

```
target/
*.class
*.log
*.iml
.idea/
.settings/
.classpath
.project
```

### 4. Create README

Create `services/architecture-tests/README.md`:

```markdown
# Architecture Tests

Full-system architecture tests for the Turaf platform using Karate framework.

## Overview

These tests validate complete system integration from entry points (BFF API, WebSocket Gateway) through all downstream services, event-driven processes, and data persistence.

## Prerequisites

- Java 17
- Maven 3.8+
- Docker (for local testing)
- AWS credentials (for deployed environment testing)

## Running Tests

### Local (Docker Compose)
```bash
mvn test -Dkarate.env=local
```

### DEV Environment
```bash
mvn test -Dkarate.env=dev
```

### QA Environment
```bash
mvn test -Dkarate.env=qa
```

### Specific Test Suite
```bash
mvn test -Dtest=ExperimentWorkflowTestRunner -Dkarate.env=local
```

## Test Reports

HTML reports are generated in `target/karate-reports/karate-summary.html`

## Documentation

See [Architecture Testing Specification](../../specs/architecture-testing.md)
```

---

## Acceptance Criteria

- [ ] Directory structure created
- [ ] Maven POM file configured with all dependencies
- [ ] .gitignore file created
- [ ] README.md created with usage instructions
- [ ] Project builds successfully: `mvn clean compile`
- [ ] No compilation errors

---

## Verification

```bash
cd services/architecture-tests
mvn clean compile
# Should complete without errors
```

---

## Notes

- Karate version 1.4.1 is the latest stable release
- AWS SDK v2 is used for modern API support
- Awaitility provides robust waiting/polling capabilities
