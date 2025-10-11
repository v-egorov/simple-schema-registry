package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.vegorov.schemaregistry.dto.RouterConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Router Transformation Engine
 *
 * Routes transformations based on input data conditions using JSON path expressions.
 * Supports conditional logic to select appropriate transformations dynamically.
 */
@Component
public class RouterTransformationEngine implements TransformationEngine {

    private final ObjectMapper objectMapper;
    private final ConfigurationValidator configValidator;

    public RouterTransformationEngine(ObjectMapper objectMapper,
                                     ConfigurationValidator configValidator) {
        this.objectMapper = objectMapper;
        this.configValidator = configValidator;
    }

    @Override
    public String getName() {
        return "router";
    }

    @Override
    public Map<String, Object> transform(Map<String, Object> inputJson, String expression)
        throws TransformationException {

        try {
            // Parse router configuration from expression (which contains JSON config)
            RouterConfiguration config = objectMapper.readValue(expression, RouterConfiguration.class);

            // Convert input to JsonNode for condition evaluation
            JsonNode inputNode = objectMapper.valueToTree(inputJson);

            // Find matching route
            String selectedTransformationId = findMatchingRoute(config, inputNode);

            if (selectedTransformationId == null) {
                throw new TransformationException("No matching route found for input data");
            }

            // Apply the selected transformation using hardcoded Java logic
            return applyTransformation(selectedTransformationId, inputJson);

        } catch (Exception e) {
            throw new TransformationException("Router transformation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateExpression(String expression) {
        try {
            // First validate against JSON schema
            configValidator.validateRouterConfig(expression);

            // Then validate the parsed configuration structure
            RouterConfiguration config = objectMapper.readValue(expression, RouterConfiguration.class);

            // Additional validation logic can be added here if needed
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find the first matching route based on conditions
     */
    private String findMatchingRoute(RouterConfiguration config, JsonNode inputNode) {
        for (RouterConfiguration.Route route : config.getRoutes()) {
            if (evaluateCondition(route.getCondition(), inputNode)) {
                return route.getTransformationId();
            }
        }

        // Return default if no route matches
        return config.getDefaultTransformationId();
    }

    /**
     * Evaluate a condition expression against the input data
     * Supports simple JSON path expressions like $.type == 'user'
     */
    private boolean evaluateCondition(String condition, JsonNode inputNode) {
        try {
            // Parse simple conditions like: $.type == 'user'
            // This is a basic implementation - a full version would use a proper expression evaluator
            String[] parts = condition.trim().split("\\s*==\\s*", 2);
            if (parts.length != 2) {
                return false;
            }

            String path = parts[0].trim();
            String expectedValue = parts[1].trim();

            // Remove quotes from expected value if present
            if ((expectedValue.startsWith("'") && expectedValue.endsWith("'")) ||
                (expectedValue.startsWith("\"") && expectedValue.endsWith("\""))) {
                expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
            }

            // Navigate to the path
            JsonNode actualNode = navigatePath(path, inputNode);

            if (actualNode == null || actualNode.isNull()) {
                return false;
            }

            // Compare values
            String actualValue = actualNode.asText();
            return expectedValue.equals(actualValue);

        } catch (Exception e) {
            // If condition evaluation fails, consider it not matching
            return false;
        }
    }

    /**
     * Navigate JSON path (simple implementation for $.field syntax)
     */
    private JsonNode navigatePath(String path, JsonNode node) {
        if (!path.startsWith("$.")) {
            return null;
        }

        String fieldPath = path.substring(2); // Remove "$."
        String[] fields = fieldPath.split("\\.");

        JsonNode current = node;
        for (String field : fields) {
            if (current == null || !current.isObject()) {
                return null;
            }
            current = current.get(field);
        }

        return current;
    }

    /**
     * Apply transformation by ID using hardcoded Java logic
     * This implements edge case transformations that require custom logic
     */
    private Map<String, Object> applyTransformation(String transformationId, Map<String, Object> input) throws TransformationException {
        switch (transformationId) {
            case "user-normalization-v1":
                return applyUserNormalization(input);
            case "electronics-enrichment-v1":
                return applyElectronicsEnrichment(input);
            case "generic-transformation-v1":
                return applyGenericTransformation(input);
            default:
                throw new TransformationException("Unknown transformation ID: " + transformationId);
        }
    }

    private Map<String, Object> applyUserNormalization(Map<String, Object> input) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("normalized_type", input.get("type"));
        result.put("user_id", input.get("id"));
        result.put("name", input.get("name"));
        return result;
    }

    private Map<String, Object> applyElectronicsEnrichment(Map<String, Object> input) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("product_type", input.get("type"));
        result.put("electronics_category", input.get("category"));
        result.put("price", input.get("price"));
        result.put("specs", input.get("specifications"));
        return result;
    }

    private Map<String, Object> applyGenericTransformation(Map<String, Object> input) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("data", input);
        return result;
    }
}