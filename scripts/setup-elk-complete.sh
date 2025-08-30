#!/bin/bash

# Complete ELK Stack Setup Script for Ecommerce Microservices

set -e

echo "🚀 Setting up ELK Stack for Ecommerce Microservices"
echo "===================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

print_status "Starting ELK Stack services..."

# Start ELK services
docker-compose up -d elasticsearch logstash kibana filebeat

print_status "Waiting for services to start..."
sleep 30

# Check service health
print_status "Checking service health..."

# Wait for Elasticsearch
print_status "Waiting for Elasticsearch to be ready..."
timeout=300
counter=0
while ! curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green\|yellow"'; do
    if [ $counter -ge $timeout ]; then
        print_error "Elasticsearch failed to start within timeout"
        exit 1
    fi
    sleep 5
    ((counter+=5))
    echo -n "."
done
print_success "Elasticsearch is ready"

# Wait for Logstash
print_status "Waiting for Logstash to be ready..."
counter=0
while ! curl -s http://localhost:9600 | grep -q '"status":"green"'; do
    if [ $counter -ge $timeout ]; then
        print_error "Logstash failed to start within timeout"
        exit 1
    fi
    sleep 5
    ((counter+=5))
    echo -n "."
done
print_success "Logstash is ready"

# Wait for Kibana
print_status "Waiting for Kibana to be ready..."
counter=0
while ! curl -s http://localhost:5601/api/status | grep -q '"level":"available"'; do
    if [ $counter -ge $timeout ]; then
        print_error "Kibana failed to start within timeout"
        exit 1
    fi
    sleep 5
    ((counter+=5))
    echo -n "."
done
print_success "Kibana is ready"

print_status "Setting up Elasticsearch ILM policies and templates..."
if [ -f "./docker/elasticsearch/setup-ilm-policies.sh" ]; then
    ./docker/elasticsearch/setup-ilm-policies.sh
    print_success "ILM policies configured"
else
    print_warning "ILM setup script not found"
fi

print_status "Setting up Kibana dashboards and index patterns..."
if [ -f "./docker/kibana/setup-dashboards.sh" ]; then
    ./docker/kibana/setup-dashboards.sh
    print_success "Kibana dashboards configured"
else
    print_warning "Kibana setup script not found"
fi

print_status "Setting up log-based alerting..."
if [ -f "./docker/elasticsearch/setup-watchers.sh" ]; then
    ./docker/elasticsearch/setup-watchers.sh
    print_success "Alerting configured"
else
    print_warning "Alerting setup script not found"
fi

print_status "Running comprehensive ELK stack tests..."
if [ -f "./scripts/test-elk-stack.sh" ]; then
    ./scripts/test-elk-stack.sh
else
    print_warning "Test script not found, running basic health checks..."
    
    # Basic health checks
    echo "Elasticsearch health:"
    curl -s http://localhost:9200/_cluster/health | jq '.'
    
    echo "Logstash stats:"
    curl -s http://localhost:9600/_node/stats | jq '.pipelines'
    
    echo "Kibana status:"
    curl -s http://localhost:5601/api/status | jq '.status'
fi

print_success "ELK Stack setup completed successfully!"

echo ""
echo "📊 Access URLs:"
echo "==============="
echo "🔍 Elasticsearch: http://localhost:9200"
echo "📈 Kibana:        http://localhost:5601"
echo "⚙️  Logstash:      http://localhost:9600"
echo "📦 Filebeat:      http://localhost:5066"

echo ""
echo "🎯 Quick Start Guide:"
echo "===================="
echo "1. Access Kibana at http://localhost:5601"
echo "2. Go to 'Discover' to view logs"
echo "3. Check 'Dashboard' for pre-built visualizations"
echo "4. Configure your microservices to send logs to:"
echo "   - TCP: localhost:5000 (JSON format)"
echo "   - Beats: localhost:5044 (Filebeat)"

echo ""
echo "📝 Log Format Example:"
echo "====================="
cat << 'EOF'
{
  "@timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.ecommerce.orderservice.OrderController",
  "message": "Order created successfully",
  "correlationId": "abc123",
  "tenantId": "tenant1",
  "userId": "user123",
  "service_name": "order-service"
}
EOF

echo ""
echo "🧪 Test Log Ingestion:"
echo "======================"
echo "Send a test log:"
echo 'echo '"'"'{"@timestamp":"'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'","level":"INFO","message":"Test log","service_name":"test"}'"'"' | nc localhost 5000'

echo ""
echo "🔧 Management Commands:"
echo "======================="
echo "Stop ELK Stack:     docker-compose stop elasticsearch logstash kibana filebeat"
echo "View logs:          docker-compose logs -f elasticsearch logstash kibana"
echo "Restart services:   docker-compose restart elasticsearch logstash kibana filebeat"
echo "Clean up:           docker-compose down -v (WARNING: This will delete all data)"

echo ""
echo "📚 Documentation:"
echo "=================="
echo "Full documentation: ./docker/elk/README.md"
echo "Configuration files: ./docker/elasticsearch/, ./docker/logstash/, ./docker/kibana/"

echo ""
print_success "ELK Stack is ready for log aggregation! 🎉"

# Optional: Open Kibana in browser (macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    read -p "Open Kibana in browser? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        open http://localhost:5601
    fi
fi