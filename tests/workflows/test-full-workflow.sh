#!/bin/bash

# Full Workflow Integration Test
# Tests complete end-to-end scenario: Consumer -> Schema -> Template -> Transform

source "$(dirname "$0")/../utils/common.sh"

echo "Running Full Workflow Integration Test"
echo "======================================"

# Generate unique identifiers to avoid conflicts
timestamp=$(date +%s)
WORKFLOW_CONSUMER="workflow-test-consumer-$timestamp"
WORKFLOW_SUBJECT="workflow-test-user-$timestamp"

echo
echo "=== Phase 1: Consumer Registration ==="
echo "Registering consumer: $WORKFLOW_CONSUMER"

response=$(post_request "/api/consumers" "{
    \"consumerId\": \"$WORKFLOW_CONSUMER\",
    \"name\": \"Workflow Test Consumer\",
    \"description\": \"Consumer for full workflow integration test\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Consumer registration should succeed"
assert_json_field "$response_body" "consumerId" "$WORKFLOW_CONSUMER"
assert_json_field "$response_body" "name" "Workflow Test Consumer"

echo "✓ Consumer registered successfully"

echo
echo "=== Phase 2: Schema Registration ==="
echo "Registering schema: $WORKFLOW_SUBJECT"

schema='{
    "type": "object",
    "properties": {
        "userId": {"type": "integer"},
        "fullName": {"type": "string"},
        "emailAddress": {"type": "string", "format": "email"},
        "registrationDate": {"type": "string"},
        "accountStatus": {"type": "string"}
    },
    "required": ["userId", "fullName"]
}'

response=$(post_request "/api/schemas/$WORKFLOW_SUBJECT" "{
    \"subject\": \"$WORKFLOW_SUBJECT\",
    \"schema\": $schema,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"User schema for workflow test\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Schema registration should succeed"
assert_json_field "$response_body" "subject" "$WORKFLOW_SUBJECT"
assert_json_field "$response_body" "version" "1.0.0"

echo "✓ Schema registered successfully"

# Create consumer output schema
consumer_schema_response=$(post_request "/api/consumers/$WORKFLOW_CONSUMER/schemas/$WORKFLOW_SUBJECT" "{
    \"subject\": \"$WORKFLOW_SUBJECT\",
    \"schema\": $schema,
    \"compatibility\": \"BACKWARD\",
    \"description\": \"Consumer output schema for workflow test\"
}")
consumer_schema_http_code=$(echo "$consumer_schema_response" | tail -n1)
assert_response "$consumer_schema_http_code" 201 "Consumer output schema registration should succeed"

echo
echo "=== Phase 3: Template Creation ==="
echo "Creating transformation template for consumer: $WORKFLOW_CONSUMER"

template='{"id": .userId, "name": .fullName, "email": .emailAddress, "registered": .registrationDate, "status": .accountStatus}'
# Escape quotes in template for JSON
escaped_template=$(echo "$template" | sed 's/"/\\"/g')

response=$(post_request "/api/consumers/$WORKFLOW_CONSUMER/subjects/$WORKFLOW_SUBJECT/templates" "{
    \"version\": \"1.0.0\",
    \"engine\": \"jslt\",
    \"inputSchema\": {
        \"subject\": \"$WORKFLOW_SUBJECT\"
    },
    \"outputSchema\": {
        \"subject\": \"$WORKFLOW_SUBJECT\",
        \"consumerId\": \"$WORKFLOW_CONSUMER\"
    },
    \"expression\": \"$escaped_template\",
    \"description\": \"Template for workflow test\"
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 201 "Template creation should succeed"
assert_json_field "$response_body" "consumerId" "$WORKFLOW_CONSUMER"
assert_contains "$response_body" '"expression"' "Should contain expression field"
assert_contains "$response_body" '.userId' "Should contain userId reference"
assert_json_field "$response_body" "engine" "jslt"

echo "✓ Transformation template created successfully"

echo
echo "=== Phase 4: Data Transformation ==="
echo "Transforming sample data for consumer: $WORKFLOW_CONSUMER"

canonical_data='{
    "userId": 12345,
    "fullName": "John Doe",
    "emailAddress": "john.doe@example.com",
    "registrationDate": "2024-01-15T10:30:00Z",
    "accountStatus": "active",
    "internalField": "should_be_filtered_out"
}'

response=$(post_request "/api/consumers/$WORKFLOW_CONSUMER/subjects/$WORKFLOW_SUBJECT/transform" "{
    \"canonicalJson\": $canonical_data
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Data transformation should succeed"
assert_contains "$response_body" '"transformedJson"' "Should contain transformed data"

# Verify transformation results
assert_contains "$response_body" '"id":12345' "Should contain transformed user ID"
assert_contains "$response_body" '"name":"John Doe"' "Should contain transformed name"
assert_contains "$response_body" '"email":"john.doe@example.com"' "Should contain transformed email"
assert_contains "$response_body" '"registered":"2024-01-15T10:30:00Z"' "Should contain transformed registration date"
assert_contains "$response_body" '"status":"active"' "Should contain transformed status"

# Verify filtering (internal field should not be present)
if echo "$response_body" | grep -q '"internalField"'; then
    log_error "Internal field was not filtered out"
    ((TESTS_FAILED++))
else
    log_success "Internal field correctly filtered out"
    ((TESTS_PASSED++))
fi

echo "✓ Data transformation completed successfully"

echo
echo "=== Phase 5: Verification ==="
echo "Verifying all components work together"

# Verify consumer exists
consumer_response=$(get_request "/api/consumers/$WORKFLOW_CONSUMER")
consumer_http_code=$(echo "$consumer_response" | tail -n1)
assert_response "$consumer_http_code" 200 "Consumer should still exist"

# Verify schema exists
schema_response=$(get_request "/api/schemas/$WORKFLOW_SUBJECT")
schema_http_code=$(echo "$schema_response" | tail -n1)
assert_response "$schema_http_code" 200 "Schema should still exist"

# Verify template exists
template_response=$(get_request "/api/consumers/$WORKFLOW_CONSUMER/subjects/$WORKFLOW_SUBJECT/templates/active")
template_http_code=$(echo "$template_response" | tail -n1)
assert_response "$template_http_code" 200 "Template should still exist"

echo "✓ All components verified successfully"

echo
echo "=== Phase 6: Multiple Transformations ==="
echo "Testing multiple data transformations"

# Test with different data
test_data_2='{
    "userId": 67890,
    "fullName": "Jane Smith",
    "emailAddress": "jane.smith@example.com",
    "registrationDate": "2024-01-16T14:20:00Z",
    "accountStatus": "inactive"
}'

response=$(post_request "/api/consumers/$WORKFLOW_CONSUMER/subjects/$WORKFLOW_SUBJECT/transform" "{
    \"canonicalJson\": $test_data_2
}")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Second transformation should succeed"
assert_contains "$response_body" '"id":67890' "Should transform second user ID"
assert_contains "$response_body" '"name":"Jane Smith"' "Should transform second user name"

echo "✓ Multiple transformations completed successfully"

echo
echo "=== Workflow Test Summary ==="
echo "✓ Consumer registration"
echo "✓ Schema registration"
echo "✓ Template creation"
echo "✓ Data transformation"
echo "✓ Component verification"
echo "✓ Multiple transformations"
echo
echo "🎉 Full workflow integration test completed successfully!"

echo
print_test_summary

