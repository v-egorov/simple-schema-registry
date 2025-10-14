#!/bin/bash

# Schema Registration from File
# Registers a JSON schema from a file with the Schema Registry API
#
# Usage: ./register-schema-from-file.sh <schema_file> <subject> [compatibility] [description]
#
# Arguments:
#   schema_file: Path to JSON schema file
#   subject: Schema subject name
#   compatibility: Schema compatibility mode (default: BACKWARD)
#   description: Optional schema description
#
# Prerequisites:
#   - jq must be installed
#   - Service must be running
#   - Schema file must contain valid JSON schema

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
if [ $# -lt 2 ]; then
    log_error "Usage: $0 <schema_file> <subject> [compatibility] [description]"
    log_error "Example: $0 user-schema.json user-profile BACKWARD 'User profile schema'"
    exit 1
fi

SCHEMA_FILE="$1"
SUBJECT="$2"
COMPATIBILITY="${3:-BACKWARD}"
DESCRIPTION="${4:-Schema registered from file}"

# Validate schema file exists
if [ ! -f "$SCHEMA_FILE" ]; then
    log_error "Schema file '$SCHEMA_FILE' does not exist"
    exit 1
fi

# Validate schema file is readable JSON
if ! jq empty "$SCHEMA_FILE" 2>/dev/null; then
    log_error "Schema file '$SCHEMA_FILE' is not valid JSON"
    exit 1
fi

log_info "Registering schema from file: $SCHEMA_FILE"
log_info "Subject: $SUBJECT"
log_info "Compatibility: $COMPATIBILITY"

# Construct JSON payload using jq
PAYLOAD=$(jq -n \
    --arg subject "$SUBJECT" \
    --arg compatibility "$COMPATIBILITY" \
    --arg description "$DESCRIPTION" \
    --argjson schema "$(cat "$SCHEMA_FILE")" \
    '{
        subject: $subject,
        schema: $schema,
        compatibility: $compatibility,
        description: $description
    }')

# Make API call
log_info "Sending registration request..."
RESPONSE=$(post_request "/api/schemas/$SUBJECT" "$PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$RESPONSE" | head -n -1)

# Check response
if [ "$HTTP_CODE" -eq 201 ]; then
    log_success "Schema registered successfully"
    echo "Response:"
    echo "$RESPONSE_BODY" | jq '.'
else
    log_error "Schema registration failed (HTTP $HTTP_CODE)"
    echo "Error response:"
    echo "$RESPONSE_BODY"
    exit 1
fi