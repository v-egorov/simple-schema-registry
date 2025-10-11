#!/bin/bash

# Transform Data Tests
# Tests the POST /api/transform/{consumerId} endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Data Tests"
echo "============================"

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-transform-consumer-$timestamp"

# Setup: Create consumer and transformation template
echo "Setup: Creating consumer and transformation template..."
create_test_consumer "$consumer_id" "Test Transform Consumer"
template='{"id": .userId, "name": .fullName, "email": .emailAddress}'
create_test_template "$consumer_id" "$template"

# Test 1: Transform valid JSON data
echo
echo "Test 1: Transform valid JSON data"
response=$(post_request "/api/transform/$consumer_id" '{
    "canonicalJson": {
        "userId": 123,
        "fullName": "John Doe",
        "emailAddress": "john@example.com",
        "internalField": "should_be_removed"
    }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Data transformation should succeed"
assert_contains "$response_body" '"transformedJson"' "Should contain transformedJson field"
assert_contains "$response_body" '"id":123' "Should contain transformed id field"
assert_contains "$response_body" '"name":"John Doe"' "Should contain transformed name field"
assert_contains "$response_body" '"email":"john@example.com"' "Should contain transformed email field"
assert_not_contains "$response_body" '"internalField"' "Should not contain internal field"
assert_not_contains "$response_body" '"userId"' "Should not contain original userId field"

# Test 2: Transform data for non-existent consumer
echo
echo "Test 2: Transform data for non-existent consumer"
response=$(post_request "/api/transform/non-existent-consumer" '{
    "canonicalJson": {
        "userId": 123
    }
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer"

# Test 3: Transform data for consumer without template
echo
echo "Test 3: Transform data for consumer without template"
no_template_consumer="test-no-template-$timestamp"
create_test_consumer "$no_template_consumer" "Consumer Without Template"

response=$(post_request "/api/transform/$no_template_consumer" '{
    "canonicalJson": {
        "userId": 789,
        "fullName": "Bob Wilson"
    }
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for consumer without template"

# Test 4: Transform with invalid JSON
echo
echo "Test 4: Transform with invalid JSON in canonicalJson"
response=$(post_request "/api/transform/test-transform-consumer" '{
    "canonicalJson": "not an object"
}')
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" -eq 400 ] || [ "$http_code" -eq 500 ]; then
    log_success "Should reject invalid JSON structure in canonicalJson"
    ((TESTS_PASSED++))
else
    log_error "Should reject invalid JSON structure in canonicalJson (expected 400 or 500, got $http_code)"
    ((TESTS_FAILED++))
fi

# Test 5: Transform with missing canonicalJson field
echo
echo "Test 5: Transform with missing canonicalJson field"
response=$(post_request "/api/transform/$consumer_id" '{
    "data": {"userId": 999}
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject request without canonicalJson field"

# Test 6: Transform with empty canonicalJson
echo
echo "Test 6: Transform with empty canonicalJson"
response=$(post_request "/api/transform/$consumer_id" '{
    "canonicalJson": {}
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should handle empty canonicalJson"
assert_contains "$response_body" '"transformedJson"' "Should return transformedJson even for empty input"

# Test 7: Transform with complex JSLT template
echo
echo "Test 7: Transform with complex JSLT template"
complex_template='{"user": {"id": .userId, "profile": {"name": .fullName, "contact": {"email": .emailAddress}}}}'
complex_consumer="test-complex-consumer-$timestamp"
create_test_consumer "$complex_consumer" "Complex Consumer"
create_test_template "$complex_consumer" "$complex_template"

response=$(post_request "/api/transform/$complex_consumer" '{
    "canonicalJson": {
        "userId": 1001,
        "fullName": "Alice Johnson",
        "emailAddress": "alice@example.com"
    }
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Complex transformation should succeed"
assert_contains "$response_body" '"user"' "Should contain user object"
assert_contains "$response_body" '"profile"' "Should contain profile object"
assert_contains "$response_body" '"contact"' "Should contain contact object"
assert_contains "$response_body" '"id":1001' "Should contain correct id"
assert_contains "$response_body" '"name":"Alice Johnson"' "Should contain correct name"
assert_contains "$response_body" '"email":"alice@example.com"' "Should contain correct email"

# Test 8: Test transformation error handling
echo
echo "Test 8: Test transformation with invalid template"
# Try to create a consumer with invalid JSLT template
invalid_template='{invalid json'
invalid_consumer="test-invalid-template-$timestamp"
create_test_consumer "$invalid_consumer" "Invalid Template Consumer"

# Template creation should fail with invalid syntax
template_result=$(create_test_template "$invalid_consumer" "$invalid_template")
if echo "$template_result" | grep -q "HTTP 400"; then
    log_success "Invalid template correctly rejected during creation"
    ((TESTS_PASSED++))
else
    log_error "Invalid template creation should have failed"
    ((TESTS_FAILED++))
fi

echo
print_test_summary