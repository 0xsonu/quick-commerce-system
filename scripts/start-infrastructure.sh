#!/bin/bash

echo "Starting Amazon Shopping Backend Infrastructure..."

# Start infrastructure services
echo "Starting infrastructure services with Docker Compose..."
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 30

# Check service health
echo "Checking service health..."

# Check MySQL
echo "Checking MySQL..."
docker-compose exec -T mysql mysqladmin ping -h localhost --silent
if [ $? -eq 0 ]; then
    echo "✅ MySQL is ready"
else
    echo "❌ MySQL is not ready"
fi

# Check MongoDB
echo "Checking MongoDB..."
docker-compose exec -T mongodb mongosh --eval "db.adminCommand('ping')" --quiet
if [ $? -eq 0 ]; then
    echo "✅ MongoDB is ready"
else
    echo "❌ MongoDB is not ready"
fi

# Check Redis
echo "Checking Redis..."
docker-compose exec -T redis redis-cli ping
if [ $? -eq 0 ]; then
    echo "✅ Redis is ready"
else
    echo "❌ Redis is not ready"
fi

# Check Kafka
echo "Checking Kafka..."
docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Kafka is ready"
else
    echo "❌ Kafka is not ready"
fi

echo ""
echo "Infrastructure services status:"
docker-compose ps

echo ""
echo "🚀 Infrastructure is ready!"
echo ""
echo "Access points:"
echo "- Kafka UI: http://localhost:8080"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo "- Jaeger: http://localhost:16686"
echo ""
echo "Database connections:"
echo "- MySQL: localhost:3306 (root/rootpassword)"
echo "- MongoDB: localhost:27017 (admin/adminpassword)"
echo "- Redis: localhost:6379"
echo ""
echo "You can now build and start the microservices!"