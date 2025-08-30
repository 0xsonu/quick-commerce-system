#!/bin/bash

# Test Prometheus monitoring infrastructure
echo "Testing Prometheus Monitoring Infrastructure..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if service is healthy
check_service() {
    local service_name=$1
    local url=$2
    local expected_status=${3:-200}
    
    echo -n "Checking $service_name... "
    
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "$expected_status"; then
        echo -e "${GREEN}✓ Healthy${NC}"
        return 0
    else
        echo -e "${RED}✗ Unhealthy${NC}"
        return 1
    fi
}

# Function to check if metrics are being collected
check_metrics() {
    local service_name=$1
    local query=$2
    
    echo -n "Checking $service_name metrics... "
    
    local result=$(curl -s "http://localhost:9090/api/v1/query?query=$query" | jq -r '.data.result | length')
    
    if [ "$result" -gt 0 ]; then
        echo -e "${GREEN}✓ Metrics available${NC}"
        return 0
    else
        echo -e "${RED}✗ No metrics${NC}"
        return 1
    fi
}

# Wait for services to start
echo "Waiting for services to start..."
sleep 30

echo -e "\n${YELLOW}=== Core Monitoring Services ===${NC}"

# Check Prometheus main server
check_service "Prometheus Main" "http://localhost:9090/-/healthy"

# Check Prometheus Federation server
check_service "Prometheus Federation" "http://localhost:9091/-/healthy"

# Check Pushgateway
check_service "Pushgateway" "http://localhost:9093/-/healthy"

# Check Node Exporter
check_service "Node Exporter" "http://localhost:9100/metrics"

# Check Blackbox Exporter
check_service "Blackbox Exporter" "http://localhost:9115/-/healthy"

# Check Grafana
check_service "Grafana" "http://localhost:3000/api/health"

echo -e "\n${YELLOW}=== Metrics Collection Tests ===${NC}"

# Test basic Prometheus metrics
check_metrics "Prometheus Self" "up{job=\"prometheus\"}"

# Test Node Exporter metrics
check_metrics "Node Exporter" "up{job=\"node-exporter\"}"

# Test Pushgateway metrics
check_metrics "Pushgateway" "up{job=\"pushgateway\"}"

# Test federation metrics
echo -n "Checking Federation metrics... "
federation_result=$(curl -s "http://localhost:9091/api/v1/query?query=up{job=\"federate\"}" | jq -r '.data.result | length')
if [ "$federation_result" -gt 0 ]; then
    echo -e "${GREEN}✓ Federation working${NC}"
else
    echo -e "${RED}✗ Federation not working${NC}"
fi

echo -e "\n${YELLOW}=== Blackbox Monitoring Tests ===${NC}"

# Test HTTP probes
check_metrics "HTTP Probes" "probe_success{job=\"blackbox-http\"}"

# Test gRPC probes
check_metrics "gRPC Probes" "probe_success{job=\"blackbox-grpc\"}"

echo -e "\n${YELLOW}=== Alert Rules Tests ===${NC}"

# Check if alert rules are loaded
echo -n "Checking alert rules... "
rules_result=$(curl -s "http://localhost:9090/api/v1/rules" | jq -r '.data.groups | length')
if [ "$rules_result" -gt 0 ]; then
    echo -e "${GREEN}✓ Alert rules loaded${NC}"
    echo "  - Found $rules_result rule groups"
else
    echo -e "${RED}✗ No alert rules${NC}"
fi

echo -e "\n${YELLOW}=== Configuration Tests ===${NC}"

# Check Prometheus configuration
echo -n "Checking Prometheus config... "
config_result=$(curl -s "http://localhost:9090/api/v1/status/config" | jq -r '.status')
if [ "$config_result" = "success" ]; then
    echo -e "${GREEN}✓ Configuration valid${NC}"
else
    echo -e "${RED}✗ Configuration invalid${NC}"
fi

# Check targets
echo -n "Checking scrape targets... "
targets_result=$(curl -s "http://localhost:9090/api/v1/targets" | jq -r '.data.activeTargets | length')
if [ "$targets_result" -gt 0 ]; then
    echo -e "${GREEN}✓ $targets_result targets configured${NC}"
else
    echo -e "${RED}✗ No targets configured${NC}"
fi

echo -e "\n${YELLOW}=== Storage Tests ===${NC}"

# Check data retention
echo -n "Checking main Prometheus retention... "
retention_result=$(curl -s "http://localhost:9090/api/v1/status/runtimeinfo" | jq -r '.data.storageRetention')
if [ "$retention_result" = "15d" ]; then
    echo -e "${GREEN}✓ 15-day retention configured${NC}"
else
    echo -e "${YELLOW}⚠ Retention: $retention_result${NC}"
fi

echo -n "Checking federation Prometheus retention... "
federation_retention=$(curl -s "http://localhost:9091/api/v1/status/runtimeinfo" | jq -r '.data.storageRetention')
if [ "$federation_retention" = "365d" ]; then
    echo -e "${GREEN}✓ 1-year retention configured${NC}"
else
    echo -e "${YELLOW}⚠ Federation retention: $federation_retention${NC}"
fi

echo -e "\n${YELLOW}=== Sample Metrics Push Test ===${NC}"

# Test pushing metrics to Pushgateway
echo -n "Testing Pushgateway... "
if echo "test_metric 42" | curl -s --data-binary @- "http://localhost:9093/metrics/job/test_job/instance/test_instance" > /dev/null; then
    echo -e "${GREEN}✓ Pushgateway accepts metrics${NC}"
    
    # Verify the metric is available
    sleep 2
    if curl -s "http://localhost:9090/api/v1/query?query=test_metric" | jq -r '.data.result | length' | grep -q "1"; then
        echo -e "${GREEN}✓ Pushed metric available in Prometheus${NC}"
    else
        echo -e "${RED}✗ Pushed metric not found in Prometheus${NC}"
    fi
else
    echo -e "${RED}✗ Failed to push to Pushgateway${NC}"
fi

echo -e "\n${YELLOW}=== Summary ===${NC}"
echo "Monitoring infrastructure test completed."
echo "Access URLs:"
echo "  - Prometheus Main: http://localhost:9090"
echo "  - Prometheus Federation: http://localhost:9091"
echo "  - Pushgateway: http://localhost:9093"
echo "  - Node Exporter: http://localhost:9100"
echo "  - Blackbox Exporter: http://localhost:9115"
echo "  - Grafana: http://localhost:3000 (admin/admin)"

echo -e "\n${YELLOW}=== Next Steps ===${NC}"
echo "1. Configure Grafana dashboards for visualization"
echo "2. Set up Alertmanager for alert routing"
echo "3. Add custom business metrics to applications"
echo "4. Configure log aggregation with ELK stack"
echo "5. Set up distributed tracing with OpenTelemetry"