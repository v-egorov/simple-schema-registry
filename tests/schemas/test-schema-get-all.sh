#!/bin/bash

# Schema Get All Versions Tests
# Tests the GET /api/schemas/{subject} endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Schema Get All Versions Tests"
echo "======================================"

# Generate unique subject name to avoid conflicts with previous test runs
timestamp=$(date +%s)
subject_name="test-multi-version-$timestamp"

# Setup: Create test schemas with multiple versions
echo
echo "Setup: Creating test schemas with multiple versions for subject: $subject_name"

# Version 1
schema_v1='{
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"}
    },
    "required": ["id"]
}'

create_test_schema "$subject_name" "$schema_v1"

# Version 2
schema_v2='{
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"}
    },
    "required": ["id"]
}'

response=$(post_request "/api/schemas/$subject_name" "{
    \"subject\": \"$subject_name\",
    \"schema\": $schema_v2,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 2 with email field\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 2 creation should succeed"

# Version 3
schema_v3='{
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"},
        "phone": {"type": "string"}
    },
    "required": ["id"]
}'

response=$(post_request "/api/schemas/$subject_name" "{
    \"subject\": \"$subject_name\",
    \"schema\": $schema_v3,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 3 with phone field\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 3 creation should succeed"

# Test 1: Get all versions of existing subject
echo
echo "Test 1: Get all versions of existing subject"
response=$(get_request "/api/schemas/$subject_name/versions")
http_code=$(echo "$response" | tail -n1)
schemas_response=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve all schema versions"
assert_contains "$schemas_response" "$subject_name" "Should return schemas for the subject"

# Test 2: Verify we have 3 versions
echo
echo "Test 2: Verify correct number of versions"
version_count=$(echo "$schemas_response" | grep -o '"version"' | wc -l)
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
versions=$(echo "$schemas_response" | grep -o '"version":[0-9]*' | sed 's/"version"://g' | tr '\n' ' ')
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
assert_contains "$schemas_response" "$subject_name" "Should contain correct subject"
assert_contains "$schemas_response" '"version"' "Should contain version numbers"
assert_contains "$schemas_response" '"schema"' "Should contain schema data"
assert_contains "$schemas_response" '"compatibility"' "Should contain compatibility info"
assert_contains "$schemas_response" '"createdAt"' "Should contain timestamps"

# Test 5: Get versions for non-existent subject
echo
echo "Test 5: Get versions for non-existent subject"
response=$(get_request "/api/schemas/non-existent-subject/versions")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should return 200 for non-existent subject"
if [ "$response_body" = "[]" ]; then
    log_success "Should return empty array for non-existent subject"
    ((TESTS_PASSED++))
else
    log_error "Should return empty array for non-existent subject (got: $response_body)"
    ((TESTS_FAILED++))
fi

# Test 6: Verify schema evolution (fields added over versions)
echo
echo "Test 6: Verify schema evolution"
# Version 1 should not have email or phone
# Version 2 should have email but not phone
# Version 3 should have both email and phone

# Check that the response contains the expected fields
assert_contains "$schemas_response" "name" "Should contain name field"
assert_contains "$schemas_response" "email" "Should contain email field"
assert_contains "$schemas_response" "phone" "Should contain phone field"

# Verify that we have at least 3 versions with different schemas
version_count=$(echo "$schemas_response" | grep -o '"version"' | wc -l)
if [ "$version_count" -ge 3 ]; then
    log_success "Found at least 3 versions with schema evolution"
    ((TESTS_PASSED++))
else
    log_error "Expected at least 3 versions, found $version_count"
    ((TESTS_FAILED++))
fi

echo
print_test_summary