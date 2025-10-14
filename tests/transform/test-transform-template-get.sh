#!/bin/bash

# Transform Template Get Tests
# Tests the GET /api/consumers/{consumerId}/subjects/{subject}/templates/active endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Template Get Tests"
echo "====================================="

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-template-get-$timestamp"

# Setup: Create consumer, schemas, and transformation template
echo "Setup: Creating consumer, schemas, and transformation template..."
create_test_consumer "$consumer_id" "Template Get Test Consumer"
subject="user-profile"
# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"userId": {"type": "integer"}, "fullName": {"type": "string"}, "emailAddress": {"type": "string"}}}'
# Create consumer output schema
create_test_consumer_schema "$consumer_id" "$subject" '{"type": "object", "properties": {"id": {"type": "integer"}, "name": {"type": "string"}, "email": {"type": "string"}}}'
template='{"id": .userId, "name": .fullName, "email": .emailAddress}'
create_test_template "$consumer_id" "$subject" "$template"

# Test 1: Get existing transformation template
echo
echo "Test 1: Get existing transformation template"
response=$(get_request "/api/consumers/$consumer_id/subjects/$subject/templates/active")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve template"
assert_json_field "$response_body" "consumerId" "$consumer_id"
assert_json_field "$response_body" "subject" "$subject"
assert_contains "$response_body" '"expression"' "Should contain expression field"
assert_contains "$response_body" '.userId' "Should contain userId reference"
assert_contains "$response_body" '.fullName' "Should contain fullName reference"
assert_contains "$response_body" '.emailAddress' "Should contain emailAddress reference"
assert_json_field "$response_body" "engine" "jslt"
assert_contains "$response_body" '"createdAt"' "Should contain createdAt timestamp"
assert_contains "$response_body" '"updatedAt"' "Should contain updatedAt timestamp"

# Test 2: Get template for non-existent consumer
echo
echo "Test 2: Get template for non-existent consumer"
response=$(get_request "/api/consumers/non-existent-consumer/subjects/$subject/templates/active")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer template"

# Test 3: Get template for consumer without template
echo
echo "Test 3: Get template for consumer without template"
no_template_consumer="test-no-template-consumer-$timestamp"
create_test_consumer "$no_template_consumer" "Consumer Without Template"

response=$(get_request "/api/consumers/$no_template_consumer/subjects/$subject/templates/active")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for consumer without template"

# Test 4: Verify template content matches what was set
echo
echo "Test 4: Verify template content integrity"
if echo "$response_body" | grep -q '.userId' && echo "$response_body" | grep -q '.fullName'; then
    log_success "Stored template contains expected content"
    ((TESTS_PASSED++))
else
    log_error "Stored template doesn't contain expected content"
    ((TESTS_FAILED++))
fi

# Test 5: Check engine field
echo
echo "Test 5: Verify engine field"
engine_value=$(echo "$response_body" | grep -o '"engine":"[^"]*"' | sed 's/"engine":"//' | sed 's/"$//')

if [ "$engine_value" = "jslt" ]; then
    log_success "Engine field is correctly set to jslt"
    ((TESTS_PASSED++))
else
    log_error "Engine field is not jslt: $engine_value"
    ((TESTS_FAILED++))
fi

# Test 6: Test with different template
echo
echo "Test 6: Test with different template content"
different_template='{"name": .fullName, "id": .userId}'
different_consumer="test-different-template-$timestamp"
create_test_consumer "$different_consumer" "Different Template Consumer"
# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"userId": {"type": "integer"}, "fullName": {"type": "string"}}}'
# Create consumer output schema
create_test_consumer_schema "$different_consumer" "$subject" '{"type": "object", "properties": {"id": {"type": "integer"}, "name": {"type": "string"}}}'
create_test_template "$different_consumer" "$subject" "$different_template"

response=$(get_request "/api/consumers/$different_consumer/subjects/$subject/templates/active")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should retrieve different template"
assert_contains "$response_body" '"expression"' "Should contain expression field"
assert_contains "$response_body" '.fullName' "Should contain fullName reference"
assert_contains "$response_body" '.userId' "Should contain userId reference"

# Test 7: Verify timestamps are present and reasonable
echo
echo "Test 7: Verify timestamp fields"
created_at=$(echo "$response_body" | grep -o '"createdAt":"[^"]*"' | sed 's/"createdAt":"//' | sed 's/"$//')
updated_at=$(echo "$response_body" | grep -o '"updatedAt":"[^"]*"' | sed 's/"updatedAt":"//' | sed 's/"$//')

if [ -n "$created_at" ] && [ -n "$updated_at" ]; then
    log_success "Both createdAt and updatedAt timestamps are present"
    ((TESTS_PASSED++))
else
    log_error "Missing timestamp fields"
    ((TESTS_FAILED++))
fi

# Test 8: Check response structure consistency
echo
echo "Test 8: Verify response structure"
required_fields=("consumerId" "subject" "expression" "engine" "createdAt" "updatedAt")
missing_fields=0

for field in "${required_fields[@]}"; do
    if ! echo "$response_body" | grep -q "\"$field\":"; then
        log_error "Missing required field: $field"
        ((missing_fields++))
    fi
done

if [ "$missing_fields" -eq 0 ]; then
    log_success "All required fields are present"
    ((TESTS_PASSED++))
else
    ((TESTS_FAILED++))
fi

# Test 9: Test consumer ID with special characters
echo
echo "Test 9: Test consumer ID with special characters"
special_template='{"result": .data}'
special_consumer="test_special_consumer_id_$timestamp"
create_test_consumer "$special_consumer" "Special Consumer"
# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"data": {"type": "string"}}}'
# Create consumer output schema
create_test_consumer_schema "$special_consumer" "$subject" '{"type": "object", "properties": {"result": {"type": "string"}}}'
create_test_template "$special_consumer" "$subject" "$special_template"

response=$(get_request "/api/consumers/$special_consumer/subjects/$subject/templates/active")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should handle special characters in consumer ID"
assert_json_field "$response_body" "consumerId" "$special_consumer"

echo
print_test_summary