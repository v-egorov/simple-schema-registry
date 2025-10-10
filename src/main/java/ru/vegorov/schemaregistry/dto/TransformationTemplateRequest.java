package ru.vegorov.schemaregistry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransformationTemplateRequest {

    @NotBlank
    private String engine = "jslt";

    // For JSLT engine - simple expression
    private String expression;

    // For router/pipeline engines - configuration object
    private RouterConfiguration routerConfig;
    private PipelineConfiguration pipelineConfig;

    private String description;

    // Constructors
    public TransformationTemplateRequest() {}

    public TransformationTemplateRequest(String engine, String expression, String description) {
        this.engine = engine;
        this.expression = expression;
        this.description = description;
    }

    public TransformationTemplateRequest(String engine, RouterConfiguration routerConfig, String description) {
        this.engine = engine;
        this.routerConfig = routerConfig;
        this.description = description;
    }

    public TransformationTemplateRequest(String engine, PipelineConfiguration pipelineConfig, String description) {
        this.engine = engine;
        this.pipelineConfig = pipelineConfig;
        this.description = description;
    }

    // Getters and Setters
    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
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