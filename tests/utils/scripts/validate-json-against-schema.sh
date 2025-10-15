#!/bin/bash

# Validate JSON against canonical schema
# Validates JSON data from a file against the canonical schema for a given subject
#
# Usage: ./validate-json-against-schema.sh <json_file> <subject> [version]
#
# Arguments:
#   json_file: Path to JSON data file to validate
#   subject: Schema subject to validate against
#   version: Optional schema version (uses latest if not specified)
#
# Prerequisites:
#   - jq must be installed
#   - Service must be running
#   - JSON file must contain valid JSON
#   - Subject must exist in schema registry

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

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

# Check dependencies
if ! command -v jq &> /dev/null; then
    log_error "jq is required but not installed. Please install jq first."
    exit 1
fi

# Validate arguments
if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    log_error "Usage: $0 <json_file> <subject> [version]"
    log_error "Example: $0 data.json user-profile"
    log_error "Example: $0 data.json user-profile 1.0.0"
    exit 1
fi

JSON_FILE="$1"
SUBJECT="$2"
VERSION="$3"

# Validate JSON file exists
if [ ! -f "$JSON_FILE" ]; then
    log_error "JSON file '$JSON_FILE' does not exist"
    exit 1
fi

# Validate JSON file is readable JSON
if ! jq empty "$JSON_FILE" 2>/dev/null; then
    log_error "JSON file '$JSON_FILE' is not valid JSON"
    exit 1
fi

log_info "Validating JSON file: $JSON_FILE"
log_info "Subject: $SUBJECT"
if [ -n "$VERSION" ]; then
    log_info "Version: $VERSION"
fi

# Get file size for progress indication
FILE_SIZE=$(wc -c < "$JSON_FILE" 2>/dev/null || echo "0")
FILE_SIZE_KB=$((FILE_SIZE / 1024))
if [ "$FILE_SIZE_KB" -gt 1024 ]; then
    FILE_SIZE_MB=$((FILE_SIZE_KB / 1024))
    SIZE_DISPLAY="${FILE_SIZE_MB}MB"
else
    SIZE_DISPLAY="${FILE_SIZE_KB}KB"
fi
log_info "File size: ${SIZE_DISPLAY}"

# Build validation request payload
# For large files, construct payload manually to avoid command line length limits
TEMP_PAYLOAD_FILE=$(mktemp)
echo "{\"subject\":\"$SUBJECT\",\"jsonData\":" > "$TEMP_PAYLOAD_FILE"
cat "$JSON_FILE" >> "$TEMP_PAYLOAD_FILE"
echo "}" >> "$TEMP_PAYLOAD_FILE"

# Build URL with optional version
URL="/api/schemas/$SUBJECT/validate"
if [ -n "$VERSION" ]; then
    URL="${URL}/versions/$VERSION"
fi

log_info "Sending validation request to: $BASE_URL$URL"

# Make API call using file to avoid command line length limits
RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 30 -X POST "$BASE_URL$URL" \
    -H "$CONTENT_TYPE" \
    -H "$ACCEPT" \
    -d @"$TEMP_PAYLOAD_FILE")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$RESPONSE" | head -n -1)

# Clean up temp file
rm -f "$TEMP_PAYLOAD_FILE"

# Process response
if [ "$HTTP_CODE" -eq 200 ]; then
    IS_VALID=$(echo "$RESPONSE_BODY" | jq -r '.valid // false')

    if [ "$IS_VALID" = "true" ]; then
        log_success "✅ JSON validation PASSED"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "File: $JSON_FILE"
        echo "Subject: $SUBJECT"
        echo "Size: ${SIZE_DISPLAY}"
        echo "Status: Valid"
        if [ -n "$VERSION" ]; then
            echo "Schema Version: $VERSION"
        else
            echo "Schema Version: Latest"
        fi
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    else
        log_error "❌ JSON validation FAILED"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "File: $JSON_FILE"
        echo "Subject: $SUBJECT"
        echo "Size: ${SIZE_DISPLAY}"
        echo "Status: Invalid"
        if [ -n "$VERSION" ]; then
            echo "Schema Version: $VERSION"
        else
            echo "Schema Version: Latest"
        fi
        echo
        echo "Validation Errors:"
        echo "$RESPONSE_BODY" | jq -r '.errors[]? // "No error details available"'
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        exit 1
    fi
else
    log_error "❌ Validation request failed (HTTP $HTTP_CODE)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "File: $JSON_FILE"
    echo "Subject: $SUBJECT"
    echo "Error: HTTP $HTTP_CODE"
    echo
    echo "Response:"
    echo "$RESPONSE_BODY"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    exit 1
fi