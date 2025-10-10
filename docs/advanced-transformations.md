# Advanced Transformations Guide

This guide covers the implementation and usage of advanced transformation engines in the JSON Schema Registry and Transformation Service. It includes detailed information about the router and pipeline engines, their Java-based implementations, and best practices for extension.

## Overview

The service supports three transformation engines:

1. **JSLT Engine** - Simple JSON-to-JSON transformations
2. **Router Engine** - Intelligent routing based on data characteristics
3. **Pipeline Engine** - Sequential multi-step transformations

## Engine Architecture

### TransformationEngine Interface

All transformation engines implement the `TransformationEngine` interface:

```java
public interface TransformationEngine {
    /**
     * Get the name of the transformation engine
     */
    String getName();

    /**
     * Transform JSON data using the provided expression/configuration
     *
     * @param inputJson The input JSON data as a Map
     * @param expression The transformation expression/configuration
     * @return The transformed JSON data as a Map
     * @throws TransformationException if transformation fails
     */
    Map<String, Object> transform(Map<String, Object> inputJson, String expression)
        throws TransformationException;

    /**
     * Validate if the expression/configuration is syntactically correct
     *
     * @param expression The transformation expression/configuration to validate
     * @return true if valid, false otherwise
     */
    boolean validateExpression(String expression);
}
```

## Router Engine Implementation

### Core Components

#### RouterTransformationEngine

```java
@Component
public class RouterTransformationEngine implements TransformationEngine {

    private final ObjectMapper objectMapper;
    private final JsltTransformationEngine jsltEngine;
    private final ConfigurationValidator configValidator;

    @Override
    public String getName() {
        return "router";
    }

    @Override
    public Map<String, Object> transform(Map<String, Object> inputJson, String expression)
        throws TransformationException {

        // 1. Parse router configuration
        RouterConfiguration config = objectMapper.readValue(expression, RouterConfiguration.class);

        // 2. Convert input to JsonNode for condition evaluation
        JsonNode inputNode = objectMapper.valueToTree(inputJson);

        // 3. Find matching route
        String selectedTransformationId = findMatchingRoute(config, inputNode);

        // 4. Apply selected transformation
        String transformationExpression = getTransformationExpression(selectedTransformationId);
        return jsltEngine.transform(inputJson, transformationExpression);
    }

    @Override
    public boolean validateExpression(String expression) {
        try {
            configValidator.validateRouterConfig(expression);
            RouterConfiguration config = objectMapper.readValue(expression, RouterConfiguration.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### Route Selection Logic

```java
private String findMatchingRoute(RouterConfiguration config, JsonNode inputNode) {
    for (RouterConfiguration.Route route : config.getRoutes()) {
        if (evaluateCondition(route.getCondition(), inputNode)) {
            return route.getTransformationId();
        }
    }
    return config.getDefaultTransformationId();
}

private boolean evaluateCondition(String condition, JsonNode inputNode) {
    // Parse conditions like: $.type == 'user'
    String[] parts = condition.trim().split("\\s*==\\s*", 2);
    if (parts.length != 2) return false;

    String path = parts[0].trim();
    String expectedValue = parts[1].trim().replaceAll("^'|'$", "");

    JsonNode actualNode = navigatePath(path, inputNode);
    return actualNode != null && expectedValue.equals(actualNode.asText());
}

private JsonNode navigatePath(String path, JsonNode node) {
    if (!path.startsWith("$.")) return null;
    String fieldPath = path.substring(2);
    String[] fields = fieldPath.split("\\.");
    JsonNode current = node;
    for (String field : fields) {
        current = current.get(field);
        if (current == null) return null;
    }
    return current;
}
```

### Configuration Classes

#### RouterConfiguration

```java
public class RouterConfiguration {
    private String type = "router";
    private List<Route> routes;
    private String defaultTransformationId;
    private ValidationConfig validation;

    public static class Route {
        private String condition;
        private String transformationId;
        private String description;
        // getters and setters...
    }

    public static class ValidationConfig {
        private String inputSchema;
        private String outputSchema;
        // getters and setters...
    }
}
```

## Pipeline Engine Implementation

### Core Components

#### PipelineTransformationEngine

```java
@Component
public class PipelineTransformationEngine implements TransformationEngine {

    private final ObjectMapper objectMapper;
    private final JsltTransformationEngine jsltEngine;
    private final ConfigurationValidator configValidator;

    @Override
    public String getName() {
        return "pipeline";
    }

    @Override
    public Map<String, Object> transform(Map<String, Object> inputJson, String expression)
        throws TransformationException {

        PipelineConfiguration config = objectMapper.readValue(expression, PipelineConfiguration.class);

        Map<String, Object> currentData = inputJson;
        List<String> executedSteps = new ArrayList<>();

        for (PipelineConfiguration.PipelineStep step : config.getSteps()) {
            executedSteps.add(step.getName());

            try {
                String stepExpression = getTransformationExpression(step.getTransformationId());
                currentData = jsltEngine.transform(currentData, stepExpression);
            } catch (TransformationException e) {
                if (!step.isContinueOnError()) {
                    throw new TransformationException(
                        String.format("Pipeline failed at step '%s': %s", step.getName(), e.getMessage()), e);
                }
                // Continue to next step if continueOnError is true
            }
        }

        return currentData;
    }

    @Override
    public boolean validateExpression(String expression) {
        try {
            configValidator.validatePipelineConfig(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Configuration Classes

#### PipelineConfiguration

```java
public class PipelineConfiguration {
    private String type = "pipeline";
    private List<PipelineStep> steps;
    private ValidationConfig validation;

    public static class PipelineStep {
        private String name;
        private String transformationId;
        private boolean continueOnError = false;
        private String description;
        // getters and setters...
    }

    public static class ValidationConfig {
        private String finalSchema;
        private Map<String, String> intermediateSchemas;
        // getters and setters...
    }
}
```

## Configuration Validation

### JSON Schema Validation

The service uses JSON Schema validation to ensure configuration integrity:

```java
@Service
public class ConfigurationValidator {

    private final JsonSchema routerConfigSchema;
    private final JsonSchema pipelineConfigSchema;

    public ConfigurationValidator(ObjectMapper objectMapper) {
        this.routerConfigSchema = createRouterConfigSchema();
        this.pipelineConfigSchema = createPipelineConfigSchema();
    }

    public void validateRouterConfig(String configJson) throws ConfigurationValidationException {
        JsonNode configNode = objectMapper.readTree(configJson);
        Set<ValidationMessage> errors = routerConfigSchema.validate(configNode);
        if (!errors.isEmpty()) {
            throw new ConfigurationValidationException("Invalid router configuration: " + errors);
        }
    }

    private JsonSchema createRouterConfigSchema() {
        String schemaJson = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "type": "object",
                "properties": {
                    "type": {"type": "string", "enum": ["router"]},
                    "routes": {
                        "type": "array",
                        "minItems": 1,
                        "items": {
                            "type": "object",
                            "properties": {
                                "condition": {"type": "string", "minLength": 1},
                                "transformationId": {"type": "string", "minLength": 1},
                                "description": {"type": "string"}
                            },
                            "required": ["condition", "transformationId"]
                        }
                    },
                    "defaultTransformationId": {"type": "string"},
                    "validation": {
                        "type": "object",
                        "properties": {
                            "inputSchema": {"type": "string"},
                            "outputSchema": {"type": "string"}
                        }
                    }
                },
                "required": ["type", "routes"]
            }
            """;

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }
}
```

## Service Integration

### TransformationService Updates

```java
@Service
public class TransformationService {

    private final TransformationTemplateRepository templateRepository;
    private final JsltTransformationEngine jsltEngine;
    private final RouterTransformationEngine routerEngine;
    private final PipelineTransformationEngine pipelineEngine;
    private final ConsumerService consumerService;
    private final ObjectMapper objectMapper;

    public List<String> getAvailableEngines() {
        return List.of("jslt", "router", "pipeline");
    }

    private TransformationEngine getEngine(String engineName) {
        return switch (engineName.toLowerCase()) {
            case "jslt" -> jsltEngine;
            case "router" -> routerEngine;
            case "pipeline" -> pipelineEngine;
            default -> throw new IllegalArgumentException("Unsupported engine: " + engineName);
        };
    }

    public TransformationTemplateResponse createOrUpdateTemplate(String consumerId,
                                                              TransformationTemplateRequest request) {

        String expression;
        String configuration;

        try {
            if ("jslt".equalsIgnoreCase(request.getEngine())) {
                expression = request.getExpression();
                configuration = null;
            } else if ("router".equalsIgnoreCase(request.getEngine())) {
                expression = objectMapper.writeValueAsString(request.getRouterConfig());
                configuration = expression;
            } else if ("pipeline".equalsIgnoreCase(request.getEngine())) {
                expression = objectMapper.writeValueAsString(request.getPipelineConfig());
                configuration = expression;
            } else {
                throw new IllegalArgumentException("Unsupported engine: " + request.getEngine());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize configuration: " + e.getMessage(), e);
        }

        // Validate and save template...
    }
}
```

## Database Schema Updates

### Migration Script

```sql
-- V2__Add_transformation_configuration.sql
ALTER TABLE transformation_templates
ADD COLUMN configuration TEXT;

ALTER TABLE transformation_templates
ALTER COLUMN template_expression DROP NOT NULL;

COMMENT ON COLUMN transformation_templates.configuration IS 'JSON configuration for router and pipeline engines';
COMMENT ON COLUMN transformation_templates.template_expression IS 'JSLT expression for jslt engine (nullable for router/pipeline engines)';
```

### Entity Updates

```java
@Entity
public class TransformationTemplateEntity {

    // ... existing fields ...

    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;

    // For JSLT engine - simple expression (nullable for router/pipeline)
    @Column(name = "template_expression", columnDefinition = "TEXT")
    private String templateExpression;

    // ... getters and setters ...
}
```

## Testing Strategies

### Unit Testing Engines

#### Router Engine Tests

```java
@SpringBootTest
class RouterTransformationEngineTest {

    @Autowired
    private RouterTransformationEngine routerEngine;

    @Test
    void transform_withUserData_shouldRouteToUserTransformation() throws TransformationException {
        String config = """
            {
                "type": "router",
                "routes": [
                    {
                        "condition": "$.type == 'user'",
                        "transformationId": "user-normalization-v1"
                    }
                ],
                "defaultTransformationId": "generic-transformation-v1"
            }
            """;

        Map<String, Object> input = Map.of(
            "type", "user",
            "id", 123,
            "name", "John Doe"
        );

        Map<String, Object> result = routerEngine.transform(input, config);

        assertNotNull(result);
        assertEquals("user", result.get("normalized_type"));
    }

    @Test
    void validateExpression_withValidConfig_shouldReturnTrue() {
        String validConfig = """
            {
                "type": "router",
                "routes": [
                    {"condition": "$.type == 'user'", "transformationId": "user-v1"}
                ]
            }
            """;

        assertTrue(routerEngine.validateExpression(validConfig));
    }
}
```

#### Pipeline Engine Tests

```java
@SpringBootTest
class PipelineTransformationEngineTest {

    @Autowired
    private PipelineTransformationEngine pipelineEngine;

    @Test
    void transform_shouldExecutePipelineSteps() throws TransformationException {
        String config = """
            {
                "type": "pipeline",
                "steps": [
                    {"name": "validate", "transformationId": "input-validation-v1"},
                    {"name": "normalize", "transformationId": "data-normalization-v1"},
                    {"name": "enrich", "transformationId": "data-enrichment-v1"}
                ]
            }
            """;

        Map<String, Object> input = Map.of("type", "user", "id", 123);

        Map<String, Object> result = pipelineEngine.transform(input, config);

        assertNotNull(result);
        assertEquals(true, result.get("validated"));
        assertEquals(true, result.get("normalized"));
        assertEquals(true, result.get("enriched"));
    }

    @Test
    void transform_withContinueOnError_shouldContinueAfterFailure() throws TransformationException {
        String config = """
            {
                "type": "pipeline",
                "steps": [
                    {"name": "failing", "transformationId": "non-existent", "continueOnError": true},
                    {"name": "success", "transformationId": "input-validation-v1"}
                ]
            }
            """;

        Map<String, Object> input = Map.of("type", "test");

        Map<String, Object> result = pipelineEngine.transform(input, config);

        assertNotNull(result);
        assertEquals(true, result.get("validated"));
    }
}
```

### Integration Testing

#### End-to-End API Tests

```bash
# Register consumer
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -d '{"consumerId": "test-app", "name": "Test Application"}'

# Create router template
curl -X POST http://localhost:8080/api/transform/templates/test-app \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "router",
    "routerConfig": {
      "type": "router",
      "routes": [
        {"condition": "$.type == \"user\"", "transformationId": "user-v1"}
      ],
      "defaultTransformationId": "generic-v1"
    }
  }'

# Test transformation
curl -X POST http://localhost:8080/api/transform/test-app \
  -H "Content-Type: application/json" \
  -d '{
    "canonicalJson": {"type": "user", "id": 123, "name": "Test User"}
  }'
```

## Extension Patterns

### Creating Custom Engines

To create a custom transformation engine:

1. **Implement the Interface**:
```java
@Component
public class CustomTransformationEngine implements TransformationEngine {

    @Override
    public String getName() {
        return "custom";
    }

    @Override
    public Map<String, Object> transform(Map<String, Object> inputJson, String expression)
        throws TransformationException {
        // Custom transformation logic
    }

    @Override
    public boolean validateExpression(String expression) {
        // Custom validation logic
    }
}
```

2. **Add Configuration Validation** (if needed):
```java
public void validateCustomConfig(String configJson) throws ConfigurationValidationException {
    // JSON Schema validation for custom configuration
}
```

3. **Register in Service**:
```java
private TransformationEngine getEngine(String engineName) {
    return switch (engineName.toLowerCase()) {
        case "jslt" -> jsltEngine;
        case "router" -> routerEngine;
        case "pipeline" -> pipelineEngine;
        case "custom" -> customEngine;
        default -> throw new IllegalArgumentException("Unsupported engine: " + engineName);
    };
}
```

### Best Practices

#### Engine Design
- Keep engines stateless and thread-safe
- Use clear, descriptive error messages
- Implement comprehensive validation
- Document configuration schemas

#### Configuration Management
- Use JSON Schema for configuration validation
- Provide clear examples and documentation
- Support backward compatibility
- Include schema references for validation

#### Error Handling
- Use custom exceptions for transformation errors
- Provide detailed error context
- Support partial failure scenarios (pipeline continueOnError)
- Log errors with sufficient context for debugging

#### Performance Considerations
- Cache compiled transformations when possible
- Use efficient JSON processing libraries
- Implement timeouts for long-running transformations
- Monitor resource usage

## Troubleshooting

### Common Issues

#### Router Engine Issues
- **No route matches**: Check condition syntax and data structure
- **Invalid condition**: Ensure JSON path expressions are correct
- **Missing transformation**: Verify transformation IDs exist

#### Pipeline Engine Issues
- **Step failure**: Check individual step configurations
- **Data corruption**: Verify intermediate data formats
- **Performance**: Monitor step execution times

#### Configuration Issues
- **Invalid JSON**: Validate configuration syntax
- **Schema violations**: Check against JSON schemas
- **Missing fields**: Ensure required fields are present

### Debugging Tools

```java
// Enable debug logging
logging.level.ru.vegorov.schemaregistry.service=DEBUG

// Test configuration validation
ConfigurationValidator validator = new ConfigurationValidator(objectMapper);
try {
    validator.validateRouterConfig(configJson);
    System.out.println("Configuration is valid");
} catch (ConfigurationValidationException e) {
    System.err.println("Configuration errors: " + e.getMessage());
}
```

## Migration Guide

### From JSLT to Advanced Engines

#### When to Use Router Engine
- Multiple data types requiring different transformations
- Conditional logic based on input characteristics
- Multi-tenant applications

#### When to Use Pipeline Engine
- Complex multi-step processing workflows
- Data validation and enrichment chains
- Sequential transformation requirements

#### Migration Steps
1. Analyze existing JSLT transformations
2. Identify routing conditions or pipeline steps
3. Convert to appropriate engine configuration
4. Test thoroughly with existing data
5. Update consumer applications

## Conclusion

The advanced transformation engines provide powerful capabilities for complex data processing scenarios while maintaining the simplicity and reliability of the core system. The extensible architecture allows for future enhancements and custom engine implementations as business needs evolve.