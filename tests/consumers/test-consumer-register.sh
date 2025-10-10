#!/bin/bash

# Consumer Registration Tests
# Tests the POST /api/consumers endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Consumer Registration Tests"
echo "===================================="

# Generate unique consumer IDs to avoid conflicts from previous test runs
TIMESTAMP=$(date +%s)
CONSUMER_1="test-mobile-app-$TIMESTAMP"
CONSUMER_2="test-minimal-$TIMESTAMP"

# Test 1: Register valid consumer
echo
echo "Test 1: Register valid consumer"
response=$(post_request "/api/consumers" "{
    \"consumerId\": \"$CONSUMER_1\",
    \"name\": \"Test Mobile App\",
    \"description\": \"Test consumer for mobile applications\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Consumer registration should succeed"
assert_json_field "$response_body" "consumerId" "$CONSUMER_1"
assert_json_field "$response_body" "name" "Test Mobile App"
assert_json_field "$response_body" "description" "Test consumer for mobile applications"
assert_contains "$response_body" '"createdAt"' "Should contain createdAt timestamp"
assert_contains "$response_body" '"updatedAt"' "Should contain updatedAt timestamp"

# Test 2: Register consumer with minimal data
echo
echo "Test 2: Register consumer with minimal required data"
response=$(post_request "/api/consumers" "{
    \"consumerId\": \"$CONSUMER_2\",
    \"name\": \"Minimal Consumer\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Minimal consumer registration should succeed"
assert_json_field "$response_body" "consumerId" "$CONSUMER_2"
assert_json_field "$response_body" "name" "Minimal Consumer"

# Test 3: Try to register duplicate consumer (should fail)
echo
echo "Test 3: Attempt duplicate consumer registration"
response=$(post_request "/api/consumers" "{
    \"consumerId\": \"$CONSUMER_1\",
    \"name\": \"Duplicate Mobile App\",
    \"description\": \"This should fail\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 400 "API should prevent duplicate consumer registration"
assert_contains "$response_body" "already exists" "Should provide descriptive error message"

# Test 4: Register consumer with invalid data (missing required field)
echo
echo "Test 4: Register consumer with missing consumerId"
response=$(post_request "/api/consumers" '{
    "name": "Invalid Consumer",
    "description": "Missing consumerId"
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject consumer with missing consumerId"

# Test 5: Register consumer with invalid data (missing name)
echo
echo "Test 5: Register consumer with missing name"
response=$(post_request "/api/consumers" '{
    "consumerId": "test-invalid",
    "description": "Missing name"
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject consumer with missing name"

# Test 6: Register consumer with empty strings
echo
echo "Test 6: Register consumer with empty consumerId"
response=$(post_request "/api/consumers" '{
    "consumerId": "",
    "name": "Empty ID Consumer"
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject consumer with empty consumerId"

echo
print_test_summary