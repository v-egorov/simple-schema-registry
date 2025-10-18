#!/bin/bash

# 404 Not Found Error Tests
# Tests various scenarios that should return 404 status codes

source "$(dirname "$0")/../utils/common.sh"

echo "Running 404 Not Found Error Tests"
echo "================================="

# Test 1: Get non-existent consumer
echo
echo "Test 1: Get non-existent consumer"
response=$(get_request "/api/consumers/definitely-does-not-exist")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer"

# Test 2: Get non-existent schema subject
echo
echo "Test 2: Get non-existent schema subject"
response=$(get_request "/api/schemas/definitely-does-not-exist-subject")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent schema subject"

# Test 3: Get non-existent schema version
echo
echo "Test 3: Get non-existent schema version"
# First create a schema to have a valid subject
create_test_schema "test-404-schema" '{
    "type": "object",
    "properties": {"id": {"type": "string"}}
}'

response=$(get_request "/api/schemas/test-404-schema/999")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent schema version"

# Test 4: Get latest version of non-existent subject
echo
echo "Test 4: Get latest version of non-existent subject"
response=$(get_request "/api/schemas/definitely-does-not-exist/latest")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for latest version of non-existent subject"

# Test 5: Transform data for non-existent consumer
echo
echo "Test 5: Transform data for non-existent consumer"
response=$(post_request "/api/transform/definitely-does-not-exist-consumer" '{
    "canonicalJson": {"userId": 123}
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for transformation with non-existent consumer"

# Test 6: Get template for non-existent consumer
echo
echo "Test 6: Get template for non-existent consumer"
response=$(get_request "/api/transform/templates/definitely-does-not-exist-consumer")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for template of non-existent consumer"

# Test 7: Check compatibility for non-existent subject
echo
echo "Test 7: Check compatibility for non-existent subject"
response=$(post_request "/api/schemas/definitely-does-not-exist/compat" '{
    "schema": {"type": "object", "properties": {"id": {"type": "string"}}},
    "subject": "definitely-does-not-exist"
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 200 "Should return 200 for compatibility check of non-existent subject (always compatible)"

# Test 8: Invalid endpoint
echo
echo "Test 8: Invalid endpoint"
response=$(get_request "/api/invalid-endpoint")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for invalid endpoint"

# Test 9: Invalid nested endpoint
echo
echo "Test 9: Invalid nested endpoint"
response=$(get_request "/api/consumers/123/invalid-nested")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for invalid nested endpoint"

# Test 10: Case-sensitive consumer ID
echo
echo "Test 10: Case-sensitive consumer lookup"
consumer_id_case="test-case-sensitive-$(date +%s)"
create_test_consumer "$consumer_id_case" "Case Sensitive Consumer"

# Try with different case
response=$(get_request "/api/consumers/TEST-CASE-SENSITIVE")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for case mismatch (assuming case-sensitive)"

# Test 11: Consumer with template but requesting wrong consumer
echo
echo "Test 11: Transform with wrong consumer ID"
consumer_id_wrong="test-wrong-consumer-$(date +%s)"
create_test_consumer "$consumer_id_wrong" "Wrong Consumer Test"
create_test_template "$consumer_id_wrong" "test-subject" '{ "id": .userId }'

response=$(post_request "/api/transform/wrong-consumer-name" '{
    "canonicalJson": {"userId": 456}
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for wrong consumer ID in transformation"

# Test 12: Version 0 (assuming versions start at 1)
echo
echo "Test 12: Schema version 0"
response=$(get_request "/api/schemas/test-404-schema/0")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for version 0"

# Test 13: Very high version number
echo
echo "Test 13: Very high version number"
response=$(get_request "/api/schemas/test-404-schema/10000")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for very high version number"

# Test 14: Empty consumer ID in path
echo
echo "Test 14: Empty consumer ID in path"
response=$(get_request "/api/consumers/")
http_code=$(echo "$response" | tail -n1)

# This might be handled differently depending on routing
if [ "$http_code" -eq 404 ] || [ "$http_code" -eq 405 ] || [ "$http_code" -eq 400 ]; then
    log_success "API handles empty consumer ID appropriately"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for empty consumer ID: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 15: Special characters in consumer ID that don't exist
echo
echo "Test 15: Special characters in non-existent consumer ID"
response=$(get_request "/api/consumers/consumer%20with%20spaces%20that%20does%20not%20exist")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer with special characters"

echo
print_test_summary