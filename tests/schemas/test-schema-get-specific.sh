#!/bin/bash

# Schema Get Specific Version Tests
# Tests the GET /api/schemas/{subject}/{version} endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Schema Get Specific Version Tests"
echo "=========================================="

# Setup: Create test schema with multiple versions
echo
echo "Setup: Creating test schema with multiple versions..."

schema_data='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"}
    },
    "required": ["id"]
}'

create_test_schema "test-specific-version" "$schema_data"

# Create version 2
schema_v2='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"}
    },
    "required": ["id"]
}'

response=$(post_request "/api/schemas" "{
    \"subject\": \"test-specific-version\",
    \"schema\": $schema_v2,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 2 with email\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 2 creation should succeed"

# Test 1: Get specific version 1
echo
echo "Test 1: Get specific version 1"
response=$(get_request "/api/schemas/test-specific-version/1")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve version 1"
assert_json_field "$response_body" "subject" "test-specific-version"
assert_json_field "$response_body" "version" 1
assert_contains "$response_body" '"name"' "Should contain name field"
assert_not_contains "$response_body" '"email"' "Should not contain email field (added in v2)"

# Test 2: Get specific version 2
echo
echo "Test 2: Get specific version 2"
response=$(get_request "/api/schemas/test-specific-version/2")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve version 2"
assert_json_field "$response_body" "subject" "test-specific-version"
assert_json_field "$response_body" "version" 2
assert_contains "$response_body" '"name"' "Should contain name field"
assert_contains "$response_body" '"email"' "Should contain email field"

# Test 3: Try to get non-existent version
echo
echo "Test 3: Get non-existent version"
response=$(get_request "/api/schemas/test-specific-version/99")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent version"

# Test 4: Try to get version from non-existent subject
echo
echo "Test 4: Get version from non-existent subject"
response=$(get_request "/api/schemas/non-existent-subject/1")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent subject"

# Test 5: Get version 0 (invalid)
echo
echo "Test 5: Get invalid version 0"
response=$(get_request "/api/schemas/test-specific-version/0")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for invalid version 0"

# Test 6: Get negative version
echo
echo "Test 6: Get negative version"
response=$(get_request "/api/schemas/test-specific-version/-1")
http_code=$(echo "$response" | tail -n1)

# This might return 400 or 404 depending on implementation
if [ "$http_code" -eq 400 ] || [ "$http_code" -eq 404 ]; then
    log_success "Correctly rejected negative version number"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for negative version: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 7: Verify response structure matches registration
echo
echo "Test 7: Verify response structure consistency"
v1_response=$(get_request "/api/schemas/test-specific-version/1" | head -n -1)
v2_response=$(get_request "/api/schemas/test-specific-version/2" | head -n -1)

# Both should have the same subject and structure
v1_subject=$(echo "$v1_response" | grep -o '"subject":"[^"]*"' | head -1)
v2_subject=$(echo "$v2_response" | grep -o '"subject":"[^"]*"' | head -1)

if [ "$v1_subject" = "$v2_subject" ]; then
    log_success "Both versions have consistent subject"
    ((TESTS_PASSED++))
else
    log_error "Subject mismatch between versions"
    ((TESTS_FAILED++))
fi

# Test 8: Check timestamps are present and valid
echo
echo "Test 8: Verify timestamps"
assert_contains "$v1_response" '"createdAt"' "Version 1 should have createdAt"
assert_contains "$v1_response" '"updatedAt"' "Version 1 should have updatedAt"
assert_contains "$v2_response" '"createdAt"' "Version 2 should have createdAt"
assert_contains "$v2_response" '"updatedAt"' "Version 2 should have updatedAt"

echo
print_test_summary