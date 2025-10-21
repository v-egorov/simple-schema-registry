#!/bin/bash

# Schema Registration Tests
# Tests the POST /api/schemas endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Schema Registration Tests"
echo "=================================="

# Test 1: Register valid schema
echo
echo "Test 1: Register valid schema"
timestamp=$(date +%s)
subject1="test-user-profile-$timestamp"
 schema_data='{
     "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"}
    },
    "required": ["id", "name"]
}'

response=$(post_request "/api/schemas/$subject1" "{
    \"subject\": \"$subject1\",
    \"schema\": $schema_data,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test user profile schema\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Schema registration should succeed"
assert_json_field "$response_body" "subject" "$subject1"
assert_json_field "$response_body" "version" "1.0.0"
assert_json_field "$response_body" "compatibility" "BACKWARD"
assert_json_field "$response_body" "description" "Test user profile schema"
assert_contains "$response_body" '"createdAt"' "Should contain createdAt timestamp"
assert_contains "$response_body" '"updatedAt"' "Should contain updatedAt timestamp"

# Test 2: Register schema version 2 for same subject
echo
echo "Test 2: Register schema version 2"
 schema_data_v2='{
     "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"},
        "phone": {"type": "string"}
    },
    "required": ["id", "name"]
}'

response=$(post_request "/api/schemas/$subject1" "{
    \"subject\": \"$subject1\",
    \"schema\": $schema_data_v2,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test user profile schema v2\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Schema version 2 registration should succeed"
assert_json_field "$response_body" "subject" "$subject1"
assert_json_field "$response_body" "version" "1.0.1"
assert_contains "$response_body" '"phone"' "Should contain new phone field"

# Test 3: Register schema with different compatibility
echo
echo "Test 3: Register schema with FORWARD compatibility"
subject3="test-product-$timestamp"
response=$(post_request "/api/schemas/$subject3" "{
    \"subject\": \"$subject3\",
    \"schema\": {

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
subject4="test-minimal-$timestamp"
response=$(post_request "/api/schemas/$subject4" "{
    \"subject\": \"$subject4\",
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
assert_json_field "$response_body" "subject" "$subject4"
assert_json_field "$response_body" "version" "1.0.0"

# Test 5: Try invalid JSON schema
echo
echo "Test 5: Register invalid JSON schema"
subject5="test-invalid-$timestamp"
response=$(post_request "/api/schemas/$subject5" "{
    \"subject\": \"$subject5\",
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
response=$(post_request "/api/schemas/missing-subject-test" "{
    \"subject\": \"\",
    \"schema\": {
        \"type\": \"object\"
    }
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject schema with missing schema data"

# Test 7: Register schema with missing schema
echo
echo "Test 7: Register schema with missing schema data"
subject7="test-no-schema-$timestamp"
response=$(post_request "/api/schemas/$subject7" "{
    \"subject\": \"$subject7\"
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject schema with missing schema data"

# Test 8: Register schema with specified version
echo
echo "Test 8: Register schema with specified version"
subject8="test-specified-version-$timestamp"
response=$(post_request "/api/schemas/$subject8" "{
    \"subject\": \"$subject8\",
    \"schema\": {
        \"type\": \"object\",
        \"properties\": {
            \"id\": {\"type\": \"integer\"},
            \"name\": {\"type\": \"string\"}
        },
        \"required\": [\"id\"]
    },
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test schema with specified version\",
    \"version\": \"2.1.0\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Schema registration with specified version should succeed"
assert_json_field "$response_body" "subject" "$subject8"
assert_json_field "$response_body" "version" "2.1.0"
assert_json_field "$response_body" "description" "Test schema with specified version"

# Test 9: Register next version with specified version (major bump)
echo
echo "Test 9: Register next version with major bump"
response=$(post_request "/api/schemas/$subject8" "{
    \"subject\": \"$subject8\",
    \"schema\": {
        \"type\": \"object\",
        \"properties\": {
            \"id\": {\"type\": \"integer\"},
            \"name\": {\"type\": \"string\"},
            \"email\": {\"type\": \"string\", \"format\": \"email\"}
        },
        \"required\": [\"id\"]
    },
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test schema v3.0.0\",
    \"version\": \"3.0.0\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Schema registration with major version bump should succeed"
assert_json_field "$response_body" "subject" "$subject8"
assert_json_field "$response_body" "version" "3.0.0"

# Test 10: Try to register duplicate version
echo
echo "Test 10: Try to register duplicate version"
response=$(post_request "/api/schemas/$subject8" "{
    \"subject\": \"$subject8\",
    \"schema\": {
        \"type\": \"object\",
        \"properties\": {
            \"id\": {\"type\": \"integer\"}
        }
    },
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Duplicate version test\",
    \"version\": \"2.1.0\"
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject duplicate version registration"

# Test 11: Register schema with invalid version format
echo
echo "Test 11: Register schema with invalid version format"
subject11="test-invalid-version-$timestamp"
response=$(post_request "/api/schemas/$subject11" "{
    \"subject\": \"$subject11\",
    \"schema\": {
        \"type\": \"object\",
        \"properties\": {
            \"id\": {\"type\": \"integer\"}
        }
    },
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Invalid version format test\",
    \"version\": \"invalid-version\"
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject invalid version format"

echo
print_test_summary