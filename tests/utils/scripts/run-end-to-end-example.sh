#!/bin/bash

# End-to-End Example Automation Script
# This script demonstrates the complete workflow of the JSON Schema Registry and Transformation Service
# using investment publications data.
#
# Features:
# - Idempotent: Can be run multiple times safely (handles existing resources)
# - Creates new versions for schemas and templates if they already exist
# - Saves transformation output to a local file for review and comparison
# - Validates both input and output data against appropriate schemas

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
EXAMPLES_DIR="$PROJECT_ROOT/tests/examples/investment-research/publications"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# HTTP request helper functions
make_request() {
    local method="$1"
    local url="$2"
    local data="$3"

    if [ -n "$data" ]; then
        curl -s -w "\n%{http_code}" --max-time 30 -X "$method" "$BASE_URL$url" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -d "$data"
    else
        curl -s -w "\n%{http_code}" --max-time 30 -X "$method" "$BASE_URL$url" \
            -H "Accept: application/json"
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

# Check if service is available
check_service_health() {
    log_info "Checking service health..."
    local response=$(get_request "/actuator/health")
    local http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" -eq 200 ]; then
        log_success "Service is healthy"
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

# Step 1: Register canonical schema
register_canonical_schema() {
    log_info "Step 1: Registering canonical schema..."

    if [ ! -f "$EXAMPLES_DIR/invest-publications.schema.json" ]; then
        log_error "Canonical schema file not found: $EXAMPLES_DIR/invest-publications.schema.json"
        return 1
    fi

    local response=$(post_request "/api/schemas/invest-publications" "{
        \"subject\": \"invest-publications\",
        \"schema\": $(cat "$EXAMPLES_DIR/invest-publications.schema.json" | jq -c .),
        \"compatibility\": \"BACKWARD\",
        \"description\": \"Test canonical schema for investment publications\"
    }")
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_success "Canonical schema registered successfully (new version created)"
        return 0
    elif [ "$http_code" -eq 409 ]; then
        log_warning "Canonical schema already exists with same content - this is expected"
        return 0
    else
        log_error "Failed to register canonical schema (HTTP $http_code): $response_body"
        return 1
    fi
}

# Step 2: Register consumer
register_consumer() {
    log_info "Step 2: Registering consumer (mobile-app)..."

    local response=$(post_request "/api/consumers" '{
        "consumerId": "mobile-app",
        "name": "Mobile Application",
        "description": "Investment publications consumer for mobile app - requires data without internal notes"
    }')
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_success "Consumer registered successfully"
        return 0
    elif [ "$http_code" -eq 409 ]; then
        log_warning "Consumer 'mobile-app' already exists - this is expected"
        return 0
    else
        log_error "Failed to register consumer (HTTP $http_code): $response_body"
        return 1
    fi
}

# Step 3: Register consumer output schema
register_consumer_schema() {
    log_info "Step 3: Registering consumer output schema..."

    if [ ! -f "$EXAMPLES_DIR/invest-publications-no-notes.schema.json" ]; then
        log_error "Consumer schema file not found: $EXAMPLES_DIR/invest-publications-no-notes.schema.json"
        return 1
    fi

    local response=$(post_request "/api/consumers/mobile-app/schemas/invest-publications" "{
        \"subject\": \"invest-publications\",
        \"schema\": $(cat "$EXAMPLES_DIR/invest-publications-no-notes.schema.json" | jq -c .),
        \"compatibility\": \"BACKWARD\",
        \"description\": \"Test consumer schema for mobile app\"
    }")
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_success "Consumer output schema registered successfully (new version created)"
        return 0
    elif [ "$http_code" -eq 409 ]; then
        log_warning "Consumer schema already exists with same content - this is expected"
        return 0
    else
        log_error "Failed to register consumer schema (HTTP $http_code): $response_body"
        return 1
    fi
}

# Step 4: Register JSLT transformation template
register_transformation_template() {
    log_info "Step 4: Registering JSLT transformation template..."

    if [ ! -f "$EXAMPLES_DIR/remove-all-notes.jslt" ]; then
        log_error "JSLT template file not found: $EXAMPLES_DIR/remove-all-notes.jslt"
        return 1
    fi

    # Read and escape the JSLT template
    local jslt_content=$(cat "$EXAMPLES_DIR/remove-all-notes.jslt" | jq -R -s . | sed 's/^"//' | sed 's/"$//')
    local escaped_jslt=$(echo "$jslt_content" | sed 's/"/\\"/g')

    local response=$(post_request "/api/consumers/mobile-app/subjects/invest-publications/templates" "{
        \"version\": \"1.0.0\",
        \"engine\": \"jslt\",
        \"expression\": \"$escaped_jslt\",
        \"inputSchema\": {
            \"subject\": \"invest-publications\"
        },
        \"outputSchema\": {
            \"subject\": \"invest-publications\",
            \"consumerId\": \"mobile-app\"
        },
        \"description\": \"Remove all internal notes from investment publications for mobile app consumption\"
    }")

    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 201 ]; then
        log_success "JSLT transformation template registered successfully (new version created)"
        return 0
    elif [ "$http_code" -eq 409 ]; then
        log_warning "Transformation template already exists with same content - this is expected"
        return 0
    else
        log_error "Failed to register transformation template (HTTP $http_code): $response_body"
        return 1
    fi
}

# Step 5: Validate input data against canonical schema
validate_input_data() {
    log_info "Step 5: Validating input data against canonical schema..."

    if [ ! -f "$EXAMPLES_DIR/all-elements-with-all-values.json" ]; then
        log_error "Input data file not found: $EXAMPLES_DIR/all-elements-with-all-values.json"
        return 1
    fi

    local response=$(post_request "/api/schemas/invest-publications/validate" "@$EXAMPLES_DIR/all-elements-with-all-values.json")
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 200 ]; then
        local is_valid=$(echo "$response_body" | jq -r '.valid')
        if [ "$is_valid" = "true" ]; then
            log_success "Input data validation passed"
            return 0
        else
            log_error "Input data validation failed: $response_body"
            return 1
        fi
    else
        log_error "Validation request failed (HTTP $http_code): $response_body"
        return 1
    fi
}

# Step 6: Transform data and validate output
transform_and_validate() {
    log_info "Step 6: Transforming data and validating output..."

    if [ ! -f "$EXAMPLES_DIR/all-elements-with-all-values.json" ]; then
        log_error "Input data file not found: $EXAMPLES_DIR/all-elements-with-all-values.json"
        return 1
    fi

    # Define output file path
    local output_file="$PROJECT_ROOT/transformed-output.json"
    log_info "Transformation output will be saved to: $output_file"

    # Transform the data
    local transform_payload="{\"subject\": \"invest-publications\", \"canonicalJson\": $(cat "$EXAMPLES_DIR/all-elements-with-all-values.json")}"
    local response=$(post_request "/api/consumers/mobile-app/subjects/invest-publications/transform" "$transform_payload")
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 200 ]; then
        log_success "Data transformation completed successfully"

        # Extract and save transformed JSON to file
        local transformed_json=$(echo "$response_body" | jq -r '.transformedJson')
        echo "$transformed_json" > "$output_file"
        log_success "Transformed data saved to $output_file"

        # Show file size for verification
        local file_size=$(wc -c < "$output_file")
        log_info "Output file size: ${file_size} bytes"

        # Validate against consumer schema using the saved file
        log_info "Validating transformed output against consumer schema..."
        local validation_response=$(post_request "/api/consumers/mobile-app/schemas/invest-publications/validate" "@$output_file")
        local validation_http_code=$(echo "$validation_response" | tail -n1)
        local validation_body=$(echo "$validation_response" | head -n -1)

        if [ "$validation_http_code" -eq 200 ]; then
            local is_valid=$(echo "$validation_body" | jq -r '.valid')
            if [ "$is_valid" = "true" ]; then
                log_success "Output data validation passed"
                log_info "You can review the transformed output in: $output_file"
                log_info "Compare with original input: $EXAMPLES_DIR/all-elements-with-all-values.json"
                return 0
            else
                log_error "Output data validation failed: $validation_body"
                log_info "Check the transformed output file for details: $output_file"
                return 1
            fi
        else
            log_error "Output validation request failed (HTTP $validation_http_code): $validation_body"
            return 1
        fi
    else
        log_error "Data transformation failed (HTTP $http_code): $response_body"
        return 1
    fi
}

# Main execution
main() {
    echo "========================================"
    echo "JSON Schema Registry - End-to-End Example"
    echo "========================================"
    echo "This script is idempotent and can be run multiple times safely."
    echo "It will create new versions of existing resources as needed."
    echo

    # Check prerequisites
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi

    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed"
        exit 1
    fi

    # Wait for service to be ready
    if ! wait_for_service; then
        log_error "Service is not available. Please ensure the application is running."
        exit 1
    fi

    echo
    echo "Starting end-to-end example execution..."
    echo

    # Execute steps
    local step=1
    local total_steps=6

    if register_canonical_schema; then
        ((step++))
    else
        log_error "Step 1 failed. Aborting."
        exit 1
    fi

    if register_consumer; then
        ((step++))
    else
        log_error "Step 2 failed. Aborting."
        exit 1
    fi

    if register_consumer_schema; then
        ((step++))
    else
        log_error "Step 3 failed. Aborting."
        exit 1
    fi

    if register_transformation_template; then
        ((step++))
    else
        log_error "Step 4 failed. Aborting."
        exit 1
    fi

    if validate_input_data; then
        ((step++))
    else
        log_error "Step 5 failed. Aborting."
        exit 1
    fi

    if transform_and_validate; then
        ((step++))
    else
        log_error "Step 6 failed. Aborting."
        exit 1
    fi

    echo
    log_success "🎉 End-to-end example completed successfully!"
    echo
    echo "Summary:"
    echo "- Registered canonical schema for investment publications (new version if existing)"
    echo "- Created mobile-app consumer (skipped if already exists)"
    echo "- Registered consumer-specific output schema (new version if existing)"
    echo "- Created JSLT transformation template to remove notes (new version if existing)"
    echo "- Validated input data against canonical schema"
    echo "- Successfully transformed data and validated output"
    echo
    echo "Output files:"
    echo "- Transformed data: $PROJECT_ROOT/transformed-output.json"
    echo "- Original input: $EXAMPLES_DIR/all-elements-with-all-values.json"
    echo
    echo "You can now:"
    echo "- Compare the transformed output with the original input"
    echo "- Explore the API further or run individual test scripts"
    echo "- Re-run this script (it handles existing resources gracefully)"
}

# Run main function
main "$@"