#!/bin/bash

# Pipeline Transformation Engine Tests
# Tests the pipeline engine's sequential multi-step transformations

source "$(dirname "$0")/../utils/common.sh"

echo "Running Pipeline Transformation Engine Tests"
echo "============================================="

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-pipeline-consumer-$timestamp"

# Setup: Create consumer
echo "Setup: Creating consumer and pipeline template..."
create_test_consumer "$consumer_id" "Pipeline Test Consumer"

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

create_pipeline_template "$consumer_id" "$pipeline_config"

# Test 1: Successful pipeline execution
echo
echo "Test 1: Successful pipeline execution with all steps"
response=$(post_request "/api/transform/$consumer_id" '{
  "canonicalJson": {
    "type": "test",
    "id": 123,
    "name": "Test Data"
  }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Pipeline execution should succeed"
assert_contains "$response_body" '"validated":true' "Should contain validation flag"
assert_contains "$response_body" '"normalized":true' "Should contain normalization flag"
assert_contains "$response_body" '"enriched":true' "Should contain enrichment flag"
assert_contains "$response_body" '"timestamp"' "Should contain timestamp from enrichment"
assert_contains "$response_body" '"type":"test"' "Should preserve original data"
assert_contains "$response_body" '"id":123' "Should preserve original id"
assert_contains "$response_body" '"name":"Test Data"' "Should preserve original name"

# Test 2: Pipeline with continueOnError
echo
echo "Test 2: Pipeline with continueOnError flag"
error_continue_consumer="test-pipeline-error-continue-$timestamp"
create_test_consumer "$error_continue_consumer" "Pipeline Error Continue Consumer"

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

create_pipeline_template "$error_continue_consumer" "$error_continue_config"

response=$(post_request "/api/transform/$error_continue_consumer" '{
  "canonicalJson": {
    "type": "test",
    "id": 456
  }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Pipeline with continueOnError should succeed"
assert_contains "$response_body" '"validated":true' "Should have validation step"
assert_contains "$response_body" '"normalized":true' "Should have normalization step after error"

# Test 3: Pipeline failure without continueOnError
echo
echo "Test 3: Pipeline failure without continueOnError"
error_fail_consumer="test-pipeline-error-fail-$timestamp"
create_test_consumer "$error_fail_consumer" "Pipeline Error Fail Consumer"

error_fail_config='{
  "type": "pipeline",
  "steps": [
    {
      "name": "validate",
      "transformationId": "input-validation-v1"
    },
    {
      "name": "failing",
      "transformationId": "non-existent-id",
      "continueOnError": false
    },
    {
      "name": "normalize",
      "transformationId": "data-normalization-v1"
    }
  ]
}'

create_pipeline_template "$error_fail_consumer" "$error_fail_config"

response=$(post_request "/api/transform/$error_fail_consumer" '{
  "canonicalJson": {
    "type": "test",
    "id": 789
  }
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 500 "Pipeline without continueOnError should fail on error step"

# Test 4: Single step pipeline
echo
echo "Test 4: Single step pipeline"
single_step_consumer="test-pipeline-single-$timestamp"
create_test_consumer "$single_step_consumer" "Pipeline Single Step Consumer"

single_step_config='{
  "type": "pipeline",
  "steps": [
    {
      "name": "validate",
      "transformationId": "input-validation-v1"
    }
  ]
}'

create_pipeline_template "$single_step_consumer" "$single_step_config"

response=$(post_request "/api/transform/$single_step_consumer" '{
  "canonicalJson": {
    "type": "single",
    "id": 111
  }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Single step pipeline should succeed"
assert_contains "$response_body" '"validated":true' "Should contain validation flag"
assert_contains "$response_body" '"type":"single"' "Should preserve original data"

# Test 5: Unknown transformation ID in pipeline
echo
echo "Test 5: Pipeline with unknown transformation ID"
unknown_id_consumer="test-pipeline-unknown-$timestamp"
create_test_consumer "$unknown_id_consumer" "Pipeline Unknown ID Consumer"

unknown_pipeline_config='{
  "type": "pipeline",
  "steps": [
    {
      "name": "unknown",
      "transformationId": "completely-unknown-id"
    }
  ]
}'

create_pipeline_template "$unknown_id_consumer" "$unknown_pipeline_config"

response=$(post_request "/api/transform/$unknown_id_consumer" '{
  "canonicalJson": {
    "type": "test"
  }
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 500 "Pipeline with unknown transformation ID should fail"

# Test 6: Pipeline configuration validation
echo
echo "Test 6: Invalid pipeline configuration"
invalid_config_consumer="test-pipeline-config-$timestamp"
create_test_consumer "$invalid_config_consumer" "Pipeline Config Validation Consumer"

# Empty steps array
invalid_pipeline_config='{
  "type": "pipeline",
  "steps": []
}'

response=$(post_request "/api/transform/templates/$invalid_config_consumer" "{
  \"engine\": \"pipeline\",
  \"pipelineConfig\": $invalid_pipeline_config
}")
http_code=$(echo "$response" | tail -n1)

# Should fail validation (empty steps)
if [ "$http_code" -eq 400 ] || [ "$http_code" -eq 500 ]; then
    log_success "Pipeline configuration validation works for empty steps"
    ((TESTS_PASSED++))
else
    log_error "Pipeline configuration validation failed for empty steps: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 7: Complex pipeline with data transformation
echo
echo "Test 7: Complex pipeline with data transformation"
complex_consumer="test-pipeline-complex-$timestamp"
create_test_consumer "$complex_consumer" "Pipeline Complex Consumer"

complex_config='{
  "type": "pipeline",
  "steps": [
    {
      "name": "validate",
      "transformationId": "input-validation-v1"
    },
    {
      "name": "normalize",
      "transformationId": "data-normalization-v1"
    },
    {
      "name": "enrich",
      "transformationId": "data-enrichment-v1"
    }
  ]
}'

create_pipeline_template "$complex_consumer" "$complex_config"

response=$(post_request "/api/transform/$complex_consumer" '{
  "canonicalJson": {
    "userId": 999,
    "fullName": "Complex User",
    "emailAddress": "complex@example.com",
    "metadata": {
      "source": "test",
      "version": "1.0"
    }
  }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Complex pipeline should succeed"
assert_contains "$response_body" '"validated":true' "Should have validation"
assert_contains "$response_body" '"normalized":true' "Should have normalization"
assert_contains "$response_body" '"enriched":true' "Should have enrichment"
assert_contains "$response_body" '"timestamp"' "Should have timestamp"
assert_contains "$response_body" '"userId":999' "Should preserve userId"
assert_contains "$response_body" '"fullName":"Complex User"' "Should preserve fullName"

echo
print_test_summary