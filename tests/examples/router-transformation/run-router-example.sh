#!/bin/bash

# Router Engine Transformation Example
# This script demonstrates how to use the router engine to route different data types
# to appropriate transformation templates based on conditions.

set -e

# Source common utilities
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../../utils/common.sh"

echo "Router Engine Transformation Example"
echo "===================================="

# Configuration
CONSUMER_ID="mobile-app"
SUBJECT="router-example"
TIMESTAMP=$(date +%s)
UNIQUE_CONSUMER="${CONSUMER_ID}-${TIMESTAMP}"

echo "Using consumer ID: $UNIQUE_CONSUMER"

# Step 1: Register consumer
echo
echo "Step 1: Registering consumer..."
create_test_consumer "$UNIQUE_CONSUMER" "Mobile Application for Router Demo"

# Step 2: Register schemas
echo
echo "Step 2: Registering schemas..."
echo "  - Canonical schema..."
"$SCRIPT_DIR/../../utils/scripts/register-schema-from-file.sh" "$SCRIPT_DIR/canonical.schema.json" "$SUBJECT" "BACKWARD" "Canonical schema for router example"

echo "  - Consumer output schema..."
"$SCRIPT_DIR/../../utils/scripts/register-schema-from-file.sh" "$SCRIPT_DIR/consumer-output.schema.json" "$SUBJECT" "$UNIQUE_CONSUMER" "BACKWARD" "Consumer output schema for router example"

# Step 3: Register transformation templates
echo
echo "Step 3: Registering transformation templates..."
echo "  - User normalization template..."
"$SCRIPT_DIR/../../utils/scripts/register-jslt-template-from-file.sh" "$SCRIPT_DIR/user-normalization.jslt" "$UNIQUE_CONSUMER" "$SUBJECT" "user-v1.0.0" "$SUBJECT" "User normalization template"

echo "  - Product enrichment template..."
"$SCRIPT_DIR/../../utils/scripts/register-jslt-template-from-file.sh" "$SCRIPT_DIR/product-enrichment.jslt" "$UNIQUE_CONSUMER" "$SUBJECT" "product-v1.0.0" "$SUBJECT" "Product enrichment template"

echo "  - Generic processing template..."
"$SCRIPT_DIR/../../utils/scripts/register-jslt-template-from-file.sh" "$SCRIPT_DIR/generic-processing.jslt" "$UNIQUE_CONSUMER" "$SUBJECT" "generic-v1.0.0" "$SUBJECT" "Generic processing template"

# Step 4: Create router template
echo
echo "Step 4: Creating router template..."
ROUTER_CONFIG=$(cat "$SCRIPT_DIR/router-config.json")

create_router_template "$UNIQUE_CONSUMER" "$SUBJECT" "$ROUTER_CONFIG"

# Step 4b: Activate router template
echo
echo "Step 4b: Activating router template..."
activate_template "$UNIQUE_CONSUMER" "$SUBJECT" "1.0.0"

# Step 5: Test transformations
echo
echo "Step 5: Testing router transformations..."

# Test user data
echo "  - Testing user data routing..."
"$SCRIPT_DIR/../../utils/scripts/transform-from-file.sh" "$SCRIPT_DIR/user-input.json" "$UNIQUE_CONSUMER" "$SUBJECT"

# Test product data
echo "  - Testing product data routing..."
"$SCRIPT_DIR/../../utils/scripts/transform-from-file.sh" "$SCRIPT_DIR/product-input.json" "$UNIQUE_CONSUMER" "$SUBJECT"

# Test order data (default route)
echo "  - Testing order data routing (default route)..."
"$SCRIPT_DIR/../../utils/scripts/transform-from-file.sh" "$SCRIPT_DIR/order-input.json" "$UNIQUE_CONSUMER" "$SUBJECT"

echo
echo "Router transformation example completed successfully!"
echo
echo "Summary:"
echo "- User data was routed to user-normalization-v1 template"
echo "- Product data was routed to product-enrichment-v1 template"
echo "- Order data was routed to generic-processing-v1 template (default)"
echo
echo "The router engine automatically selects the appropriate transformation"
echo "based on the 'type' field in the input data."