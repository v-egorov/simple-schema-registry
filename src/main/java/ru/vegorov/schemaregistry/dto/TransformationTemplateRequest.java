package ru.vegorov.schemaregistry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransformationTemplateRequest {

    @NotBlank
    private String version;

    @NotBlank
    private String engine = "jslt";

    @NotNull
    private Long inputSchemaId;

    @NotNull
    private Long outputSchemaId;

    // For JSLT engine - simple expression
    // For router/pipeline engines, this is derived from configuration
    private String expression;

    // For router/pipeline engines - configuration object
    @Valid
    private RouterConfiguration routerConfig;
    @Valid
    private PipelineConfiguration pipelineConfig;

    private String description;

    // Constructors
    public TransformationTemplateRequest() {}

    public TransformationTemplateRequest(String version, String engine, Long inputSchemaId, Long outputSchemaId,
                                       String expression, String description) {
        this.version = version;
        this.engine = engine;
        this.inputSchemaId = inputSchemaId;
        this.outputSchemaId = outputSchemaId;
        this.expression = expression;
        this.description = description;
    }

    public TransformationTemplateRequest(String version, String engine, Long inputSchemaId, Long outputSchemaId,
                                       RouterConfiguration routerConfig, String description) {
        this.version = version;
        this.engine = engine;
        this.inputSchemaId = inputSchemaId;
        this.outputSchemaId = outputSchemaId;
        this.routerConfig = routerConfig;
        this.description = description;
    }

    public TransformationTemplateRequest(String version, String engine, Long inputSchemaId, Long outputSchemaId,
                                       PipelineConfiguration pipelineConfig, String description) {
        this.version = version;
        this.engine = engine;
        this.inputSchemaId = inputSchemaId;
        this.outputSchemaId = outputSchemaId;
        this.pipelineConfig = pipelineConfig;
        this.description = description;
    }

    // Getters and Setters
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public Long getInputSchemaId() {
        return inputSchemaId;
    }

    public void setInputSchemaId(Long inputSchemaId) {
        this.inputSchemaId = inputSchemaId;
    }

    public Long getOutputSchemaId() {
        return outputSchemaId;
    }

    public void setOutputSchemaId(Long outputSchemaId) {
        this.outputSchemaId = outputSchemaId;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public RouterConfiguration getRouterConfig() {
        return routerConfig;
    }

    public void setRouterConfig(RouterConfiguration routerConfig) {
        this.routerConfig = routerConfig;
    }

    public PipelineConfiguration getPipelineConfig() {
        return pipelineConfig;
    }

    public void setPipelineConfig(PipelineConfiguration pipelineConfig) {
        this.pipelineConfig = pipelineConfig;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}