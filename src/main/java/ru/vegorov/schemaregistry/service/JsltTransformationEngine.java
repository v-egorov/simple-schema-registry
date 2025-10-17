package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * JSLT-based transformation engine implementation
 */
@Component
public class JsltTransformationEngine implements TransformationEngine {

    private static final Logger logger = LoggerFactory.getLogger(JsltTransformationEngine.class);

    private final ObjectMapper objectMapper;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

    @Value("${app.logging.performance.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    public JsltTransformationEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "jslt";
    }

    @Override
    public Map<String, Object> transform(Map<String, Object> inputJson, String expression)
        throws TransformationException {
        return transform(inputJson, expression, null);
    }

    /**
     * Transform JSON data using JSLT expression with optional custom functions.
     *
     * @param inputJson Input JSON data as Map
     * @param expression JSLT expression string
     * @param functionRegistry Optional function registry for custom functions
     * @return Transformed JSON data as Map
     * @throws TransformationException if transformation fails
     */
    public Map<String, Object> transform(Map<String, Object> inputJson, String expression, JsltFunctionRegistry functionRegistry)
        throws TransformationException {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        try {
            // Parse the JSLT expression with custom functions if registry is provided
            Expression jsltExpression;
            if (functionRegistry != null && !functionRegistry.getFunctionNames().isEmpty()) {
                jsltExpression = Parser.compileString(expression, functionRegistry.getAllFunctions());
            } else {
                jsltExpression = Parser.compileString(expression);
            }

            // Convert input Map to JsonNode
            JsonNode inputNode = objectMapper.valueToTree(inputJson);

            // Apply transformation
            JsonNode resultNode = jsltExpression.apply(inputNode);

            // Convert result back to Map
            Map<String, Object> result = objectMapper.convertValue(resultNode, new TypeReference<Map<String, Object>>() {});

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow JSLT transformation detected: inputSize={}, hasFunctions={}, duration={}ms",
                        inputJson.size(), functionRegistry != null, duration);
                } else {
                    logger.debug("JSLT transformation performance: inputSize={}, hasFunctions={}, duration={}ms",
                        inputJson.size(), functionRegistry != null, duration);
                }
            }

            return result;

        } catch (JsltException e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("JSLT transformation failed: inputSize={}, hasFunctions={}, duration={}ms, error={}",
                    inputJson.size(), functionRegistry != null, duration, e.getMessage(), e);
            }
            throw new TransformationException("JSLT transformation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("JSLT transformation failed: inputSize={}, hasFunctions={}, duration={}ms, error={}",
                    inputJson.size(), functionRegistry != null, duration, e.getMessage(), e);
            }
            throw new TransformationException("Transformation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateExpression(String expression) {
        return validateExpression(expression, null);
    }

    /**
     * Validate JSLT expression with optional function registry.
     *
     * @param expression JSLT expression to validate
     * @param functionRegistry Optional function registry for validation
     * @return true if expression is valid
     */
    public boolean validateExpression(String expression, JsltFunctionRegistry functionRegistry) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        try {
            Expression jsltExpression;
            if (functionRegistry != null && !functionRegistry.getFunctionNames().isEmpty()) {
                jsltExpression = Parser.compileString(expression, functionRegistry.getAllFunctions());
            } else {
                jsltExpression = Parser.compileString(expression);
            }

            return true;
        } catch (JsltException e) {
            // Expected: Invalid JSLT syntax
            return false;
        } catch (RuntimeException e) {
            // Unexpected runtime issues (null pointers, etc.)
            // Log for debugging but treat as invalid expression
            logger.warn("Unexpected error validating JSLT expression", e);
            return false;
        }
        // Don't catch Exception - let programming errors surface
    }
}