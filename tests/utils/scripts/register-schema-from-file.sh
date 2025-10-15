#!/bin/bash

# Schema Registration from File
# Registers a JSON schema from a file with the Schema Registry API
#
# Usage: ./register-schema-from-file.sh <schema_file> <subject> [consumer_id] [compatibility] [description]
#
# Arguments:
#   schema_file: Path to JSON schema file
#   subject: Schema subject name
#   consumer_id: Consumer ID (optional, for consumer output schemas)
#   compatibility: Schema compatibility mode (default: BACKWARD)
#   description: Optional schema description
#
# Prerequisites:
#   - jq must be installed
#   - Service must be running
#   - Schema file must contain valid JSON schema
#
# Examples:
#   # Register canonical schema
#   ./register-schema-from-file.sh user-schema.json user-profile BACKWARD "User profile schema"
#
#   # Register consumer output schema
#   ./register-schema-from-file.sh output-schema.json user-profile consumer-123 BACKWARD "Consumer output schema"

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
    log_error "Usage: $0 <schema_file> <subject> [consumer_id] [compatibility] [description]"
    log_error ""
    log_error "Examples:"
    log_error "  # Register canonical schema"
    log_error "  $0 user-schema.json user-profile BACKWARD 'User profile schema'"
    log_error ""
    log_error "  # Register consumer output schema"
    log_error "  $0 output-schema.json user-profile consumer-123 BACKWARD 'Consumer output schema'"
    exit 1
fi

SCHEMA_FILE="$1"
SUBJECT="$2"

# Detect schema type based on parameters
if [ $# -ge 3 ]; then
    # Check if 3rd parameter looks like compatibility mode
    if [[ "$3" =~ ^(BACKWARD|FORWARD|FULL)$ ]]; then
        # Canonical schema: subject, compatibility, description
        CONSUMER_ID=""
        COMPATIBILITY="$3"
        DESCRIPTION="${4:-Schema registered from file}"
        SCHEMA_TYPE="canonical"
    else
        # Consumer output schema: subject, consumer_id, compatibility, description
        CONSUMER_ID="$3"
        COMPATIBILITY="${4:-BACKWARD}"
        DESCRIPTION="${5:-Schema registered from file}"
        SCHEMA_TYPE="consumer_output"

        # Validate consumer ID format
        if [[ ! "$CONSUMER_ID" =~ ^[a-zA-Z0-9_-]+$ ]]; then
            log_error "Invalid consumer ID format: $CONSUMER_ID"
            log_error "Consumer ID must contain only letters, numbers, hyphens, and underscores"
            exit 1
        fi
    fi
else
    # Default canonical schema
    CONSUMER_ID=""
    COMPATIBILITY="BACKWARD"
    DESCRIPTION="Schema registered from file"
    SCHEMA_TYPE="canonical"
fi

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
log_info "Schema Type: $SCHEMA_TYPE"
if [ -n "$CONSUMER_ID" ]; then
    log_info "Consumer ID: $CONSUMER_ID"
fi
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

# Determine API endpoint
if [ -n "$CONSUMER_ID" ]; then
    ENDPOINT="/api/consumers/$CONSUMER_ID/schemas/$SUBJECT"
else
    ENDPOINT="/api/schemas/$SUBJECT"
fi

# Make API call
log_info "Sending registration request..."
RESPONSE=$(post_request "$ENDPOINT" "$PAYLOAD")
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