package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PipelineTransformationEngineTest {

    private PipelineTransformationEngine pipelineEngine;
    private ObjectMapper objectMapper;
    private ConfigurationValidator configValidator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        JsltTransformationEngine jsltEngine = new JsltTransformationEngine(objectMapper);
        configValidator = new ConfigurationValidator(objectMapper);
        pipelineEngine = new PipelineTransformationEngine(objectMapper, jsltEngine, configValidator);
    }

    @Test
    void getName_shouldReturnPipeline() {
        assertEquals("pipeline", pipelineEngine.getName());
    }

    @Test
    void validateExpression_withValidConfig_shouldReturnTrue() {
        String validConfig = """
            {
                "type": "pipeline",
                "steps": [
                    {
                        "name": "validate-input",
                        "transformationId": "input-validation-v1"
                    },
                    {
                        "name": "normalize-data",
                        "transformationId": "data-normalization-v1"
                    }
                ]
            }
            """;

        assertTrue(pipelineEngine.validateExpression(validConfig));
    }

    @Test
    void validateExpression_withInvalidConfig_shouldReturnFalse() {
        String invalidConfig = """
            {
                "type": "pipeline",
                "steps": []
            }
            """;

        assertFalse(pipelineEngine.validateExpression(invalidConfig));
    }

    @Test
    void transform_shouldExecutePipelineSteps() throws TransformationException {
        String config = """
            {
                "type": "pipeline",
                "steps": [
                    {
                        "name": "validate-input",
                        "transformationId": "input-validation-v1"
                    },
                    {
                        "name": "normalize-data",
                        "transformationId": "data-normalization-v1"
                    },
                    {
                        "name": "enrich-data",
                        "transformationId": "data-enrichment-v1"
                    }
                ]
            }
            """;

        Map<String, Object> input = Map.of(
            "type", "user",
            "id", 123,
            "name", "John Doe"
        );

        Map<String, Object> result = pipelineEngine.transform(input, config);

        assertNotNull(result);
        assertEquals(true, result.get("validated"));
        assertEquals(true, result.get("normalized"));
        assertEquals(true, result.get("enriched"));
        assertNotNull(result.get("timestamp"));
        // Original input fields should still be present
        assertEquals("user", result.get("type"));
        assertEquals(123, result.get("id"));
        assertEquals("John Doe", result.get("name"));
    }

    @Test
    void transform_withContinueOnError_shouldContinueAfterFailure() throws TransformationException {
        String config = """
            {
                "type": "pipeline",
                "steps": [
                    {
                        "name": "failing-step",
                        "transformationId": "non-existent-id",
                        "continueOnError": true
                    },
                    {
                        "name": "success-step",
                        "transformationId": "input-validation-v1"
                    }
                ]
            }
            """;

        Map<String, Object> input = Map.of("type", "test");

        // This should succeed because continueOnError is true
        Map<String, Object> result = pipelineEngine.transform(input, config);

        assertNotNull(result);
        assertEquals(true, result.get("validated"));
        assertEquals("test", result.get("type"));
    }

    @Test
    void transform_withoutContinueOnError_shouldFailOnError() {
        String config = """
            {
                "type": "pipeline",
                "steps": [
                    {
                        "name": "failing-step",
                        "transformationId": "non-existent-id",
                        "continueOnError": false
                    },
                    {
                        "name": "success-step",
                        "transformationId": "input-validation-v1"
                    }
                ]
            }
            """;

        Map<String, Object> input = Map.of("type", "test");

        // This should fail because continueOnError is false
        assertThrows(TransformationException.class, () ->
            pipelineEngine.transform(input, config));
    }

    @Test
    void transform_withEmptyPipeline_shouldReturnInputUnchanged() throws TransformationException {
        String config = """
            {
                "type": "pipeline",
                "steps": []
            }
            """;

        Map<String, Object> input = Map.of("test", "data");

        // Empty pipeline should fail validation
        assertFalse(pipelineEngine.validateExpression(config));
    }
}