# Router Engine Transformation Example

This example demonstrates how to use the Router Engine in the JSON Schema Registry and Transformation Service to intelligently route different types of data to appropriate transformation templates based on conditions.

## Overview

The Router Engine allows you to define routing rules that examine the input data and select the appropriate transformation template to apply. This is useful for:

- Multi-tenant applications with different data processing needs
- Content-based routing based on data characteristics
- Conditional transformations based on input properties

## Example Scenario

In this example, we have three types of data:
- **User data**: Contains user information that needs normalization
- **Product data**: Contains product information that needs enrichment
- **Order data**: Contains order information that gets generic processing

The router examines the `type` field and routes each data type to the appropriate transformation.

## Files

- `canonical.schema.json` - JSON Schema for input data validation
- `consumer-output.schema.json` - JSON Schema for output data validation
- `router-config.json` - Router engine configuration with routing rules
- `user-input.json` - Sample user data input
- `product-input.json` - Sample product data input
- `order-input.json` - Sample order data input (routes to default)
- `user-normalization.jslt` - JSLT expression for user data processing
- `product-enrichment.jslt` - JSLT expression for product data processing
- `generic-processing.jslt` - JSLT expression for default processing
- `*-template.json` - Template configuration files (for reference)
- `user-expected-output.json` - Expected output for user data
- `product-expected-output.json` - Expected output for product data
- `order-expected-output.json` - Expected output for order data

## Router Configuration

The router configuration defines:

```json
{
  "type": "router",
  "routes": [
    {
      "condition": "$.type == 'user'",
      "transformationId": "user-normalization-v1",
      "description": "Normalize user data for mobile app"
    },
    {
      "condition": "$.type == 'product'",
      "transformationId": "product-enrichment-v1",
      "description": "Enrich product data with additional fields"
    }
  ],
  "defaultTransformationId": "generic-processing-v1",
  "validation": {
    "inputSchema": "canonical-data-schema-v1",
    "outputSchema": "consumer-data-schema-v1"
  }
}
```

### Routing Logic

1. **User Data** (`type == 'user'`): Routes to `user-normalization-v1`
   - Adds `processed: true` and `route: "user"`
   - Maps user fields appropriately

2. **Product Data** (`type == 'product'`): Routes to `product-enrichment-v1`
   - Adds `processed: true`, `route: "product"`, and `enriched: true`
   - Includes product-specific fields

3. **Other Data** (default route): Routes to `generic-processing-v1`
   - Adds `processed: true`, `route: "generic"`, and `normalized: true`
   - Provides basic processing for unrecognized types

## Running the Example

1. **Prerequisites**: Ensure PostgreSQL and the application are running.

2. **Execute the example**:
   ```bash
   ./tests/examples/router-transformation/run-router-example.sh
   ```

3. **Expected Output**:
   ```
   Router Engine Transformation Example
   ====================================

   Using consumer ID: mobile-app-1700000000

   Step 1: Registering consumer...
   Consumer registered successfully

   Step 2: Registering schemas...
     - Canonical schema...
   Schema registered successfully
     - Consumer output schema...
   Consumer schema registered successfully

   Step 3: Registering transformation templates...
     - User normalization template...
   Template registered successfully
     - Product enrichment template...
   Template registered successfully
     - Generic processing template...
   Template registered successfully

   Step 4: Creating router template...
   Router template created successfully

   Step 5: Testing router transformations...
     - Testing user data routing...
       User input routed to: user
       Processed: true
     - Testing product data routing...
       Product input routed to: product
       Enriched: true
     - Testing order data routing (default route)...
       Order input routed to: generic
       Normalized: true

   Router transformation example completed successfully!

   Summary:
   - User data was routed to user-normalization-v1 template
   - Product data was routed to product-enrichment-v1 template
   - Order data was routed to generic-processing-v1 template (default)
   ```

## Manual Testing

You can also test the router manually using curl:

```bash
# Transform user data
curl -X POST http://localhost:8080/api/consumers/mobile-app/subjects/router-example/transform \
  -H "Content-Type: application/json" \
  -d @tests/examples/router-transformation/user-input.json

# Transform product data
curl -X POST http://localhost:8080/api/consumers/mobile-app/subjects/router-example/transform \
  -H "Content-Type: application/json" \
  -d @tests/examples/router-transformation/product-input.json

# Transform order data (default route)
curl -X POST http://localhost:8080/api/consumers/mobile-app/subjects/router-example/transform \
  -H "Content-Type: application/json" \
  -d @tests/examples/router-transformation/order-input.json
```

## Key Concepts

### Condition Evaluation
The router uses JSONPath expressions to evaluate conditions:
- `$.type == 'user'` - Checks if the `type` field equals 'user'
- Conditions support basic equality comparisons
- Future versions may support more complex expressions

### Transformation References
- Router templates reference other templates by `transformationId`
- Referenced templates must exist and be accessible
- Templates can be JSLT, router, or pipeline engines

### Default Routing
- When no route conditions match, data routes to `defaultTransformationId`
- Ensures all data gets processed even for unexpected types
- Provides fallback processing logic

### Schema Validation
- Optional `validation` section enables schema validation
- `inputSchema` validates data before routing
- `outputSchema` validates final transformed data

## Benefits of Router Engine

1. **Separation of Concerns**: Different data types get specialized processing
2. **Maintainability**: Easy to add new routes without changing existing logic
3. **Flexibility**: Can route based on any data characteristics
4. **Fallback Handling**: Default route ensures no data is lost
5. **Performance**: Only executes relevant transformations

## Extending the Example

To add a new data type:

1. Create a new JSLT template for the data type
2. Register the template with the service
3. Add a new route to the router configuration
4. Test with sample data

Example new route:
```json
{
  "condition": "$.type == 'invoice'",
  "transformationId": "invoice-processing-v1",
  "description": "Process invoice data"
}
```

This demonstrates the router engine's extensibility for handling diverse data processing requirements.