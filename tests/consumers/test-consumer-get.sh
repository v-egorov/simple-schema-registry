#!/bin/bash

# Consumer Get Tests
# Tests the GET /api/consumers/{consumerId} endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Consumer Get Tests"
echo "=========================="

# Setup: Create a test consumer
echo
echo "Setup: Creating test consumer..."
create_test_consumer "test-get-consumer" "Get Test Consumer" "Consumer for get testing"

# Test 1: Get existing consumer
echo
echo "Test 1: Get existing consumer"
response=$(get_request "/api/consumers/test-get-consumer")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve consumer"
assert_json_field "$response_body" "consumerId" "test-get-consumer"
assert_json_field "$response_body" "name" "Get Test Consumer"
assert_json_field "$response_body" "description" "Consumer for get testing"
assert_contains "$response_body" '"createdAt"' "Should contain createdAt timestamp"
assert_contains "$response_body" '"updatedAt"' "Should contain updatedAt timestamp"

# Test 2: Get consumer with minimal data
echo
echo "Test 2: Get consumer with minimal data"
create_test_consumer "test-minimal-consumer" "Minimal Consumer"

response=$(get_request "/api/consumers/test-minimal-consumer")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should retrieve consumer with minimal data"
assert_json_field "$response_body" "consumerId" "test-minimal-consumer"
assert_json_field "$response_body" "name" "Minimal Consumer"

# Test 3: Try to get non-existent consumer
echo
echo "Test 3: Get non-existent consumer"
response=$(get_request "/api/consumers/non-existent-consumer")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer"

# Test 4: Get consumer with special characters in ID
echo
echo "Test 4: Get consumer with special characters"
create_test_consumer "test_special-consumer.id" "Special Consumer" "Consumer with special chars in ID"

response=$(get_request "/api/consumers/test_special-consumer.id")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should handle special characters in consumer ID"
assert_json_field "$response_body" "consumerId" "test_special-consumer.id"

# Test 5: Verify timestamps are valid
echo
echo "Test 5: Verify timestamp format"
response_body=$(get_request "/api/consumers/test-get-consumer" | head -n -1)

# Check if createdAt and updatedAt look like ISO timestamps
if echo "$response_body" | grep -q '"createdAt":"[^"]*T[^"]*Z\?"'; then
    log_success "createdAt timestamp is in valid ISO format"
    ((TESTS_PASSED++))
else
    log_error "createdAt timestamp is not in expected ISO format"
    ((TESTS_FAILED++))
fi

if echo "$response_body" | grep -q '"updatedAt":"[^"]*T[^"]*Z\?"'; then
    log_success "updatedAt timestamp is in valid ISO format"
    ((TESTS_PASSED++))
else
    log_error "updatedAt timestamp is not in expected ISO format"
    ((TESTS_FAILED++))
fi

# Test 6: Check response consistency
echo
echo "Test 6: Verify response consistency across calls"
response1=$(get_request "/api/consumers/test-get-consumer" | head -n -1)
sleep 1
response2=$(get_request "/api/consumers/test-get-consumer" | head -n -1)

# createdAt should be the same, updatedAt might be the same or different
created1=$(echo "$response1" | grep -o '"createdAt":"[^"]*"' | head -1)
created2=$(echo "$response2" | grep -o '"createdAt":"[^"]*"' | head -1)

if [ "$created1" = "$created2" ]; then
    log_success "createdAt timestamp is consistent across calls"
    ((TESTS_PASSED++))
else
    log_error "createdAt timestamp changed between calls"
    ((TESTS_FAILED++))
fi

echo
print_test_summary