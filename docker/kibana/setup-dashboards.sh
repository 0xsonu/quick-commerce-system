#!/bin/bash

# Script to set up Kibana dashboards and index patterns for ecommerce logs

KIBANA_URL="http://localhost:5601"
ELASTICSEARCH_URL="http://localhost:9200"

echo "Setting up Kibana dashboards for ecommerce logs..."

# Wait for Kibana to be ready
until curl -s "$KIBANA_URL/api/status" | grep -q '"level":"available"'; do
  echo "Waiting for Kibana to be ready..."
  sleep 10
done

echo "Kibana is ready. Creating index patterns and dashboards..."

# Create index pattern for ecommerce logs
curl -X POST "$KIBANA_URL/api/saved_objects/index-pattern/ecommerce-logs-pattern" \
  -H "Content-Type: application/json" \
  -H "kbn-xsrf: true" \
  -d '{
    "attributes": {
      "title": "ecommerce-logs-*",
      "timeFieldName": "@timestamp",
      "fields": "[{\"name\":\"@timestamp\",\"type\":\"date\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"message\",\"type\":\"string\",\"searchable\":true,\"aggregatable\":false},{\"name\":\"log_level\",\"type\":\"string\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"service_name\",\"type\":\"string\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"correlation_id\",\"type\":\"string\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"tenant_id\",\"type\":\"string\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"user_id\",\"type\":\"string\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"http_status\",\"type\":\"number\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"http_duration\",\"type\":\"number\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"exception_class\",\"type\":\"string\",\"searchable\":true,\"aggregatable\":true},{\"name\":\"tags\",\"type\":\"string\",\"searchable\":true,\"aggregatable\":true}]"
    }
  }'

echo ""

# Set default index pattern
curl -X POST "$KIBANA_URL/api/kibana/settings/defaultIndex" \
  -H "Content-Type: application/json" \
  -H "kbn-xsrf: true" \
  -d '{
    "value": "ecommerce-logs-pattern"
  }'

echo ""

# Create saved search for error logs
curl -X POST "$KIBANA_URL/api/saved_objects/search/error-logs-search" \
  -H "Content-Type: application/json" \
  -H "kbn-xsrf: true" \
  -d '{
    "attributes": {
      "title": "Error Logs",
      "description": "Search for error level logs across all services",
      "hits": 0,
      "columns": ["@timestamp", "service_name", "log_level", "message", "exception_class"],
      "sort": [["@timestamp", "desc"]],
      "version": 1,
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs-pattern\",\"query\":{\"match\":{\"log_level\":\"ERROR\"}},\"filter\":[],\"highlight\":{\"pre_tags\":[\"@kibana-highlighted-field@\"],\"post_tags\":[\"@/kibana-highlighted-field@\"],\"fields\":{\"*\":{}},\"fragment_size\":2147483647}}"
      }
    }
  }'

echo ""

# Create saved search for HTTP requests
curl -X POST "$KIBANA_URL/api/saved_objects/search/http-requests-search" \
  -H "Content-Type: application/json" \
  -H "kbn-xsrf: true" \
  -d '{
    "attributes": {
      "title": "HTTP Requests",
      "description": "Search for HTTP request logs with response times",
      "hits": 0,
      "columns": ["@timestamp", "service_name", "http_method", "http_path", "http_status", "http_duration"],
      "sort": [["@timestamp", "desc"]],
      "version": 1,
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs-pattern\",\"query\":{\"exists\":{\"field\":\"http_duration\"}},\"filter\":[],\"highlight\":{\"pre_tags\":[\"@kibana-highlighted-field@\"],\"post_tags\":[\"@/kibana-highlighted-field@\"],\"fields\":{\"*\":{}},\"fragment_size\":2147483647}}"
      }
    }
  }'

echo ""

# Create visualization for log levels pie chart
curl -X POST "$KIBANA_URL/api/saved_objects/visualization/log-levels-pie" \
  -H "Content-Type: application/json" \
  -H "kbn-xsrf: true" \
  -d '{
    "attributes": {
      "title": "Log Levels Distribution",
      "visState": "{\"title\":\"Log Levels Distribution\",\"type\":\"pie\",\"params\":{\"addTooltip\":true,\"addLegend\":true,\"legendPosition\":\"right\",\"isDonut\":true},\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"count\",\"schema\":\"metric\",\"params\":{}},{\"id\":\"2\",\"enabled\":true,\"type\":\"terms\",\"schema\":\"segment\",\"params\":{\"field\":\"log_level\",\"size\":10,\"order\":\"desc\",\"orderBy\":\"1\"}}]}",
      "uiStateJSON": "{}",
      "description": "Distribution of log levels across all services",
      "version": 1,
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs-pattern\",\"query\":{\"match_all\":{}},\"filter\":[]}"
      }
    }
  }'

echo ""

# Create visualization for services bar chart
curl -X POST "$KIBANA_URL/api/saved_objects/visualization/services-bar" \
  -H "Content-Type: application/json" \
  -H "kbn-xsrf: true" \
  -d '{
    "attributes": {
      "title": "Logs by Service",
      "visState": "{\"title\":\"Logs by Service\",\"type\":\"histogram\",\"params\":{\"grid\":{\"categoryLines\":false,\"style\":{\"color\":\"#eee\"}},\"categoryAxes\":[{\"id\":\"CategoryAxis-1\",\"type\":\"category\",\"position\":\"bottom\",\"show\":true,\"style\":{},\"scale\":{\"type\":\"linear\"},\"labels\":{\"show\":true,\"truncate\":100},\"title\":{}}],\"valueAxes\":[{\"id\":\"ValueAxis-1\",\"name\":\"LeftAxis-1\",\"type\":\"value\",\"position\":\"left\",\"show\":true,\"style\":{},\"scale\":{\"type\":\"linear\",\"mode\":\"normal\"},\"labels\":{\"show\":true,\"rotate\":0,\"filter\":false,\"truncate\":100},\"title\":{\"text\":\"Count\"}}],\"seriesParams\":[{\"show\":\"true\",\"type\":\"histogram\",\"mode\":\"stacked\",\"data\":{\"label\":\"Count\",\"id\":\"1\"},\"valueAxis\":\"ValueAxis-1\",\"drawLinesBetweenPoints\":true,\"showCircles\":true}],\"addTooltip\":true,\"addLegend\":true,\"legendPosition\":\"right\",\"times\":[],\"addTimeMarker\":false},\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"count\",\"schema\":\"metric\",\"params\":{}},{\"id\":\"2\",\"enabled\":true,\"type\":\"terms\",\"schema\":\"segment\",\"params\":{\"field\":\"service_name\",\"size\":20,\"order\":\"desc\",\"orderBy\":\"1\"}}]}",
      "uiStateJSON": "{}",
      "description": "Number of logs by service",
      "version": 1,
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"index\":\"ecommerce-logs-pattern\",\"query\":{\"match_all\":{}},\"filter\":[]}"
      }
    }
  }'

echo ""

# Create dashboard
curl -X POST "$KIBANA_URL/api/saved_objects/dashboard/ecommerce-logs-dashboard" \
  -H "Content-Type: application/json" \
  -H "kbn-xsrf: true" \
  -d '{
    "attributes": {
      "title": "Ecommerce Logs Overview",
      "description": "Overview dashboard for ecommerce microservices logs",
      "panelsJSON": "[{\"version\":\"8.11.0\",\"gridData\":{\"x\":0,\"y\":0,\"w\":24,\"h\":15,\"i\":\"1\"},\"panelIndex\":\"1\",\"embeddableConfig\":{},\"panelRefName\":\"panel_1\"},{\"version\":\"8.11.0\",\"gridData\":{\"x\":24,\"y\":0,\"w\":24,\"h\":15,\"i\":\"2\"},\"panelIndex\":\"2\",\"embeddableConfig\":{},\"panelRefName\":\"panel_2\"}]",
      "timeRestore": false,
      "timeTo": "now",
      "timeFrom": "now-24h",
      "refreshInterval": {
        "pause": false,
        "value": 30000
      },
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"query\":{\"match_all\":{}},\"filter\":[]}"
      }
    },
    "references": [
      {
        "name": "panel_1",
        "type": "visualization",
        "id": "log-levels-pie"
      },
      {
        "name": "panel_2",
        "type": "visualization",
        "id": "services-bar"
      }
    ]
  }'

echo ""
echo "Kibana dashboards and index patterns created successfully!"
echo "Access Kibana at: $KIBANA_URL"
echo "Default dashboard: $KIBANA_URL/app/dashboards#/view/ecommerce-logs-dashboard"