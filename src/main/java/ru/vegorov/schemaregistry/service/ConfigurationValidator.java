package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service for validating transformation engine configurations using JSON Schema
 */
@Service
public class ConfigurationValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchema routerConfigSchema;
    private final JsonSchema pipelineConfigSchema;

    public ConfigurationValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        // Initialize JSON schemas for configuration validation
        this.routerConfigSchema = createRouterConfigSchema();
        this.pipelineConfigSchema = createPipelineConfigSchema();
    }

    /**
     * Validate router configuration
     */
    public void validateRouterConfig(String configJson) throws ConfigurationValidationException {
        try {
            JsonNode configNode = objectMapper.readTree(configJson);
            Set<ValidationMessage> errors = routerConfigSchema.validate(configNode);

            if (!errors.isEmpty()) {
                throw new ConfigurationValidationException("Invalid router configuration: " + errors);
            }
        } catch (Exception e) {
            throw new ConfigurationValidationException("Failed to validate router configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Validate pipeline configuration
     */
    public void validatePipelineConfig(String configJson) throws ConfigurationValidationException {
        try {
            JsonNode configNode = objectMapper.readTree(configJson);
            Set<ValidationMessage> errors = pipelineConfigSchema.validate(configNode);

            if (!errors.isEmpty()) {
                throw new ConfigurationValidationException("Invalid pipeline configuration: " + errors);
            }
        } catch (Exception e) {
            throw new ConfigurationValidationException("Failed to validate pipeline configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Create JSON schema for router configuration
     */
    private JsonSchema createRouterConfigSchema() {
        String schemaJson = """
            {
                "type": "object",
                "properties": {
                    "type": {
                        "type": "string",
                        "enum": ["router"]
                    },
                    "routes": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "condition": {
                                    "type": "string",
                                    "minLength": 1
                                },
                                "transformationId": {
                                    "type": "string",
                                    "minLength": 1
                                },
                                "description": {
                                    "type": "string"
                                }
                            },
                            "required": ["condition", "transformationId"]
                        }
                    },
                    "defaultTransformationId": {
                        "type": "string"
                    },
                    "validation": {
                        "type": "object",
                        "properties": {
                            "inputSchema": {
                                "type": "string"
                            },
                            "outputSchema": {
                                "type": "string"
                            }
                        }
                    }
                },
                "required": ["type", "routes"]
            }
            """;

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        return factory.getSchema(schemaJson);
    }

    /**
     * Create JSON schema for pipeline configuration
     */
    private JsonSchema createPipelineConfigSchema() {
        String schemaJson = """
            {
                "type": "object",
                "properties": {
                    "type": {
                        "type": "string",
                        "enum": ["pipeline"]
                    },
                    "steps": {
                        "type": "array",
                        "minItems": 1,
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {
                                    "type": "string",
                                    "minLength": 1
                                },
                                "transformationId": {
                                    "type": "string",
                                    "minLength": 1
                                },
                                "continueOnError": {
                                    "type": "boolean",
                                    "default": false
                                },
                                "description": {
                                    "type": "string"
                                }
                            },
                            "required": ["name", "transformationId"]
                        }
                    },
                    "validation": {
                        "type": "object",
                        "properties": {
                            "finalSchema": {
                                "type": "string"
                            },
                            "intermediateSchemas": {
                                "type": "object",
                                "patternProperties": {
                                    ".*": {
                                        "type": "string"
                                    }
                                }
                            }
                        }
                    }
                },
                "required": ["type", "steps"]
            }
            """;

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        return factory.getSchema(schemaJson);
    }

    /**
     * Custom exception for configuration validation errors
     */
    public static class ConfigurationValidationException extends Exception {
        public ConfigurationValidationException(String message) {
            super(message);
        }

        public ConfigurationValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}