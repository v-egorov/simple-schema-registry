#!/bin/bash

# Pipeline Engine Transformation Example
# This script demonstrates how to use the pipeline engine to execute
# sequential multi-step transformations on order data.

set -e

# Source common utilities
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../../utils/common.sh"

echo "Pipeline Engine Transformation Example"
echo "====================================="

# Configuration
CONSUMER_ID="order-processor"
SUBJECT="pipeline-example"
TIMESTAMP=$(date +%s)
UNIQUE_CONSUMER="${CONSUMER_ID}-${TIMESTAMP}"

echo "Using consumer ID: $UNIQUE_CONSUMER"

# Step 1: Register consumer
echo
echo "Step 1: Registering consumer..."
create_test_consumer "$UNIQUE_CONSUMER" "Order Processing Pipeline Demo"

# Step 2: Register schemas
echo
echo "Step 2: Registering schemas..."
echo "  - Order input schema..."
"$SCRIPT_DIR/../../utils/scripts/register-schema-from-file.sh" "$SCRIPT_DIR/order-input.schema.json" "$SUBJECT" "BACKWARD" "Order input schema for pipeline example"

echo "  - Order output schema..."
"$SCRIPT_DIR/../../utils/scripts/register-schema-from-file.sh" "$SCRIPT_DIR/order-output.schema.json" "$SUBJECT" "$UNIQUE_CONSUMER" "BACKWARD" "Order output schema for pipeline example"

# Step 3: Register transformation templates
echo
echo "Step 3: Registering transformation templates..."
echo "  - Input validation template..."
"$SCRIPT_DIR/../../utils/scripts/register-jslt-template-from-file.sh" "$SCRIPT_DIR/input-validation-template.json" "$UNIQUE_CONSUMER" "$SUBJECT" "validation-v1.0.0" "$SUBJECT" "Input validation template"

echo "  - Data normalization template..."
"$SCRIPT_DIR/../../utils/scripts/register-jslt-template-from-file.sh" "$SCRIPT_DIR/data-normalization-template.json" "$UNIQUE_CONSUMER" "$SUBJECT" "normalization-v1.0.0" "$SUBJECT" "Data normalization template"

echo "  - Data enrichment template..."
"$SCRIPT_DIR/../../utils/scripts/register-jslt-template-from-file.sh" "$SCRIPT_DIR/data-enrichment-template.json" "$UNIQUE_CONSUMER" "$SUBJECT" "enrichment-v1.0.0" "$SUBJECT" "Data enrichment template"

# Step 4: Create pipeline template
echo
echo "Step 4: Creating pipeline template..."
PIPELINE_CONFIG=$(cat "$SCRIPT_DIR/pipeline-config.json")

create_pipeline_template "$UNIQUE_CONSUMER" "$SUBJECT" "$PIPELINE_CONFIG"

# Step 5: Test pipeline transformation
echo
echo "Step 5: Testing pipeline transformation..."

echo "  - Processing order through pipeline..."
"$SCRIPT_DIR/../../utils/scripts/transform-from-file.sh" "$SCRIPT_DIR/order-input.json" "$UNIQUE_CONSUMER" "$SUBJECT"

echo
echo "Pipeline transformation example completed successfully!"
echo
echo "Summary:"
echo "- Order data was processed through 3 sequential steps:"
echo "  1. Input validation: Added validation metadata"
echo "  2. Data normalization: Standardized data formats"
echo "  3. Data enrichment: Added computed fields (totals, timestamps)"
echo
echo "The pipeline engine executes transformations in sequence, passing"
echo "the output of each step as input to the next step."