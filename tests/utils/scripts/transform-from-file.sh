#!/bin/bash

# Data Transformation from File
# Transforms JSON data from a file using the Schema Registry API
#
# Usage: ./transform-from-file.sh <data_file> <consumer_id> <subject> [version]
#
# Arguments:
#   data_file: Path to JSON data file to transform
#   consumer_id: Consumer ID that has transformation template
#   subject: Schema subject for the data
#   version: Optional template version (uses active version if not specified)
#
# Prerequisites:
#   - jq must be installed
#   - Service must be running
#   - Data file must contain valid JSON
#   - Consumer must be registered with transformation template
#   - Subject must match consumer's registered subjects

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

# Check dependencies
if ! command -v jq &> /dev/null; then
    log_error "jq is required but not installed. Please install jq first."
    exit 1
fi

# Validate arguments
if [ $# -lt 3 ] || [ $# -gt 4 ]; then
    log_error "Usage: $0 <data_file> <consumer_id> <subject> [version]"
    log_error "Example: $0 user-data.json mobile-app user-profile"
    log_error "Example: $0 user-data.json mobile-app user-profile 1.0.0"
    exit 1
fi

DATA_FILE="$1"
CONSUMER_ID="$2"
SUBJECT="$3"
VERSION="$4"

# Validate data file exists
if [ ! -f "$DATA_FILE" ]; then
    log_error "Data file '$DATA_FILE' does not exist"
    exit 1
fi

# Validate data file is readable JSON
if ! jq empty "$DATA_FILE" 2>/dev/null; then
    log_error "Data file '$DATA_FILE' is not valid JSON"
    exit 1
fi

log_info "Transforming data from file: $DATA_FILE"
log_info "Consumer: $CONSUMER_ID"
log_info "Subject: $SUBJECT"
if [ -n "$VERSION" ]; then
    log_info "Version: $VERSION"
fi

# Construct JSON payload using jq
PAYLOAD=$(jq -n \
    --argjson data "$(cat "$DATA_FILE")" \
    '{
        canonicalJson: $data
    }')

# Build URL with optional version
URL="/api/consumers/$CONSUMER_ID/subjects/$SUBJECT/transform"
if [ -n "$VERSION" ]; then
    URL="${URL}/versions/$VERSION"
fi

# Make API call
log_info "Sending transformation request..."
RESPONSE=$(post_request "$URL" "$PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$RESPONSE" | head -n -1)

# Check response
if [ "$HTTP_CODE" -eq 200 ]; then
    log_success "Data transformed successfully"
    echo "Response:"
    echo "$RESPONSE_BODY" | jq '.'
else
    log_error "Data transformation failed (HTTP $HTTP_CODE)"
    echo "Error response:"
    echo "$RESPONSE_BODY"
    exit 1
fi