#!/usr/bin/env bash

# Grafana Setup Validation Script
# This script validates the Grafana configuration and dashboard setup

set -e

echo "🔍 Validating Grafana Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    case $status in
        "SUCCESS")
            echo -e "${GREEN}✅ $message${NC}"
            ;;
        "WARNING")
            echo -e "${YELLOW}⚠️  $message${NC}"
            ;;
        "ERROR")
            echo -e "${RED}❌ $message${NC}"
            ;;
        "INFO")
            echo -e "ℹ️  $message"
            ;;
    esac
}

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_status "ERROR" "docker-compose is not installed or not in PATH"
    exit 1
fi

print_status "SUCCESS" "Docker Compose is available"

# Check if Grafana configuration files exist
config_files=(
    "docker/grafana/provisioning/datasources/prometheus.yml"
    "docker/grafana/provisioning/dashboards/dashboard.yml"
    "docker/grafana/provisioning/dashboards/system-overview.json"
    "docker/grafana/provisioning/dashboards/service-level-red-metrics.json"
    "docker/grafana/provisioning/dashboards/jvm-performance.json"
    "docker/grafana/provisioning/dashboards/database-performance.json"
    "docker/grafana/provisioning/dashboards/business-intelligence.json"
)

print_status "INFO" "Checking Grafana configuration files..."

for file in "${config_files[@]}"; do
    if [[ -f "$file" ]]; then
        print_status "SUCCESS" "Found: $file"
    else
        print_status "ERROR" "Missing: $file"
        exit 1
    fi
done

# Validate JSON dashboard files
print_status "INFO" "Validating JSON dashboard files..."

dashboard_files=(
    "docker/grafana/provisioning/dashboards/system-overview.json"
    "docker/grafana/provisioning/dashboards/service-level-red-metrics.json"
    "docker/grafana/provisioning/dashboards/jvm-performance.json"
    "docker/grafana/provisioning/dashboards/database-performance.json"
    "docker/grafana/provisioning/dashboards/business-intelligence.json"
)

for file in "${dashboard_files[@]}"; do
    if python3 -m json.tool "$file" > /dev/null 2>&1; then
        print_status "SUCCESS" "Valid JSON: $file"
    else
        print_status "ERROR" "Invalid JSON: $file"
        exit 1
    fi
done

# Check if Grafana service is defined in docker-compose.yml
if grep -q "grafana:" docker-compose.yml; then
    print_status "SUCCESS" "Grafana service found in docker-compose.yml"
else
    print_status "ERROR" "Grafana service not found in docker-compose.yml"
    exit 1
fi

# Check if Prometheus services are defined
prometheus_services=("prometheus:" "prometheus-federation:")
for service in "${prometheus_services[@]}"; do
    if grep -q "$service" docker-compose.yml; then
        print_status "SUCCESS" "Service found in docker-compose.yml: $service"
    else
        print_status "WARNING" "Service not found in docker-compose.yml: $service"
    fi
done

# Validate YAML files (basic syntax check)
print_status "INFO" "Validating YAML configuration files..."

yaml_files=(
    "docker/grafana/provisioning/datasources/prometheus.yml"
    "docker/grafana/provisioning/dashboards/dashboard.yml"
)

for file in "${yaml_files[@]}"; do
    # Basic YAML syntax validation using grep and basic checks
    if [[ -f "$file" ]] && [[ -s "$file" ]]; then
        # Check for basic YAML structure
        if grep -q "apiVersion:" "$file" && ! grep -q $'\t' "$file"; then
            print_status "SUCCESS" "Valid YAML structure: $file"
        else
            print_status "WARNING" "YAML structure check failed: $file (may still be valid)"
        fi
    else
        print_status "ERROR" "YAML file is empty or missing: $file"
        exit 1
    fi
done

# Check dashboard UIDs for uniqueness
print_status "INFO" "Checking dashboard UID uniqueness..."

uid_list=""
for file in "${dashboard_files[@]}"; do
    uid=$(python3 -c "import json; print(json.load(open('$file')).get('uid', 'NO_UID'))")
    if [[ "$uid" == "NO_UID" ]]; then
        print_status "WARNING" "No UID found in: $file"
    elif echo "$uid_list" | grep -q "$uid"; then
        print_status "ERROR" "Duplicate UID '$uid' found in: $file"
        exit 1
    else
        uid_list="$uid_list $uid"
        print_status "SUCCESS" "Unique UID '$uid' in: $file"
    fi
done

# Check if services are running (optional)
print_status "INFO" "Checking if monitoring services are running..."

if docker-compose ps | grep -q "ecommerce-grafana.*Up"; then
    print_status "SUCCESS" "Grafana service is running"
    
    # Test Grafana API
    if curl -s -f http://localhost:3000/api/health > /dev/null; then
        print_status "SUCCESS" "Grafana API is responding"
    else
        print_status "WARNING" "Grafana API is not responding (service may be starting)"
    fi
else
    print_status "INFO" "Grafana service is not running (use 'docker-compose up -d grafana' to start)"
fi

if docker-compose ps | grep -q "ecommerce-prometheus.*Up"; then
    print_status "SUCCESS" "Prometheus service is running"
    
    # Test Prometheus API
    if curl -s -f http://localhost:9090/-/healthy > /dev/null; then
        print_status "SUCCESS" "Prometheus API is responding"
    else
        print_status "WARNING" "Prometheus API is not responding (service may be starting)"
    fi
else
    print_status "INFO" "Prometheus service is not running (use 'docker-compose up -d prometheus' to start)"
fi

# Summary
print_status "SUCCESS" "Grafana setup validation completed successfully!"

echo ""
echo "📊 Dashboard Summary:"
echo "  • System Overview: http://localhost:3000/d/system-overview"
echo "  • Service RED Metrics: http://localhost:3000/d/service-red-metrics"
echo "  • JVM Performance: http://localhost:3000/d/jvm-performance"
echo "  • Database Performance: http://localhost:3000/d/database-performance"
echo "  • Business Intelligence: http://localhost:3000/d/business-intelligence"

echo ""
echo "🚀 To start the monitoring stack:"
echo "  docker-compose up -d prometheus prometheus-federation grafana"

echo ""
echo "🔐 Grafana Access:"
echo "  URL: http://localhost:3000"
echo "  Username: admin"
echo "  Password: admin"

print_status "SUCCESS" "All validations passed! Grafana is ready for deployment."