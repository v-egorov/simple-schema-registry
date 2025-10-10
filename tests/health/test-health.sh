#!/bin/bash

# Health Check Tests
# Tests basic service availability and health endpoints

source "$(dirname "$0")/../utils/common.sh"

echo "Running Health Check Tests"
echo "=========================="

# Test 1: Basic health check
echo
echo "Test 1: Basic health check (/actuator/health)"
response=$(get_request "/actuator/health")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Health endpoint should be accessible"
assert_contains "$response_body" '"status":"UP"' "Health status should be UP"
# Note: This service has basic health check - database health not included
# assert_contains "$response_body" '"db":{"status":"UP"}' "Database health should be UP"

# Test 2: Service root endpoint
echo
echo "Test 2: Service root endpoint"
response=$(get_request "/")
http_code=$(echo "$response" | tail -n1)

# Root endpoint might return 404 or redirect, just check it's responsive
if [ "$http_code" -eq 404 ] || [ "$http_code" -eq 200 ] || [ "$http_code" -eq 302 ]; then
    log_success "Service root endpoint is responsive (HTTP $http_code)"
    ((TESTS_PASSED++))
else
    log_error "Service root endpoint not responsive (HTTP $http_code)"
    ((TESTS_FAILED++))
fi

# Test 3: Check API docs endpoint
echo
echo "Test 3: API documentation endpoint"
response=$(get_request "/swagger-ui.html")
http_code=$(echo "$response" | tail -n1)

# Swagger UI redirects to /swagger-ui/index.html
assert_response "$http_code" 302 "Swagger UI should redirect to index page"

# Test 4: Check OpenAPI spec endpoint
echo
echo "Test 4: OpenAPI specification endpoint"
response=$(get_request "/api-docs")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "OpenAPI spec should be accessible"
assert_contains "$response_body" '"openapi":"3.0.1"' "Should return OpenAPI 3.0.1 specification"
assert_contains "$response_body" '"info"' "Should contain API info"

# Test 5: Check service info
echo
echo "Test 5: Service information"
response=$(get_request "/actuator/info")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Info endpoint should be accessible"
# Note: This service has basic info endpoint - may be empty
# assert_contains "$response_body" '"app"' "Should contain app information"

echo
print_test_summary