package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.vegorov.schemaregistry.dto.RouterConfiguration;
import ru.vegorov.schemaregistry.service.ConfigurationValidator.ConfigurationValidationException;

import java.time.Duration;
import java.time.Instant;
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

    private static final Logger logger = LoggerFactory.getLogger(RouterTransformationEngine.class);

    private final ObjectMapper objectMapper;
    private final ConfigurationValidator configValidator;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

    @Value("${app.logging.performance.slow-threshold-ms:1000}")
    private long slowThresholdMs;

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
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

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
            Map<String, Object> result = applyTransformation(selectedTransformationId, inputJson);

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow router transformation detected: inputSize={}, routes={}, selectedRoute={}, duration={}ms",
                        inputJson.size(), config.getRoutes().size(), selectedTransformationId, duration);
                } else {
                    logger.debug("Router transformation performance: inputSize={}, routes={}, selectedRoute={}, duration={}ms",
                        inputJson.size(), config.getRoutes().size(), selectedTransformationId, duration);
                }
            }

            return result;

        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Router transformation failed: inputSize={}, duration={}ms, error={}",
                    inputJson.size(), duration, e.getMessage(), e);
            }
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

            // Additional validation: router must have at least one route or a default transformation
            if ((config.getRoutes() == null || config.getRoutes().isEmpty()) &&
                config.getDefaultTransformationId() == null) {
                return false;
            }

            return true;
        } catch (ConfigurationValidationException | JsonProcessingException e) {
            // Expected validation failures - invalid config or JSON
            return false;
        } catch (RuntimeException e) {
            // Unexpected runtime errors
            logger.error("Unexpected error during router expression validation", e);
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