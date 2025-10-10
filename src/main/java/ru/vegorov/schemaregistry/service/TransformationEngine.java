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
     * @return The transformed JSON data as a Map
     * @throws TransformationException if transformation fails
     */
    Map<String, Object> transform(Map<String, Object> inputJson, String expression)
        throws TransformationException;

    /**
     * Validate if the expression is syntactically correct
     *
     * @param expression The transformation expression to validate
     * @return true if valid, false otherwise
     */
    boolean validateExpression(String expression);
}