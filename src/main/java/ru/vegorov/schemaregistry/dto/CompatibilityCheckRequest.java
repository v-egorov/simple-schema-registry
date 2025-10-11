package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class CompatibilityCheckRequest {

    @NotNull
    private Map<String, Object> schema;

    // Constructors
    public CompatibilityCheckRequest() {}

    public CompatibilityCheckRequest(Map<String, Object> schema) {
        this.schema = schema;
    }

    // Getters and Setters
    public Map<String, Object> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }
}