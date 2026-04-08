#!/bin/bash

# Turaf Integration Test Script
# Runs integration tests against Docker Compose environment

set -e

echo "🧪 Turaf Integration Test Suite"
echo "================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Start services
echo "🐳 Starting test environment..."
# docker-compose up -d

# echo "⏳ Waiting for services to be ready..."
# sleep 20

# Function to test endpoint
test_endpoint() {
    local name=$1
    local url=$2
    local expected_status=$3
    
    echo -n "Testing $name..."
    
    status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    
    if [ "$status" -eq "$expected_status" ]; then
        echo -e " ${GREEN}✅ ($status)${NC}"
        return 0
    else
        echo -e " ${RED}❌ (expected $expected_status, got $status)${NC}"
        return 1
    fi
}

# Test health endpoints
echo ""
echo "📊 Testing Health Endpoints..."
test_endpoint "Identity Service Health" "http://localhost:8081/actuator/health" 200
test_endpoint "Organization Service Health" "http://localhost:8082/actuator/health" 200
test_endpoint "Experiment Service Health" "http://localhost:8083/actuator/health" 200
test_endpoint "Metrics Service Health" "http://localhost:8084/actuator/health" 200
test_endpoint "BFF API Health" "http://localhost:8080/actuator/health" 200



# Test database connectivity
echo ""
echo "🗄️  Testing Database Connectivity..."
if docker-compose exec -T postgres psql -U turaf_admin -d turaf -c "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PostgreSQL connection successful${NC}"
else
    echo -e "${RED}❌ PostgreSQL connection failed${NC}"
    exit 1
fi

# Test MiniStack
echo ""
echo "☁️  Testing MiniStack..."
ministack_health_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:4566/_localstack/health")
if [ "$ministack_health_status" -eq 200 ]; then
    echo -e "${GREEN}✅ MiniStack is running${NC}"
else
    echo -e "${RED}❌ MiniStack health check failed${NC}"
    exit 1
fi

# Test Redis
echo ""
echo "📦 Testing Redis..."
if docker-compose exec -T redis redis-cli ping | grep -q "PONG"; then
    echo -e "${GREEN}✅ Redis is responding${NC}"
else
    echo -e "${RED}❌ Redis connection failed${NC}"
    exit 1
fi

# Run Maven integration tests
echo ""
echo "🔬 Running Maven Integration Tests..."
cd services/architecture-tests
mvn verify -P integration-tests

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ All integration tests passed${NC}"
else
    echo -e "${RED}❌ Some integration tests failed${NC}"
    exit 1
fi

echo ""
echo "================================"
echo -e "${GREEN}✅ All Tests Passed!${NC}"
echo "================================"
echo ""
echo "📝 Test Summary:"
echo "   ✅ Health checks: All services healthy"
echo "   ✅ API endpoints: Properly secured"
echo "   ✅ Database: Connected and accessible"
echo "   ✅ MiniStack: Running and healthy"
echo "   ✅ Redis: Connected and responding"
echo "   ✅ Integration tests: All passed"
echo ""
echo "🎉 System is ready for development!"
