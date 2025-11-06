package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;
import ru.vegorov.schemaregistry.repository.TransformationTemplateRepository;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouterTransformationEngineTest {

    private RouterTransformationEngine routerEngine;
    private ObjectMapper objectMapper;
    private ConfigurationValidator configValidator;

    @Mock
    private TransformationTemplateRepository templateRepository;

    @Mock
    private JsltTransformationEngine jsltEngine;

    @Mock
    private JsltFunctionRegistry functionRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        configValidator = new ConfigurationValidator(objectMapper);
        routerEngine = new RouterTransformationEngine(objectMapper, configValidator,
            templateRepository, jsltEngine, functionRegistry);
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
        // Mock template entity
        TransformationTemplateEntity mockTemplate = new TransformationTemplateEntity();
        mockTemplate.setTemplateExpression("{\"processed\": true, \"route\": \"user\", \"type\": .type, \"id\": .id, \"name\": .name}");

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("test-consumer", "test-subject", "user-normalization-v1"))
            .thenReturn(java.util.Optional.of(mockTemplate));

        when(jsltEngine.transform(any(), anyString(), anyString(), anyString())).thenReturn(Map.of(
            "processed", true,
            "route", "user",
            "type", "user",
            "id", 123,
            "name", "John Doe"
        ));

        String config = """
            {
                "type": "router",
                "consumerId": "test-consumer",
                "subject": "test-subject",
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

        Map<String, Object> result = routerEngine.transform(input, config, "test-consumer", "test-subject");

        assertNotNull(result);
        assertEquals(true, result.get("processed"));
        assertEquals("user", result.get("route"));
        assertEquals("user", result.get("type"));
        assertEquals(123, result.get("id"));
        assertEquals("John Doe", result.get("name"));
    }

    @Test
    void transform_withProductData_shouldRouteToDefaultTransformation() throws TransformationException {
        // Mock default template entity
        TransformationTemplateEntity mockTemplate = new TransformationTemplateEntity();
        mockTemplate.setTemplateExpression("{\"processed\": true, \"route\": \"generic\", \"data\": .}");

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("test-consumer", "test-subject", "generic-transformation-v1"))
            .thenReturn(java.util.Optional.of(mockTemplate));

        when(jsltEngine.transform(any(), anyString(), anyString(), anyString())).thenReturn(Map.of(
            "processed", true,
            "route", "generic",
            "data", Map.of("type", "product", "id", 456, "name", "Widget")
        ));

        String config = """
            {
                "type": "router",
                "consumerId": "test-consumer",
                "subject": "test-subject",
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

        Map<String, Object> result = routerEngine.transform(input, config, "test-consumer", "test-subject");

        assertNotNull(result);
        assertEquals(true, result.get("processed"));
        assertEquals("generic", result.get("route"));
    }

    @Test
    void transform_withInvalidCondition_shouldUseDefaultRoute() throws TransformationException {
        // Mock default template entity
        TransformationTemplateEntity mockTemplate = new TransformationTemplateEntity();
        mockTemplate.setTemplateExpression("{\"processed\": true, \"route\": \"generic\", \"data\": .}");

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("test-consumer", "test-subject", "generic-transformation-v1"))
            .thenReturn(java.util.Optional.of(mockTemplate));

        when(jsltEngine.transform(any(), anyString(), anyString(), anyString())).thenReturn(Map.of(
            "processed", true,
            "route", "generic",
            "data", Map.of("type", "user")
        ));

        String config = """
            {
                "type": "router",
                "consumerId": "test-consumer",
                "subject": "test-subject",
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

        Map<String, Object> result = routerEngine.transform(input, config, "test-consumer", "test-subject");

        assertNotNull(result);
        assertEquals(true, result.get("processed"));
        assertEquals("generic", result.get("route"));
    }
}