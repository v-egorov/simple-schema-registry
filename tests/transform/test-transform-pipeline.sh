#!/bin/bash

# Pipeline Transformation Engine Tests
# Tests the pipeline engine template creation

source "$(dirname "$0")/../utils/common.sh"

echo "Running Pipeline Transformation Engine Tests"
echo "============================================="

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-pipeline-consumer-$timestamp"

# Setup: Create consumer, schemas, and pipeline template
echo "Setup: Creating consumer, schemas, and pipeline template..."
create_test_consumer "$consumer_id" "Pipeline Test Consumer"
subject="test-subject"
# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"type": {"type": "string"}, "id": {"type": "integer"}, "name": {"type": "string"}}}'
# Create consumer output schema
create_test_consumer_schema "$consumer_id" "$subject" '{"type": "object", "properties": {"validated": {"type": "boolean"}, "normalized": {"type": "boolean"}, "enriched": {"type": "boolean"}, "timestamp": {"type": "string"}, "type": {"type": "string"}, "id": {"type": "integer"}, "name": {"type": "string"}}}'

# Pipeline configuration with multiple steps
pipeline_config='{
  "type": "pipeline",
  "steps": [
    {
      "name": "validate",
      "transformationId": "input-validation-v1",
      "description": "Validate input data"
    },
    {
      "name": "normalize",
      "transformationId": "data-normalization-v1",
      "description": "Normalize data structure"
    },
    {
      "name": "enrich",
      "transformationId": "data-enrichment-v1",
      "description": "Enrich with additional data"
    }
  ]
}'

create_pipeline_template "$consumer_id" "$subject" "$pipeline_config"

# Test 1: Pipeline template creation
echo
echo "Test 1: Pipeline template creation"
# Check that pipeline template was created successfully
get_response=$(get_request "/api/consumers/$consumer_id/subjects/$subject/templates/active")
get_http_code=$(echo "$get_response" | tail -n1)
get_body=$(echo "$get_response" | head -n -1)

assert_response "$get_http_code" 200 "Should be able to retrieve pipeline template"
assert_json_field "$get_body" "engine" "pipeline"
assert_contains "$get_body" '"configuration"' "Should contain pipeline configuration"

# Test 2: Pipeline template with continueOnError
echo
echo "Test 2: Pipeline template with continueOnError configuration"
error_continue_consumer="test-pipeline-error-continue-$timestamp"
create_test_consumer "$error_continue_consumer" "Pipeline Error Continue Consumer"
# Create schemas
create_test_schema "$subject" '{"type": "object", "properties": {"type": {"type": "string"}, "id": {"type": "integer"}}}'
create_test_consumer_schema "$error_continue_consumer" "$subject" '{"type": "object", "properties": {"validated": {"type": "boolean"}, "normalized": {"type": "boolean"}}}'

error_continue_config='{
  "type": "pipeline",
  "steps": [
    {
      "name": "validate",
      "transformationId": "input-validation-v1"
    },
    {
      "name": "failing",
      "transformationId": "non-existent-id",
      "continueOnError": true
    },
    {
      "name": "normalize",
      "transformationId": "data-normalization-v1"
    }
  ]
}'

create_pipeline_template "$error_continue_consumer" "$subject" "$error_continue_config"

# Verify template was created
get_response=$(get_request "/api/consumers/$error_continue_consumer/subjects/$subject/templates/active")
get_http_code=$(echo "$get_response" | tail -n1)

assert_response "$get_http_code" 200 "Pipeline template with continueOnError should be created"

echo
print_test_summary