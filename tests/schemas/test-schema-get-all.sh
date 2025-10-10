#!/bin/bash

# Schema Get All Versions Tests
# Tests the GET /api/schemas/{subject} endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Schema Get All Versions Tests"
echo "======================================"

# Setup: Create test schemas with multiple versions
echo
echo "Setup: Creating test schemas with multiple versions..."

# Version 1
schema_v1='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"}
    },
    "required": ["id"]
}'

create_test_schema "test-multi-version" "$schema_v1"

# Version 2
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
    \"subject\": \"test-multi-version\",
    \"schema\": $schema_v2,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 2 with email field\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 2 creation should succeed"

# Version 3
schema_v3='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"},
        "phone": {"type": "string"}
    },
    "required": ["id"]
}'

response=$(post_request "/api/schemas" "{
    \"subject\": \"test-multi-version\",
    \"schema\": $schema_v3,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 3 with phone field\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 3 creation should succeed"

# Test 1: Get all versions of existing subject
echo
echo "Test 1: Get all versions of existing subject"
response=$(get_request "/api/schemas/test-multi-version")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve all schema versions"
assert_contains "$response_body" '[' "Should return JSON array"

# Test 2: Verify we have 3 versions
echo
echo "Test 2: Verify correct number of versions"
version_count=$(echo "$response_body" | grep -o '"version"' | wc -l)
if [ "$version_count" -eq 3 ]; then
    log_success "Found 3 versions as expected"
    ((TESTS_PASSED++))
else
    log_error "Expected 3 versions, found $version_count"
    ((TESTS_FAILED++))
fi

# Test 3: Verify versions are in correct order (newest first)
echo
echo "Test 3: Verify version ordering"
versions=$(echo "$response_body" | grep -o '"version":[0-9]*' | sed 's/"version"://g' | tr '\n' ' ')
if echo "$versions" | grep -q "3 2 1"; then
    log_success "Versions are ordered correctly (newest first)"
    ((TESTS_PASSED++))
else
    log_error "Versions not ordered correctly. Found: $versions"
    ((TESTS_FAILED++))
fi

# Test 4: Verify each version has required fields
echo
echo "Test 4: Verify version structure"
assert_contains "$response_body" '"subject":"test-multi-version"' "Should contain correct subject"
assert_contains "$response_body" '"version"' "Should contain version numbers"
assert_contains "$response_body" '"schema"' "Should contain schema data"
assert_contains "$response_body" '"compatibility"' "Should contain compatibility info"
assert_contains "$response_body" '"createdAt"' "Should contain timestamps"

# Test 5: Get versions for non-existent subject
echo
echo "Test 5: Get versions for non-existent subject"
response=$(get_request "/api/schemas/non-existent-subject")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should return 200 for non-existent subject"
assert_contains "$response_body" '[]' "Should return empty array for non-existent subject"

# Test 6: Verify schema evolution (fields added over versions)
echo
echo "Test 6: Verify schema evolution"
# Version 1 should not have email or phone
# Version 2 should have email but not phone
# Version 3 should have both email and phone

v1_data=$(echo "$response_body" | sed 's/.*{"version":1,\([^}]*\)}.*/\1/')
v2_data=$(echo "$response_body" | sed 's/.*{"version":2,\([^}]*\)}.*/\1/')
v3_data=$(echo "$response_body" | sed 's/.*{"version":3,\([^}]*\)}.*/\1/')

if echo "$v1_data" | grep -q '"email"'; then
    log_error "Version 1 should not have email field"
    ((TESTS_FAILED++))
else
    log_success "Version 1 correctly does not have email field"
    ((TESTS_PASSED++))
fi

if echo "$v2_data" | grep -q '"email"' && ! echo "$v2_data" | grep -q '"phone"'; then
    log_success "Version 2 correctly has email but not phone"
    ((TESTS_PASSED++))
else
    log_error "Version 2 schema evolution incorrect"
    ((TESTS_FAILED++))
fi

if echo "$v3_data" | grep -q '"email"' && echo "$v3_data" | grep -q '"phone"'; then
    log_success "Version 3 correctly has both email and phone"
    ((TESTS_PASSED++))
else
    log_error "Version 3 schema evolution incorrect"
    ((TESTS_FAILED++))
fi

echo
print_test_summary