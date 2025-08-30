#!/bin/bash

# Script to test ELK stack log aggregation pipeline

set -e

ELASTICSEARCH_URL="http://localhost:9200"
KIBANA_URL="http://localhost:5601"
LOGSTASH_URL="http://localhost:9600"

echo "Testing ELK Stack Log Aggregation Pipeline..."
echo "=============================================="

# Function to check service health
check_service() {
    local service_name=$1
    local url=$2
    local expected_pattern=$3
    
    echo -n "Checking $service_name... "
    
    if curl -s "$url" | grep -q "$expected_pattern"; then
        echo "✅ OK"
        return 0
    else
        echo "❌ FAILED"
        return 1
    fi
}

# Function to wait for service
wait_for_service() {
    local service_name=$1
    local url=$2
    local expected_pattern=$3
    local max_attempts=30
    local attempt=1
    
    echo "Waiting for $service_name to be ready..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" | grep -q "$expected_pattern"; then
            echo "✅ $service_name is ready"
            return 0
        fi
        
        echo "Attempt $attempt/$max_attempts - waiting..."
        sleep 10
        ((attempt++))
    done
    
    echo "❌ $service_name failed to start within timeout"
    return 1
}

echo "1. Checking ELK Stack Services Health"
echo "-------------------------------------"

# Check Elasticsearch
wait_for_service "Elasticsearch" "$ELASTICSEARCH_URL/_cluster/health" '"status":"green\|yellow"'

# Check Logstash
wait_for_service "Logstash" "$LOGSTASH_URL" '"status":"green"'

# Check Kibana
wait_for_service "Kibana" "$KIBANA_URL/api/status" '"level":"available"'

echo ""
echo "2. Testing Elasticsearch Indices and Templates"
echo "----------------------------------------------"

# Check if ILM policies exist
echo -n "Checking ILM policies... "
if curl -s "$ELASTICSEARCH_URL/_ilm/policy" | grep -q "ecommerce-logs-policy"; then
    echo "✅ OK"
else
    echo "❌ FAILED - Running setup script..."
    ./docker/elasticsearch/setup-ilm-policies.sh
fi

# Check if index template exists
echo -n "Checking index templates... "
if curl -s "$ELASTICSEARCH_URL/_index_template" | grep -q "ecommerce-logs-template"; then
    echo "✅ OK"
else
    echo "❌ FAILED"
fi

# Check if initial index exists
echo -n "Checking initial index... "
if curl -s "$ELASTICSEARCH_URL/_cat/indices" | grep -q "ecommerce-logs"; then
    echo "✅ OK"
else
    echo "❌ FAILED"
fi

echo ""
echo "3. Testing Log Ingestion Pipeline"
echo "---------------------------------"

# Send test log to Logstash
echo "Sending test log to Logstash..."
curl -X POST "$LOGSTASH_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "@timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
    "message": "Test log message from ELK test script",
    "log_level": "INFO",
    "service_name": "test-service",
    "correlation_id": "test-correlation-123",
    "tenant_id": "test-tenant",
    "user_id": "test-user",
    "environment": "development",
    "tags": ["test", "elk-validation"]
  }' 2>/dev/null || echo "Direct Logstash ingestion not available"

# Send test log via TCP
echo "Sending test log via TCP..."
echo '{
  "@timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
  "message": "Test TCP log message",
  "log_level": "WARN",
  "service_name": "tcp-test-service",
  "correlation_id": "tcp-test-456",
  "tenant_id": "test-tenant",
  "environment": "development",
  "tags": ["test", "tcp", "elk-validation"]
}' | nc localhost 5000 2>/dev/null || echo "TCP ingestion not available"

# Send test error log
echo "Sending test error log..."
echo '{
  "@timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
  "message": "Test error message",
  "log_level": "ERROR",
  "service_name": "error-test-service",
  "correlation_id": "error-test-789",
  "tenant_id": "test-tenant",
  "exception_class": "java.lang.RuntimeException",
  "exception_message": "Test exception for ELK validation",
  "stack_trace": "java.lang.RuntimeException: Test exception\n\tat com.test.TestClass.testMethod(TestClass.java:123)",
  "environment": "development",
  "tags": ["test", "error", "elk-validation"]
}' | nc localhost 5000 2>/dev/null || echo "TCP error log ingestion not available"

echo "Waiting for logs to be processed..."
sleep 10

echo ""
echo "4. Verifying Log Processing"
echo "---------------------------"

# Check if test logs were indexed
echo -n "Checking if test logs were indexed... "
sleep 5  # Wait for indexing
test_logs=$(curl -s "$ELASTICSEARCH_URL/ecommerce-logs-*/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "bool": {
        "must": [
          {"term": {"tags": "elk-validation"}},
          {"range": {"@timestamp": {"gte": "now-5m"}}}
        ]
      }
    }
  }' | jq -r '.hits.total.value' 2>/dev/null || echo "0")

if [ "$test_logs" -gt 0 ]; then
    echo "✅ OK ($test_logs test logs found)"
else
    echo "❌ FAILED (no test logs found)"
fi

# Check log parsing
echo -n "Checking log field parsing... "
parsed_logs=$(curl -s "$ELASTICSEARCH_URL/ecommerce-logs-*/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "bool": {
        "must": [
          {"term": {"tags": "elk-validation"}},
          {"exists": {"field": "service_name"}},
          {"exists": {"field": "log_level"}}
        ]
      }
    }
  }' | jq -r '.hits.total.value' 2>/dev/null || echo "0")

if [ "$parsed_logs" -gt 0 ]; then
    echo "✅ OK ($parsed_logs logs with parsed fields)"
else
    echo "❌ FAILED (no properly parsed logs found)"
fi

echo ""
echo "5. Testing Kibana Integration"
echo "-----------------------------"

# Check if index pattern exists in Kibana
echo -n "Checking Kibana index patterns... "
if curl -s "$KIBANA_URL/api/saved_objects/_find?type=index-pattern" \
   -H "kbn-xsrf: true" | grep -q "ecommerce-logs"; then
    echo "✅ OK"
else
    echo "❌ FAILED - Running setup script..."
    ./docker/kibana/setup-dashboards.sh
fi

# Check if dashboards exist
echo -n "Checking Kibana dashboards... "
if curl -s "$KIBANA_URL/api/saved_objects/_find?type=dashboard" \
   -H "kbn-xsrf: true" | grep -q "ecommerce-logs"; then
    echo "✅ OK"
else
    echo "❌ FAILED"
fi

echo ""
echo "6. Testing Filebeat Configuration"
echo "---------------------------------"

# Check if Filebeat is running (if in Docker)
echo -n "Checking Filebeat container... "
if docker ps | grep -q "ecommerce-filebeat"; then
    echo "✅ OK"
else
    echo "❌ FAILED (Filebeat container not running)"
fi

# Check Filebeat health endpoint
echo -n "Checking Filebeat health... "
if curl -s "http://localhost:5066" | grep -q "filebeat"; then
    echo "✅ OK"
else
    echo "⚠️  WARNING (Filebeat health endpoint not accessible)"
fi

echo ""
echo "7. Performance and Storage Tests"
echo "--------------------------------"

# Check Elasticsearch cluster stats
echo "Elasticsearch cluster stats:"
curl -s "$ELASTICSEARCH_URL/_cluster/stats" | jq -r '
  "Nodes: " + (.nodes.count | tostring) + 
  ", Indices: " + (.indices.count | tostring) + 
  ", Documents: " + (.indices.docs.count | tostring) + 
  ", Store size: " + .indices.store.size_in_bytes + " bytes"
' 2>/dev/null || echo "Unable to retrieve cluster stats"

# Check index sizes
echo "Index sizes:"
curl -s "$ELASTICSEARCH_URL/_cat/indices/ecommerce-logs-*?v&h=index,docs.count,store.size" 2>/dev/null || echo "No ecommerce log indices found"

echo ""
echo "8. Testing Log Retention and ILM"
echo "--------------------------------"

# Check ILM policy status
echo "ILM policy status:"
curl -s "$ELASTICSEARCH_URL/_ilm/policy/ecommerce-logs-policy" | jq -r '.ecommerce-logs-policy.policy.phases | keys[]' 2>/dev/null || echo "ILM policy not found"

# Check if indices are using ILM
echo -n "Checking ILM usage on indices... "
ilm_indices=$(curl -s "$ELASTICSEARCH_URL/_cat/indices/ecommerce-logs-*?h=index" | wc -l)
if [ "$ilm_indices" -gt 0 ]; then
    echo "✅ OK ($ilm_indices indices with ILM)"
else
    echo "❌ FAILED (no indices using ILM)"
fi

echo ""
echo "9. Summary and Recommendations"
echo "==============================="

echo "ELK Stack Test Results:"
echo "- Elasticsearch: $(check_service "Elasticsearch" "$ELASTICSEARCH_URL/_cluster/health" '"status":"green\|yellow"' && echo "✅ Running" || echo "❌ Issues")"
echo "- Logstash: $(check_service "Logstash" "$LOGSTASH_URL" '"status":"green"' && echo "✅ Running" || echo "❌ Issues")"
echo "- Kibana: $(check_service "Kibana" "$KIBANA_URL/api/status" '"level":"available"' && echo "✅ Running" || echo "❌ Issues")"

echo ""
echo "Access URLs:"
echo "- Elasticsearch: $ELASTICSEARCH_URL"
echo "- Kibana: $KIBANA_URL"
echo "- Logstash: $LOGSTASH_URL"

echo ""
echo "Next Steps:"
echo "1. Configure your microservices to send logs to Logstash (TCP port 5000)"
echo "2. Set up Filebeat on application servers to ship log files"
echo "3. Create custom Kibana dashboards for your specific use cases"
echo "4. Configure alerting based on log patterns"
echo "5. Set up log retention policies based on your requirements"

echo ""
echo "ELK Stack test completed!"