#!/bin/bash

# Script to set up Elasticsearch Index Lifecycle Management policies for ecommerce logs

ELASTICSEARCH_URL="http://localhost:9200"

echo "Setting up ILM policies for ecommerce logs..."

# Wait for Elasticsearch to be ready
until curl -s "$ELASTICSEARCH_URL/_cluster/health" | grep -q '"status":"green\|yellow"'; do
  echo "Waiting for Elasticsearch to be ready..."
  sleep 5
done

echo "Elasticsearch is ready. Creating ILM policies..."

# Create ILM policy for application logs (30 days retention)
curl -X PUT "$ELASTICSEARCH_URL/_ilm/policy/ecommerce-logs-policy" \
  -H "Content-Type: application/json" \
  -d '{
    "policy": {
      "phases": {
        "hot": {
          "min_age": "0ms",
          "actions": {
            "rollover": {
              "max_size": "5gb",
              "max_age": "1d",
              "max_docs": 10000000
            },
            "set_priority": {
              "priority": 100
            }
          }
        },
        "warm": {
          "min_age": "7d",
          "actions": {
            "set_priority": {
              "priority": 50
            },
            "allocate": {
              "number_of_replicas": 0
            },
            "forcemerge": {
              "max_num_segments": 1
            }
          }
        },
        "cold": {
          "min_age": "14d",
          "actions": {
            "set_priority": {
              "priority": 0
            },
            "allocate": {
              "number_of_replicas": 0
            }
          }
        },
        "delete": {
          "min_age": "30d",
          "actions": {
            "delete": {}
          }
        }
      }
    }
  }'

echo ""

# Create ILM policy for error logs (90 days retention)
curl -X PUT "$ELASTICSEARCH_URL/_ilm/policy/ecommerce-error-logs-policy" \
  -H "Content-Type: application/json" \
  -d '{
    "policy": {
      "phases": {
        "hot": {
          "min_age": "0ms",
          "actions": {
            "rollover": {
              "max_size": "2gb",
              "max_age": "1d",
              "max_docs": 1000000
            },
            "set_priority": {
              "priority": 100
            }
          }
        },
        "warm": {
          "min_age": "7d",
          "actions": {
            "set_priority": {
              "priority": 50
            },
            "allocate": {
              "number_of_replicas": 0
            },
            "forcemerge": {
              "max_num_segments": 1
            }
          }
        },
        "cold": {
          "min_age": "30d",
          "actions": {
            "set_priority": {
              "priority": 0
            },
            "allocate": {
              "number_of_replicas": 0
            }
          }
        },
        "delete": {
          "min_age": "90d",
          "actions": {
            "delete": {}
          }
        }
      }
    }
  }'

echo ""

# Create ILM policy for audit logs (1 year retention)
curl -X PUT "$ELASTICSEARCH_URL/_ilm/policy/ecommerce-audit-logs-policy" \
  -H "Content-Type: application/json" \
  -d '{
    "policy": {
      "phases": {
        "hot": {
          "min_age": "0ms",
          "actions": {
            "rollover": {
              "max_size": "10gb",
              "max_age": "7d",
              "max_docs": 50000000
            },
            "set_priority": {
              "priority": 100
            }
          }
        },
        "warm": {
          "min_age": "30d",
          "actions": {
            "set_priority": {
              "priority": 50
            },
            "allocate": {
              "number_of_replicas": 0
            },
            "forcemerge": {
              "max_num_segments": 1
            }
          }
        },
        "cold": {
          "min_age": "90d",
          "actions": {
            "set_priority": {
              "priority": 0
            },
            "allocate": {
              "number_of_replicas": 0
            }
          }
        },
        "delete": {
          "min_age": "365d",
          "actions": {
            "delete": {}
          }
        }
      }
    }
  }'

echo ""

# Create index template for application logs
curl -X PUT "$ELASTICSEARCH_URL/_index_template/ecommerce-logs-template" \
  -H "Content-Type: application/json" \
  -d '{
    "index_patterns": ["ecommerce-logs-*"],
    "template": {
      "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
        "index.lifecycle.name": "ecommerce-logs-policy",
        "index.lifecycle.rollover_alias": "ecommerce-logs",
        "index.refresh_interval": "5s",
        "index.codec": "best_compression"
      },
      "mappings": {
        "properties": {
          "@timestamp": { "type": "date" },
          "message": { "type": "text", "analyzer": "standard" },
          "log_level": { "type": "keyword" },
          "logger_name": { "type": "keyword" },
          "service_name": { "type": "keyword" },
          "correlation_id": { "type": "keyword" },
          "tenant_id": { "type": "keyword" },
          "user_id": { "type": "keyword" },
          "environment": { "type": "keyword" },
          "http_status": { "type": "integer" },
          "http_duration": { "type": "long" },
          "exception_class": { "type": "keyword" },
          "tags": { "type": "keyword" }
        }
      }
    },
    "priority": 200,
    "composed_of": [],
    "version": 1,
    "_meta": {
      "description": "Template for ecommerce application logs"
    }
  }'

echo ""

# Create initial index with alias
curl -X PUT "$ELASTICSEARCH_URL/ecommerce-logs-000001" \
  -H "Content-Type: application/json" \
  -d '{
    "aliases": {
      "ecommerce-logs": {
        "is_write_index": true
      }
    }
  }'

echo ""
echo "ILM policies and index templates created successfully!"

# Show created policies
echo "Created ILM policies:"
curl -s "$ELASTICSEARCH_URL/_ilm/policy" | jq -r 'keys[]' | grep ecommerce

echo ""
echo "Setup complete!"