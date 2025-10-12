#!/bin/bash

# JSLT Template Registration from File
# Registers a JSLT transformation template from a file for a consumer
#
# Usage: ./register-jslt-template-from-file.sh <jslt_file> <consumer_id> [description]
#
# Arguments:
#   jslt_file: Path to JSLT template file
#   consumer_id: Consumer ID to register the template for
#   description: Optional description of the template (default: "JSLT template from file")
#
# Prerequisites:
#   - Service must be running
#   - Consumer must already be registered
#   - JSLT file must exist and be readable

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

# Validate arguments
if [ $# -lt 2 ]; then
    log_error "Usage: $0 <jslt_file> <consumer_id> [description]"
    log_error "Example: $0 remove-notes.jslt mobile-app 'Remove notes from publications'"
    exit 1
fi

JSLT_FILE="$1"
CONSUMER_ID="$2"
DESCRIPTION="${3:-JSLT template from file}"

# Validate JSLT file exists
if [ ! -f "$JSLT_FILE" ]; then
    log_error "JSLT file '$JSLT_FILE' does not exist"
    exit 1
fi

# Read JSLT content
if ! JSLT_CONTENT=$(cat "$JSLT_FILE"); then
    log_error "Failed to read JSLT file '$JSLT_FILE'"
    exit 1
fi

log_info "Registering JSLT template from file: $JSLT_FILE"
log_info "Consumer: $CONSUMER_ID"
log_info "Description: $DESCRIPTION"

# Construct JSON payload (escape quotes in JSLT content for JSON)
ESCAPED_CONTENT=$(echo "$JSLT_CONTENT" | sed 's/"/\\"/g' | sed 's/$/\\n/' | tr -d '\n' | sed 's/\\n$//')

PAYLOAD="{
    \"engine\": \"jslt\",
    \"expression\": \"$ESCAPED_CONTENT\",
    \"description\": \"$DESCRIPTION\"
}"

# Make API call
log_info "Sending template registration request..."
RESPONSE=$(post_request "/api/consumers/templates/$CONSUMER_ID" "$PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$RESPONSE" | head -n -1)

# Check response
if [ "$HTTP_CODE" -eq 200 ]; then
    log_success "JSLT template registered successfully"
    echo "Response:"
    echo "$RESPONSE_BODY" | jq '.'
else
    log_error "Template registration failed (HTTP $HTTP_CODE)"
    echo "Error response:"
    echo "$RESPONSE_BODY"
    exit 1
fi