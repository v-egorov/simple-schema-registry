#!/bin/bash

# Transform Template Get Tests
# Tests the GET /api/transform/templates/{consumerId} endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Template Get Tests"
echo "====================================="

# Setup: Create consumer and transformation template
echo
echo "Setup: Creating consumer and transformation template..."

create_test_consumer "test-template-get" "Template Get Test Consumer"

template='. | {id: .userId, name: .fullName, email: .emailAddress}'
create_test_template "test-template-get" "$template"

# Test 1: Get existing transformation template
echo
echo "Test 1: Get existing transformation template"
response=$(get_request "/api/transform/templates/test-template-get")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve template"
assert_json_field "$response_body" "consumerId" "test-template-get"
assert_json_field "$response_body" "template" "$template"
assert_json_field "$response_body" "engine" "JSLT"
assert_contains "$response_body" '"createdAt"' "Should contain createdAt timestamp"
assert_contains "$response_body" '"updatedAt"' "Should contain updatedAt timestamp"

# Test 2: Get template for non-existent consumer
echo
echo "Test 2: Get template for non-existent consumer"
response=$(get_request "/api/transform/templates/non-existent-consumer")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer template"

# Test 3: Get template for consumer without template
echo
echo "Test 3: Get template for consumer without template"
create_test_consumer "test-no-template-consumer" "Consumer Without Template"

response=$(get_request "/api/transform/templates/test-no-template-consumer")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for consumer without template"

# Test 4: Verify template content matches what was set
echo
echo "Test 4: Verify template content integrity"
stored_template=$(echo "$response_body" | grep -o '"template":"[^"]*"' | sed 's/"template":"//' | sed 's/"$//')

if [ "$stored_template" = "$template" ]; then
    log_success "Stored template matches original"
    ((TESTS_PASSED++))
else
    log_error "Stored template doesn't match original"
    log_error "Expected: $template"
    log_error "Got: $stored_template"
    ((TESTS_FAILED++))
fi

# Test 5: Check engine field
echo
echo "Test 5: Verify engine field"
engine_value=$(echo "$response_body" | grep -o '"engine":"[^"]*"' | sed 's/"engine":"//' | sed 's/"$//')

if [ "$engine_value" = "JSLT" ]; then
    log_success "Engine field is correctly set to JSLT"
    ((TESTS_PASSED++))
else
    log_error "Engine field is not JSLT: $engine_value"
    ((TESTS_FAILED++))
fi

# Test 6: Test with different template
echo
echo "Test 6: Test with different template content"
different_template='def format_user(user): {name: user.fullName, id: user.userId}'
create_test_template "test-different-template" "$different_template"

response=$(get_request "/api/transform/templates/test-different-template")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should retrieve different template"
assert_json_field "$response_body" "template" "$different_template"

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
required_fields=("consumerId" "template" "engine" "createdAt" "updatedAt")
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
special_template='. | {result: .data}'
create_test_template "test_special-consumer.id" "$special_template"

response=$(get_request "/api/transform/templates/test_special-consumer.id")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should handle special characters in consumer ID"
assert_json_field "$response_body" "consumerId" "test_special-consumer.id"

echo
print_test_summary