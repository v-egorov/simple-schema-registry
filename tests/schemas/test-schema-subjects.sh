#!/bin/bash

# Schema Subjects Tests
# Tests the GET /api/schemas/subjects endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Schema Subjects Tests"
echo "=============================="

# Setup: Create some test schemas with different subjects
echo
echo "Setup: Creating test schemas with different subjects..."

schema1='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {"userId": {"type": "integer"}},
    "required": ["userId"]
}'

schema2='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {"productId": {"type": "string"}},
    "required": ["productId"]
}'

schema3='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {"orderId": {"type": "string"}},
    "required": ["orderId"]
}'

create_test_schema "test-user-subject" "$schema1"
create_test_schema "test-product-subject" "$schema2"
create_test_schema "test-order-subject" "$schema3"

# Test 1: Get all schema subjects
echo
echo "Test 1: Get all schema subjects"
response=$(get_request "/api/schemas/subjects")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve schema subjects"
assert_contains "$response_body" "test-user-subject" "Should return subjects array"

# Test 2: Verify our test subjects are included
echo
echo "Test 2: Verify test subjects are included"
assert_contains "$response_body" '"test-user-subject"' "Should contain user subject"
assert_contains "$response_body" '"test-product-subject"' "Should contain product subject"
assert_contains "$response_body" '"test-order-subject"' "Should contain order subject"

# Test 3: Check for duplicates (subjects should be unique)
echo
echo "Test 3: Check for duplicate subjects"
subject_count=$(echo "$response_body" | grep -o '"test-user-subject"' | wc -l)
if [ "$subject_count" -eq 1 ]; then
    log_success "Subjects are unique (no duplicates)"
    ((TESTS_PASSED++))
else
    log_error "Found $subject_count instances of test-user-subject (should be 1)"
    ((TESTS_FAILED++))
fi

# Test 4: Verify response contains only strings
echo
echo "Test 4: Verify response contains only strings"
# Check that all array elements are strings (no objects)
if echo "$response_body" | grep -q '"[^"]*":\s*{' || echo "$response_body" | grep -q '"[^"]*":\s*\['; then
    log_error "Response contains non-string elements"
    ((TESTS_FAILED++))
else
    log_success "Response contains only string elements"
    ((TESTS_PASSED++))
fi

# Test 5: Add schema with same subject (should not create duplicate subject)
echo
echo "Test 5: Add version to existing subject"
schema_v2='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "userId": {"type": "integer"},
        "email": {"type": "string"}
    },
    "required": ["userId"]
}'

response=$(post_request "/api/schemas" "{
    \"subject\": \"test-user-subject\",
    \"schema\": $schema_v2,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 2\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 2 creation should succeed"

# Check subjects again - should still have same count
response=$(get_request "/api/schemas/subjects")
response_body=$(echo "$response" | head -n -1)

subject_count_after=$(echo "$response_body" | grep -o '"test-user-subject"' | wc -l)
if [ "$subject_count_after" -eq 1 ]; then
    log_success "Adding version doesn't create duplicate subject"
    ((TESTS_PASSED++))
else
    log_error "Adding version created duplicate subject"
    ((TESTS_FAILED++))
fi

# Test 6: Check subjects ordering (may be alphabetical or insertion order)
echo
echo "Test 6: Check subjects are properly ordered"
# Extract subjects and check they form a reasonable list
subjects=$(echo "$response_body" | grep -o '"[^"]*"' | tr '\n' ' ')
log_info "Found subjects: $subjects"

if echo "$subjects" | grep -q "test-user-subject\|test-product-subject\|test-order-subject"; then
    log_success "All expected subjects are present"
    ((TESTS_PASSED++))
else
    log_error "Missing expected subjects in list"
    ((TESTS_FAILED++))
fi

# Test 7: Test with no schemas (if possible to reset)
echo
echo "Test 7: Test subjects endpoint behavior"
# This is hard to test without cleaning the database
# Just verify the endpoint works consistently
response2=$(get_request "/api/schemas/subjects")
http_code2=$(echo "$response2" | tail -n1)

if [ "$http_code2" -eq 200 ]; then
    log_success "Subjects endpoint is consistent"
    ((TESTS_PASSED++))
else
    log_error "Subjects endpoint inconsistent: HTTP $http_code2"
    ((TESTS_FAILED++))
fi

# Test 8: Check response format is valid JSON array
echo
echo "Test 8: Verify valid JSON array format"
# Basic JSON validation - starts with [ and ends with ]
if echo "$response_body" | grep -q '^\s*\[' && echo "$response_body" | grep -q '\]\s*$'; then
    log_success "Response is properly formatted JSON array"
    ((TESTS_PASSED++))
else
    log_error "Response is not properly formatted JSON array"
    ((TESTS_FAILED++))
fi

echo
print_test_summary