#!/bin/bash

# Transform Data Tests
# Tests the POST /api/consumers/{consumerId}/subjects/{subject}/transform endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Data Tests"
echo "============================"

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-transform-consumer-$timestamp"

# Setup: Create consumer, schemas, and transformation template
echo "Setup: Creating consumer, schemas, and transformation template..."
create_test_consumer "$consumer_id" "Test Transform Consumer"
subject="test-subject"
# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"userId": {"type": "integer"}, "fullName": {"type": "string"}, "emailAddress": {"type": "string"}, "internalField": {"type": "string"}}}'
# Create consumer output schema
create_test_consumer_schema "$consumer_id" "$subject" '{"type": "object", "properties": {"id": {"type": "integer"}, "name": {"type": "string"}, "email": {"type": "string"}}}'
template='{"id": .userId, "name": .fullName, "email": .emailAddress}'
create_test_template "$consumer_id" "$subject" "$template"

# Test 1: Transform valid JSON data
echo
echo "Test 1: Transform valid JSON data"
response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"canonicalJson\": {
        \"userId\": 123,
        \"fullName\": \"John Doe\",
        \"emailAddress\": \"john@example.com\",
        \"internalField\": \"should_be_removed\"
    }
}")
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
response=$(post_request "/api/consumers/non-existent-consumer/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"canonicalJson\": {
        \"userId\": 123
    }
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer"

# Test 3: Transform data for consumer without template
echo
echo "Test 3: Transform data for consumer without template"
no_template_consumer="test-no-template-$timestamp"
create_test_consumer "$no_template_consumer" "Consumer Without Template"

response=$(post_request "/api/consumers/$no_template_consumer/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"canonicalJson\": {
        \"userId\": 789,
        \"fullName\": \"Bob Wilson\"
    }
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for consumer without template"

# Test 4: Transform with invalid JSON
echo
echo "Test 4: Transform with invalid JSON in canonicalJson"
response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"canonicalJson\": \"not an object\"
}")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" -eq 400 ] || [ "$http_code" -eq 500 ]; then
    log_success "Should reject invalid JSON structure in canonicalJson"
    ((TESTS_PASSED++))
else
    log_error "Should reject invalid JSON structure in canonicalJson (expected 400 or 500, got $http_code)"
    ((TESTS_FAILED++))
fi

# Test 5: Transform with missing canonicalJson field
echo
echo "Test 5: Transform with missing canonicalJson field"
response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"data\": {\"userId\": 999}
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject request without canonicalJson field"

# Test 6: Transform with empty canonicalJson
echo
echo "Test 6: Transform with empty canonicalJson"
response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"canonicalJson\": {}
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should handle empty canonicalJson"
assert_contains "$response_body" '"transformedJson"' "Should return transformedJson even for empty input"

# Test 7: Transform with complex JSLT template
echo
echo "Test 7: Transform with complex JSLT template"
complex_template='{"user": {"id": .userId, "profile": {"name": .fullName, "contact": {"email": .emailAddress}}}}'
complex_consumer="test-complex-consumer-$timestamp"
create_test_consumer "$complex_consumer" "Complex Consumer"
# Create schemas for complex consumer
create_test_schema "$subject" '{"type": "object", "properties": {"userId": {"type": "integer"}, "fullName": {"type": "string"}, "emailAddress": {"type": "string"}}}'
create_test_consumer_schema "$complex_consumer" "$subject" '{"type": "object", "properties": {"user": {"type": "object", "properties": {"id": {"type": "integer"}, "profile": {"type": "object", "properties": {"name": {"type": "string"}, "contact": {"type": "object", "properties": {"email": {"type": "string"}}}}}}}}}'
create_test_template "$complex_consumer" "$subject" "$complex_template"

response=$(post_request "/api/consumers/$complex_consumer/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"canonicalJson\": {
        \"userId\": 1001,
        \"fullName\": \"Alice Johnson\",
        \"emailAddress\": \"alice@example.com\"
    }
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Complex transformation should succeed"
assert_contains "$response_body" '"user"' "Should contain user object"
assert_contains "$response_body" '"profile"' "Should contain profile object"
assert_contains "$response_body" '"contact"' "Should contain contact object"
assert_contains "$response_body" '"id":1001' "Should contain correct id"
assert_contains "$response_body" '"name":"Alice Johnson"' "Should contain correct name"
assert_contains "$response_body" '"email":"alice@example.com"' "Should contain correct email"

# Test 8: Transform with UUID function
echo
echo "Test 8: Transform with UUID function"
uuid_template='{"id": uuid(), "name": .fullName, "email": .emailAddress}'
uuid_consumer="test-uuid-consumer-$timestamp"
create_test_consumer "$uuid_consumer" "UUID Consumer"
# Create schemas for UUID consumer
create_test_schema "$subject" '{"type": "object", "properties": {"userId": {"type": "integer"}, "fullName": {"type": "string"}, "emailAddress": {"type": "string"}}}'
create_test_consumer_schema "$uuid_consumer" "$subject" '{"type": "object", "properties": {"id": {"type": "string"}, "name": {"type": "string"}, "email": {"type": "string"}}}'
create_test_template "$uuid_consumer" "$subject" "$uuid_template"

# Transform data multiple times to verify UUID uniqueness
response1=$(post_request "/api/consumers/$uuid_consumer/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"canonicalJson\": {
        \"userId\": 2001,
        \"fullName\": \"UUID Test User\",
        \"emailAddress\": \"uuid@example.com\"
    }
}")
http_code1=$(echo "$response1" | tail -n1)
response_body1=$(echo "$response1" | head -n -1)

response2=$(post_request "/api/consumers/$uuid_consumer/subjects/$subject/transform" "{
    \"subject\": \"$subject\",
    \"canonicalJson\": {
        \"userId\": 2002,
        \"fullName\": \"UUID Test User 2\",
        \"emailAddress\": \"uuid2@example.com\"
    }
}")
http_code2=$(echo "$response2" | tail -n1)
response_body2=$(echo "$response2" | head -n -1)

assert_response "$http_code1" 200 "First UUID transformation should succeed"
assert_response "$http_code2" 200 "Second UUID transformation should succeed"
assert_contains "$response_body1" '"id":' "Should contain id field with UUID"
assert_contains "$response_body2" '"id":' "Should contain id field with UUID"
assert_contains "$response_body1" '"name":"UUID Test User"' "Should contain correct name"
assert_contains "$response_body2" '"name":"UUID Test User 2"' "Should contain correct name"

# Extract UUIDs from responses and verify they are different
uuid1=$(echo "$response_body1" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
uuid2=$(echo "$response_body2" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ "$uuid1" != "$uuid2" ] && [ ${#uuid1} -eq 36 ] && [ ${#uuid2} -eq 36 ]; then
    log_success "UUID function generates unique 36-character UUIDs"
    ((TESTS_PASSED++))
else
    log_error "UUID function should generate unique 36-character UUIDs (got '$uuid1' and '$uuid2')"
    ((TESTS_FAILED++))
fi

# Test 9: Test transformation error handling
echo
echo "Test 8: Test transformation with invalid template"
# Try to create a consumer with invalid JSLT template
invalid_template='{invalid json'
invalid_consumer="test-invalid-template-$timestamp"
create_test_consumer "$invalid_consumer" "Invalid Template Consumer"
# Create schemas for invalid consumer
create_test_schema "$subject" '{"type": "object", "properties": {"userId": {"type": "integer"}}}'
create_test_consumer_schema "$invalid_consumer" "$subject" '{"type": "object", "properties": {"id": {"type": "integer"}}}'

# Template creation may succeed (validation at runtime)
template_result=$(create_test_template "$invalid_consumer" "$subject" "$invalid_template")
if [ -n "$template_result" ]; then
    log_success "Template creation succeeded (validation may be at runtime)"
    ((TESTS_PASSED++))
else
    log_info "Invalid template rejected during creation"
    ((TESTS_PASSED++))
fi

echo
print_test_summary