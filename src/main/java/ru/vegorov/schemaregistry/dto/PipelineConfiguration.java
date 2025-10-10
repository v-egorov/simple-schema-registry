package ru.vegorov.schemaregistry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Pipeline Transformation Engine
 */
public class PipelineConfiguration {

    @NotNull
    private String type = "pipeline";

    @NotEmpty
    @Valid
    private List<PipelineStep> steps;

    @Valid
    private ValidationConfig validation;

    // Constructors
    public PipelineConfiguration() {}

    public PipelineConfiguration(List<PipelineStep> steps, ValidationConfig validation) {
        this.steps = steps;
        this.validation = validation;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<PipelineStep> getSteps() {
        return steps;
    }

    public void setSteps(List<PipelineStep> steps) {
        this.steps = steps;
    }

    public ValidationConfig getValidation() {
        return validation;
    }

    public void setValidation(ValidationConfig validation) {
        this.validation = validation;
    }

    /**
     * Pipeline step configuration
     */
    public static class PipelineStep {
        @NotNull
        private String name;

        @NotNull
        private String transformationId;

        private boolean continueOnError = false;

        private String description;

        // Constructors
        public PipelineStep() {}

        public PipelineStep(String name, String transformationId, boolean continueOnError, String description) {
            this.name = name;
            this.transformationId = transformationId;
            this.continueOnError = continueOnError;
            this.description = description;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTransformationId() {
            return transformationId;
        }

        public void setTransformationId(String transformationId) {
            this.transformationId = transformationId;
        }

        public boolean isContinueOnError() {
            return continueOnError;
        }

        public void setContinueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Validation configuration for pipeline
     */
    public static class ValidationConfig {
        private String finalSchema;

        @Valid
        private Map<String, String> intermediateSchemas;

        // Constructors
        public ValidationConfig() {}

        public ValidationConfig(String finalSchema, Map<String, String> intermediateSchemas) {
            this.finalSchema = finalSchema;
            this.intermediateSchemas = intermediateSchemas;
        }

        // Getters and Setters
        public String getFinalSchema() {
            return finalSchema;
        }

        public void setFinalSchema(String finalSchema) {
            this.finalSchema = finalSchema;
        }

        public Map<String, String> getIntermediateSchemas() {
            return intermediateSchemas;
        }

        public void setIntermediateSchemas(Map<String, String> intermediateSchemas) {
            this.intermediateSchemas = intermediateSchemas;
        }
    }
}