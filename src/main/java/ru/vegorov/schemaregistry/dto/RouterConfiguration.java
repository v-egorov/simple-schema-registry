package ru.vegorov.schemaregistry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Configuration for Router Transformation Engine
 */
public class RouterConfiguration {

    @NotNull
    private String type = "router";

    @NotEmpty
    @Valid
    private List<Route> routes;

    private String defaultTransformationId;

    @Valid
    private ValidationConfig validation;

    // Constructors
    public RouterConfiguration() {}

    public RouterConfiguration(List<Route> routes, String defaultTransformationId, ValidationConfig validation) {
        this.routes = routes;
        this.defaultTransformationId = defaultTransformationId;
        this.validation = validation;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public String getDefaultTransformationId() {
        return defaultTransformationId;
    }

    public void setDefaultTransformationId(String defaultTransformationId) {
        this.defaultTransformationId = defaultTransformationId;
    }

    public ValidationConfig getValidation() {
        return validation;
    }

    public void setValidation(ValidationConfig validation) {
        this.validation = validation;
    }

    /**
     * Route configuration
     */
    public static class Route {
        @NotNull
        private String condition;

        @NotNull
        private String transformationId;

        private String description;

        // Constructors
        public Route() {}

        public Route(String condition, String transformationId, String description) {
            this.condition = condition;
            this.transformationId = transformationId;
            this.description = description;
        }

        // Getters and Setters
        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getTransformationId() {
            return transformationId;
        }

        public void setTransformationId(String transformationId) {
            this.transformationId = transformationId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Validation configuration
     */
    public static class ValidationConfig {
        private String inputSchema;
        private String outputSchema;

        // Constructors
        public ValidationConfig() {}

        public ValidationConfig(String inputSchema, String outputSchema) {
            this.inputSchema = inputSchema;
            this.outputSchema = outputSchema;
        }

        // Getters and Setters
        public String getInputSchema() {
            return inputSchema;
        }

        public void setInputSchema(String inputSchema) {
            this.inputSchema = inputSchema;
        }

        public String getOutputSchema() {
            return outputSchema;
        }

        public void setOutputSchema(String outputSchema) {
            this.outputSchema = outputSchema;
        }
    }
}