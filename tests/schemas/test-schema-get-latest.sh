#!/bin/bash

# Schema Get Latest Version Tests
# Tests the GET /api/schemas/{subject}/latest endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Schema Get Latest Version Tests"
echo "========================================"

# Setup: Create test schema with multiple versions
echo
echo "Setup: Creating test schema with multiple versions..."
timestamp=$(date +%s)
subject1="test-latest-version-$timestamp"

schema_v1='{
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"}
    },
    "required": ["id"]
}'

create_test_schema "$subject1" "$schema_v1"

# Create version 2
schema_v2='{
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"}
    },
    "required": ["id"]
}'

response=$(post_request "/api/schemas/$subject1" "{
    \"subject\": \"$subject1\",
    \"schema\": $schema_v2,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 2 with email\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 2 creation should succeed"

# Create version 3
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

response=$(post_request "/api/schemas/$subject1" "{
    \"subject\": \"$subject1\",
    \"schema\": $schema_v3,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 3 with phone\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 3 creation should succeed"

# Test 1: Get latest version
echo
echo "Test 1: Get latest version"
response=$(get_request "/api/schemas/$subject1")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve latest version"
assert_json_field "$response_body" "subject" "$subject1"
assert_json_field "$response_body" "version" "1.0.2"
assert_contains "$response_body" '"phone"' "Should contain phone field from v3"

# Test 2: Verify it's actually the latest
echo
echo "Test 2: Verify it's the latest version"
# Get all versions and check the highest version number
all_versions=$(get_request "/api/schemas/$subject1/versions" | head -n -1)
latest_from_all=$(echo "$all_versions" | grep -o '"version":[0-9]*' | sed 's/"version"://g' | sort -n | tail -1)
latest_from_endpoint=$(echo "$response_body" | grep -o '"version":[0-9]*' | sed 's/"version"://g')

if [ "$latest_from_all" = "$latest_from_endpoint" ]; then
    log_success "Latest endpoint returns highest version number"
    ((TESTS_PASSED++))
else
    log_error "Latest endpoint version ($latest_from_endpoint) doesn't match highest version ($latest_from_all)"
    ((TESTS_FAILED++))
fi

# Test 3: Get latest for subject with only one version
echo
echo "Test 3: Get latest for single version subject"
subject3="test-single-version-$timestamp"
single_schema='{
    "type": "object",
    "properties": {"productId": {"type": "string"}},
    "required": ["productId"]
}'

create_test_schema "$subject3" "$single_schema"

response=$(get_request "/api/schemas/$subject3")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should retrieve latest for single version"
assert_json_field "$response_body" "subject" "$subject3"
assert_json_field "$response_body" "version" "1.0.0"

# Test 4: Try to get latest for non-existent subject
echo
echo "Test 4: Get latest for non-existent subject"
response=$(get_request "/api/schemas/non-existent-subject/latest")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent subject"

# Test 5: Add another version and verify latest updates
echo
echo "Test 5: Verify latest updates after new version"
schema_v4='{
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"},
        "phone": {"type": "string"},
        "address": {"type": "string"}
    },
    "required": ["id"]
}'

response=$(post_request "/api/schemas/$subject1" "{
    \"subject\": \"$subject1\",
    \"schema\": $schema_v4,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Version 4 with address\"
}")
assert_response "$(echo "$response" | tail -n1)" 201 "Version 4 creation should succeed"

# Now get latest again
response=$(get_request "/api/schemas/$subject1")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should retrieve updated latest version"
assert_json_field "$response_body" "version" "1.0.3"
assert_contains "$response_body" '"address"' "Should contain address field from v4"

# Test 6: Compare with specific version endpoint
echo
echo "Test 6: Compare latest with specific version 4"
specific_v4=$(get_request "/api/schemas/$subject1/versions/1.0.3" | head -n -1)
latest_response=$(get_request "/api/schemas/$subject1" | head -n -1)

# They should be identical (except possibly timestamps)
v4_version=$(echo "$specific_v4" | grep -o '"version":[0-9]*')
latest_version=$(echo "$latest_response" | grep -o '"version":[0-9]*')

if [ "$v4_version" = "$latest_version" ]; then
    log_success "Latest version matches specific version 4"
    ((TESTS_PASSED++))
else
    log_error "Latest version doesn't match specific version 4"
    ((TESTS_FAILED++))
fi

echo
print_test_summary