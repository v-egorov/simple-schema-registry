#!/bin/bash

# Consumer Registration Script
# Registers a new consumer with the Schema Registry API
#
# Usage: ./register-consumer.sh <consumer_id> <name> [description]
#
# Arguments:
#   consumer_id: Unique identifier for the consumer (required)
#   name: Human-readable name for the consumer (required)
#   description: Optional description of the consumer
#
# Prerequisites:
#   - Service must be running
#   - Consumer ID must be unique and follow format: letters, numbers, hyphens, underscores
#
# Examples:
#   # Register a simple consumer
#   ./register-consumer.sh mobile-app "Mobile Application" "Consumer for mobile app data"
#
#   # Register with minimal data
#   ./register-consumer.sh web-client "Web Client"

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common.sh"

# Validate arguments
if [ $# -lt 2 ]; then
    log_error "Usage: $0 <consumer_id> <name> [description]"
    log_error ""
    log_error "Examples:"
    log_error "  # Register a consumer with description"
    log_error "  $0 mobile-app 'Mobile Application' 'Consumer for mobile app data'"
    log_error ""
    log_error "  # Register with minimal data"
    log_error "  $0 web-client 'Web Client'"
    exit 1
fi

CONSUMER_ID="$1"
NAME="$2"
DESCRIPTION="${3:-Consumer registered via script}"

# Validate consumer ID format
if [[ ! "$CONSUMER_ID" =~ ^[a-zA-Z0-9_-]+$ ]]; then
    log_error "Invalid consumer ID format: $CONSUMER_ID"
    log_error "Consumer ID must contain only letters, numbers, hyphens, and underscores"
    exit 1
fi

# Validate name is not empty
if [ -z "$NAME" ]; then
    log_error "Consumer name cannot be empty"
    exit 1
fi

log_info "Registering consumer: $CONSUMER_ID"
log_info "Name: $NAME"
log_info "Description: $DESCRIPTION"

# Construct JSON payload using jq
PAYLOAD=$(jq -n \
    --arg consumerId "$CONSUMER_ID" \
    --arg name "$NAME" \
    --arg description "$DESCRIPTION" \
    '{
        consumerId: $consumerId,
        name: $name,
        description: $description
    }')

# Make API call
log_info "Sending registration request..."
RESPONSE=$(post_request "/api/consumers" "$PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$RESPONSE" | head -n -1)

# Check response
if [ "$HTTP_CODE" -eq 201 ]; then
    log_success "Consumer registered successfully"
    echo "Response:"
    echo "$RESPONSE_BODY" | jq '.'
else
    log_error "Consumer registration failed (HTTP $HTTP_CODE)"
    echo "Error response:"
    echo "$RESPONSE_BODY"
    exit 1
fi