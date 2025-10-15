package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * Built-in JSLT functions for common transformation patterns.
 */
@Component
public class JsltBuiltInFunctions {

    private static final Logger logger = LoggerFactory.getLogger(JsltBuiltInFunctions.class);

    private final ObjectMapper objectMapper;

    public JsltBuiltInFunctions(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Create the extract_forecast_today function.
     * Extracts forecast data from additionalMetadataFields array.
     * Searches for forecastTodayValue and forecastTodayDirection fields and constructs
     * a forecastToday object with value and direction properties.
     *
     * Usage in JSLT: extract_forecast_today(.details.additionalMetadataFields)
     *
     * @return Function implementation
     */
    public Function createExtractForecastTodayFunction() {
        return new ExtractForecastTodayFunction();
    }

    /**
     * JSLT Function implementation for extracting forecast data.
     */
    private class ExtractForecastTodayFunction implements Function {

        @Override
        public String getName() {
            return "extract_forecast_today";
        }

        @Override
        public int getMinArguments() {
            return 1;
        }

        @Override
        public int getMaxArguments() {
            return 1;
        }

        @Override
        public JsonNode call(JsonNode input, JsonNode[] args) {
            if (args.length != 1) {
                throw new IllegalArgumentException("extract-forecast-today expects exactly 1 argument");
            }

            JsonNode metadataFields = args[0];
            if (metadataFields == null || !metadataFields.isArray()) {
                return null;
            }

            String value = null;
            String direction = null;

            // Search through metadata fields for forecast data
            Iterator<JsonNode> elements = metadataFields.elements();
            while (elements.hasNext()) {
                JsonNode field = elements.next();
                if (field.isObject() && field.has("id") && field.has("val")) {
                    String fieldId = field.get("id").asText();
                    String fieldValue = field.get("val").asText();

                    if ("forecastTodayValue".equals(fieldId)) {
                        value = fieldValue;
                    } else if ("forecastTodayDirection".equals(fieldId)) {
                        direction = fieldValue;
                    }
                }
            }

            // Only return object if we found both value and direction
            if (value != null && direction != null) {
                ObjectNode result = objectMapper.createObjectNode();
                try {
                    // Try to parse value as number
                    result.put("value", Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    // If not a number, keep as string
                    result.put("value", value);
                }
                result.put("direction", direction);
                return result;
            }

            return null;
        }
    }
}