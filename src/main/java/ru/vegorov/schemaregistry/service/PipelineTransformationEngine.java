package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.vegorov.schemaregistry.dto.PipelineConfiguration;
import ru.vegorov.schemaregistry.service.ConfigurationValidator.ConfigurationValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pipeline Transformation Engine
 *
 * Executes a sequence of transformations in order, with support for
 * error handling and intermediate result validation.
 */
@Component
public class PipelineTransformationEngine implements TransformationEngine {

    private static final Logger logger = LoggerFactory.getLogger(PipelineTransformationEngine.class);

    private final ObjectMapper objectMapper;
    private final JsltTransformationEngine jsltEngine;
    private final ConfigurationValidator configValidator;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

    @Value("${app.logging.performance.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    public PipelineTransformationEngine(ObjectMapper objectMapper, JsltTransformationEngine jsltEngine,
                                      ConfigurationValidator configValidator) {
        this.objectMapper = objectMapper;
        this.jsltEngine = jsltEngine;
        this.configValidator = configValidator;
    }

    @Override
    public String getName() {
        return "pipeline";
    }

    @Override
    public Map<String, Object> transform(Map<String, Object> inputJson, String expression)
        throws TransformationException {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        try {
            // Parse pipeline configuration from expression (which contains JSON config)
            PipelineConfiguration config = objectMapper.readValue(expression, PipelineConfiguration.class);

            // Execute pipeline steps
            Map<String, Object> currentData = inputJson;
            List<String> executedSteps = new ArrayList<>();
            List<TransformationException> errors = new ArrayList<>();

            for (PipelineConfiguration.PipelineStep step : config.getSteps()) {
                try {
                    executedSteps.add(step.getName());

                    // Get transformation expression for this step
                    String stepExpression = getTransformationExpression(step.getTransformationId());

                    // Apply transformation
                    currentData = jsltEngine.transform(currentData, stepExpression);

                } catch (TransformationException e) {
                    errors.add(new TransformationException(
                        String.format("Pipeline step '%s' failed: %s", step.getName(), e.getMessage()), e));

                    if (!step.isContinueOnError()) {
                        // Stop pipeline execution on error
                        if (performanceLoggingEnabled) {
                            long duration = Duration.between(start, Instant.now()).toMillis();
                            logger.error("Pipeline transformation failed: stepsExecuted={}, totalSteps={}, duration={}ms, error={}",
                                executedSteps.size(), config.getSteps().size(), duration, e.getMessage(), e);
                        }
                        throw new TransformationException(
                            String.format("Pipeline failed at step '%s': %s", step.getName(), e.getMessage()), e);
                    }
                    // Continue to next step if continueOnError is true
                }
            }

            // If we have errors but continued, we could log them or include in response
            // For now, just return the final result

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow pipeline transformation detected: steps={}, inputSize={}, errors={}, duration={}ms",
                        config.getSteps().size(), inputJson.size(), errors.size(), duration);
                } else {
                    logger.debug("Pipeline transformation performance: steps={}, inputSize={}, errors={}, duration={}ms",
                        config.getSteps().size(), inputJson.size(), errors.size(), duration);
                }
            }

            return currentData;

        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Pipeline transformation failed: inputSize={}, duration={}ms, error={}",
                    inputJson.size(), duration, e.getMessage(), e);
            }
            throw new TransformationException("Pipeline transformation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateExpression(String expression) {
        try {
            // First validate against JSON schema
            configValidator.validatePipelineConfig(expression);

            // Then validate the parsed configuration structure
            PipelineConfiguration config = objectMapper.readValue(expression, PipelineConfiguration.class);

            // Additional validation logic can be added here if needed
            return true;
        } catch (ConfigurationValidationException | JsonProcessingException e) {
            // Expected validation failures - invalid config or JSON
            return false;
        } catch (RuntimeException e) {
            // Unexpected runtime errors
            logger.error("Unexpected error during pipeline expression validation", e);
            return false;
        }
    }

    /**
     * Get transformation expression by ID
     * This is a placeholder - in a real implementation, this would look up
     * transformations from a registry or database
     */
    private String getTransformationExpression(String transformationId) throws TransformationException {
        // Placeholder implementation - map known transformation IDs to expressions
        switch (transformationId) {
            case "input-validation-v1":
                return ". + { \"validated\": true }"; // Merge validation flag
            case "data-normalization-v1":
                return ". + { \"normalized\": true }"; // Merge normalization flag
            case "data-enrichment-v1":
                return ". + { \"enriched\": true, \"timestamp\": now() }"; // Merge enrichment data
            default:
                throw new TransformationException("Unknown transformation ID: " + transformationId);
        }
    }
}