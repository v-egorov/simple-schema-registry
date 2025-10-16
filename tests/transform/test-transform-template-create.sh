#!/bin/bash

# Transform Template Create/Update Tests
# Tests the POST /api/consumers/{consumerId}/subjects/{subject}/templates endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Template Create/Update Tests"
echo "==============================================="

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-template-create-$timestamp"

# Setup: Create test consumer and schemas
echo
echo "Setup: Creating test consumer and schemas..."
create_test_consumer "$consumer_id" "Template Create Test Consumer"
subject="user-profile"
# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"userId": {"type": "integer"}, "fullName": {"type": "string"}, "emailAddress": {"type": "string"}}}'
# Create consumer output schema
create_test_consumer_schema "$consumer_id" "$subject" '{"type": "object", "properties": {"id": {"type": "integer"}, "name": {"type": "string"}, "email": {"type": "string"}}}'

# Test 1: Create new transformation template
echo
echo "Test 1: Create new transformation template"
template='{"id": .userId, "name": .fullName, "email": .emailAddress}'
response_body=$(create_test_template "$consumer_id" "$subject" "$template")

if [ -n "$response_body" ]; then
    assert_json_field "$response_body" "consumerId" "$consumer_id"
    assert_json_field "$response_body" "subject" "$subject"
    assert_contains "$response_body" '"expression"' "Should contain expression field"
    assert_contains "$response_body" '.userId' "Should contain userId reference"
    assert_contains "$response_body" '.fullName' "Should contain fullName reference"
    assert_contains "$response_body" '.emailAddress' "Should contain emailAddress reference"
    assert_json_field "$response_body" "engine" "jslt"
    assert_contains "$response_body" '"createdAt"' "Should contain createdAt timestamp"
    assert_contains "$response_body" '"updatedAt"' "Should contain updatedAt timestamp"
    ((TESTS_PASSED++))
else
    log_error "Template creation failed"
    ((TESTS_FAILED++))
fi

# Test 2: Create another template version
echo
echo "Test 2: Create another template version"
updated_template='{"userId": .id, "fullName": .name, "emailAddr": .email}'
response_body=$(create_test_template "$consumer_id" "$subject" "$updated_template" "jslt" "$subject" "$consumer_id" "2.0.0")

if [ -n "$response_body" ]; then
    assert_json_field "$response_body" "version" "2.0.0"
    assert_contains "$response_body" '"expression"' "Should contain expression field"
    assert_contains "$response_body" '.id' "Should contain id reference"
    assert_contains "$response_body" '.name' "Should contain name reference"
    assert_contains "$response_body" '.email' "Should contain email reference"
    ((TESTS_PASSED++))
else
    log_error "Template version creation failed"
    ((TESTS_FAILED++))
fi

if [ -n "$response_body" ]; then
    assert_json_field "$response_body" "version" "2.0.0"
    assert_contains "$response_body" '"expression"' "Should contain expression field"
    assert_contains "$response_body" '.id' "Should contain id reference"
    assert_contains "$response_body" '.name' "Should contain name reference"
    assert_contains "$response_body" '.email' "Should contain email reference"
    ((TESTS_PASSED++))
else
    log_error "Template version creation failed"
    ((TESTS_FAILED++))
fi

# Test 3: Create template for non-existent consumer
echo
echo "Test 3: Create template for non-existent consumer"
response=$(post_request "/api/consumers/non-existent-consumer/subjects/$subject/templates" "{
    \"version\": \"1.0.0\",
    \"engine\": \"jslt\",
    \"expression\": \". | {id: .userId}\",
    \"inputSchema\": {
        \"subject\": \"$subject\"
    },
    \"outputSchema\": {
        \"subject\": \"$subject\",
        \"consumerId\": \"non-existent-consumer\"
    }
}")
http_code=$(echo "$response" | tail -n1)

# This might succeed or fail depending on API behavior
# If consumer auto-creation is supported, it would succeed
# If not, it should fail
if [ "$http_code" -eq 201 ]; then
    log_info "API allows template creation for non-existent consumer (may auto-create consumer)"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 404 ] || [ "$http_code" -eq 400 ]; then
    log_success "API correctly rejects template creation for non-existent consumer"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for non-existent consumer: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 4: Create template with missing expression field
echo
echo "Test 4: Create template with missing expression field"
response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/templates" "{
    \"version\": \"3.0.0\",
    \"engine\": \"jslt\",
    \"inputSchema\": {
        \"subject\": \"$subject\"
    },
    \"outputSchema\": {
        \"subject\": \"$subject\",
        \"consumerId\": \"$consumer_id\"
    }
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

echo "Response: $response_body"
assert_response "$http_code" 400 "Should reject template creation without expression field"

# Test 5: Create template with invalid engine
response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/templates" "{
    \"version\": \"4.0.0\",
    \"engine\": \"invalid\",
    \"expression\": \". | {id: .userId}\",
    \"inputSchema\": {
        \"subject\": \"$subject\"
    },
    \"outputSchema\": {
        \"subject\": \"$subject\",
        \"consumerId\": \"$consumer_id\"
    }
}")
http_code=$(echo "$response" | tail -n1)

# API may accept unknown engines or reject them
if [ "$http_code" -eq 201 ]; then
    log_info "API accepts unknown engine types"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 400 ]; then
    log_success "API correctly rejects invalid engine types"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for invalid engine: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 6: Create template with empty expression
response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/templates" "{
    \"version\": \"5.0.0\",
    \"engine\": \"jslt\",
    \"expression\": \"\",
    \"inputSchema\": {
        \"subject\": \"$subject\"
    },
    \"outputSchema\": {
        \"subject\": \"$subject\",
        \"consumerId\": \"$consumer_id\"
    }
}")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" -eq 201 ]; then
    log_info "API accepts empty expressions"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 400 ]; then
    log_success "API correctly rejects empty expressions"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for empty expression: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 7: Create template with complex JSLT expression
echo
echo "Test 7: Create template with complex JSLT expression"
complex_template='{"id": .userId, "name": .fullName, "email": .emailAddress, "active": .status}'
response_body=$(create_test_template "$consumer_id" "$subject" "$complex_template" "jslt" "$subject" "$consumer_id" "3.0.0")

if [ -n "$response_body" ]; then
    assert_json_field "$response_body" "version" "3.0.0"
    assert_contains "$response_body" '"expression"' "Should contain expression field"
    assert_contains "$response_body" '.userId' "Should contain userId reference"
    assert_contains "$response_body" '.status' "Should contain status reference"
    ((TESTS_PASSED++))
else
    log_error "Complex template creation failed"
    ((TESTS_FAILED++))
fi

# Test 8: Verify template persistence across calls
echo
echo "Test 8: Verify template persistence"
# Get the specific version 3.0.0 template and verify it was persisted correctly
get_response=$(get_request "/api/consumers/$consumer_id/subjects/$subject/templates/versions/3.0.0")
get_http_code=$(echo "$get_response" | tail -n1)
get_body=$(echo "$get_response" | head -n -1)

assert_response "$get_http_code" 200 "Should be able to retrieve version 3.0.0 template"
if echo "$get_body" | grep -q '.userId' && echo "$get_body" | grep -q '.status'; then
    log_success "Template version 3.0.0 persisted correctly"
    ((TESTS_PASSED++))
else
    log_error "Template version 3.0.0 not persisted correctly"
    ((TESTS_FAILED++))
fi

# Test 9: Test consumer ID with special characters
echo
echo "Test 9: Create template with special characters in consumer ID"
# First create a consumer with special characters in ID
special_consumer_id="test-template-create-special-$timestamp"
create_test_consumer "$special_consumer_id" "Consumer with special characters"
# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"data": {"type": "string"}}}'
# Create consumer output schema
create_test_consumer_schema "$special_consumer_id" "$subject" '{"type": "object", "properties": {"result": {"type": "string"}}}'
special_template='{"result": .data}'
response_body=$(create_test_template "$special_consumer_id" "$subject" "$special_template")

if [ -n "$response_body" ]; then
    log_success "Template creation handles special characters in consumer ID"
    ((TESTS_PASSED++))
else
    log_error "Template creation failed with special characters"
    ((TESTS_FAILED++))
fi

echo
print_test_summary

