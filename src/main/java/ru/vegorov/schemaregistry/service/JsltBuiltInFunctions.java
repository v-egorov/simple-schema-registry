package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;

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

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

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
      * Create the filter_additional_metadata_fields function.
      * Filters out specified IDs from additionalMetadataFields array.
      *
      * Usage in JSLT: filter_additional_metadata_fields(.details.additionalMetadataFields, ["id1", "id2"])
      *
      * @return Function implementation
      */
     public Function createFilterAdditionalMetadataFieldsFunction() {
         return new FilterAdditionalMetadataFieldsFunction();
     }

     /**
      * Create the uuid function.
      * Generates a random UUID (Universally Unique Identifier) string.
      *
      * Usage in JSLT: uuid()
      *
      * @return Function implementation
      */
     public Function createUuidFunction() {
         return new UuidFunction();
     }

     /**
      * Create the uuid-no-dashes function.
      * Generates a random UUID string with dashes removed.
      *
      * Usage in JSLT: uuid-no-dashes()
      *
      * @return Function implementation
      */
      public Function createUuidNoDashesFunction() {
          return new UuidNoDashesFunction();
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
            Instant start = performanceLoggingEnabled ? Instant.now() : null;

            if (args.length != 1) {
                if (businessLoggingEnabled) {
                    logger.error("extract-forecast-today called with invalid arguments: expected 1, got {}", args.length);
                }
                throw new IllegalArgumentException("extract-forecast-today expects exactly 1 argument");
            }

            JsonNode metadataFields = args[0];
            if (metadataFields == null || !metadataFields.isArray()) {
                if (businessLoggingEnabled) {
                    logger.debug("extract-forecast-today called with invalid metadata fields: null or not array");
                }
                return null;
            }

            if (businessLoggingEnabled) {
                logger.debug("Extracting forecast data from {} metadata fields", metadataFields.size());
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
                        if (businessLoggingEnabled) {
                            logger.debug("Found forecast value: {}", value);
                        }
                    } else if ("forecastTodayDirection".equals(fieldId)) {
                        direction = fieldValue;
                        if (businessLoggingEnabled) {
                            logger.debug("Found forecast direction: {}", direction);
                        }
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

                if (performanceLoggingEnabled) {
                    long duration = Duration.between(start, Instant.now()).toMillis();
                    logger.debug("extract-forecast-today completed successfully: duration={}ms", duration);
                }

                return result;
            }

            if (businessLoggingEnabled) {
                logger.debug("extract-forecast-today: missing value or direction, returning null");
            }

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.debug("extract-forecast-today completed with no result: duration={}ms", duration);
            }

            return null;
        }
    }

    /**
     * JSLT Function implementation for filtering additional metadata fields.
     */
    private class FilterAdditionalMetadataFieldsFunction implements Function {

        @Override
        public String getName() {
            return "filter_additional_metadata_fields";
        }

        @Override
        public int getMinArguments() {
            return 2;
        }

        @Override
        public int getMaxArguments() {
            return 2;
        }

        @Override
        public JsonNode call(JsonNode input, JsonNode[] args) {
            Instant start = performanceLoggingEnabled ? Instant.now() : null;

            if (args.length != 2) {
                if (businessLoggingEnabled) {
                    logger.error("filter_additional_metadata_fields called with invalid arguments: expected 2, got {}", args.length);
                }
                throw new IllegalArgumentException("filter_additional_metadata_fields expects exactly 2 arguments");
            }

            JsonNode metadataFields = args[0];
            if (metadataFields == null || metadataFields.isMissingNode() || !metadataFields.isArray()) {
                if (businessLoggingEnabled) {
                    logger.debug("filter_additional_metadata_fields: metadata fields is null, missing, or not an array (type: {}), returning empty array",
                        metadataFields != null ? metadataFields.getNodeType() : "null");
                }
                if (performanceLoggingEnabled) {
                    long duration = Duration.between(start, Instant.now()).toMillis();
                    logger.debug("filter_additional_metadata_fields completed: duration={}ms", duration);
                }
                return objectMapper.createArrayNode();
            }

            JsonNode excludeIds = args[1];
            if (excludeIds == null || !excludeIds.isArray()) {
                if (businessLoggingEnabled) {
                    logger.error("filter_additional_metadata_fields: second argument is not an array");
                }
                throw new IllegalArgumentException("Second argument must be an array of IDs to exclude");
            }

            // Collect exclude IDs
            java.util.Set<String> excludeSet = new java.util.HashSet<>();
            Iterator<JsonNode> excludeElements = excludeIds.elements();
            while (excludeElements.hasNext()) {
                JsonNode idNode = excludeElements.next();
                if (!idNode.isTextual()) {
                    if (businessLoggingEnabled) {
                        logger.error("filter_additional_metadata_fields: exclude ID is not a string");
                    }
                    throw new IllegalArgumentException("Exclude IDs must be strings");
                }
                excludeSet.add(idNode.asText());
            }

            if (businessLoggingEnabled) {
                logger.debug("Filtering metadata fields: total={}, exclude={}", metadataFields.size(), excludeSet);
            }

            ArrayNode result = objectMapper.createArrayNode();
            Iterator<JsonNode> elements = metadataFields.elements();
            while (elements.hasNext()) {
                JsonNode field = elements.next();
                if (!field.isObject()) {
                    if (businessLoggingEnabled) {
                        logger.error("filter_additional_metadata_fields: array element is not an object");
                    }
                    throw new IllegalArgumentException("Array elements must be objects");
                }
                if (!field.has("id")) {
                    if (businessLoggingEnabled) {
                        logger.error("filter_additional_metadata_fields: array element missing 'id' field");
                    }
                    throw new IllegalArgumentException("Array elements must have an 'id' field");
                }
                String fieldId = field.get("id").asText();
                if (!excludeSet.contains(fieldId)) {
                    result.add(field);
                }
            }

            if (businessLoggingEnabled) {
                logger.debug("filter_additional_metadata_fields: filtered to {} fields", result.size());
            }

             if (performanceLoggingEnabled) {
                 long duration = Duration.between(start, Instant.now()).toMillis();
                 logger.debug("filter_additional_metadata_fields completed: duration={}ms", duration);
             }

             return result;
         }
     }

     /**
      * JSLT Function implementation for generating UUIDs.
      */
     private class UuidFunction implements Function {

         @Override
         public String getName() {
             return "uuid";
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
             Instant start = performanceLoggingEnabled ? Instant.now() : null;

             if (args.length != 0) {
                 if (businessLoggingEnabled) {
                     logger.error("uuid called with invalid arguments: expected 0, got {}", args.length);
                 }
                 throw new IllegalArgumentException("uuid expects exactly 0 arguments");
             }

             if (businessLoggingEnabled) {
                 logger.debug("Generating UUID");
             }

             String uuid = UUID.randomUUID().toString();

             if (performanceLoggingEnabled) {
                 long duration = Duration.between(start, Instant.now()).toMillis();
                 logger.debug("uuid completed successfully: duration={}ms", duration);
             }

             return objectMapper.createObjectNode().textNode(uuid);
         }
     }

      /**
       * JSLT Function implementation for generating UUIDs without dashes.
       */
      private class UuidNoDashesFunction implements Function {

          @Override
          public String getName() {
              return "uuid-no-dashes";
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
              Instant start = performanceLoggingEnabled ? Instant.now() : null;

              if (args.length != 0) {
                  if (businessLoggingEnabled) {
                      logger.error("uuid-no-dashes called with invalid arguments: expected 0, got {}", args.length);
                  }
                  throw new IllegalArgumentException("uuid-no-dashes expects exactly 0 arguments");
              }

              if (businessLoggingEnabled) {
                  logger.debug("Generating UUID without dashes");
              }

              String uuid = UUID.randomUUID().toString().replace("-", "");

              if (performanceLoggingEnabled) {
                  long duration = Duration.between(start, Instant.now()).toMillis();
                  logger.debug("uuid-no-dashes completed successfully: duration={}ms", duration);
              }

              return objectMapper.createObjectNode().textNode(uuid);
          }
      }
  }
