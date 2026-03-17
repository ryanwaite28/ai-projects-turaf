# Task: Setup Lambda Project

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 2 hours  

## Objective

Setup AWS Lambda project structure for the Notification Service using Java and Maven.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] AWS Lambda Java runtime knowledge

## Scope

**Files to Create**:
- `services/notification-service/pom.xml`
- `services/notification-service/src/main/java/com/turaf/notification/NotificationHandler.java`
- `services/notification-service/src/main/resources/application.properties`

## Implementation Details

### Maven POM

```xml
<project>
    <artifactId>notification-service</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-core</artifactId>
            <version>1.2.2</version>
        </dependency>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-events</artifactId>
            <version>3.11.0</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>ses</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.jknack</groupId>
            <artifactId>handlebars</artifactId>
            <version>4.3.1</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.4.1</version>
                <configuration>
                    <createDependencyReducedPom>false</createDependencyReducedPom>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## Acceptance Criteria

- [ ] Maven project configured
- [ ] Lambda dependencies added
- [ ] Shade plugin configured
- [ ] Handler class created
- [ ] Project builds successfully

## Testing Requirements

**Unit Tests**:
- Test handler initialization

**Test Files to Create**:
- `NotificationHandlerTest.java`

## References

- Specification: `specs/notification-service.md` (Lambda Configuration section)
- Related Tasks: 002-implement-event-handlers
