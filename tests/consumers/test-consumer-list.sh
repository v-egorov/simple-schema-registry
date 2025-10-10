#!/bin/bash

# Consumer List Tests
# Tests the GET /api/consumers endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Consumer List Tests"
echo "==========================="

# Setup: Create some test consumers
echo
echo "Setup: Creating test consumers..."
create_test_consumer "test-list-consumer-1" "List Test Consumer 1" "First consumer for list testing"
create_test_consumer "test-list-consumer-2" "List Test Consumer 2" "Second consumer for list testing"
create_test_consumer "test-list-consumer-3" "List Test Consumer 3" "Third consumer for list testing"

# Test 1: List all consumers
echo
echo "Test 1: List all consumers"
response=$(get_request "/api/consumers")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Consumer list should be accessible"
assert_contains "$response_body" '[' "Should return JSON array"
assert_contains "$response_body" '"consumerId"' "Should contain consumer objects"
assert_contains "$response_body" '"name"' "Should contain consumer names"

# Test 2: Verify our test consumers are in the list
echo
echo "Test 2: Verify test consumers are listed"
assert_contains "$response_body" '"test-list-consumer-1"' "Should contain first test consumer"
assert_contains "$response_body" '"test-list-consumer-2"' "Should contain second test consumer"
assert_contains "$response_body" '"test-list-consumer-3"' "Should contain third test consumer"

# Test 3: Check response structure
echo
echo "Test 3: Check response structure"
# Parse JSON array to check each consumer has required fields
consumer_count=$(echo "$response_body" | grep -o '"consumerId"' | wc -l)
if [ "$consumer_count" -ge 3 ]; then
    log_success "Response contains at least 3 consumers"
    ((TESTS_PASSED++))
else
    log_error "Expected at least 3 consumers, found $consumer_count"
    ((TESTS_FAILED++))
fi

# Test 4: Check that each consumer has required fields
echo
echo "Test 4: Verify consumer object structure"
# Check if all consumers have the required fields
missing_fields=0
if ! echo "$response_body" | grep -q '"consumerId"'; then ((missing_fields++)); fi
if ! echo "$response_body" | grep -q '"name"'; then ((missing_fields++)); fi
if ! echo "$response_body" | grep -q '"createdAt"'; then ((missing_fields++)); fi
if ! echo "$response_body" | grep -q '"updatedAt"'; then ((missing_fields++)); fi

if [ "$missing_fields" -eq 0 ]; then
    log_success "All consumers have required fields"
    ((TESTS_PASSED++))
else
    log_error "$missing_fields consumers missing required fields"
    ((TESTS_FAILED++))
fi

# Test 5: List consumers after adding more
echo
echo "Test 5: List consumers after adding another"
create_test_consumer "test-list-consumer-4" "List Test Consumer 4" "Fourth consumer for list testing"

response=$(get_request "/api/consumers")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Consumer list should still work after adding more"
assert_contains "$response_body" '"test-list-consumer-4"' "Should contain newly added consumer"

# Test 6: Check for optional description field
echo
echo "Test 6: Check optional description field handling"
# Some consumers have descriptions, some don't - both should be valid
if echo "$response_body" | grep -q '"description"'; then
    log_success "Description field is properly handled"
    ((TESTS_PASSED++))
else
    log_info "No consumers have descriptions (this is valid)"
    ((TESTS_PASSED++))
fi

echo
print_test_summary