#!/bin/bash

# Transform Template Create/Update Tests
# Tests the POST /api/transform/templates/{consumerId} endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Template Create/Update Tests"
echo "==============================================="

# Setup: Create consumer
echo
echo "Setup: Creating test consumer..."

create_test_consumer "test-template-create" "Template Create Test Consumer"

# Test 1: Create new transformation template
echo
echo "Test 1: Create new transformation template"
template='. | {id: .userId, name: .fullName, email: .emailAddress}'
response=$(post_request "/api/transform/templates/test-template-create" "{
    \"template\": \"$template\",
    \"engine\": \"JSLT\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Template creation should succeed"
assert_json_field "$response_body" "consumerId" "test-template-create"
assert_json_field "$response_body" "template" "$template"
assert_json_field "$response_body" "engine" "JSLT"
assert_contains "$response_body" '"createdAt"' "Should contain createdAt timestamp"
assert_contains "$response_body" '"updatedAt"' "Should contain updatedAt timestamp"

# Test 2: Update existing template
echo
echo "Test 2: Update existing transformation template"
updated_template='. | {userId: .id, fullName: .name, emailAddr: .email}'
response=$(post_request "/api/transform/templates/test-template-create" "{
    \"template\": \"$updated_template\",
    \"engine\": \"JSLT\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Template update should succeed"
assert_json_field "$response_body" "template" "$updated_template"

# Test 3: Create template for non-existent consumer
echo
echo "Test 3: Create template for non-existent consumer"
response=$(post_request "/api/transform/templates/non-existent-consumer" "{
    \"template\": \". | {id: .userId}\",
    \"engine\": \"JSLT\"
}")
http_code=$(echo "$response" | tail -n1)

# This might succeed or fail depending on API behavior
# If consumer auto-creation is supported, it would succeed
# If not, it should fail
if [ "$http_code" -eq 200 ]; then
    log_info "API allows template creation for non-existent consumer (may auto-create consumer)"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 404 ] || [ "$http_code" -eq 400 ]; then
    log_success "API correctly rejects template creation for non-existent consumer"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for non-existent consumer: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 4: Create template with missing template field
echo
echo "Test 4: Create template with missing template field"
response=$(post_request "/api/transform/templates/test-template-create" "{
    \"engine\": \"JSLT\"
}")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 400 "Should reject template creation without template field"

# Test 5: Create template with invalid engine
echo
echo "Test 5: Create template with invalid engine"
response=$(post_request "/api/transform/templates/test-template-create" "{
    \"template\": \". | {id: .userId}\",
    \"engine\": \"INVALID_ENGINE\"
}")
http_code=$(echo "$response" | tail -n1)

# API may accept unknown engines or reject them
if [ "$http_code" -eq 200 ]; then
    log_info "API accepts unknown engine types"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 400 ]; then
    log_success "API correctly rejects invalid engine types"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for invalid engine: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 6: Create template with empty template
echo
echo "Test 6: Create template with empty template"
response=$(post_request "/api/transform/templates/test-template-create" "{
    \"template\": \"\",
    \"engine\": \"JSLT\"
}")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" -eq 200 ]; then
    log_info "API accepts empty templates"
    ((TESTS_PASSED++))
elif [ "$http_code" -eq 400 ]; then
    log_success "API correctly rejects empty templates"
    ((TESTS_PASSED++))
else
    log_error "Unexpected response for empty template: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 7: Create template with complex JSLT expression
echo
echo "Test 7: Create template with complex JSLT expression"
complex_template='def format_user(u): {id: u.userId, name: u.fullName, email: u.emailAddress, active: u.status == "active"}'
response=$(post_request "/api/transform/templates/test-template-create" "{
    \"template\": \"$complex_template\",
    \"engine\": \"JSLT\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Complex JSLT template creation should succeed"
assert_json_field "$response_body" "template" "$complex_template"

# Test 8: Verify template persistence across calls
echo
echo "Test 8: Verify template persistence"
# Get the template back and verify it matches
get_response=$(get_request "/api/transform/templates/test-template-create")
get_http_code=$(echo "$get_response" | tail -n1)
get_body=$(echo "$get_response" | head -n -1)

assert_response "$get_http_code" 200 "Should be able to retrieve created template"

stored_template=$(echo "$get_body" | grep -o '"template":"[^"]*"' | sed 's/"template":"//' | sed 's/"$//')
if [ "$stored_template" = "$complex_template" ]; then
    log_success "Template persisted correctly"
    ((TESTS_PASSED++))
else
    log_error "Template not persisted correctly"
    ((TESTS_FAILED++))
fi

# Test 9: Test consumer ID with special characters
echo
echo "Test 9: Create template with special characters in consumer ID"
special_template='. | {result: .data}'
response=$(post_request "/api/transform/templates/test_special-consumer.id" "{
    \"template\": \"$special_template\",
    \"engine\": \"JSLT\"
}")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" -eq 200 ]; then
    log_success "Template creation handles special characters in consumer ID"
    ((TESTS_PASSED++))
else
    log_error "Template creation failed with special characters: HTTP $http_code"
    ((TESTS_FAILED++))
fi

echo
print_test_summary