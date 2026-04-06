# Dockerfile for Flyway Migration Service
FROM flyway/flyway:10-alpine

# Copy migration scripts
COPY migrations /flyway/sql

# Set working directory
WORKDIR /flyway

# Flyway will be executed via command line with environment-specific configs
# Example: flyway -url=jdbc:postgresql://... -user=... -password=... migrate
