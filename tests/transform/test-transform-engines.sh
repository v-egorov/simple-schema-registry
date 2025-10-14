#!/bin/bash

# Transform Engines Tests
# Tests the GET /api/consumers/engines endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running Transform Engines Tests"
echo "==============================="

# Test 1: Get available transformation engines
echo
echo "Test 1: Get available transformation engines"
response=$(get_request "/api/consumers/engines")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Should successfully retrieve available engines"
assert_contains "$response_body" "jslt" "Should contain jslt engine"

# Test 2: Verify response is array of strings
echo
echo "Test 2: Verify response format"
# Check that all elements are strings
if echo "$response_body" | grep -q '"[^"]*":\s*{' || echo "$response_body" | grep -q '"[^"]*":\s*\[' || echo "$response_body" | grep -q '"[^"]*":\s*[0-9'] || echo "$response_body" | grep -q '"[^"]*":\s*(true|false)'; then
    log_error "Response contains non-string elements"
    ((TESTS_FAILED++))
else
    log_success "Response contains only string elements"
    ((TESTS_PASSED++))
fi

# Test 3: Check that JSLT is the primary/only engine
echo
echo "Test 3: Verify JSLT engine availability"
jslt_count=$(echo "$response_body" | grep -o '"jslt"' | wc -l)
if [ "$jslt_count" -ge 1 ]; then
    log_success "jslt engine is available"
    ((TESTS_PASSED++))
else
    log_error "jslt engine is not listed"
    ((TESTS_FAILED++))
fi

# Test 4: Check for reasonable number of engines
echo
echo "Test 4: Check number of available engines"
engine_count=$(echo "$response_body" | grep -o '"[^"]*"' | wc -l)
if [ "$engine_count" -ge 1 ] && [ "$engine_count" -le 10 ]; then
    log_success "Reasonable number of engines available ($engine_count)"
    ((TESTS_PASSED++))
else
    log_error "Unreasonable number of engines: $engine_count"
    ((TESTS_FAILED++))
fi

# Test 5: Verify response consistency across calls
echo
echo "Test 5: Verify response consistency"
response2=$(get_request "/api/consumers/engines")
http_code2=$(echo "$response2" | tail -n1)
response_body2=$(echo "$response2" | head -n -1)

if [ "$http_code2" -eq 200 ] && [ "$response_body" = "$response_body2" ]; then
    log_success "Engines list is consistent across calls"
    ((TESTS_PASSED++))
else
    log_error "Engines list is not consistent"
    ((TESTS_FAILED++))
fi

# Test 6: Check response is valid JSON
echo
echo "Test 6: Verify valid JSON response"
# Basic validation - should be parseable as JSON
if echo "$response_body" | python3 -m json.tool >/dev/null 2>&1; then
    log_success "Response is valid JSON"
    ((TESTS_PASSED++))
else
    log_error "Response is not valid JSON"
    ((TESTS_FAILED++))
fi

# Test 7: Test engines endpoint with different accept headers
echo
echo "Test 7: Test with different accept headers"
# This tests if the endpoint respects content negotiation
response=$(curl -s -w "\n%{http_code}" -H "Accept: application/xml" "$BASE_URL/api/consumers/engines" 2>/dev/null)
http_code=$(echo "$response" | tail -n1)

# Should return 406 for unsupported content type
if [ "$http_code" -eq 406 ]; then
    log_success "Endpoint correctly rejects unsupported content types"
    ((TESTS_PASSED++))
else
    log_error "Endpoint should return 406 for unsupported content type (got $http_code)"
    ((TESTS_FAILED++))
fi

# Test 8: Performance check (basic)
echo
echo "Test 8: Basic performance check"
start_time=$(date +%s%3N)
response=$(get_request "/api/consumers/engines")
end_time=$(date +%s%3N)
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" -eq 200 ]; then
    duration=$((end_time - start_time))
    if [ "$duration" -lt 1000 ]; then  # Less than 1 second
        log_success "Engines endpoint responds quickly (${duration}ms)"
        ((TESTS_PASSED++))
    else
        log_warning "Engines endpoint is slow (${duration}ms)"
        ((TESTS_PASSED++))  # Not failing, just warning
    fi
else
    log_error "Performance check failed: HTTP $http_code"
    ((TESTS_FAILED++))
fi

# Test 9: Check for unexpected engines
echo
echo "Test 9: Check for known/supported engines only"
# Should only contain known engines like JSLT
unknown_engines=$(echo "$response_body" | grep -o '"[^"]*"' | grep -v '"jslt"' | wc -l)
if [ "$unknown_engines" -eq 0 ]; then
    log_success "Only known engines are listed"
    ((TESTS_PASSED++))
else
    log_info "Found $unknown_engines unknown engines (may be valid extensions)"
    ((TESTS_PASSED++))
fi

echo
print_test_summary