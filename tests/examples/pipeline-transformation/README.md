# Pipeline Engine Transformation Example

This example demonstrates how to use the Pipeline Engine in the JSON Schema Registry and Transformation Service to execute sequential multi-step transformations on order data.

## Overview

The Pipeline Engine allows you to define a sequence of transformation steps that are executed in order, where the output of each step becomes the input for the next step. This is useful for:

- **Data Processing Workflows**: Complex data transformations requiring multiple stages
- **ETL Pipelines**: Extract, Transform, Load operations with intermediate validation
- **Data Enrichment**: Progressive addition of computed fields and metadata
- **Quality Assurance**: Validation and normalization at each processing stage

## Example Scenario

In this example, we process customer order data through a 3-step pipeline:

1. **Input Validation**: Validates data structure and adds validation metadata
2. **Data Normalization**: Standardizes formats and cleans data inconsistencies
3. **Data Enrichment**: Adds computed fields like totals, timestamps, and processing metadata

The pipeline transforms raw order data into enriched, validated output ready for downstream consumers.

## Files

- `order-input.schema.json` - JSON Schema for input order data validation
- `order-output.schema.json` - JSON Schema for processed order data validation
- `pipeline-config.json` - Pipeline engine configuration with sequential steps
- `order-input.json` - Sample raw order data input
- `order-expected-output.json` - Expected output after pipeline processing
- `input-validation-template.json` - Template configuration for validation step
- `data-normalization-template.json` - Template configuration for normalization step
- `data-enrichment-template.json` - Template configuration for enrichment step
- `run-pipeline-example.sh` - Executable demonstration script

## Pipeline Configuration

The pipeline configuration defines the sequence of transformations:

```json
{
  "type": "pipeline",
  "steps": [
    {
      "name": "validate-input",
      "transformationId": "input-validation-v1",
      "description": "Validate input data structure and add validation metadata"
    },
    {
      "name": "normalize-data",
      "transformationId": "data-normalization-v1",
      "description": "Normalize data formats and clean inconsistencies"
    },
    {
      "name": "enrich-data",
      "transformationId": "data-enrichment-v1",
      "description": "Add computed fields, totals, and processing metadata"
    }
  ],
  "validation": {
    "finalSchema": "order-processed-schema-v1"
  }
}
```

### Pipeline Steps

1. **Input Validation** (`input-validation-v1`):
   - Adds `validation.validated: true` to track processing status
   - Ensures data structure meets requirements

2. **Data Normalization** (`data-normalization-v1`):
   - Adds `validation.normalized: true` to track processing status
   - Standardizes data formats and cleans inconsistencies

3. **Data Enrichment** (`data-enrichment-v1`):
   - Computes `lineTotal` for each item (`quantity * price`)
   - Calculates `orderTotal` as sum of all line totals
   - Adds `itemCount` as total quantity across all items
   - Adds `processedAt` timestamp
   - Sets `validation.enriched: true`

## Data Flow

### Input Data
```json
{
  "orderId": "ORD-2024-001",
  "customerId": "CUST-12345",
  "items": [
    {
      "productId": "PROD-001",
      "name": "Wireless Headphones",
      "quantity": 2,
      "price": 99.99
    },
    {
      "productId": "PROD-002",
      "name": "USB Cable",
      "quantity": 1,
      "price": 15.50
    }
  ],
  "orderDate": "2024-11-06T10:30:00Z",
  "status": "confirmed"
}
```

### Output Data
```json
{
  "orderId": "ORD-2024-001",
  "customerId": "CUST-12345",
  "items": [
    {
      "productId": "PROD-001",
      "name": "Wireless Headphones",
      "quantity": 2,
      "price": 99.99,
      "lineTotal": 199.98
    },
    {
      "productId": "PROD-002",
      "name": "USB Cable",
      "quantity": 1,
      "price": 15.50,
      "lineTotal": 15.50
    }
  ],
  "orderDate": "2024-11-06T10:30:00Z",
  "status": "confirmed",
  "orderTotal": 215.48,
  "itemCount": 3,
  "processedAt": "2024-11-06T10:35:00Z",
  "validation": {
    "validated": true,
    "normalized": true,
    "enriched": true
  }
}
```

## Running the Example

1. **Prerequisites**: Ensure PostgreSQL and the application are running.

2. **Execute the example**:
   ```bash
   ./tests/examples/pipeline-transformation/run-pipeline-example.sh
   ```

3. **Expected Output**:
   ```
   Pipeline Engine Transformation Example
   =====================================

   Using consumer ID: order-processor-1700000000

   Step 1: Registering consumer...
   Consumer registered successfully

   Step 2: Registering schemas...
     - Order input schema...
   Schema registered successfully
     - Order output schema...
   Consumer schema registered successfully

   Step 3: Registering transformation templates...
     - Input validation template...
   Template registered successfully
     - Data normalization template...
   Template registered successfully
     - Data enrichment template...
   Template registered successfully

   Step 4: Creating pipeline template...
   Pipeline template created successfully

   Step 5: Testing pipeline transformation...
     - Processing order through pipeline...
       Order processed successfully through pipeline

   Pipeline transformation example completed successfully!

   Summary:
   - Order data was processed through 3 sequential steps:
     1. Input validation: Added validation metadata
     2. Data normalization: Standardized data formats
     3. Data enrichment: Added computed fields (totals, timestamps)

   The pipeline engine executes transformations in sequence, passing
   the output of each step as input to the next step.
   ```

## Manual Testing

You can also test the pipeline manually using curl:

```bash
# Transform order data through pipeline
curl -X POST http://localhost:8080/api/consumers/order-processor/subjects/pipeline-example/transform \
  -H "Content-Type: application/json" \
  -d @tests/examples/pipeline-transformation/order-input.json
```

## Key Concepts

### Sequential Execution
- Steps execute in the order defined in the configuration
- Each step's output becomes the next step's input
- Pipeline fails if any step fails (unless `continueOnError` is set)

### Error Handling
- By default, pipeline stops on first error
- Individual steps can be configured with `continueOnError: true` to continue despite failures
- Error information is logged and can be included in final output

### Transformation References
- Pipeline steps reference transformation templates by `transformationId`
- Referenced templates must exist and be accessible
- Templates can be JSLT, router, or other pipeline engines

### Schema Validation
- Optional `validation.finalSchema` validates the final pipeline output
- Intermediate validation can be configured per step if needed
- Ensures data quality throughout the processing pipeline

## Benefits of Pipeline Engine

1. **Modular Processing**: Break complex transformations into manageable steps
2. **Reusability**: Individual transformation steps can be reused in different pipelines
3. **Maintainability**: Easy to modify, add, or remove processing steps
4. **Debugging**: Clear visibility into each processing stage
5. **Error Recovery**: Configurable error handling and recovery strategies
6. **Performance Monitoring**: Track performance and success of each step

## Extending the Example

To add a new processing step:

1. Create a new JSLT template for the additional processing
2. Register the template with the service
3. Add a new step to the pipeline configuration
4. Update schemas if the output structure changes
5. Test the extended pipeline

Example new step:
```json
{
  "name": "quality-check",
  "transformationId": "quality-check-v1",
  "description": "Perform quality checks on processed data"
}
```

This demonstrates the pipeline engine's extensibility for building complex, multi-stage data processing workflows.