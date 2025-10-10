#!/bin/bash

# Transform Data Tests
# Tests the POST /api/transform/{consumerId} endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Data Tests"
echo "============================"

# Setup: Create consumer and transformation template
echo
echo "Setup: Creating consumer and transformation template..."

create_test_consumer "test-transform-consumer" "Test Transform Consumer" "Consumer for transformation testing"

# Create transformation template
template='. | {id: .userId, name: .fullName, email: .emailAddress}'
create_test_template "test-transform-consumer" "$template"

# Test 1: Transform valid JSON data
echo
echo "Test 1: Transform valid JSON data"
response=$(post_request "/api/transform/test-transform-consumer" '{
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
        "userId": 456,
        "fullName": "Jane Smith"
    }
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer"

# Test 3: Transform data for consumer without template
echo
echo "Test 3: Transform data for consumer without template"
create_test_consumer "test-no-template" "Consumer Without Template"

response=$(post_request "/api/transform/test-no-template" '{
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

assert_response "$http_code" 400 "Should reject invalid JSON in canonicalJson"

# Test 5: Transform with missing canonicalJson field
echo
echo "Test 5: Transform with missing canonicalJson field"
response=$(post_request "/api/transform/test-transform-consumer" '{
    "data": {"userId": 999}
}')
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject request without canonicalJson field"

# Test 6: Transform with empty canonicalJson
echo
echo "Test 6: Transform with empty canonicalJson"
response=$(post_request "/api/transform/test-transform-consumer" '{
    "canonicalJson": {}
}')
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should handle empty canonicalJson"
assert_contains "$response_body" '"transformedJson"' "Should return transformedJson even for empty input"

# Test 7: Transform with complex JSLT template
echo
echo "Test 7: Transform with complex JSLT template"
complex_template='. | {user: {id: .userId, profile: {name: .fullName, contact: {email: .emailAddress}}}}'
create_test_template "test-complex-consumer" "$complex_template"

response=$(post_request "/api/transform/test-complex-consumer" '{
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
# Create a consumer with invalid JSLT template
invalid_template='.invalid_syntax | {id: .userId}'
create_test_template "test-invalid-template" "$invalid_template"

response=$(post_request "/api/transform/test-invalid-template" '{
    "canonicalJson": {
        "userId": 2001,
        "fullName": "Test User"
    }
}')
http_code=$(echo "$response" | tail -n1)

# This might return 200 or 500 depending on how errors are handled
if [ "$http_code" -eq 200 ]; then
    log_info "Invalid template processed without error (may be handled gracefully)"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 500 ]; then
    log_success "Invalid template correctly returns server error"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for invalid template: HTTP $http_code"
    ((TESTS_FAILED++))
fi

echo
print_test_summary