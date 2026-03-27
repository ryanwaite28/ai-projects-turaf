#!/bin/bash

# Turaf Local Development Setup Script
# This script sets up the local development environment

set -e

echo "🚀 Turaf Local Development Setup"
echo "=================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
echo "📋 Checking prerequisites..."

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    echo "Please install Docker from https://www.docker.com/get-started"
    exit 1
fi
echo -e "${GREEN}✅ Docker found${NC}"

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    echo "Please install Docker Compose"
    exit 1
fi
echo -e "${GREEN}✅ Docker Compose found${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed${NC}"
    echo "Please install Java 17 or later"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java 17 or later is required (found Java $JAVA_VERSION)${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java $JAVA_VERSION found${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed${NC}"
    echo "Please install Maven 3.8 or later"
    exit 1
fi
echo -e "${GREEN}✅ Maven found${NC}"

echo ""
echo "📦 Building services..."

# Build all services
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Build successful${NC}"
echo ""

echo "🐳 Starting Docker infrastructure..."

# Start infrastructure services first
docker-compose up -d postgres localstack redis

echo "⏳ Waiting for infrastructure to be ready..."
sleep 10

# Wait for PostgreSQL
echo "Waiting for PostgreSQL..."
until docker-compose exec -T postgres pg_isready -U turaf_admin -d turaf > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}✅ PostgreSQL is ready${NC}"

# Wait for LocalStack
echo "Waiting for LocalStack..."
until curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}✅ LocalStack is ready${NC}"

echo ""
echo "🚀 Starting application services..."

# Start all application services
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 15

# Check service health
check_health() {
    local service=$1
    local port=$2
    local max_attempts=30
    local attempt=0
    
    echo -n "Checking $service (port $port)..."
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo -e " ${GREEN}✅${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo -e " ${RED}❌ (timeout)${NC}"
    return 1
}

# Check each service
check_health "Identity Service" 8081
check_health "Organization Service" 8082
check_health "Experiment Service" 8083
check_health "Metrics Service" 8084
check_health "BFF API" 8080

echo ""
echo "=================================="
echo -e "${GREEN}✅ Setup Complete!${NC}"
echo "=================================="
echo ""
echo "📍 Service URLs:"
echo "   BFF API Gateway:      http://localhost:8080"
echo "   Identity Service:     http://localhost:8081"
echo "   Organization Service: http://localhost:8082"
echo "   Experiment Service:   http://localhost:8083"
echo "   Metrics Service:      http://localhost:8084"
echo "   WebSocket Gateway:    ws://localhost:3000"
echo ""
echo "🔧 Management Tools:"
echo "   LocalStack:           http://localhost:4566"
echo "   PgAdmin (optional):   http://localhost:5050"
echo ""
echo "📊 Health Checks:"
echo "   curl http://localhost:8080/actuator/health"
echo ""
echo "📝 Useful Commands:"
echo "   View logs:            docker-compose logs -f [service-name]"
echo "   Stop services:        docker-compose down"
echo "   Restart service:      docker-compose restart [service-name]"
echo ""
echo "Happy coding! 🎉"
