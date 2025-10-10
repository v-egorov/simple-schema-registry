package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RouterTransformationEngineTest {

    private RouterTransformationEngine routerEngine;
    private ObjectMapper objectMapper;
    private ConfigurationValidator configValidator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        JsltTransformationEngine jsltEngine = new JsltTransformationEngine(objectMapper);
        configValidator = new ConfigurationValidator(objectMapper);
        routerEngine = new RouterTransformationEngine(objectMapper, jsltEngine, configValidator);
    }

    @Test
    void getName_shouldReturnRouter() {
        assertEquals("router", routerEngine.getName());
    }

    @Test
    void validateExpression_withValidConfig_shouldReturnTrue() {
        String validConfig = """
            {
                "type": "router",
                "routes": [
                    {
                        "condition": "$.type == 'user'",
                        "transformationId": "user-normalization-v1",
                        "description": "Normalize user data"
                    }
                ],
                "defaultTransformationId": "generic-transformation-v1"
            }
            """;

        assertTrue(routerEngine.validateExpression(validConfig));
    }

    @Test
    void validateExpression_withInvalidConfig_shouldReturnFalse() {
        String invalidConfig = """
            {
                "type": "router",
                "routes": []
            }
            """;

        assertFalse(routerEngine.validateExpression(invalidConfig));
    }

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
        assertEquals(123, result.get("user_id"));
        assertEquals("John Doe", result.get("name"));
    }

    @Test
    void transform_withProductData_shouldRouteToDefaultTransformation() throws TransformationException {
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
            "type", "product",
            "id", 456,
            "name", "Widget"
        );

        Map<String, Object> result = routerEngine.transform(input, config);

        assertNotNull(result);
        assertEquals(input, result.get("data"));
    }

    @Test
    void transform_withInvalidCondition_shouldUseDefaultRoute() throws TransformationException {
        String config = """
            {
                "type": "router",
                "routes": [
                    {
                        "condition": "$.invalidField == 'value'",
                        "transformationId": "user-normalization-v1"
                    }
                ],
                "defaultTransformationId": "generic-transformation-v1"
            }
            """;

        Map<String, Object> input = Map.of("type", "user");

        Map<String, Object> result = routerEngine.transform(input, config);

        assertNotNull(result);
        assertEquals(input, result.get("data"));
    }
}