package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransformationTemplateRequest {

    @NotBlank
    private String engine = "jslt";

    @NotNull
    private String expression;

    private String description;

    // Constructors
    public TransformationTemplateRequest() {}

    public TransformationTemplateRequest(String engine, String expression, String description) {
        this.engine = engine;
        this.expression = expression;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}