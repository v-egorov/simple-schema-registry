package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JSLT-based transformation engine implementation
 */
@Component
public class JsltTransformationEngine implements TransformationEngine {

    private static final Logger logger = LoggerFactory.getLogger(JsltTransformationEngine.class);

    private final ObjectMapper objectMapper;

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
        try {
            // Parse the JSLT expression
            Expression jsltExpression = Parser.compileString(expression);

            // Convert input Map to JsonNode
            JsonNode inputNode = objectMapper.valueToTree(inputJson);

            // Apply transformation
            JsonNode resultNode = jsltExpression.apply(inputNode);

            // Convert result back to Map
            return objectMapper.convertValue(resultNode, new TypeReference<Map<String, Object>>() {});

        } catch (JsltException e) {
            throw new TransformationException("JSLT transformation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TransformationException("Transformation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        try {
            Parser.compileString(expression);
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