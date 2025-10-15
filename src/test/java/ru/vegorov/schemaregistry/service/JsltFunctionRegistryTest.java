package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsltFunctionRegistryTest {

    private JsltFunctionRegistry registry;
    private ObjectMapper objectMapper;

    // Simple test function implementation
    private static class TestFunction implements Function {
        @Override
        public String getName() {
            return "test-function";
        }

        @Override
        public int getMinArguments() {
            return 0;
        }

        @Override
        public int getMaxArguments() {
            return 0;
        }

        @Override
        public JsonNode call(JsonNode input, JsonNode[] args) {
            return input;
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        registry = new JsltFunctionRegistry();
    }

    @Test
    void testRegisterAndCheckRegistration() {
        Function testFunction = new TestFunction();

        // Register a function
        registry.register("test-function", testFunction);

        // Verify it's registered
        assertTrue(registry.isRegistered("test-function"));
        assertFalse(registry.isRegistered("non-existent"));

        // Verify function names
        assertTrue(registry.getFunctionNames().contains("test-function"));
        assertEquals(1, registry.size());
    }

    @Test
    void testUnregister() {
        Function testFunction = new TestFunction();
        registry.register("test-function", testFunction);
        assertTrue(registry.isRegistered("test-function"));

        registry.unregister("test-function");
        assertFalse(registry.isRegistered("test-function"));
        assertEquals(0, registry.size());
    }

    @Test
    void testClear() {
        Function func1 = new TestFunction();
        Function func2 = new TestFunction();
        registry.register("func1", func1);
        registry.register("func2", func2);
        assertEquals(2, registry.size());

        registry.clear();
        assertEquals(0, registry.size());
        assertFalse(registry.isRegistered("func1"));
        assertFalse(registry.isRegistered("func2"));
    }
}