#!/bin/bash

# Multi-Version JSON Schema Support Tests
# Tests support for different JSON Schema versions (draft-04, draft-06, draft-07, draft-2019-09)

source "$(dirname "$0")/../utils/common.sh"

echo "Running Multi-Version JSON Schema Support Tests"
echo "==============================================="

# Test 1: Register draft-06 schema
echo
echo "Test 1: Register draft-06 schema"
timestamp=$(date +%s)
subject1="test-draft06-$timestamp"
schema_data=$(cat "$(dirname "$0")/../examples/compatibility/draft06-schema.json")

response=$(post_request "/api/schemas/$subject1" "{
    \"subject\": \"$subject1\",
    \"schema\": $schema_data,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test draft-06 schema\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Draft-06 schema registration should succeed"
assert_json_field "$response_body" "subject" "$subject1"
assert_json_field "$response_body" "version" "1.0.0"

# Test 2: Register draft-07 schema
echo
echo "Test 2: Register draft-07 schema"
subject2="test-draft07-$timestamp"
schema_data=$(cat "$(dirname "$0")/../examples/compatibility/draft07-schema.json")

response=$(post_request "/api/schemas/$subject2" "{
    \"subject\": \"$subject2\",
    \"schema\": $schema_data,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test draft-07 schema\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Draft-07 schema registration should succeed"
assert_json_field "$response_body" "subject" "$subject2"
assert_json_field "$response_body" "version" "1.0.0"

# Test 3: Register draft-2019-09 schema
echo
echo "Test 3: Register draft-2019-09 schema"
subject3="test-draft2019-$timestamp"
schema_data=$(cat "$(dirname "$0")/../examples/compatibility/draft2019-schema.json")

response=$(post_request "/api/schemas/$subject3" "{
    \"subject\": \"$subject3\",
    \"schema\": $schema_data,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test draft-2019-09 schema\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Draft-2019-09 schema registration should succeed"
assert_json_field "$response_body" "subject" "$subject3"
assert_json_field "$response_body" "version" "1.0.0"

# Test 4: Reject draft-2020-12 schema
echo
echo "Test 4: Reject draft-2020-12 schema"
subject4="test-draft2020-$timestamp"
schema_data=$(cat "$(dirname "$0")/../examples/compatibility/draft2020-schema.json")

response=$(post_request "/api/schemas/$subject4" "{
    \"subject\": \"$subject4\",
    \"schema\": $schema_data,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test draft-2020-12 schema\"
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Draft-2020-12 schema registration should be rejected"

# Test 5: Validate data against draft-06 schema
echo
echo "Test 5: Validate data against draft-06 schema"
valid_data='{"id": 123, "name": "John Doe", "email": "john@example.com"}'

response=$(post_request "/api/schemas/$subject1/validate" "{
    \"subject\": \"$subject1\",
    \"jsonData\": $valid_data
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Validation against draft-06 schema should succeed"
assert_json_field "$response_body" "valid" "true"
assert_json_field "$response_body" "subject" "$subject1"
assert_json_field "$response_body" "schemaVersion" "1.0.0"

# Test 6: Validate data against draft-07 schema
echo
echo "Test 6: Validate data against draft-07 schema"
response=$(post_request "/api/schemas/$subject2/validate" "{
    \"subject\": \"$subject2\",
    \"jsonData\": $valid_data
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Validation against draft-07 schema should succeed"
assert_json_field "$response_body" "valid" "true"
assert_json_field "$response_body" "subject" "$subject2"
assert_json_field "$response_body" "schemaVersion" "1.0.0"

# Test 7: Validate data against draft-2019-09 schema
echo
echo "Test 7: Validate data against draft-2019-09 schema"
response=$(post_request "/api/schemas/$subject3/validate" "{
    \"subject\": \"$subject3\",
    \"jsonData\": $valid_data
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Validation against draft-2019-09 schema should succeed"
assert_json_field "$response_body" "valid" "true"
assert_json_field "$response_body" "subject" "$subject3"
assert_json_field "$response_body" "schemaVersion" "1.0.0"



# Test 9: Test invalid schema version
echo
echo "Test 9: Reject unsupported schema version"
subject5="test-invalid-version-$timestamp"
invalid_schema_data='{
    "$schema": "http://json-schema.org/draft-03/schema#",
    "type": "object",
    "properties": {
        "id": {"type": "integer"}
    }
}'

response=$(post_request "/api/schemas/$subject5" "{
    \"subject\": \"$subject5\",
    \"schema\": $invalid_schema_data,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Test invalid schema version\"
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Unsupported schema version should be rejected"

echo
echo "All multi-version schema tests completed successfully!"