# Task: Setup Lambda Project

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Setup AWS Lambda project structure for the Reporting Service using Java and Maven.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] AWS Lambda Java runtime knowledge

## Scope

**Files to Create**:
- `services/reporting-service/pom.xml`
- `services/reporting-service/src/main/java/com/turaf/reporting/ReportingHandler.java`
- `services/reporting-service/src/main/resources/application.properties`

## Implementation Details

### Maven POM

```xml
<project>
    <artifactId>reporting-service</artifactId>
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
            <artifactId>s3</artifactId>
        </dependency>
        <dependency>
            <groupId>com.itextpdf</groupId>
            <artifactId>itext7-core</artifactId>
            <version>7.2.5</version>
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

### Lambda Handler

```java
public class ReportingHandler implements RequestHandler<EventBridgeEvent, Void> {
    
    @Override
    public Void handleRequest(EventBridgeEvent event, Context context) {
        context.getLogger().log("Received event: " + event);
        return null;
    }
}
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
- `ReportingHandlerTest.java`

## References

- Specification: `specs/reporting-service.md` (Lambda Configuration section)
- Related Tasks: 002-implement-event-handler
