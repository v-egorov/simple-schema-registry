#!/bin/bash

# Transform Template Activate Tests
# Tests the PUT /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}/activate endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Template Activate Tests"
echo "=========================================="

# Generate unique consumer ID to avoid conflicts
timestamp=$(date +%s)
consumer_id="test-template-activate-$timestamp"

# Setup: Create consumer, schemas, and multiple transformation templates
echo "Setup: Creating consumer, schemas, and transformation templates..."
create_test_consumer "$consumer_id" "Template Activate Test Consumer"
subject="user-profile"

# Create canonical input schema
create_test_schema "$subject" '{"type": "object", "properties": {"userId": {"type": "integer"}, "fullName": {"type": "string"}, "emailAddress": {"type": "string"}}}'

# Create consumer output schema
create_test_consumer_schema "$consumer_id" "$subject" '{"type": "object", "properties": {"id": {"type": "integer"}, "name": {"type": "string"}, "email": {"type": "string"}}}'

# Create first template (v1.0.0) - should be active by default
template_v1='{"id": .userId, "name": .fullName, "email": .emailAddress}'
create_test_template "$consumer_id" "$subject" "$template_v1" "jslt" "$subject" "$consumer_id" "1.0.0"

# Create second template (v2.0.0) - should be inactive initially
template_v2='{"user_id": .userId, "full_name": .fullName, "email_addr": .emailAddress}'
create_test_template "$consumer_id" "$subject" "$template_v2" "jslt" "$subject" "$consumer_id" "2.0.0"

# Test 1: Verify initial state - v1.0.0 should be active
echo
echo "Test 1: Verify initial active version"
response=$(get_request "/api/consumers/$consumer_id/subjects/$subject/templates/active")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve active template"
assert_json_field "$response_body" "version" "1.0.0"
assert_json_field "$response_body" "isActive" true

# Test 2: Activate already active version (should succeed without changes)
echo
echo "Test 2: Activate already active version"
response=$(make_request "PUT" "/api/consumers/$consumer_id/subjects/$subject/templates/versions/1.0.0/activate")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should succeed when activating already active version"
assert_json_field "$response_body" "version" "1.0.0"
assert_json_field "$response_body" "isActive" true

# Test 3: Activate different version (v2.0.0)
echo
echo "Test 3: Activate different version"
response=$(make_request "PUT" "/api/consumers/$consumer_id/subjects/$subject/templates/versions/2.0.0/activate")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully activate v2.0.0"
assert_json_field "$response_body" "version" "2.0.0"
assert_json_field "$response_body" "isActive" true
assert_json_field "$response_body" "consumerId" "$consumer_id"
assert_json_field "$response_body" "subject" "$subject"

# Test 4: Verify v2.0.0 is now active
echo
echo "Test 4: Verify v2.0.0 is now active"
response=$(get_request "/api/consumers/$consumer_id/subjects/$subject/templates/active")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should retrieve v2.0.0 as active"
assert_json_field "$response_body" "version" "2.0.0"
assert_json_field "$response_body" "isActive" true

# Test 5: Switch back to v1.0.0
echo
echo "Test 5: Switch back to v1.0.0"
response=$(make_request "PUT" "/api/consumers/$consumer_id/subjects/$subject/templates/versions/1.0.0/activate")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully activate v1.0.0 again"
assert_json_field "$response_body" "version" "1.0.0"
assert_json_field "$response_body" "isActive" true

# Test 6: Try to activate non-existent version
echo
echo "Test 6: Try to activate non-existent version"
response=$(make_request "PUT" "/api/consumers/$consumer_id/subjects/$subject/templates/versions/3.0.0/activate")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 404 "Should return 404 for non-existent version"
assert_contains "$response_body" "not found" "Should contain error message"

# Test 7: Try to activate version for non-existent consumer
echo
echo "Test 7: Try to activate version for non-existent consumer"
response=$(make_request "PUT" "/api/consumers/non-existent-consumer/subjects/$subject/templates/versions/1.0.0/activate")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent consumer"

# Test 8: Try to activate version for non-existent subject
echo
echo "Test 8: Try to activate version for non-existent subject"
response=$(make_request "PUT" "/api/consumers/$consumer_id/subjects/non-existent-subject/templates/versions/1.0.0/activate")
http_code=$(echo "$response" | tail -n1)

assert_response "$http_code" 404 "Should return 404 for non-existent subject"

# Test 9: Verify response structure for successful activation
echo
echo "Test 9: Verify response structure for successful activation"
response=$(make_request "PUT" "/api/consumers/$consumer_id/subjects/$subject/templates/versions/2.0.0/activate")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully activate version"
required_fields=("consumerId" "subject" "version" "engine" "isActive" "createdAt" "updatedAt")
missing_fields=0

for field in "${required_fields[@]}"; do
    if ! echo "$response_body" | grep -q "\"$field\":"; then
        log_error "Missing required field: $field"
        ((missing_fields++))
    fi
done

if [ "$missing_fields" -eq 0 ]; then
    log_success "All required fields are present in response"
    ((TESTS_PASSED++))
else
    ((TESTS_FAILED++))
fi

# Test 10: Test activation with special characters in version
echo
echo "Test 10: Test activation with special version format"
# Create template with different version format
template_special='{"result": .userId}'
create_test_template "$consumer_id" "$subject" "$template_special" "jslt" "$subject" "$consumer_id" "1.0.1-beta"

response=$(make_request "PUT" "/api/consumers/$consumer_id/subjects/$subject/templates/versions/1.0.1-beta/activate")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should handle special version formats"
assert_json_field "$response_body" "version" "1.0.1-beta"
assert_json_field "$response_body" "isActive" true

# Test 11: Verify only one version is active at a time
echo
echo "Test 11: Verify only one version is active at a time"
# Get all versions and count active ones
response=$(get_request "/api/consumers/$consumer_id/subjects/$subject/templates")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should retrieve all template versions"
active_count=$(echo "$response_body" | grep -o '"isActive":true' | wc -l)

if [ "$active_count" -eq 1 ]; then
    log_success "Only one version is active at a time"
    ((TESTS_PASSED++))
else
    log_error "Expected 1 active version, found $active_count"
    ((TESTS_FAILED++))
fi

# Test 12: Test activation updates timestamps
echo
echo "Test 12: Verify activation updates timestamps"
# Get current updatedAt before activation
response=$(get_request "/api/consumers/$consumer_id/subjects/$subject/templates/active")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

old_updated_at=$(echo "$response_body" | grep -o '"updatedAt":"[^"]*"' | sed 's/"updatedAt":"//' | sed 's/"$//')

# Wait a moment and activate a different version
sleep 1
response=$(make_request "PUT" "/api/consumers/$consumer_id/subjects/$subject/templates/versions/1.0.0/activate")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

new_updated_at=$(echo "$response_body" | grep -o '"updatedAt":"[^"]*"' | sed 's/"updatedAt":"//' | sed 's/"$//')

if [ "$new_updated_at" != "$old_updated_at" ]; then
    log_success "Activation updates the updatedAt timestamp"
    ((TESTS_PASSED++))
else
    log_error "Activation should update the updatedAt timestamp"
    ((TESTS_FAILED++))
fi

echo
print_test_summary