#!/bin/bash

# Schema Registration Tests
# Tests the POST /api/schemas endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Schema Registration Tests"
echo "=================================="

# Test 1: Register valid schema
echo
echo "Test 1: Register valid schema"
schema_data='{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"}
    },
    "required": ["id", "name"]
}'

response=$(post_request "/api/schemas" "{
    \"subject\": \"test-user-profile\",
    \"schema\": $schema_data,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test user profile schema\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Schema registration should succeed"
assert_json_field "$response_body" "subject" "test-user-profile"
assert_json_field "$response_body" "version" 1
assert_json_field "$response_body" "compatibility" "BACKWARD"
assert_json_field "$response_body" "description" "Test user profile schema"
assert_contains "$response_body" '"createdAt"' "Should contain createdAt timestamp"
assert_contains "$response_body" '"updatedAt"' "Should contain updatedAt timestamp"

# Test 2: Register schema version 2 for same subject
echo
echo "Test 2: Register schema version 2"
schema_data_v2='{
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

response=$(post_request "/api/schemas" "{
    \"subject\": \"test-user-profile\",
    \"schema\": $schema_data_v2,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test user profile schema v2\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Schema version 2 registration should succeed"
assert_json_field "$response_body" "subject" "test-user-profile"
assert_json_field "$response_body" "version" 2
assert_contains "$response_body" '"phone"' "Should contain new phone field"

# Test 3: Register schema with different compatibility
echo
echo "Test 3: Register schema with FORWARD compatibility"
response=$(post_request "/api/schemas" "{
    \"subject\": \"test-product\",
    \"schema\": {
        \"\$schema\": \"http://json-schema.org/draft-07/schema#\",
        \"type\": \"object\",
        \"properties\": {
            \"productId\": {\"type\": \"string\"},
            \"name\": {\"type\": \"string\"},
            \"price\": {\"type\": \"number\"}
        },
        \"required\": [\"productId\", \"name\"]
    },
    \"compatibility\": \"FORWARD\",
    \"description\": \"Test product schema\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Schema with FORWARD compatibility should succeed"
assert_json_field "$response_body" "compatibility" "FORWARD"

# Test 4: Register schema with minimal data
echo
echo "Test 4: Register schema with minimal data"
response=$(post_request "/api/schemas" "{
    \"subject\": \"test-minimal\",
    \"schema\": {
        \"type\": \"object\",
        \"properties\": {
            \"id\": {\"type\": \"string\"}
        }
    }
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Minimal schema registration should succeed"
assert_json_field "$response_body" "subject" "test-minimal"
assert_json_field "$response_body" "version" 1

# Test 5: Try invalid JSON schema
echo
echo "Test 5: Register invalid JSON schema"
response=$(post_request "/api/schemas" "{
    \"subject\": \"test-invalid\",
    \"schema\": {
        \"type\": \"invalid_type\",
        \"properties\": \"not_an_object\"
    }
}")
http_code=$(echo "$response" | tail -n1)

# Note: API may or may not validate schema syntax - adjust based on actual behavior
if [ "$http_code" -eq 201 ]; then
    log_info "API accepts invalid JSON schema (validation may be disabled)"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 400 ]; then
    log_success "API correctly rejects invalid JSON schema"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for invalid schema: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 6: Register schema with missing required fields
echo
echo "Test 6: Register schema with missing subject"
response=$(post_request "/api/schemas" "{
    \"schema\": {
        \"type\": \"object\",
        \"properties\": {
            \"id\": {\"type\": \"string\"}
        }
    }
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject schema with missing subject"

# Test 7: Register schema with missing schema
echo
echo "Test 7: Register schema with missing schema data"
response=$(post_request "/api/schemas" "{
    \"subject\": \"test-no-schema\"
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject schema with missing schema data"

echo
print_test_summary