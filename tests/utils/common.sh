#!/bin/bash

# Common utilities for API test scripts
# JSON Schema Registry and Transformation Service

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
CONTENT_TYPE="Content-Type: application/json"
ACCEPT="Accept: application/json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Test assertion functions
assert_response() {
    local actual_code="$1"
    local expected_code="$2"
    local message="$3"

    ((TESTS_RUN++))

    if [ "$actual_code" -eq "$expected_code" ]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message (expected: $expected_code, got: $actual_code)"
        ((TESTS_FAILED++))
        return 1
    fi
}

assert_json_field() {
    local json_response="$1"
    local field="$2"
    local expected_value="$3"
    local message="${4:-Field check}"

    ((TESTS_RUN++))

    # Extract field value using grep and sed (simple JSON parsing)
    local actual_value=$(echo "$json_response" | grep -o "\"$field\": *\"[^\"]*\"" | sed "s/\"$field\": *\"//;s/\"$//" 2>/dev/null)

    # If not found as string, try as number or boolean
    if [ -z "$actual_value" ]; then
        actual_value=$(echo "$json_response" | grep -o "\"$field\": *[^,}]*" | sed "s/\"$field\": *//" 2>/dev/null)
    fi

    if [ "$actual_value" = "$expected_value" ]; then
        log_success "$message: $field = $expected_value"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message: $field (expected: $expected_value, got: $actual_value)"
        ((TESTS_FAILED++))
        return 1
    fi
}

assert_contains() {
    local haystack="$1"
    local needle="$2"
    local message="$3"

    ((TESTS_RUN++))

    if echo "$haystack" | grep -F -q "$needle"; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message (needle '$needle' not found in haystack)"
        ((TESTS_FAILED++))
        return 1
    fi
}

assert_not_contains() {
    local haystack="$1"
    local needle="$2"
    local message="$3"

    ((TESTS_RUN++))

    if echo "$haystack" | grep -q "$needle"; then
        log_error "$message (needle '$needle' should not be found in haystack)"
        ((TESTS_FAILED++))
        return 1
    else
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    fi
}

assert_not_empty() {
    local value="$1"
    local message="$2"

    ((TESTS_RUN++))

    if [ -n "$value" ]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message (value is empty)"
        ((TESTS_FAILED++))
        return 1
    fi
}

# HTTP request helper functions
make_request() {
    local method="$1"
    local url="$2"
    local data="$3"

    if [ -n "$data" ]; then
        curl -s -w "\n%{http_code}" --max-time 10 -X "$method" "$BASE_URL$url" \
            -H "$CONTENT_TYPE" \
            -H "$ACCEPT" \
            -d "$data"
    else
        curl -s -w "\n%{http_code}" --max-time 10 -X "$method" "$BASE_URL$url" \
            -H "$ACCEPT"
    fi
}

get_request() {
    local url="$1"
    make_request "GET" "$url"
}

post_request() {
    local url="$1"
    local data="$2"
    make_request "POST" "$url" "$data"
}

# Test data cleanup functions
cleanup_consumer() {
    local consumer_id="$1"
    log_info "Cleaning up consumer: $consumer_id"
    # Note: API doesn't have delete endpoints, so we just log
    # In a real scenario, you might need to truncate tables or use test-specific endpoints
}

cleanup_schema() {
    local subject="$1"
    log_info "Cleaning up schema subject: $subject"
    # Note: API doesn't have delete endpoints
}

cleanup_template() {
    local consumer_id="$1"
    log_info "Cleaning up template for consumer: $consumer_id"
    # Note: API doesn't have delete endpoints
}

# Test data creation helpers (for setup)
create_test_consumer() {
    local consumer_id="$1"
    local name="$2"
    local description="${3:-Test consumer}"

    local response=$(post_request "/api/consumers" "{
        \"consumerId\": \"$consumer_id\",
        \"name\": \"$name\",
        \"description\": \"$description\"
    }")

    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_info "Created test consumer: $consumer_id"
        echo "$response_body"
    else
        log_error "Failed to create test consumer: $consumer_id (HTTP $http_code)"
        echo ""
    fi
}

create_test_schema() {
    local subject="$1"
    local schema_data="$2"

    local response=$(post_request "/api/schemas/$subject" "{
        \"subject\": \"$subject\",
        \"schema\": $schema_data,
        \"compatibility\": \"BACKWARD\",
        \"description\": \"Test schema for $subject\"
    }")

    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_info "Created test schema: $subject"
        echo "$response_body"
    else
        log_error "Failed to create test schema: $subject (HTTP $http_code)"
        echo ""
    fi
}

create_test_consumer_schema() {
    local consumer_id="$1"
    local subject="$2"
    local schema_data="$3"

    local response=$(post_request "/api/consumers/$consumer_id/schemas/$subject" "{
        \"subject\": \"$subject\",
        \"schema\": $schema_data,
        \"compatibility\": \"BACKWARD\",
        \"description\": \"Test consumer schema for $consumer_id\"
    }")

    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_info "Created test consumer schema: $consumer_id/$subject"
        echo "$response_body"
    else
        log_error "Failed to create test consumer schema: $consumer_id/$subject (HTTP $http_code)"
        echo ""
    fi
}

create_test_template() {
    local consumer_id="$1"
    local subject="$2"
    local template="$3"
    local engine="${4:-jslt}"
    local input_subject="${5:-$subject}"
    local output_consumer_id="${6:-$consumer_id}"
    local version="${7:-1.0.0}"

    # Escape quotes in template for JSON
    local escaped_template=$(echo "$template" | sed 's/"/\\"/g')

    local response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/templates" "{
        \"version\": \"$version\",
        \"engine\": \"$engine\",
        \"expression\": \"$escaped_template\",
        \"inputSchema\": {
            \"subject\": \"$input_subject\"
        },
        \"outputSchema\": {
            \"subject\": \"$subject\",
            \"consumerId\": \"$output_consumer_id\"
        },
        \"description\": \"Test template for $consumer_id\"
    }")

    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_info "Created test template for consumer: $consumer_id, subject: $subject"
        echo "$response_body"
    else
        log_error "Failed to create test template for consumer: $consumer_id, subject: $subject (HTTP $http_code)"
        log_error "Response: $response_body"
        echo ""
    fi
}

create_router_template() {
    local consumer_id="$1"
    local subject="$2"
    local router_config="$3"
    local input_subject="${4:-$subject}"
    local output_consumer_id="${5:-$consumer_id}"
    local version="${6:-1.0.0}"

    local response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/templates" "{
        \"version\": \"$version\",
        \"engine\": \"router\",
        \"routerConfig\": $router_config,
        \"inputSchema\": {
            \"subject\": \"$input_subject\"
        },
        \"outputSchema\": {
            \"subject\": \"$subject\",
            \"consumerId\": \"$output_consumer_id\"
        },
        \"description\": \"Router test template for $consumer_id\"
    }")

    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_info "Created router template for consumer: $consumer_id, subject: $subject"
        echo "$response_body"
    else
        log_error "Failed to create router template for consumer: $consumer_id, subject: $subject (HTTP $http_code)"
        log_error "Response: $response_body"
        echo ""
    fi
}

create_pipeline_template() {
    local consumer_id="$1"
    local subject="$2"
    local pipeline_config="$3"
    local input_subject="${4:-$subject}"
    local output_consumer_id="${5:-$consumer_id}"
    local version="${6:-1.0.0}"

    local response=$(post_request "/api/consumers/$consumer_id/subjects/$subject/templates" "{
        \"version\": \"$version\",
        \"engine\": \"pipeline\",
        \"pipelineConfig\": $pipeline_config,
        \"inputSchema\": {
            \"subject\": \"$input_subject\"
        },
        \"outputSchema\": {
            \"subject\": \"$subject\",
            \"consumerId\": \"$output_consumer_id\"
        },
        \"description\": \"Pipeline test template for $consumer_id\"
    }")

    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_info "Created pipeline template for consumer: $consumer_id, subject: $subject"
        echo "$response_body"
    else
        log_error "Failed to create pipeline template for consumer: $consumer_id, subject: $subject (HTTP $http_code)"
        echo ""
    fi
}

# Test summary function
print_test_summary() {
    echo
    echo "=== Test Summary ==="
    echo "Tests run: $TESTS_RUN"
    echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Failed: ${RED}$TESTS_FAILED${NC}"

    if [ "$TESTS_FAILED" -eq 0 ]; then
        echo -e "${GREEN}All tests passed! ✓${NC}"
        return 0
    else
        echo -e "${RED}Some tests failed! ✗${NC}"
        return 1
    fi
}

# Check if service is available
check_service_health() {
    local response=$(get_request "/actuator/health")
    local http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" -eq 200 ]; then
        log_info "Service is healthy"
        return 0
    else
        log_error "Service is not healthy (HTTP $http_code)"
        return 1
    fi
}

# Wait for service to be ready
wait_for_service() {
    local max_attempts="${1:-30}"
    local attempt=1

    log_info "Waiting for service to be ready..."

    while [ $attempt -le $max_attempts ]; do
        if check_service_health >/dev/null 2>&1; then
            log_success "Service is ready after $attempt attempts"
            return 0
        fi

        log_info "Attempt $attempt/$max_attempts - service not ready yet"
        sleep 2
        ((attempt++))
    done

    log_error "Service failed to become ready after $max_attempts attempts"
    return 1
}