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
