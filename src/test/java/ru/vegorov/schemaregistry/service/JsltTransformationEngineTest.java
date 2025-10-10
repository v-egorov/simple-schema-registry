package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JsltTransformationEngineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsltTransformationEngine engine = new JsltTransformationEngine(objectMapper);

    @Test
    void getName_shouldReturnJslt() {
        assertThat(engine.getName()).isEqualTo("jslt");
    }

    @Test
    void transform_simpleFieldMapping_shouldWork() throws TransformationException {
        // Given
        Map<String, Object> input = new HashMap<>();
        input.put("user_id", 123);
        input.put("full_name", "John Doe");

        String expression = "{ \"id\": .user_id, \"name\": .full_name }";

        // When
        Map<String, Object> result = engine.transform(input, expression);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("id")).isEqualTo(123);
        assertThat(result.get("name")).isEqualTo("John Doe");
    }

    @Test
    void validateExpression_validExpression_shouldReturnTrue() {
        String validExpression = "{ \"result\": .input }";
        assertThat(engine.validateExpression(validExpression)).isTrue();
    }

    @Test
    void validateExpression_invalidExpression_shouldReturnFalse() {
        String invalidExpression = "{ invalid json";
        assertThat(engine.validateExpression(invalidExpression)).isFalse();
    }
}