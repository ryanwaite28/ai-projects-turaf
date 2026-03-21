# Multi-stage Dockerfile for building Spring Boot services
# Usage: docker build --build-arg SERVICE_NAME=identity-service -t turaf-identity-service .

ARG SERVICE_NAME

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build

ARG SERVICE_NAME
WORKDIR /build

# Copy parent POM
COPY pom.xml ./

# Copy common module (shared dependency)
COPY common/pom.xml ./common/
COPY common/src ./common/src

# Copy service-specific files
COPY services/${SERVICE_NAME}/pom.xml ./services/${SERVICE_NAME}/
COPY services/${SERVICE_NAME}/src ./services/${SERVICE_NAME}/src

# Build the service
WORKDIR /build/services/${SERVICE_NAME}
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /build/services/${SERVICE_NAME}/target/*.jar app.jar

# Add curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 spring && \
    adduser -D -u 1001 -G spring spring && \
    chown -R spring:spring /app

USER spring

# Expose port (will be overridden by service-specific port)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
