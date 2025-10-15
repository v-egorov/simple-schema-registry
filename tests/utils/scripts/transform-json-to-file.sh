#!/bin/bash

# Transform JSON data and save to file
# Transforms JSON data from a file using the Schema Registry API and saves result to output file
#
# Usage: ./transform-json-to-file.sh <input_file> <consumer_id> <subject> <output_file> [version]
#
# Arguments:
#   input_file: Path to JSON data file to transform
#   consumer_id: Consumer ID that has transformation template
#   subject: Schema subject for the data
#   output_file: Path where transformed JSON will be saved
#   version: Optional template version (uses active version if not specified)
#
# Prerequisites:
#   - jq must be installed
#   - Service must be running
#   - Input file must contain valid JSON
#   - Consumer must be registered with transformation template
#   - Subject must match consumer's registered subjects
#   - Output file will be overwritten if it exists

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
if [ $# -lt 4 ] || [ $# -gt 5 ]; then
    log_error "Usage: $0 <input_file> <consumer_id> <subject> <output_file> [version]"
    log_error "Example: $0 user-data.json mobile-app user-profile output.json"
    log_error "Example: $0 user-data.json mobile-app user-profile output.json 1.0.0"
    exit 1
fi

INPUT_FILE="$1"
CONSUMER_ID="$2"
SUBJECT="$3"
OUTPUT_FILE="$4"
VERSION="$5"

# Validate input file exists
if [ ! -f "$INPUT_FILE" ]; then
    log_error "Input file '$INPUT_FILE' does not exist"
    exit 1
fi

# Validate input file is readable JSON
if ! jq empty "$INPUT_FILE" 2>/dev/null; then
    log_error "Input file '$INPUT_FILE' is not valid JSON"
    exit 1
fi

# Check if output file already exists
if [ -f "$OUTPUT_FILE" ]; then
    log_warning "Output file '$OUTPUT_FILE' already exists and will be overwritten"
fi

log_info "Transforming JSON data"
log_info "Input file: $INPUT_FILE"
log_info "Consumer: $CONSUMER_ID"
log_info "Subject: $SUBJECT"
log_info "Output file: $OUTPUT_FILE"
if [ -n "$VERSION" ]; then
    log_info "Version: $VERSION"
fi

# Get input file size
INPUT_SIZE=$(wc -c < "$INPUT_FILE" 2>/dev/null || echo "0")
INPUT_SIZE_KB=$((INPUT_SIZE / 1024))
if [ "$INPUT_SIZE_KB" -gt 1024 ]; then
    INPUT_SIZE_MB=$((INPUT_SIZE_KB / 1024))
    INPUT_SIZE_DISPLAY="${INPUT_SIZE_MB}MB"
else
    INPUT_SIZE_DISPLAY="${INPUT_SIZE_KB}KB"
fi
log_info "Input size: ${INPUT_SIZE_DISPLAY}"

# Build transformation request payload
# For large files, construct payload manually to avoid command line length limits
TEMP_PAYLOAD_FILE=$(mktemp)
echo "{\"subject\":\"$SUBJECT\",\"canonicalJson\":" > "$TEMP_PAYLOAD_FILE"
cat "$INPUT_FILE" >> "$TEMP_PAYLOAD_FILE"
echo "}" >> "$TEMP_PAYLOAD_FILE"

# Build URL with optional version
URL="/api/consumers/$CONSUMER_ID/subjects/$SUBJECT/transform"
if [ -n "$VERSION" ]; then
    URL="${URL}/versions/$VERSION"
fi

log_info "Sending transformation request to: $BASE_URL$URL"

# Make API call using file to avoid command line length limits
RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 60 -X POST "$BASE_URL$URL" \
    -H "$CONTENT_TYPE" \
    -H "$ACCEPT" \
    -d @"$TEMP_PAYLOAD_FILE")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$RESPONSE" | head -n -1)

# Clean up temp file
rm -f "$TEMP_PAYLOAD_FILE"

# Process response
if [ "$HTTP_CODE" -eq 200 ]; then
    log_success "✅ Transformation completed successfully"

    # Extract transformed JSON from response
    TRANSFORMED_JSON=$(echo "$RESPONSE_BODY" | jq -r '.transformedJson')

    # Validate that we got valid JSON
    if [ "$TRANSFORMED_JSON" = "null" ] || [ -z "$TRANSFORMED_JSON" ]; then
        log_error "❌ No transformed JSON found in response"
        echo "Response: $RESPONSE_BODY"
        exit 1
    fi

    # Save to output file with pretty formatting
    echo "$TRANSFORMED_JSON" | jq '.' > "$OUTPUT_FILE"

    # Get output file size
    OUTPUT_SIZE=$(wc -c < "$OUTPUT_FILE" 2>/dev/null || echo "0")
    OUTPUT_SIZE_KB=$((OUTPUT_SIZE / 1024))
    if [ "$OUTPUT_SIZE_KB" -gt 1024 ]; then
        OUTPUT_SIZE_MB=$((OUTPUT_SIZE_KB / 1024))
        OUTPUT_SIZE_DISPLAY="${OUTPUT_SIZE_MB}MB"
    else
        OUTPUT_SIZE_DISPLAY="${OUTPUT_SIZE_KB}KB"
    fi

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Input file: $INPUT_FILE (${INPUT_SIZE_DISPLAY})"
    echo "Output file: $OUTPUT_FILE (${OUTPUT_SIZE_DISPLAY})"
    echo "Consumer: $CONSUMER_ID"
    echo "Subject: $SUBJECT"
    if [ -n "$VERSION" ]; then
        echo "Version: $VERSION"
    else
        echo "Version: Active"
    fi
    echo "Status: Transformed successfully"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

else
    log_error "❌ Transformation failed (HTTP $HTTP_CODE)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Input file: $INPUT_FILE"
    echo "Consumer: $CONSUMER_ID"
    echo "Subject: $SUBJECT"
    echo "Output file: $OUTPUT_FILE (not created)"
    echo "Error: HTTP $HTTP_CODE"
    echo
    echo "Error response:"
    echo "$RESPONSE_BODY" | jq '.'
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    exit 1
fi