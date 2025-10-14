#!/bin/bash

# JSLT Template Registration from File
# Registers a JSLT transformation template from a file for a consumer and subject
#
# Usage: ./register-jslt-template-from-file.sh <jslt_file> <consumer_id> <subject> [version] [input_subject] [description]
#
# Arguments:
#   jslt_file: Path to JSLT template file
#   consumer_id: Consumer ID to register the template for
#   subject: Schema subject for the template
#   version: Template version (default: 1.0.0)
#   input_subject: Input schema subject (default: same as subject)
#   description: Optional description of the template (default: "JSLT template from file")
#
# Prerequisites:
#   - Service must be running
#   - Consumer must already be registered
#   - Input and output schemas must exist
#   - JSLT file must exist and be readable

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

# Validate arguments
if [ $# -lt 3 ]; then
    log_error "Usage: $0 <jslt_file> <consumer_id> <subject> [version] [input_subject] [description]"
    log_error "Example: $0 remove-notes.jslt mobile-app user-profile 1.0.0 user-profile 'Remove notes from publications'"
    exit 1
fi

JSLT_FILE="$1"
CONSUMER_ID="$2"
SUBJECT="$3"
VERSION="${4:-1.0.0}"
INPUT_SUBJECT="${5:-$SUBJECT}"
DESCRIPTION="${6:-JSLT template from file}"

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
log_info "Subject: $SUBJECT"
log_info "Version: $VERSION"
log_info "Input Subject: $INPUT_SUBJECT"
log_info "Description: $DESCRIPTION"

# Construct JSON payload using jq for proper escaping
PAYLOAD=$(jq -n \
    --arg version "$VERSION" \
    --arg expression "$JSLT_CONTENT" \
    --arg description "$DESCRIPTION" \
    --arg inputSubject "$INPUT_SUBJECT" \
    --arg subject "$SUBJECT" \
    --arg consumerId "$CONSUMER_ID" \
    '{
        version: $version,
        engine: "jslt",
        templateExpression: $expression,
        inputSchema: {
            subject: $inputSubject
        },
        outputSchema: {
            subject: $subject,
            consumerId: $consumerId
        },
        description: $description
    }')

# Make API call
log_info "Sending template registration request..."
RESPONSE=$(post_request "/api/consumers/$CONSUMER_ID/subjects/$SUBJECT/templates" "$PAYLOAD")
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