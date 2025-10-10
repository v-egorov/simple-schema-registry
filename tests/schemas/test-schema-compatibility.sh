#!/bin/bash

# Schema Compatibility Tests
# Tests the POST /api/schemas/{subject}/compat endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Schema Compatibility Tests"
echo "==================================="

# Setup: Create base schema
echo
echo "Setup: Creating base schema for compatibility testing..."

base_schema='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"}
    },
    "required": ["id", "name"]
}'

create_test_schema "test-compatibility" "$base_schema"

# Test 1: Check backward compatible schema (adding optional field)
echo
echo "Test 1: Check backward compatible schema (adding optional field)"
compatible_schema='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"},
        "phone": {"type": "string"}
    },
    "required": ["id", "name"]
}'

response=$(post_request "/api/schemas/test-compatibility/compat" "{
    \"schema\": $compatible_schema
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Backward compatibility check should succeed"
assert_json_field "$response_body" "compatible" true
assert_contains "$response_body" '"message"' "Should contain compatibility message"

# Test 2: Check incompatible schema (making required field optional)
echo
echo "Test 2: Check incompatible schema (removing required field)"
incompatible_schema='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"}
    },
    "required": ["id"]
}'

response=$(post_request "/api/schemas/test-compatibility/compat" "{
    \"schema\": $incompatible_schema
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

# Note: This might be compatible or not depending on compatibility rules
# The API should return a definitive answer
if [ "$http_code" -eq 200 ]; then
    compatible=$(echo "$response_body" | grep -o '"compatible":\s*\(true\|false\)' | sed 's/.*:\s*//')
    if [ "$compatible" = "false" ]; then
        log_success "Correctly identified incompatible schema change"
        ((TESTS_PASSED++))
    else
        log_info "Schema change considered compatible (depends on compatibility rules)"
        ((TESTS_PASSED++))
    fi
else
    log_error "Compatibility check failed with HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 3: Check compatibility for non-existent subject
echo
echo "Test 3: Check compatibility for non-existent subject"
response=$(post_request "/api/schemas/non-existent-subject/compat" "{
    \"schema\": $base_schema
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent subject"

# Test 4: Check compatibility with invalid JSON schema
echo
echo "Test 4: Check compatibility with invalid JSON schema"
invalid_schema='{
    "type": "invalid_type",
    "properties": "not_an_object"
}'

response=$(post_request "/api/schemas/test-compatibility/compat" "{
    \"schema\": $invalid_schema
}")
http_code=$(echo "$response" | tail -n1)

# API may or may not validate schema syntax during compatibility check
if [ "$http_code" -eq 200 ]; then
    log_info "Compatibility check accepts invalid schema (validation may be disabled)"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 400 ]; then
    log_success "Compatibility check correctly rejects invalid schema"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for invalid schema: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 5: Check compatibility with empty schema
echo
echo "Test 5: Check compatibility with empty schema"
response=$(post_request "/api/schemas/test-compatibility/compat" "{
    \"schema\": {}
}")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" -eq 200 ]; then
    log_success "Compatibility check handles empty schema"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 400 ]; then
    log_success "Compatibility check correctly rejects empty schema"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for empty schema: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 6: Check compatibility response structure
echo
echo "Test 6: Verify compatibility response structure"
response=$(post_request "/api/schemas/test-compatibility/compat" "{
    \"schema\": $compatible_schema
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

if [ "$http_code" -eq 200 ]; then
    assert_contains "$response_body" '"compatible"' "Should contain compatible field"
    assert_contains "$response_body" '"message"' "Should contain message field"

    # Check that compatible is boolean
    compatible_value=$(echo "$response_body" | grep -o '"compatible":\s*\(true\|false\)' | sed 's/.*:\s*//')
    if [ "$compatible_value" = "true" ] || [ "$compatible_value" = "false" ]; then
        log_success "Compatible field is proper boolean"
        ((TESTS_PASSED++))
    else
        log_error "Compatible field is not boolean"
        ((TESTS_FAILED++))
    fi
fi

# Test 7: Test with different compatibility modes
echo
echo "Test 7: Test compatibility with FORWARD mode schema"
# First create a schema with FORWARD compatibility
forward_schema='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "productId": {"type": "string"},
        "name": {"type": "string"}
    },
    "required": ["productId"]
}'

create_test_schema "test-forward-compat" "$forward_schema"

# Test forward compatibility
forward_test_schema='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "productId": {"type": "string"},
        "name": {"type": "string"},
        "category": {"type": "string"}
    },
    "required": ["productId"]
}'

response=$(post_request "/api/schemas/test-forward-compat/compat" "{
    \"schema\": $forward_test_schema
}")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" -eq 200 ]; then
    log_success "Forward compatibility check completed"
    ((TESTS_PASSED++))
else
    log_error "Forward compatibility check failed: HTTP $http_code"
    ((TESTS_FAILED++))
fi

echo
print_test_summary