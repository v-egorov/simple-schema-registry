# JSLT Custom Functions Guide

This guide provides comprehensive documentation on creating transformations using custom JSLT functions in the Schema Registry. It covers the infrastructure, implementation patterns, engine integration, and provides a complete end-to-end example.

## JSLT Function Infrastructure

The JSLT custom functions infrastructure consists of several key components that work together to enable custom function registration and execution:

### Core Components

1. **`JsltFunctionRegistry`** - Central registry for managing custom functions
2. **`JsltBuiltInFunctions`** - Factory for creating built-in function implementations
3. **`JsltFunctionConfiguration`** - Spring configuration that registers built-in functions
4. **`JsltTransformationEngine`** - Enhanced JSLT engine that supports custom functions
5. **`TransformationService`** - Service layer that integrates function registry with transformations

### Function Registry Architecture

The `JsltFunctionRegistry` provides a thread-safe, centralized way to manage custom functions:

- **Registration**: Functions are registered by name and can be looked up during expression compilation
- **Isolation**: Each transformation can use different sets of functions if needed
- **Validation**: Functions are validated during both template creation and execution
- **Extensibility**: New functions can be added without modifying core engine code

## Implementing Custom Functions in Java

### Function Interface

Custom functions must implement the JSLT `Function` interface:

```java
import com.schibsted.spt.data.jslt.Function;
import com.fasterxml.jackson.databind.JsonNode;

public class MyCustomFunction implements Function {
    @Override
    public String getName() {
        return "my_function_name";
    }

    @Override
    public int getMinArguments() {
        return 1; // Minimum number of arguments
    }

    @Override
    public int getMaxArguments() {
        return 1; // Maximum number of arguments
    }

    @Override
    public JsonNode call(JsonNode input, JsonNode[] args) {
        // Function implementation
        return processArguments(args);
    }
}
```

### Function Implementation Patterns

#### 1. Data Extraction Functions

Functions that extract and transform data from complex structures:

```java
private class ExtractForecastTodayFunction implements Function {
    @Override
    public JsonNode call(JsonNode input, JsonNode[] args) {
        JsonNode metadataFields = args[0];
        if (!metadataFields.isArray()) return null;

        String value = null;
        String direction = null;

        // Search through array for specific fields
        for (JsonNode field : metadataFields) {
            if (field.has("id") && field.has("val")) {
                String fieldId = field.get("id").asText();
                String fieldValue = field.get("val").asText();

                if ("forecastTodayValue".equals(fieldId)) {
                    value = fieldValue;
                } else if ("forecastTodayDirection".equals(fieldId)) {
                    direction = fieldValue;
                }
            }
        }

        // Return structured result if both fields found
        if (value != null && direction != null) {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("value", Double.parseDouble(value));
            result.put("direction", direction);
            return result;
        }

        return null;
    }
}
```

#### 2. Data Transformation Functions

Functions that modify data types or structures:

```java
private class StringToNumberFunction implements Function {
    @Override
    public JsonNode call(JsonNode input, JsonNode[] args) {
        if (args.length != 1) return null;

        String strValue = args[0].asText();
        try {
            double numValue = Double.parseDouble(strValue);
            return objectMapper.createObjectNode().put("value", numValue);
        } catch (NumberFormatException e) {
            return null; // Invalid number
        }
    }
}
```

### Function Registration

Functions are registered through the `JsltFunctionConfiguration` class:

```java
@Configuration
public class JsltFunctionConfiguration {

    private final JsltFunctionRegistry functionRegistry;
    private final JsltBuiltInFunctions builtInFunctions;

    @PostConstruct
    public void registerBuiltInFunctions() {
        functionRegistry.register("extract_forecast_today",
            builtInFunctions.createExtractForecastTodayFunction());

        // Register UUID generation function
        functionRegistry.register("uuid",
            builtInFunctions.createUuidFunction());
     }
}
```

## JSLT Engine Integration

### Expression Compilation

The `JsltTransformationEngine` integrates custom functions during the compilation phase:

```java
public Map<String, Object> transform(Map<String, Object> inputJson,
                                   String expression,
                                   JsltFunctionRegistry functionRegistry) {
    Expression jsltExpression;
    if (functionRegistry != null && !functionRegistry.getFunctionNames().isEmpty()) {
        // Compile with custom functions available
        jsltExpression = Parser.compileString(expression,
                                            functionRegistry.getAllFunctions());
    } else {
        // Compile without custom functions
        jsltExpression = Parser.compileString(expression);
    }

    // Apply transformation...
}
```

### Validation Process

Functions are also available during expression validation:

```java
public boolean validateExpression(String expression,
                                JsltFunctionRegistry functionRegistry) {
    try {
        Expression jsltExpression;
        if (functionRegistry != null && !functionRegistry.getFunctionNames().isEmpty()) {
            jsltExpression = Parser.compileString(expression,
                                                functionRegistry.getAllFunctions());
        } else {
            jsltExpression = Parser.compileString(expression);
        }
        return true;
    } catch (JsltException e) {
        return false;
    }
}
```

### Service Layer Integration

The `TransformationService` automatically provides the function registry to JSLT transformations:

```java
if (engine instanceof JsltTransformationEngine && functionRegistry != null) {
    // Use JSLT engine with custom functions
    transformedJson = ((JsltTransformationEngine) engine).transform(
        request.getCanonicalJson(),
        template.getTemplateExpression(),
        functionRegistry
    );
}
```

## End-to-End Example: ForecastToday Transformation

This example demonstrates extracting forecast data from a complex nested structure using the `extract_forecast_today` custom function.

### Input Schema

```json
{
  "$schema": "https://json-schema.org/draft-04/schema",
  "type": "object",
  "title": "Input Schema with Nested additionalMetadataFields",
  "properties": {
    "id": {
      "type": "integer",
      "description": "Unique identifier"
    },
    "name": {
      "type": "string",
      "description": "Entity name"
    },
    "details": {
      "type": "object",
      "description": "Nested details object containing metadata",
      "properties": {
        "category": {
          "type": "string",
          "description": "Category classification"
        },
        "status": {
          "type": "string",
          "description": "Current status"
        },
        "additionalMetadataFields": {
          "type": "array",
          "description": "Array of key-value metadata pairs",
          "items": {
            "type": "object",
            "properties": {
              "id": {
                "type": "string",
                "description": "Metadata field identifier"
              },
              "val": {
                "type": "string",
                "description": "Metadata field value (as string)"
              }
            },
            "required": ["id", "val"],
            "additionalProperties": false
          }
        },
        "lastUpdated": {
          "type": "string",
          "format": "date-time",
          "description": "Last update timestamp"
        }
      },
      "required": ["category", "additionalMetadataFields"],
      "additionalProperties": false
    },
    "createdAt": {
      "type": "string",
      "format": "date-time",
      "description": "Creation timestamp"
    }
  },
  "required": ["id", "name", "details"],
  "additionalProperties": false
}
```

### Input JSON Data

```json
{
  "id": 12345,
  "name": "Financial Instrument ABC",
  "details": {
    "category": "equity",
    "status": "active",
    "additionalMetadataFields": [
      {
        "id": "forecastTodayValue",
        "val": "77.85"
      },
      {
        "id": "forecastTodayDirection",
        "val": "up"
      },
      {
        "id": "riskLevel",
        "val": "medium"
      },
      {
        "id": "lastAnalyzed",
        "val": "2025-10-14T20:30:00Z"
      }
    ],
    "lastUpdated": "2025-10-14T22:15:00Z"
  },
  "createdAt": "2025-10-01T10:00:00Z"
}
```

### JSLT Transformation Template

```json
{
  "id": .id,
  "name": .name,
  "createdAt": .createdAt,
  "details": {
    "category": .details.category,
    "status": .details.status,
    "lastUpdated": .details.lastUpdated,
    "forecastToday": extract_forecast_today(.details.additionalMetadataFields)
  }
}
```

### Output Schema

```json
{
  "$schema": "https://json-schema.org/draft-04/schema",
  "type": "object",
  "title": "Output Schema with Transformed forecastToday",
  "properties": {
    "id": {
      "type": "integer",
      "description": "Unique identifier (preserved from input)"
    },
    "name": {
      "type": "string",
      "description": "Entity name (preserved from input)"
    },
    "details": {
      "type": "object",
      "description": "Transformed details object",
      "properties": {
        "category": {
          "type": "string",
          "description": "Category classification (preserved from input)"
        },
        "status": {
          "type": "string",
          "description": "Current status (preserved from input)"
        },
        "lastUpdated": {
          "type": "string",
          "format": "date-time",
          "description": "Last update timestamp (preserved from input)"
        },
        "forecastToday": {
          "type": "object",
          "description": "Transformed forecast data (created if forecastTodayValue exists in input)",
          "properties": {
            "value": {
              "type": "number",
              "description": "Forecast value converted from string to number"
            },
            "direction": {
              "type": "string",
              "description": "Forecast direction (flat, up, down)"
            }
          },
          "required": ["value", "direction"],
          "additionalProperties": false
        }
      },
      "required": ["category"],
      "additionalProperties": false
    },
    "createdAt": {
      "type": "string",
      "format": "date-time",
      "description": "Creation timestamp (preserved from input)"
    }
  },
  "required": ["id", "name", "details"],
  "additionalProperties": false
}
```

### Expected Output JSON

```json
{
  "id": 12345,
  "name": "Financial Instrument ABC",
  "createdAt": "2025-10-01T10:00:00Z",
  "details": {
    "category": "equity",
    "status": "active",
    "lastUpdated": "2025-10-14T22:15:00Z",
    "forecastToday": {
      "value": 77.85,
      "direction": "up"
    }
  }
}
```

### Function Implementation Details

The `extract_forecast_today` function:

1. **Input**: Takes an array of metadata field objects
2. **Processing**: Searches for `forecastTodayValue` and `forecastTodayDirection` fields
3. **Type Conversion**: Converts the value string to a number using `Double.parseDouble()`
4. **Output**: Returns a structured object with `value` (number) and `direction` (string) properties
5. **Error Handling**: Returns `null` if required fields are missing or invalid

### UUID Generation Function

The `uuid()` function generates a random RFC 4122 compliant UUID (Universally Unique Identifier) string. This is useful for creating unique identifiers during data transformation.

#### Function Signature

```java
public Function createUuidFunction()
```

#### Usage in JSLT

```jslt
{
  "id": uuid(),
  "name": .name,
  "email": .email,
  "createdAt": .timestamp
}
```

#### Parameters

- **Arguments**: 0 (no arguments required)
- **Returns**: String containing a UUID in the format `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

#### Example Transformation

**Input JSON:**

```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "department": "Engineering"
}
```

**JSLT Template:**

```jslt
{
  "userId": uuid(),
  "name": .name,
  "email": .email,
  "department": .department,
  "createdAt": .timestamp
}
```

**Output JSON:**

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "department": "Engineering",
  "createdAt": "2025-10-21T09:00:00Z"
}
```

**Note**: Each call to `uuid()` generates a unique value, so multiple calls in the same transformation will produce different UUIDs.

### UUID Generation Function (No Dashes)

The `uuid-no-dashes()` function generates a random RFC 4122 compliant UUID string with dashes removed. This is useful when you need a compact UUID format for systems that don't support dashes.

#### Function Signature

```java
public Function createUuidNoDashesFunction()
```

#### Usage in JSLT

```jslt
{
  "id": uuid-no-dashes(),
  "name": .name,
  "email": .email,
  "createdAt": .timestamp
}
```

#### Parameters

- **Arguments**: 0 (no arguments required)
- **Returns**: String containing a UUID in the format `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` (32 characters)

#### Example Transformation

**Input JSON:**

```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "department": "Engineering"
}
```

**JSLT Template:**

```jslt
{
  "userId": uuid-no-dashes(),
  "name": .name,
  "email": .email,
  "department": .department,
  "createdAt": .timestamp
}
```

**Output JSON:**

```json
{
  "userId": "550e8400e29b41d4a716446655440000",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "department": "Engineering",
  "createdAt": "2025-10-21T09:00:00Z"
}
```

**Note**: Like `uuid()`, each call to `uuid-no-dashes()` generates a unique value.

### Testing the Transformation

You can test this transformation using the provided test files:

```bash
# Run the end-to-end example
./tests/utils/scripts/run-end-to-end-example.sh

# Or test the transformation directly
./tests/transform/test-transform-data.sh
```

## Best Practices

### Function Design

1. **Single Responsibility**: Each function should do one thing well
2. **Consistent Naming**: Use snake_case for function names (JSLT convention)
3. **Error Handling**: Return `null` for invalid inputs rather than throwing exceptions
4. **Type Safety**: Validate argument types and handle conversion errors gracefully
5. **Documentation**: Document function behavior, parameters, and return values

### Performance Considerations

1. **Efficient Processing**: Avoid unnecessary iterations over large data structures
2. **Memory Management**: Be mindful of object creation in tight loops
3. **Caching**: Consider caching expensive operations if functions are called frequently

### Security Considerations

1. **Input Validation**: Always validate function arguments
2. **Resource Limits**: Implement reasonable limits on data processing
3. **Safe Operations**: Avoid functions that could execute arbitrary code or access external resources

## Extending the System

### Adding New Built-in Functions

1. Add function implementation to `JsltBuiltInFunctions`
2. Register the function in `JsltFunctionConfiguration`
3. Update tests in `JsltBuiltInFunctionsTest`
4. Document the function in this guide

### Custom Function Libraries

For domain-specific functions, consider creating separate function libraries:

```java
@Component
public class DomainSpecificFunctions {

    public Function createBusinessLogicFunction() {
        return new BusinessLogicFunction();
    }
}
```

This approach keeps the core system clean while allowing extensibility for specific use cases.

