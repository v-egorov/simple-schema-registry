#!/bin/bash

# Router Transformation Engine Tests
# Tests the router engine template creation

source "$(dirname "$0")/../utils/common.sh"

echo "Running Router Transformation Engine Tests"
echo "==========================================="

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-router-consumer-$timestamp"

# Setup: Create consumer, schemas, and router template
echo "Setup: Creating consumer, schemas, and router template..."
create_test_consumer "$consumer_id" "Router Test Consumer"
subject="test-subject"
# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"type": {"type": "string"}, "id": {"type": "integer"}, "name": {"type": "string"}, "email": {"type": "string"}}}'
# Create consumer output schema
create_test_consumer_schema "$consumer_id" "$subject" '{"type": "object", "properties": {"processed": {"type": "boolean"}, "route": {"type": "string"}, "type": {"type": "string"}, "id": {"type": "integer"}, "name": {"type": "string"}, "email": {"type": "string"}}}'

# Router configuration with multiple routes
router_config='{
  "type": "router",
  "routes": [
    {
      "condition": "$.type == '\''user'\''",
      "transformationId": "user-normalization-v1",
      "description": "Normalize user data"
    },
    {
      "condition": "$.type == '\''product'\''",
      "transformationId": "electronics-enrichment-v1",
      "description": "Enrich electronics product data"
    }
  ],
  "defaultTransformationId": "generic-transformation-v1"
}'

create_router_template "$consumer_id" "$subject" "$router_config"

# Test 1: Router template creation
echo
echo "Test 1: Router template creation"
# Check that router template was created successfully
get_response=$(get_request "/api/consumers/$consumer_id/subjects/$subject/templates/active")
get_http_code=$(echo "$get_response" | tail -n1)
get_body=$(echo "$get_response" | head -n -1)

assert_response "$get_http_code" 200 "Should be able to retrieve router template"
assert_json_field "$get_body" "engine" "router"
assert_contains "$get_body" '"configuration"' "Should contain router configuration"

# Test 2: Router template with validation
echo
echo "Test 2: Router template with validation configuration"
validation_consumer="test-router-validation-$timestamp"
create_test_consumer "$validation_consumer" "Router Validation Consumer"
# Create schemas
create_test_schema "$subject" '{"type": "object", "properties": {"type": {"type": "string"}, "id": {"type": "integer"}}}'
create_test_consumer_schema "$validation_consumer" "$subject" '{"type": "object", "properties": {"processed": {"type": "boolean"}, "route": {"type": "string"}}}'

validation_router_config='{
  "type": "router",
  "routes": [
    {
      "condition": "$.type == '\''user'\''",
      "transformationId": "user-processing-v1"
    }
  ],
  "defaultTransformationId": "generic-processing-v1",
  "validation": {
    "inputSchema": "canonical-data-schema-v1",
    "outputSchema": "consumer-data-schema-v1"
  }
}'

create_router_template "$validation_consumer" "$subject" "$validation_router_config"

# Verify template was created
get_response=$(get_request "/api/consumers/$validation_consumer/subjects/$subject/templates/active")
get_http_code=$(echo "$get_response" | tail -n1)

assert_response "$get_http_code" 200 "Router template with validation should be created"

echo
print_test_summary