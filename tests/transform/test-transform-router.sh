#!/bin/bash

# Router Transformation Engine Tests
# Tests the router engine's intelligent routing functionality

source "$(dirname "$0")/../utils/common.sh"

echo "Running Router Transformation Engine Tests"
echo "==========================================="

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-router-consumer-$timestamp"

# Setup: Create consumer
echo "Setup: Creating consumer and router template..."
create_test_consumer "$consumer_id" "Router Test Consumer"

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

create_router_template "$consumer_id" "$router_config"

# Test 1: Route to user normalization
echo
echo "Test 1: Route user data to user normalization"
response=$(post_request "/api/transform/$consumer_id" '{
  "canonicalJson": {
    "type": "user",
    "id": 123,
    "name": "John Doe",
    "email": "john@example.com"
  }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "User data routing should succeed"
assert_contains "$response_body" '"normalized_type":"user"' "Should contain normalized_type field"
assert_contains "$response_body" '"user_id":123' "Should contain user_id field"
assert_contains "$response_body" '"name":"John Doe"' "Should contain name field"

# Test 2: Route to electronics enrichment
echo
echo "Test 2: Route product data to electronics enrichment"
response=$(post_request "/api/transform/$consumer_id" '{
  "canonicalJson": {
    "type": "product",
    "category": "laptop",
    "price": 999.99,
    "specifications": {
      "ram": "16GB",
      "storage": "512GB SSD"
    }
  }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Product data routing should succeed"
assert_contains "$response_body" '"product_type":"product"' "Should contain product_type field"
assert_contains "$response_body" '"electronics_category":"laptop"' "Should contain electronics_category field"
assert_contains "$response_body" '"price":999.99' "Should contain price field"
assert_contains "$response_body" '"specs"' "Should contain specs field"

# Test 3: Route to default transformation
echo
echo "Test 3: Route unknown type to default transformation"
response=$(post_request "/api/transform/$consumer_id" '{
  "canonicalJson": {
    "type": "unknown",
    "data": "some value",
    "number": 42
  }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Default routing should succeed"
assert_contains "$response_body" '"data"' "Should contain data object (pass-through)"
assert_contains "$response_body" '"type":"unknown"' "Should contain original type"
assert_contains "$response_body" '"number":42' "Should contain original number"

# Test 4: Router with no routes (only default)
echo
echo "Test 4: Router with only default route"
default_only_consumer="test-router-default-$timestamp"
create_test_consumer "$default_only_consumer" "Router Default Only Consumer"

default_router_config='{
  "type": "router",
  "routes": [],
  "defaultTransformationId": "generic-transformation-v1"
}'

create_router_template "$default_only_consumer" "$default_router_config"

response=$(post_request "/api/transform/$default_only_consumer" '{
  "canonicalJson": {
    "type": "any",
    "value": "test"
  }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Default-only router should succeed"
assert_contains "$response_body" '"data"' "Should contain data object"

# Test 5: Invalid condition syntax
echo
echo "Test 5: Router with invalid condition syntax"
invalid_condition_consumer="test-router-invalid-$timestamp"
create_test_consumer "$invalid_condition_consumer" "Router Invalid Condition Consumer"

invalid_router_config='{
  "type": "router",
  "routes": [
    {
      "condition": "invalid syntax $.type",
      "transformationId": "user-normalization-v1"
    }
  ],
  "defaultTransformationId": "generic-transformation-v1"
}'

# Template creation should succeed, but transformation may fail
create_router_template "$invalid_condition_consumer" "$invalid_router_config"

response=$(post_request "/api/transform/$invalid_condition_consumer" '{
  "canonicalJson": {
    "type": "user",
    "id": 456
  }
}')
http_code=$(echo "$response" | tail -n1)

# Should either succeed with default route or fail gracefully
if [ "$http_code" -eq 200 ]; then
    log_success "Router handled invalid condition gracefully (used default route)"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 500 ]; then
    log_info "Router failed on invalid condition (expected behavior)"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for invalid condition: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 6: Unknown transformation ID
echo
echo "Test 6: Router with unknown transformation ID"
unknown_id_consumer="test-router-unknown-$timestamp"
create_test_consumer "$unknown_id_consumer" "Router Unknown ID Consumer"

unknown_router_config='{
  "type": "router",
  "routes": [
    {
      "condition": "$.type == '\''user'\''",
      "transformationId": "unknown-transformation-id"
    }
  ],
  "defaultTransformationId": "generic-transformation-v1"
}'

create_router_template "$unknown_id_consumer" "$unknown_router_config"

response=$(post_request "/api/transform/$unknown_id_consumer" '{
  "canonicalJson": {
    "type": "user",
    "id": 789
  }
}')
http_code=$(echo "$response" | tail -n1)

# Should fail with 500 due to unknown transformation ID
assert_response "$http_code" 500 "Should fail with unknown transformation ID"

# Test 7: Router configuration validation
echo
echo "Test 7: Invalid router configuration"
invalid_config_consumer="test-router-config-$timestamp"
create_test_consumer "$invalid_config_consumer" "Router Config Validation Consumer"

# Missing required fields
invalid_router_config='{
  "type": "router"
}'

response=$(post_request "/api/transform/templates/$invalid_config_consumer" "{
  \"engine\": \"router\",
  \"routerConfig\": $invalid_router_config
}")
http_code=$(echo "$response" | tail -n1)

# Should fail validation
if [ "$http_code" -eq 400 ] || [ "$http_code" -eq 500 ]; then
    log_success "Router configuration validation works"
    ((TESTS_PASSED++))
else
    log_error "Router configuration validation failed: HTTP $http_code"
    ((TESTS_FAILED++))
fi

echo
print_test_summary