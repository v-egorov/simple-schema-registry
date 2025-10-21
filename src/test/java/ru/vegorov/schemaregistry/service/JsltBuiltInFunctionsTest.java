package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsltBuiltInFunctionsTest {

    private JsltBuiltInFunctions functions;
    private ObjectMapper objectMapper;
    private Function extractForecastTodayFunction;
    private Function filterAdditionalMetadataFieldsFunction;
    private Function uuidFunction;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        functions = new JsltBuiltInFunctions(objectMapper);
        extractForecastTodayFunction = functions.createExtractForecastTodayFunction();
        filterAdditionalMetadataFieldsFunction = functions.createFilterAdditionalMetadataFieldsFunction();
        uuidFunction = functions.createUuidFunction();
    }

    @Test
    void testExtractForecastToday_Success() throws Exception {
        // Create test input similar to the example
        String jsonInput = """
            [
                {"id": "forecastTodayValue", "val": "77.85"},
                {"id": "forecastTodayDirection", "val": "up"},
                {"id": "otherField", "val": "ignored"}
            ]
            """;

        JsonNode input = objectMapper.createObjectNode(); // Not used in JSLT call
        JsonNode[] args = new JsonNode[]{objectMapper.readTree(jsonInput)};

        // Call the function
        JsonNode result = extractForecastTodayFunction.call(input, args);

        // Verify result
        assertNotNull(result);
        assertTrue(result.isObject());
        assertTrue(result.has("value"));
        assertTrue(result.has("direction"));
        assertEquals(77.85, result.get("value").asDouble());
        assertEquals("up", result.get("direction").asText());
    }

    @Test
    void testExtractForecastToday_MissingDirection() throws Exception {
        String jsonInput = """
            [
                {"id": "forecastTodayValue", "val": "77.85"}
            ]
            """;

        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{objectMapper.readTree(jsonInput)};
        JsonNode result = extractForecastTodayFunction.call(input, args);

        // Should return null when direction is missing
        assertNull(result);
    }

    @Test
    void testExtractForecastToday_MissingValue() throws Exception {
        String jsonInput = """
            [
                {"id": "forecastTodayDirection", "val": "up"}
            ]
            """;

        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{objectMapper.readTree(jsonInput)};
        JsonNode result = extractForecastTodayFunction.call(input, args);

        // Should return null when value is missing
        assertNull(result);
    }

    @Test
    void testExtractForecastToday_InvalidValue() throws Exception {
        String jsonInput = """
            [
                {"id": "forecastTodayValue", "val": "not-a-number"},
                {"id": "forecastTodayDirection", "val": "up"}
            ]
            """;

        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{objectMapper.readTree(jsonInput)};
        JsonNode result = extractForecastTodayFunction.call(input, args);

        // Should handle non-numeric values as strings
        assertNotNull(result);
        assertEquals("not-a-number", result.get("value").asText());
        assertEquals("up", result.get("direction").asText());
    }

    @Test
    void testExtractForecastToday_NullInput() {
        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{null};
        JsonNode result = extractForecastTodayFunction.call(input, args);
        assertNull(result);
    }

    @Test
    void testExtractForecastToday_NonArrayInput() {
        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{objectMapper.createObjectNode().put("test", "value")};
        JsonNode result = extractForecastTodayFunction.call(input, args);
        assertNull(result);
    }

    @Test
    void testExtractForecastToday_WrongArgumentCount() {
        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{}; // No arguments

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            extractForecastTodayFunction.call(input, args);
        });
        assertTrue(exception.getMessage().contains("expects exactly 1 argument"));
    }

    @Test
    void testFilterAdditionalMetadataFields_Success() throws Exception {
        // Create test input
        String jsonInput = """
            [
                {"id": "forecastTodayValue", "val": "77.85"},
                {"id": "forecastTodayDirection", "val": "up"},
                {"id": "otherField", "val": "keep"}
            ]
            """;

        String excludeInput = """
            ["forecastTodayValue", "forecastTodayDirection"]
            """;

        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{
            objectMapper.readTree(jsonInput),
            objectMapper.readTree(excludeInput)
        };

        // Call the function
        JsonNode result = filterAdditionalMetadataFieldsFunction.call(input, args);

        // Verify result
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
        assertEquals("otherField", result.get(0).get("id").asText());
        assertEquals("keep", result.get(0).get("val").asText());
    }

    @Test
    void testFilterAdditionalMetadataFields_NullInput() throws Exception {
        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{
            null,
            objectMapper.readTree("[]")
        };

        JsonNode result = filterAdditionalMetadataFieldsFunction.call(input, args);

        // Should return empty array for null input
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(0, result.size());
    }

    @Test
    void testFilterAdditionalMetadataFields_ObjectInput() throws Exception {
        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{
            objectMapper.createObjectNode().put("test", "value"), // Object instead of array
            objectMapper.readTree("[]")
        };

        JsonNode result = filterAdditionalMetadataFieldsFunction.call(input, args);

        // Should return empty array for non-array input
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(0, result.size());
    }

    @Test
    void testUuidFunction_Success() {
        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{};

        // Call the function
        JsonNode result = uuidFunction.call(input, args);

        // Verify result
        assertNotNull(result);
        assertTrue(result.isTextual());
        String uuidString = result.asText();

        // Verify UUID format (should be 36 characters with dashes)
        assertEquals(36, uuidString.length());
        assertTrue(uuidString.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testUuidFunction_GeneratesUniqueValues() {
        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{};

        // Call the function multiple times
        JsonNode result1 = uuidFunction.call(input, args);
        JsonNode result2 = uuidFunction.call(input, args);
        JsonNode result3 = uuidFunction.call(input, args);

        // Verify all results are different
        String uuid1 = result1.asText();
        String uuid2 = result2.asText();
        String uuid3 = result3.asText();

        assertNotEquals(uuid1, uuid2);
        assertNotEquals(uuid1, uuid3);
        assertNotEquals(uuid2, uuid3);
    }

    @Test
    void testUuidFunction_WrongArgumentCount() {
        JsonNode input = objectMapper.createObjectNode();
        JsonNode[] args = new JsonNode[]{objectMapper.createObjectNode().put("test", "value")}; // 1 argument

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            uuidFunction.call(input, args);
        });
        assertTrue(exception.getMessage().contains("uuid expects exactly 0 arguments"));
    }
}