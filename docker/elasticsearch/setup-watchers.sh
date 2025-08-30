#!/bin/bash

# Script to set up Elasticsearch Watchers for log-based alerting

ELASTICSEARCH_URL="http://localhost:9200"

echo "Setting up Elasticsearch Watchers for log-based alerting..."

# Wait for Elasticsearch to be ready
until curl -s "$ELASTICSEARCH_URL/_cluster/health" | grep -q '"status":"green\|yellow"'; do
  echo "Waiting for Elasticsearch to be ready..."
  sleep 5
done

echo "Elasticsearch is ready. Creating watchers..."

# Note: Watchers require X-Pack license (Basic license includes some features)
# For development, we'll create simple alerting using Elasticsearch APIs

# Create error rate watcher
echo "Creating error rate watcher..."
curl -X PUT "$ELASTICSEARCH_URL/_watcher/watch/error_rate_alert" \
  -H "Content-Type: application/json" \
  -d @docker/elasticsearch/watchers/error-rate-alert.json

echo ""

# Create service down watcher
echo "Creating service down watcher..."
curl -X PUT "$ELASTICSEARCH_URL/_watcher/watch/service_down_alert" \
  -H "Content-Type: application/json" \
  -d @docker/elasticsearch/watchers/service-down-alert.json

echo ""

# Create slow response watcher
echo "Creating slow response watcher..."
curl -X PUT "$ELASTICSEARCH_URL/_watcher/watch/slow_response_alert" \
  -H "Content-Type: application/json" \
  -d @docker/elasticsearch/watchers/slow-response-alert.json

echo ""

# Alternative: Create simple alerting using Elasticsearch alerting API (if available)
echo "Setting up alternative alerting using Elasticsearch alerting..."

# Create a simple monitor for error logs
curl -X POST "$ELASTICSEARCH_URL/_plugins/_alerting/monitors" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "monitor",
    "name": "High Error Rate Monitor",
    "enabled": true,
    "schedule": {
      "period": {
        "interval": 1,
        "unit": "MINUTES"
      }
    },
    "inputs": [{
      "search": {
        "indices": ["ecommerce-logs-*"],
        "query": {
          "size": 0,
          "query": {
            "bool": {
              "must": [
                {
                  "term": {
                    "log_level": "ERROR"
                  }
                },
                {
                  "range": {
                    "@timestamp": {
                      "gte": "now-5m"
                    }
                  }
                }
              ]
            }
          }
        }
      }
    }],
    "triggers": [{
      "name": "High error rate trigger",
      "severity": "1",
      "condition": {
        "script": {
          "source": "ctx.results[0].hits.total.value > 10"
        }
      },
      "actions": [{
        "name": "Log high error rate",
        "destination_id": "log_destination",
        "message_template": {
          "source": "High error rate detected: {{ctx.results.0.hits.total.value}} errors in last 5 minutes"
        }
      }]
    }]
  }' 2>/dev/null || echo "Elasticsearch alerting plugin not available, using basic monitoring"

echo ""

# List created watchers
echo "Created watchers:"
curl -s "$ELASTICSEARCH_URL/_watcher/_query/watches" | jq -r '.watches[].watch.metadata.name // .watches[].watch._id' 2>/dev/null || echo "Watcher API not available"

echo ""
echo "Watcher setup complete!"
echo ""
echo "Note: Full alerting features require Elasticsearch X-Pack license."
echo "For production, consider using:"
echo "1. Elasticsearch Watcher (X-Pack)"
echo "2. ElastAlert2 (open source alternative)"
echo "3. Prometheus Alertmanager with log-based metrics"