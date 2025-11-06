package ru.vegorov.schemaregistry.service;

import java.util.Map;

/**
 * Interface for JSON transformation engines
 */
public interface TransformationEngine {

    /**
     * Get the name of the transformation engine
     */
    String getName();

    /**
     * Transform JSON data using the provided expression
     *
     * @param inputJson The input JSON data as a Map
     * @param expression The transformation expression (e.g., JSLT expression)
     * @param consumerId The consumer ID context for template lookups
     * @param subject The subject context for template lookups
     * @return The transformed JSON data as a Map
     * @throws TransformationException if transformation fails
     */
    Map<String, Object> transform(Map<String, Object> inputJson, String expression, String consumerId, String subject)
        throws TransformationException;

    /**
     * Validate if the expression is syntactically correct
     *
     * @param expression The transformation expression to validate
     * @return true if valid, false otherwise
     */
    boolean validateExpression(String expression);
}